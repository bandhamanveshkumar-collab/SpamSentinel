package com.spamsentinel.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Telephony
import com.spamsentinel.app.SpamSentinelApp
import com.spamsentinel.app.data.db.MessageEntity
import com.spamsentinel.app.domain.agents.DecisionAgent
import com.spamsentinel.app.domain.ml.LinearSpamModel
import com.spamsentinel.app.domain.signals.SenderAnalyzer
import com.spamsentinel.app.receiver.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SmsObserverService : Service() {

    private var contentObserver: ContentObserver? = null
    private var lastProcessedSmsId: Long = -1L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        registerSmsObserver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun registerSmsObserver() {
        if (contentObserver != null) return

        contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                checkLatestSmsMessage(applicationContext)
            }
        }

        try {
            contentResolver.registerContentObserver(
                Telephony.Sms.CONTENT_URI,
                true,
                contentObserver!!
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkLatestSmsMessage(context: Context) {
        val app = context.applicationContext as? SpamSentinelApp ?: return
        val repository = app.container.repository
        val model = LinearSpamModel.getInstance()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                model.init(context)

                val cursor = contentResolver.query(
                    Telephony.Sms.Inbox.CONTENT_URI,
                    arrayOf(Telephony.Sms.Inbox._ID, Telephony.Sms.Inbox.ADDRESS, Telephony.Sms.Inbox.BODY, Telephony.Sms.Inbox.DATE),
                    null,
                    null,
                    "${Telephony.Sms.Inbox.DATE} DESC LIMIT 1"
                ) ?: return@launch

                if (cursor.moveToFirst()) {
                    val idIdx = cursor.getColumnIndex(Telephony.Sms.Inbox._ID)
                    val addressIdx = cursor.getColumnIndex(Telephony.Sms.Inbox.ADDRESS)
                    val bodyIdx = cursor.getColumnIndex(Telephony.Sms.Inbox.BODY)
                    val dateIdx = cursor.getColumnIndex(Telephony.Sms.Inbox.DATE)

                    val smsId = if (idIdx >= 0) cursor.getLong(idIdx) else System.currentTimeMillis()
                    if (smsId == lastProcessedSmsId) {
                        cursor.close()
                        return@launch
                    }
                    lastProcessedSmsId = smsId

                    val sender = if (addressIdx >= 0) cursor.getString(addressIdx) ?: "Unknown" else "Unknown"
                    val body = if (bodyIdx >= 0) cursor.getString(bodyIdx) ?: "" else ""
                    val timestamp = if (dateIdx >= 0) cursor.getLong(dateIdx) else System.currentTimeMillis()

                    cursor.close()

                    if (body.isBlank()) return@launch

                    val protectionActive = repository.preferences.protectionActive.first()
                    if (!protectionActive) return@launch

                    val tauLow = repository.preferences.tauLow.first()
                    val tauHigh = repository.preferences.tauHigh.first()
                    val strictMode = repository.preferences.strictMode.first()

                    val rule = repository.senderRuleDao.getRuleForSender(sender)
                    val verdict = DecisionAgent.decide(
                        sender = sender,
                        messageBody = body,
                        model = model,
                        tauLow = tauLow,
                        tauHigh = tauHigh,
                        strictMode = strictMode,
                        senderRule = rule?.ruleType
                    )

                    val senderAnalysis = SenderAnalyzer.analyze(sender, body)

                    val entity = MessageEntity(
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

                    val insertedId = repository.insertMessage(entity)

                    NotificationHelper.showVerdictNotification(
                        context = context,
                        messageId = insertedId,
                        sender = sender,
                        body = body,
                        verdict = verdict
                    )
                } else {
                    cursor.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        contentObserver?.let {
            contentResolver.unregisterContentObserver(it)
        }
    }
}
