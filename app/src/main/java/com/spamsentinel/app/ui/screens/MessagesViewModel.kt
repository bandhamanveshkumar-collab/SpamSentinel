package com.spamsentinel.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spamsentinel.app.data.db.MessageEntity
import com.spamsentinel.app.data.repo.SpamRepository
import com.spamsentinel.app.domain.model.ScamType
import com.spamsentinel.app.domain.model.VerdictAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MessagesTab {
    ALL,
    SPAM,
    SAFE,
    REVIEW,
    QUARANTINE
}

data class MessagesUiState(
    val selectedTab: MessagesTab = MessagesTab.ALL,
    val searchQuery: String = "",
    val scamTypeFilter: ScamType? = null,
    val privacyBlur: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val isMultiSelectMode: Boolean = false,
    val messages: List<MessageEntity> = emptyList()
)

class MessagesViewModel(
    private val repository: SpamRepository
) : ViewModel() {

    private val selectedTab = MutableStateFlow(MessagesTab.ALL)
    private val searchQuery = MutableStateFlow("")
    private val scamTypeFilter = MutableStateFlow<ScamType?>(null)
    private val selectedIds = MutableStateFlow<Set<Long>>(emptySet())

    val uiState: StateFlow<MessagesUiState> = combine(
        selectedTab,
        searchQuery,
        scamTypeFilter,
        selectedIds,
        repository.preferences.privacyBlur,
        repository.allMessages
    ) { flows ->
        val tab = flows[0] as MessagesTab
        val query = flows[1] as String
        val scamFilter = flows[2] as? ScamType
        @Suppress("UNCHECKED_CAST")
        val ids = flows[3] as Set<Long>
        val blur = flows[4] as Boolean
        @Suppress("UNCHECKED_CAST")
        val allMsgs = flows[5] as List<MessageEntity>

        val filtered = allMsgs.filter { msg ->
            val matchesTab = when (tab) {
                MessagesTab.ALL -> true
                MessagesTab.SPAM -> msg.action == VerdictAction.BLOCK
                MessagesTab.SAFE -> msg.action == VerdictAction.ALLOW
                MessagesTab.REVIEW -> msg.action == VerdictAction.REVIEW
                MessagesTab.QUARANTINE -> msg.action == VerdictAction.BLOCK
            }

            val matchesQuery = if (query.isBlank()) true else {
                msg.sender.contains(query, ignoreCase = true) || msg.body.contains(query, ignoreCase = true)
            }

            val matchesScam = if (scamFilter == null) true else msg.scamType == scamFilter

            matchesTab && matchesQuery && matchesScam
        }

        MessagesUiState(
            selectedTab = tab,
            searchQuery = query,
            scamTypeFilter = scamFilter,
            privacyBlur = blur,
            selectedIds = ids,
            isMultiSelectMode = ids.isNotEmpty(),
            messages = filtered
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MessagesUiState()
    )

    fun setTab(tab: MessagesTab) {
        selectedTab.value = tab
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setScamTypeFilter(type: ScamType?) {
        scamTypeFilter.value = type
    }

    fun toggleSelection(id: Long) {
        val current = selectedIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        selectedIds.value = current
    }

    fun clearSelection() {
        selectedIds.value = emptySet()
    }

    fun bulkMarkAsSafe() {
        val ids = selectedIds.value
        viewModelScope.launch {
            ids.forEach { id ->
                val msg = repository.messageDao.getMessageById(id) ?: return@launch
                repository.updateMessageLabel(id, VerdictAction.ALLOW, VerdictAction.ALLOW)
                repository.recordFeedback(id, msg.sender, msg.body, msg.spamProbability, VerdictAction.ALLOW)
            }
            clearSelection()
        }
    }

    fun bulkBlockSender() {
        val ids = selectedIds.value
        viewModelScope.launch {
            ids.forEach { id ->
                val msg = repository.messageDao.getMessageById(id) ?: return@launch
                repository.updateMessageLabel(id, VerdictAction.BLOCK, VerdictAction.BLOCK)
                repository.recordFeedback(id, msg.sender, msg.body, msg.spamProbability, VerdictAction.BLOCK)
                repository.addSenderRule(msg.sender, VerdictAction.BLOCK, "Bulk blocked by user")
            }
            clearSelection()
        }
    }

    fun bulkDelete() {
        val ids = selectedIds.value
        viewModelScope.launch {
            ids.forEach { repository.deleteMessage(it) }
            clearSelection()
        }
    }

    fun deleteMessage(id: Long) {
        viewModelScope.launch {
            repository.deleteMessage(id)
        }
    }
}
