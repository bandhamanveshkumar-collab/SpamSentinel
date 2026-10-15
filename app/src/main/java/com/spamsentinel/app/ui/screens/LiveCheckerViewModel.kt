package com.spamsentinel.app.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spamsentinel.app.data.db.MessageEntity
import com.spamsentinel.app.data.repo.SpamRepository
import com.spamsentinel.app.domain.agents.DecisionAgent
import com.spamsentinel.app.domain.ml.LinearSpamModel
import com.spamsentinel.app.domain.model.ScamType
import com.spamsentinel.app.domain.model.SpamVerdict
import com.spamsentinel.app.domain.model.VerdictAction
import com.spamsentinel.app.receiver.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Random

data class LiveCheckerUiState(
    val senderInput: String = "AX-HDFCBK",
    val textInput: String = "",
    val verdict: SpamVerdict? = null,
    val isEvaluating: Boolean = false,
    val demoSeeded: Boolean = false
)

data class SampleSms(
    val title: String,
    val sender: String,
    val text: String,
    val category: String
)

class LiveCheckerViewModel(
    private val repository: SpamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveCheckerUiState())
    val uiState: StateFlow<LiveCheckerUiState> = _uiState

    private var evaluateJob: Job? = null

    val sampleSmsList = listOf(
        SampleSms("KYC Phishing", "AX-HDFCBK", "Urgent: Your HDFC Bank account will be suspended today. Update PAN immediately at http://hdfc-bank-kyc.xyz", "Scam"),
        SampleSms("Lottery Win", "VM-PRIZE", "WINNER! You have won £900,000 cash reward in UK Lottery. Call 08000215888 now to claim your prize!", "Scam"),
        SampleSms("Electricity Bill", "BZ-POWER", "Dear Customer, Electricity power disconnection notice for tonight 9:30 PM due to unpaid bill. Call officer immediately.", "Scam"),
        SampleSms("Part-time Job", "9876543210", "Part-time job offer! Earn Rs 5000/day by reviewing YouTube videos. Contact HR on WhatsApp now.", "Scam"),
        SampleSms("Bank OTP", "AX-HDFCBK", "Your OTP for netbanking transaction of Rs 5,000 is 482910. Valid for 10 mins. Do not share with anyone.", "Safe"),
        SampleSms("Friend Chat", "+919876543210", "Hey mate, are we still meeting for lunch today at 1 PM near office?", "Safe"),
        SampleSms("Meeting Confirm", "HR-DEPT", "Your interview is scheduled for tomorrow at 11 AM at Bangalore HQ.", "Safe"),
        SampleSms("Delivery Update", "IN-POST", "Your parcel #IN84920 has been dispatched and will be delivered by today 6 PM.", "Safe")
    )

    fun onSenderChanged(newSender: String) {
        _uiState.value = _uiState.value.copy(senderInput = newSender)
        triggerEvaluation()
    }

    fun onTextChanged(newText: String) {
        _uiState.value = _uiState.value.copy(textInput = newText)
        triggerEvaluation()
    }

    fun loadSample(sample: SampleSms) {
        _uiState.value = _uiState.value.copy(
            senderInput = sample.sender,
            textInput = sample.text
        )
        triggerEvaluation()
    }

    private fun triggerEvaluation() {
        evaluateJob?.cancel()
        val text = _uiState.value.textInput
        val sender = _uiState.value.senderInput

        if (text.isBlank()) {
            _uiState.value = _uiState.value.copy(verdict = null, isEvaluating = false)
            return
        }

        _uiState.value = _uiState.value.copy(isEvaluating = true)

        evaluateJob = viewModelScope.launch {
            delay(150) // 150 ms debounce
            val model = LinearSpamModel.getInstance()
            val tauLow = repository.preferences.tauLow.first()
            val tauHigh = repository.preferences.tauHigh.first()
            val strictMode = repository.preferences.strictMode.first()

            val verdict = DecisionAgent.decide(
                sender = sender,
                messageBody = text,
                model = model,
                tauLow = tauLow,
                tauHigh = tauHigh,
                strictMode = strictMode
            )

            _uiState.value = _uiState.value.copy(
                verdict = verdict,
                isEvaluating = false
            )
        }
    }

    fun simulateIncomingSms(context: Context) {
        val text = _uiState.value.textInput
        val sender = _uiState.value.senderInput
        if (text.isBlank()) return

        viewModelScope.launch {
            val model = LinearSpamModel.getInstance()
            model.init(context)

            val tauLow = repository.preferences.tauLow.first()
            val tauHigh = repository.preferences.tauHigh.first()
            val strictMode = repository.preferences.strictMode.first()

            val verdict = DecisionAgent.decide(
                sender = sender,
                messageBody = text,
                model = model,
                tauLow = tauLow,
                tauHigh = tauHigh,
                strictMode = strictMode
            )

            val entity = MessageEntity(
                sender = sender,
                senderType = "MOBILE_10",
                body = text,
                timestamp = System.currentTimeMillis(),
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
                body = text,
                verdict = verdict
            )
        }
    }

    fun seedDemoData() {
        viewModelScope.launch {
            val rand = Random(42)
            val now = System.currentTimeMillis()
            val dayMs = 24 * 60 * 60 * 1000L

            val demoEntities = ArrayList<MessageEntity>()

            sampleSmsList.forEachIndexed { idx, sample ->
                val time = now - (idx * 2 * dayMs) - rand.nextInt(3600000)
                val action = if (sample.category == "Scam") VerdictAction.BLOCK else VerdictAction.ALLOW
                val prob = if (action == VerdictAction.BLOCK) 0.94f else 0.05f

                demoEntities.add(
                    MessageEntity(
                        sender = sample.sender,
                        senderType = if (sample.sender.contains("-")) "ALPHANUMERIC" else "MOBILE_10",
                        body = sample.text,
                        timestamp = time,
                        spamProbability = prob,
                        confidence = 0.95f,
                        action = action,
                        topTermsJson = "[]",
                        signalsJson = "{}",
                        scamType = if (action == VerdictAction.BLOCK) ScamType.PHISHING_KYC else ScamType.NONE,
                        modelVersion = 1,
                        latencyMs = 7L
                    )
                )
            }

            repository.insertMessages(demoEntities)
            _uiState.value = _uiState.value.copy(demoSeeded = true)
        }
    }
}
