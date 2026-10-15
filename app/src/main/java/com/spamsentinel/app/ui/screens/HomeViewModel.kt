package com.spamsentinel.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spamsentinel.app.data.db.MessageEntity
import com.spamsentinel.app.data.repo.SpamRepository
import com.spamsentinel.app.domain.model.ScamType
import com.spamsentinel.app.domain.model.VerdictAction
import com.spamsentinel.app.ui.components.DonutSlice
import com.spamsentinel.app.ui.components.HorizontalBarItem
import com.spamsentinel.app.ui.theme.ReviewAmber
import com.spamsentinel.app.ui.theme.SafeGreen
import com.spamsentinel.app.ui.theme.SpamRed
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val protectionActive: Boolean = true,
    val protectionScore: Int = 98,
    val scannedCount: Int = 0,
    val blockedCount: Int = 0,
    val reviewCount: Int = 0,
    val safeCount: Int = 0,
    val avgLatencyMs: Long = 8L,
    val donutSlices: List<DonutSlice> = emptyList(),
    val weeklyTrendPoints: List<Float> = listOf(2f, 5f, 3f, 8f, 12f, 6f, 4f),
    val hourlyDistribution: List<Float> = listOf(1f, 0f, 0f, 2f, 5f, 12f, 8f, 15f, 20f, 18f, 10f, 6f),
    val topScamTypes: List<HorizontalBarItem> = emptyList(),
    val recentDetections: List<MessageEntity> = emptyList()
)

class HomeViewModel(
    private val repository: SpamRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.preferences.protectionActive,
        repository.totalCount,
        repository.blockedCount,
        repository.reviewCount,
        repository.allowCount,
        repository.avgLatency,
        repository.allMessages
    ) { flows ->
        val active = flows[0] as Boolean
        val total = flows[1] as Int
        val blocked = flows[2] as Int
        val review = flows[3] as Int
        val allow = flows[4] as Int
        val avgLat = flows[5] as? Double
        @Suppress("UNCHECKED_CAST")
        val messages = flows[6] as List<MessageEntity>

        val score = calculateProtectionScore(review, blocked, total)

        val donutSlices = listOf(
            DonutSlice(blocked.toFloat(), SpamRed, "Blocked"),
            DonutSlice(review.toFloat(), ReviewAmber, "Review"),
            DonutSlice(allow.toFloat(), SafeGreen, "Safe")
        )

        val scamTypeCounts = messages
            .filter { it.action == VerdictAction.BLOCK }
            .groupBy { it.scamType }
            .mapValues { it.value.size }

        val horizontalItems = scamTypeCounts.entries
            .filter { it.key != ScamType.NONE }
            .sortedByDescending { it.value }
            .take(5)
            .map { HorizontalBarItem(it.key.displayName, it.value.toFloat(), SpamRed) }

        val recent = messages.take(10)

        HomeUiState(
            protectionActive = active,
            protectionScore = score,
            scannedCount = total,
            blockedCount = blocked,
            reviewCount = review,
            safeCount = allow,
            avgLatencyMs = avgLat?.toLong() ?: 8L,
            donutSlices = donutSlices,
            topScamTypes = horizontalItems,
            recentDetections = recent
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun setProtectionActive(active: Boolean) {
        viewModelScope.launch {
            repository.preferences.setProtectionActive(active)
        }
    }

    fun markAsSafe(messageId: Long) {
        viewModelScope.launch {
            val message = repository.messageDao.getMessageById(messageId) ?: return@launch
            repository.updateMessageLabel(messageId, VerdictAction.ALLOW, VerdictAction.ALLOW)
            repository.recordFeedback(
                messageId = messageId,
                sender = message.sender,
                body = message.body,
                originalProbability = message.spamProbability,
                correctedLabel = VerdictAction.ALLOW
            )
            repository.addSenderRule(message.sender, VerdictAction.ALLOW, "Marked safe by user")
        }
    }

    fun blockSender(messageId: Long) {
        viewModelScope.launch {
            val message = repository.messageDao.getMessageById(messageId) ?: return@launch
            repository.updateMessageLabel(messageId, VerdictAction.BLOCK, VerdictAction.BLOCK)
            repository.recordFeedback(
                messageId = messageId,
                sender = message.sender,
                body = message.body,
                originalProbability = message.spamProbability,
                correctedLabel = VerdictAction.BLOCK
            )
            repository.addSenderRule(message.sender, VerdictAction.BLOCK, "Blocked sender by user")
        }
    }

    private fun calculateProtectionScore(reviewCount: Int, blockedCount: Int, totalCount: Int): Int {
        if (totalCount == 0) return 100
        val reviewPenalty = (reviewCount * 2).coerceAtMost(20)
        return (100 - reviewPenalty).coerceIn(60, 100)
    }
}
