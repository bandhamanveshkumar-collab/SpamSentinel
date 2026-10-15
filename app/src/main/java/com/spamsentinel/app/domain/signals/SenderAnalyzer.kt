package com.spamsentinel.app.domain.signals

enum class SenderType {
    ALPHANUMERIC,
    MOBILE_10,
    SHORT_CODE,
    INTERNATIONAL
}

data class SenderAnalysisResult(
    val senderType: SenderType,
    val isPersonalNumberPromo: Boolean
)

object SenderAnalyzer {

    private val headerRegex = Regex("""^[A-Za-z]{2}-[A-Za-z0-9]{3,8}$""")
    private val shortCodeRegex = Regex("""^\d{5,6}$""")
    private val mobile10Regex = Regex("""^(\+91)?[6-9]\d{9}$""")

    fun analyze(sender: String, messageBody: String): SenderAnalysisResult {
        val s = sender.trim()
        val type = when {
            headerRegex.matches(s) -> SenderType.ALPHANUMERIC
            shortCodeRegex.matches(s) -> SenderType.SHORT_CODE
            mobile10Regex.matches(s) -> SenderType.MOBILE_10
            s.startsWith("+") && !s.startsWith("+91") -> SenderType.INTERNATIONAL
            s.length <= 11 && s.all { it.isLetterOrDigit() || it == '-' } -> SenderType.ALPHANUMERIC
            else -> SenderType.MOBILE_10
        }

        val isPromo = type == SenderType.MOBILE_10 && containsPromoKeywords(messageBody)
        return SenderAnalysisResult(senderType = type, isPersonalNumberPromo = isPromo)
    }

    private fun containsPromoKeywords(text: String): Boolean {
        val lower = text.lowercase()
        val promoTerms = listOf("offer", "discount", "flat", "sale", "hurry", "buy now", "limited time", "cashback", "click link", "loan", "earn", "whatsapp")
        return promoTerms.any { lower.contains(it) }
    }
}
