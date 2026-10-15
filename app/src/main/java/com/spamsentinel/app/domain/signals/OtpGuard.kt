package com.spamsentinel.app.domain.signals

data class OtpAnalysisResult(
    val isOtpMessage: Boolean,
    val isRiskyOtp: Boolean,
    val otpWarning: String? = null
)

object OtpGuard {

    private val otpKeywords = listOf("otp", "one time password", "verification code", "verify code", "security code", "debited", "credited", "bank alert")
    private val codePattern = Regex("""\b\d{4,8}\b""")

    fun analyze(messageBody: String, hasLink: Boolean): OtpAnalysisResult {
        val lower = messageBody.lowercase()
        val containsOtpWord = otpKeywords.any { lower.contains(it) }
        val containsCode = codePattern.containsMatchIn(messageBody)

        val isOtp = containsOtpWord && containsCode
        if (!isOtp) {
            return OtpAnalysisResult(isOtpMessage = false, isRiskyOtp = false)
        }

        val asksToShare = lower.contains("share") || lower.contains("do not share") || lower.contains("don't share") || lower.contains("never share")
        val isRisky = hasLink || lower.contains("share code") || lower.contains("send this otp")

        val warning = when {
            hasLink -> "WARNING: OTP message contains link. Never tap unknown links!"
            asksToShare -> "Never share your OTP or verification code with anyone!"
            else -> "Valid transaction / verification OTP."
        }

        return OtpAnalysisResult(
            isOtpMessage = true,
            isRiskyOtp = isRisky,
            otpWarning = warning
        )
    }
}
