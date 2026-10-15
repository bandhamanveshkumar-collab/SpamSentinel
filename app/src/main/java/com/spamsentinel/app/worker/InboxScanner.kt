package com.spamsentinel.app.worker

import android.content.Context
import android.provider.Telephony
import com.spamsentinel.app.data.db.MessageEntity
import com.spamsentinel.app.data.repo.SpamRepository
import com.spamsentinel.app.domain.agents.DecisionAgent
import com.spamsentinel.app.domain.ml.LinearSpamModel
import com.spamsentinel.app.domain.signals.SenderAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class InboxScanProgress(
    val totalCount: Int = 0,
    val scannedCount: Int = 0,
    val isComplete: Boolean = false,
    val errorMessage: String? = null
)

class InboxScanner(
    private val context: Context,
    private val repository: SpamRepository
) {

    fun scanInbox(): Flow<InboxScanProgress> = flow {
        val contentResolver = context.contentResolver
        val cursor = try {
            contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.Inbox.ADDRESS, Telephony.Sms.Inbox.BODY, Telephony.Sms.Inbox.DATE),
                null,
                null,
                "${Telephony.Sms.Inbox.DATE} DESC"
            )
        } catch (e: Exception) {
            emit(InboxScanProgress(errorMessage = "Permission denied or SMS access unavailable"))
            return@flow
        }

        if (cursor == null) {
            emit(InboxScanProgress(errorMessage = "SMS Cursor returned null"))
            return@flow
        }

        val total = cursor.count
        emit(InboxScanProgress(totalCount = total, scannedCount = 0))

        if (total == 0) {
            cursor.close()
            emit(InboxScanProgress(totalCount = 0, scannedCount = 0, isComplete = true))
            return@flow
        }

        val model = LinearSpamModel.getInstance()
        model.init(context)

        val tauLow = repository.preferences.tauLow.first()
        val tauHigh = repository.preferences.tauHigh.first()
        val strictMode = repository.preferences.strictMode.first()

        val addressIdx = cursor.getColumnIndex(Telephony.Sms.Inbox.ADDRESS)
        val bodyIdx = cursor.getColumnIndex(Telephony.Sms.Inbox.BODY)
        val dateIdx = cursor.getColumnIndex(Telephony.Sms.Inbox.DATE)

        var scanned = 0
        val batch = ArrayList<MessageEntity>()

        while (cursor.moveToNext()) {
            val sender = cursor.getString(addressIdx) ?: "Unknown"
            val body = cursor.getString(bodyIdx) ?: ""
            val timestamp = cursor.getLong(dateIdx)

            if (body.isBlank()) {
                scanned++
                continue
            }

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

            batch.add(entity)
            scanned++

            if (batch.size >= 50 || scanned == total) {
                repository.insertMessages(batch)
                batch.clear()
                emit(InboxScanProgress(totalCount = total, scannedCount = scanned))
            }
        }

        cursor.close()
        emit(InboxScanProgress(totalCount = total, scannedCount = scanned, isComplete = true))
    }.flowOn(Dispatchers.IO)
}
