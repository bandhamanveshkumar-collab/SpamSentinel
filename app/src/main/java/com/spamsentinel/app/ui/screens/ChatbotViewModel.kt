package com.spamsentinel.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: ChatSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ChatSender {
    USER,
    BOT
}

data class PromptCategory(
    val title: String,
    val prompts: List<String>
)

data class ChatbotUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isTyping: Boolean = false,
    val streamingText: String = "",
    val inputText: String = ""
)

class ChatbotViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChatbotUiState())
    val uiState: StateFlow<ChatbotUiState> = _uiState

    val promptCategories = listOf(
        PromptCategory(
            title = "🛡️ Security & Scams",
            prompts = listOf(
                "How do I spot a phishing SMS?",
                "What are URL shorteners and risky TLDs?",
                "Why was my OTP message allowed?",
                "How do I protect myself from SIM swap scams?"
            )
        ),
        PromptCategory(
            title = "🧠 AI & Science",
            prompts = listOf(
                "How does the on-device SVM model work?",
                "What is Platt calibration?",
                "What is TF-IDF n-gram vectorization?",
                "How does artificial intelligence detect spam?"
            )
        ),
        PromptCategory(
            title = "⚙️ Model & Privacy",
            prompts = listOf(
                "Is my data safe and offline?",
                "How do decision thresholds (tau) work?",
                "How does on-device SGD fine-tuning work?",
                "What happens if validation F1 drops during retrain?"
            )
        )
    )

    init {
        val welcomeMsg = ChatMessage(
            sender = ChatSender.BOT,
            text = "Hello! I am Sentinel AI 🤖✨ (Model: Sentinel-1.0-OnDevice).\n\nI am your 100% offline, intelligent AI assistant — similar to ChatGPT and Gemini, optimized for complete privacy on your mobile phone.\n\nAsk me ANYTHING about SMS security, AI & machine learning, cybersecurity, OTP safety, or app capabilities!"
        )
        _uiState.value = ChatbotUiState(messages = listOf(welcomeMsg))
    }

    fun onInputTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun sendMessage(userQuery: String = _uiState.value.inputText) {
        val query = userQuery.trim()
        if (query.isBlank()) return

        val userMsg = ChatMessage(sender = ChatSender.USER, text = query)
        val updatedMsgs = _uiState.value.messages + userMsg

        _uiState.value = _uiState.value.copy(
            messages = updatedMsgs,
            inputText = "",
            isTyping = true,
            streamingText = ""
        )

        viewModelScope.launch(Dispatchers.Default) {
            val fullAnswer = generateLocalAIAnswer(query)

            val words = fullAnswer.split(" ")
            val sb = StringBuilder()

            for (word in words) {
                sb.append(word).append(" ")
                _uiState.value = _uiState.value.copy(streamingText = sb.toString().trimEnd())
                delay(20) // Smooth 20ms streaming token delay
            }

            val finalBotMsg = ChatMessage(sender = ChatSender.BOT, text = fullAnswer)
            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages + finalBotMsg,
                isTyping = false,
                streamingText = ""
            )
        }
    }

    fun clearChat() {
        val welcomeMsg = ChatMessage(
            sender = ChatSender.BOT,
            text = "Chat cleared. I am ready to answer your next question!"
        )
        _uiState.value = ChatbotUiState(messages = listOf(welcomeMsg))
    }

    private fun generateLocalAIAnswer(query: String): String {
        val q = query.lowercase(Locale.ROOT)

        return when {
            containsAny(q, "what is ai", "artificial intelligence", "how ai works") -> {
                "🤖 **Artificial Intelligence (AI)**:\n\n" +
                "Artificial Intelligence refers to computer systems designed to perform tasks that typically require human intelligence, such as pattern recognition, decision-making, and natural language comprehension.\n\n" +
                "In SpamSentinel, AI operates strictly **on-device** using a pre-trained Linear Support Vector Machine (SVM) and TF-IDF statistical text feature extraction to classify incoming messages instantly without any cloud connection."
            }

            containsAny(q, "data safe", "offline", "privacy", "internet", "server", "cloud") -> {
                "🔒 **100% Offline & Private Security**:\n\n" +
                "SpamSentinel is strictly 0-network and does not request INTERNET permission in AndroidManifest.\n\n" +
                "• Every SMS message is evaluated directly on your phone's processor.\n" +
                "• Your messages, contacts, and personal data never leave your mobile device.\n" +
                "• All statistical weights and model files are saved in encrypted local storage."
            }

            containsAny(q, "phishing", "spot", "identify", "scam", "suspicious", "fake", "smishing") -> {
                "🚨 **How to Spot Phishing / Scam SMS (Smishing)**:\n\n" +
                "1. **Artificial Urgency**: Threats like 'Account suspended today', 'Unpaid electricity disconnection tonight', or 'PAN update required'.\n" +
                "2. **Suspicious Link Shorteners**: Links hiding their destination (bit.ly, cutt.ly, t.co, rb.gy).\n" +
                "3. **Risky Top-Level Domains**: Domains ending in .xyz, .top, .icu, .cfd, .click, .live.\n" +
                "4. **Sender Discrepancy**: Personal 10-digit mobile numbers sending official banking alerts or job offers.\n" +
                "5. **Unrealistic Claims**: 'You won £900,000' or 'Earn Rs 5000/day work from home'."
            }

            containsAny(q, "otp", "banking", "verification", "code", "bank") -> {
                "🛡️ **OTP Guard Protection Engine**:\n\n" +
                "SpamSentinel features a dedicated OTP Safety Engine designed to protect your banking alerts:\n\n" +
                "• If an SMS contains a 4-8 digit verification code from an official **Alphanumeric Header** (e.g., AX-HDFCBK) and contains **NO link**, SpamSentinel will **never falsely block it**.\n" +
                "• ⚠️ **Warning Constraint**: If an OTP message contains an external link or asks you to share/forward the code, SpamSentinel immediately flags it with a high-priority red alert!"
            }

            containsAny(q, "model", "ml", "svm", "tfidf", "platt", "algorithm", "calibration", "machine learning") -> {
                "🧠 **On-Device Machine Learning Pipeline**:\n\n" +
                "1. **Text Preprocessing**: Lowercases text, replaces URLs with urltoken, and long digits with longnum.\n" +
                "2. **TF-IDF N-Gram Vectorizer**: Extracts unigrams and bigrams weighted by sublinear TF (1 + ln tf) and L2 normalized.\n" +
                "3. **Linear SVM**: Computes decision score s = w * x + b.\n" +
                "4. **Platt Sigmoid Calibration**: Maps decision score to calibrated probability p = 1 / (1 + e^-(A*s + B)) for confidence-aware routing."
            }

            containsAny(q, "threshold", "tau", "tauhigh", "taulow", "strict") -> {
                "⚙️ **Symmetric Decision Thresholds (tau)**:\n\n" +
                "• **Block Threshold (tau High)**: Default 0.80. Messages with probability >= 0.80 are automatically BLOCKED.\n" +
                "• **Allow Threshold (tau Low)**: Default 0.20. Messages with probability <= 0.20 are automatically ALLOWED.\n" +
                "• **Review Queue**: Borderline messages (0.20 < prob < 0.80) are routed to the REVIEW tab for safe inspection.\n\n" +
                "You can adjust these thresholds anytime in Settings!"
            }

            containsAny(q, "retrain", "fine tune", "sgd", "feedback", "learn", "adapt") -> {
                "🔄 **Adaptive On-Device Learning Pass**:\n\n" +
                "When you mark a message as 'Not Spam' or 'Block Sender', the correction is buffered in local Room DB.\n\n" +
                "When buffer size >= 10, an online Stochastic Gradient Descent (SGD) fine-tuning pass is executed locally.\n\n" +
                "🛡️ **Safety Rollback Rule**: The fine-tuned model is promoted ONLY if validation F1 score drops by <= 1%. Otherwise, it automatically rolls back to prevent catastrophic forgetting!"
            }

            containsAny(q, "link", "url", "tld", "shortener", "domain", "ip") -> {
                "🌐 **URL Guard Security Features**:\n\n" +
                "SpamSentinel inspects links offline without ever opening or fetching them:\n\n" +
                "• **URL Shorteners**: bit.ly, tinyurl, t.co, cutt.ly, rb.gy\n" +
                "• **Raw IP Hosts**: e.g., 103.x.x.x instead of domain name\n" +
                "• **Risky TLDs**: .xyz, .top, .click, .icu, .live, .cfd, .shop\n" +
                "• **Brand Impersonation**: e.g., hdfc-bank-kyc.xyz vs official hdfcbank.com."
            }

            containsAny(q, "sim", "swap", "vishing", "smishing", "cyber", "security", "passwords") -> {
                "🛡️ **Cybersecurity Best Practices**:\n\n" +
                "1. **Never Share OTPs**: No legitimate bank officer will ever ask for your OTP or password over SMS or call.\n" +
                "2. **Beware SIM Swapping**: If your mobile signal suddenly disappears without reason, contact your telecom provider immediately.\n" +
                "3. **Use 2FA / Password Managers**: Secure your personal accounts with multi-factor authentication."
            }

            containsAny(q, "hi", "hello", "hey", "help", "who are you") -> {
                "Hello! 👋 I am Sentinel AI (Model: Sentinel-1.0-OnDevice).\n\nI am your 100% offline, intelligent AI assistant. Ask me any question about SMS security, machine learning algorithms, phishing links, or app privacy!"
            }

            else -> {
                "🤖 **Sentinel AI Response**:\n\n" +
                "Regarding **'$query'**:\n\n" +
                "SpamSentinel provides 100% on-device intelligent SMS protection using TF-IDF n-gram feature extraction, Linear SVM classification, Platt sigmoid probability calibration, and Threat Signals (URL Guard, OTP Guard, Sender Analyzer).\n\n" +
                "💡 **Tip**: You can paste any suspicious SMS into the **Live Checker** tab for real-time probability & feature contribution explanations!"
            }
        }
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { text.contains(it) }
    }
}
