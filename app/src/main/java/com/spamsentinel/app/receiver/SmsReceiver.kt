package com.spamsentinel.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.spamsentinel.app.SpamSentinelApp
import com.spamsentinel.app.data.db.MessageEntity
import com.spamsentinel.app.domain.agents.DecisionAgent
import com.spamsentinel.app.domain.ml.LinearSpamModel
import com.spamsentinel.app.domain.signals.SenderAnalyzer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val app = context.applicationContext as? SpamSentinelApp ?: return
        val repository = app.container.repository
        val model = LinearSpamModel.getInstance()

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.Default).launch {
            try {
                // Ensure model is ready
                model.init(context)

                val smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                if (smsMessages.isNullOrEmpty()) return@launch

                // Merge multipart SMS messages grouped by sender
                val senderBodyMap = HashMap<String, StringBuilder>()
                var timestamp = System.currentTimeMillis()

                for (sms in smsMessages) {
                    val sender = sms.displayOriginatingAddress ?: sms.originatingAddress ?: "Unknown"
                    timestamp = sms.timestampMillis
                    val bodyPart = sms.messageBody ?: ""

                    senderBodyMap.getOrPut(sender) { StringBuilder() }.append(bodyPart)
                }

                val protectionActive = repository.preferences.protectionActive.first()
                val tauLow = repository.preferences.tauLow.first()
                val tauHigh = repository.preferences.tauHigh.first()
                val strictMode = repository.preferences.strictMode.first()

                for ((sender, bodyBuilder) in senderBodyMap) {
                    val body = bodyBuilder.toString()
                    if (body.isBlank()) continue

                    // Check sender rule
                    val rule = repository.senderRuleDao.getRuleForSender(sender)
                    val senderRuleAction = rule?.ruleType

                    val verdict = DecisionAgent.decide(
                        sender = sender,
                        messageBody = body,
                        model = model,
                        tauLow = tauLow,
                        tauHigh = tauHigh,
                        strictMode = strictMode,
                        senderRule = senderRuleAction
                    )

                    val senderAnalysis = SenderAnalyzer.analyze(sender, body)

                    val messageEntity = MessageEntity(
                        sender = sender,
                        senderType = senderAnalysis.senderType.name,
                        body = body,
                        timestamp = timestamp,
                        spamProbability = verdict.spamProbability,
                        confidence = verdict.confidence,
                        action = verdict.action,
                        topTermsJson = Json.encodeToString(verdict.topSpamTerms),
                        signalsJson = Json.encodeToString(verdict.threatSignals),
                        scamType = verdict.scamType,
                        modelVersion = model.version,
                        latencyMs = verdict.latencyMs
                    )

                    val insertedId = repository.insertMessage(messageEntity)

                    if (protectionActive) {
                        NotificationHelper.showVerdictNotification(
                            context = context,
                            messageId = insertedId,
                            sender = sender,
                            body = body,
                            verdict = verdict
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
