package com.spamsentinel.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spamsentinel.app.data.db.MessageEntity
import com.spamsentinel.app.data.repo.SpamRepository
import com.spamsentinel.app.domain.agents.DecisionAgent
import com.spamsentinel.app.domain.ml.LinearSpamModel
import com.spamsentinel.app.domain.model.SpamVerdict
import com.spamsentinel.app.domain.model.VerdictAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class MessageDetailUiState(
    val message: MessageEntity? = null,
    val verdict: SpamVerdict? = null,
    val isLoading: Boolean = true
)

class MessageDetailViewModel(
    private val repository: SpamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessageDetailUiState())
    val uiState: StateFlow<MessageDetailUiState> = _uiState

    fun loadMessage(id: Long) {
        viewModelScope.launch {
            _uiState.value = MessageDetailUiState(isLoading = true)
            val msg = repository.messageDao.getMessageById(id)
            if (msg == null) {
                _uiState.value = MessageDetailUiState(isLoading = false)
                return@launch
            }

            val model = LinearSpamModel.getInstance()
            val tauLow = repository.preferences.tauLow.first()
            val tauHigh = repository.preferences.tauHigh.first()
            val strictMode = repository.preferences.strictMode.first()

            val verdict = DecisionAgent.decide(
                sender = msg.sender,
                messageBody = msg.body,
                model = model,
                tauLow = tauLow,
                tauHigh = tauHigh,
                strictMode = strictMode
            )

            _uiState.value = MessageDetailUiState(
                message = msg,
                verdict = verdict,
                isLoading = false
            )
        }
    }

    fun markAsSafe() {
        val msg = _uiState.value.message ?: return
        viewModelScope.launch {
            repository.updateMessageLabel(msg.id, VerdictAction.ALLOW, VerdictAction.ALLOW)
            repository.recordFeedback(msg.id, msg.sender, msg.body, msg.spamProbability, VerdictAction.ALLOW)
            repository.addSenderRule(msg.sender, VerdictAction.ALLOW, "Marked safe in detail screen")
            loadMessage(msg.id)
        }
    }

    fun markAsSpam() {
        val msg = _uiState.value.message ?: return
        viewModelScope.launch {
            repository.updateMessageLabel(msg.id, VerdictAction.BLOCK, VerdictAction.BLOCK)
            repository.recordFeedback(msg.id, msg.sender, msg.body, msg.spamProbability, VerdictAction.BLOCK)
            repository.addSenderRule(msg.sender, VerdictAction.BLOCK, "Marked spam in detail screen")
            loadMessage(msg.id)
        }
    }
}
