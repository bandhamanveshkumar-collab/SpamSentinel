package com.spamsentinel.app.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spamsentinel.app.data.db.AuditLogEntity
import com.spamsentinel.app.data.db.SenderRuleEntity
import com.spamsentinel.app.data.repo.SpamRepository
import com.spamsentinel.app.domain.agents.FeedbackAgent
import com.spamsentinel.app.domain.ml.LinearSpamModel
import com.spamsentinel.app.domain.model.ParitySample
import com.spamsentinel.app.domain.model.VerdictAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStreamReader

@Serializable
data class ParityCheckResult(
    val isPassed: Boolean = false,
    val maxErrorDelta: Float = 0f,
    val sampleCount: Int = 0
)

data class SettingsUiState(
    val protectionActive: Boolean = true,
    val tauLow: Float = 0.20f,
    val tauHigh: Float = 0.80f,
    val strictMode: Boolean = false,
    val bMin: Int = 10,
    val retrainOnCharging: Boolean = true,
    val themeMode: String = "SYSTEM",
    val biometricEnabled: Boolean = false,
    val privacyBlur: Boolean = false,
    val trustContacts: Boolean = true,
    val weeklyDigestEnabled: Boolean = true,
    val modelVersion: Int = 1,
    val auditLogs: List<AuditLogEntity> = emptyList(),
    val senderRules: List<SenderRuleEntity> = emptyList(),
    val parityCheckResult: ParityCheckResult? = null,
    val isRetraining: Boolean = false
)

class SettingsViewModel(
    private val repository: SpamRepository
) : ViewModel() {

    private val isRetraining = MutableStateFlow(false)
    private val parityCheckResult = MutableStateFlow<ParityCheckResult?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.preferences.protectionActive,
        repository.preferences.tauLow,
        repository.preferences.tauHigh,
        repository.preferences.strictMode,
        repository.preferences.bMin,
        repository.preferences.retrainOnCharging,
        repository.preferences.themeMode,
        repository.preferences.biometricEnabled,
        repository.preferences.privacyBlur,
        repository.preferences.trustContacts,
        repository.preferences.weeklyDigestEnabled,
        repository.recentAuditLogs,
        repository.allRules,
        isRetraining,
        parityCheckResult
    ) { flows ->
        val active = flows[0] as Boolean
        val low = flows[1] as Float
        val high = flows[2] as Float
        val strict = flows[3] as Boolean
        val bmin = flows[4] as Int
        val charging = flows[5] as Boolean
        val theme = flows[6] as String
        val bio = flows[7] as Boolean
        val blur = flows[8] as Boolean
        val trust = flows[9] as Boolean
        val digest = flows[10] as Boolean
        @Suppress("UNCHECKED_CAST")
        val logs = flows[11] as List<AuditLogEntity>
        @Suppress("UNCHECKED_CAST")
        val rules = flows[12] as List<SenderRuleEntity>
        val retrain = flows[13] as Boolean
        val parity = flows[14] as? ParityCheckResult

        val model = LinearSpamModel.getInstance()

        SettingsUiState(
            protectionActive = active,
            tauLow = low,
            tauHigh = high,
            strictMode = strict,
            bMin = bmin,
            retrainOnCharging = charging,
            themeMode = theme,
            biometricEnabled = bio,
            privacyBlur = blur,
            trustContacts = trust,
            weeklyDigestEnabled = digest,
            modelVersion = model.version,
            auditLogs = logs,
            senderRules = rules,
            isRetraining = retrain,
            parityCheckResult = parity
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setProtectionActive(active: Boolean) { viewModelScope.launch { repository.preferences.setProtectionActive(active) } }
    fun setTauLow(tau: Float) { viewModelScope.launch { repository.preferences.setTauLow(tau) } }
    fun setTauHigh(tau: Float) { viewModelScope.launch { repository.preferences.setTauHigh(tau) } }
    fun setStrictMode(enabled: Boolean) { viewModelScope.launch { repository.preferences.setStrictMode(enabled) } }
    fun setBMin(bMin: Int) { viewModelScope.launch { repository.preferences.setBMin(bMin) } }
    fun setRetrainOnCharging(enabled: Boolean) { viewModelScope.launch { repository.preferences.setRetrainOnCharging(enabled) } }
    fun setThemeMode(mode: String) { viewModelScope.launch { repository.preferences.setThemeMode(mode) } }
    fun setBiometricEnabled(enabled: Boolean) { viewModelScope.launch { repository.preferences.setBiometricEnabled(enabled) } }
    fun setPrivacyBlur(enabled: Boolean) { viewModelScope.launch { repository.preferences.setPrivacyBlur(enabled) } }
    fun setTrustContacts(trust: Boolean) { viewModelScope.launch { repository.preferences.setTrustContacts(trust) } }
    fun setWeeklyDigestEnabled(enabled: Boolean) { viewModelScope.launch { repository.preferences.setWeeklyDigestEnabled(enabled) } }

    fun addRule(sender: String, ruleType: VerdictAction) {
        if (sender.isBlank()) return
        viewModelScope.launch {
            repository.addSenderRule(sender, ruleType, "Added in settings")
        }
    }

    fun removeRule(sender: String) {
        viewModelScope.launch {
            repository.removeSenderRule(sender)
        }
    }

    fun triggerRetrainNow(context: Context) {
        viewModelScope.launch {
            isRetraining.value = true
            FeedbackAgent.processFeedbackAndFineTune(context, repository)
            isRetraining.value = false
        }
    }

    fun runModelSelfTest(context: Context) {
        viewModelScope.launch {
            val json = Json { ignoreUnknownKeys = true }
            val model = LinearSpamModel.getInstance()
            model.init(context)

            val parityStream = context.assets.open("parity.json")
            val parityText = InputStreamReader(parityStream).readText()
            val samples = json.decodeFromString<List<ParitySample>>(parityText)

            var maxDiff = 0f
            for (sample in samples) {
                val kotlinP = model.predictProbability(sample.text)
                val diff = Math.abs(kotlinP - sample.p)
                if (diff > maxDiff) maxDiff = diff
            }

            parityCheckResult.value = ParityCheckResult(
                isPassed = maxDiff < 0.20f,
                maxErrorDelta = maxDiff,
                sampleCount = samples.size
            )
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.deleteAllMessages()
            repository.senderRuleDao.deleteAllRules()
            repository.auditLogDao.clearLogs()
            repository.feedbackDao.clearBuffer()
        }
    }
}
