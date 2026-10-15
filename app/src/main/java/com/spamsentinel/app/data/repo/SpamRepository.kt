package com.spamsentinel.app.data.repo

import com.spamsentinel.app.data.db.AppDatabase
import com.spamsentinel.app.data.db.AuditLogEntity
import com.spamsentinel.app.data.db.FeedbackBufferEntity
import com.spamsentinel.app.data.db.MessageEntity
import com.spamsentinel.app.data.db.ModelHistoryEntity
import com.spamsentinel.app.data.db.SenderRuleEntity
import com.spamsentinel.app.data.prefs.AppPreferences
import com.spamsentinel.app.domain.model.VerdictAction
import kotlinx.coroutines.flow.Flow

class SpamRepository(
    private val database: AppDatabase,
    val preferences: AppPreferences
) {
    val messageDao = database.messageDao()
    val feedbackDao = database.feedbackDao()
    val modelHistoryDao = database.modelHistoryDao()
    val senderRuleDao = database.senderRuleDao()
    val auditLogDao = database.auditLogDao()

    val allMessages: Flow<List<MessageEntity>> = messageDao.getAllMessages()
    val totalCount: Flow<Int> = messageDao.getCount()
    val blockedCount: Flow<Int> = messageDao.getBlockedCount()
    val reviewCount: Flow<Int> = messageDao.getReviewCount()
    val allowCount: Flow<Int> = messageDao.getAllowCount()
    val avgLatency: Flow<Double?> = messageDao.getAvgLatency()

    val allRules: Flow<List<SenderRuleEntity>> = senderRuleDao.getAllRules()
    val modelHistory: Flow<List<ModelHistoryEntity>> = modelHistoryDao.getAllHistory()
    val recentAuditLogs: Flow<List<AuditLogEntity>> = auditLogDao.getRecentLogs(50)
    val feedbackCount: Flow<Int> = feedbackDao.getFeedbackCountFlow()

    suspend fun insertMessage(message: MessageEntity): Long {
        return messageDao.insertMessage(message)
    }

    suspend fun insertMessages(messages: List<MessageEntity>) {
        messageDao.insertMessages(messages)
    }

    suspend fun updateMessageLabel(id: Long, userLabel: VerdictAction, newAction: VerdictAction) {
        messageDao.updateMessageLabel(id, userLabel, newAction)
    }

    suspend fun deleteMessage(id: Long) {
        messageDao.deleteMessage(id)
    }

    suspend fun deleteAllMessages() {
        messageDao.deleteAllMessages()
    }

    suspend fun recordFeedback(
        messageId: Long,
        sender: String,
        body: String,
        originalProbability: Float,
        correctedLabel: VerdictAction
    ): Boolean {
        // Feedback rate limit and flip-flop check
        val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000)
        val recentCount = feedbackDao.getRecentCorrectionCount(oneHourAgo)
        if (recentCount >= 30) {
            auditLogDao.insertLog(
                AuditLogEntity(
                    eventType = "CORRECTION_RATE_LIMITED",
                    description = "Correction ignored: rate limit exceeded (max 30/hr)."
                )
            )
            return false
        }

        val lastFeedback = feedbackDao.getLastFeedbackForMessage(messageId)
        if (lastFeedback != null && lastFeedback.correctedLabel == correctedLabel) {
            auditLogDao.insertLog(
                AuditLogEntity(
                    eventType = "FLIP_FLOP_IGNORED",
                    description = "Duplicate correction on message $messageId ignored."
                )
            )
            return false
        }

        feedbackDao.insertFeedback(
            FeedbackBufferEntity(
                messageId = messageId,
                sender = sender,
                body = body,
                originalProbability = originalProbability,
                correctedLabel = correctedLabel
            )
        )
        return true
    }

    suspend fun addSenderRule(sender: String, ruleType: VerdictAction, notes: String = "") {
        senderRuleDao.insertRule(
            SenderRuleEntity(sender = sender, ruleType = ruleType, notes = notes)
        )
    }

    suspend fun removeSenderRule(sender: String) {
        senderRuleDao.deleteRule(sender)
    }

    suspend fun logAuditEvent(eventType: String, description: String, detailsJson: String = "{}") {
        auditLogDao.insertLog(
            AuditLogEntity(
                eventType = eventType,
                description = description,
                detailsJson = detailsJson
            )
        )
    }
}
