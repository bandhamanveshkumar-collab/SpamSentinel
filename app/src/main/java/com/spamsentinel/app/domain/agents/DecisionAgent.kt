package com.spamsentinel.app.domain.agents

import com.spamsentinel.app.domain.ml.LinearSpamModel
import com.spamsentinel.app.domain.model.SpamVerdict
import com.spamsentinel.app.domain.model.ThreatSignals
import com.spamsentinel.app.domain.model.VerdictAction
import com.spamsentinel.app.domain.signals.OtpGuard
import com.spamsentinel.app.domain.signals.ScamTypeTagger
import com.spamsentinel.app.domain.signals.SenderAnalyzer
import com.spamsentinel.app.domain.signals.SenderType
import com.spamsentinel.app.domain.signals.UrlAnalyzer
import kotlin.math.max

object DecisionAgent {

    fun decide(
        sender: String,
        messageBody: String,
        model: LinearSpamModel,
        tauLow: Float = 0.20f,
        tauHigh: Float = 0.80f,
        strictMode: Boolean = false,
        senderRule: VerdictAction? = null
    ): SpamVerdict {
        val startTime = System.currentTimeMillis()

        // Explicit Sender Rule override
        if (senderRule != null) {
            val probability = if (senderRule == VerdictAction.BLOCK) 0.99f else 0.01f
            return SpamVerdict(
                action = senderRule,
                spamProbability = probability,
                confidence = 0.99f,
                topSpamTerms = emptyList(),
                topHamTerms = emptyList(),
                threatSignals = ThreatSignals(),
                scamType = ScamTypeTagger.tag(messageBody),
                latencyMs = System.currentTimeMillis() - startTime
            )
        }

        // ML Probability
        val probability = model.predictProbability(messageBody)
        val confidence = max(probability, 1f - probability)

        // Explainer Agent
        val (topSpam, topHam) = Explainer.explain(messageBody, model)

        // Threat Signals Layer
        val senderAnalysis = SenderAnalyzer.analyze(sender, messageBody)
        val urlAnalysis = UrlAnalyzer.analyze(messageBody)
        val otpAnalysis = OtpGuard.analyze(messageBody, urlAnalysis.hasUrl)
        val scamType = ScamTypeTagger.tag(messageBody)

        val threatSignals = ThreatSignals(
            isHeaderSender = senderAnalysis.senderType == SenderType.ALPHANUMERIC,
            isPersonalNumberPromo = senderAnalysis.isPersonalNumberPromo,
            isOtpMessage = otpAnalysis.isOtpMessage,
            hasRiskyUrl = urlAnalysis.hasRiskyUrl,
            flaggedUrlHost = urlAnalysis.host,
            urlWarning = urlAnalysis.warning,
            isNonAscii = isNonAsciiMessage(messageBody)
        )

        // Initial Threshold Action (Paper Section 3)
        var action = when {
            probability >= tauHigh -> VerdictAction.BLOCK
            probability <= tauLow -> VerdictAction.ALLOW
            else -> VerdictAction.REVIEW
        }

        // Strict Mode Enhancement
        if (strictMode && action == VerdictAction.REVIEW) {
            if (threatSignals.hasRiskyUrl || threatSignals.isPersonalNumberPromo) {
                action = VerdictAction.BLOCK
            }
        }

        // OTP Guard Override (Safety Rule: Never block legitimate bank OTP without link)
        if (threatSignals.isOtpMessage && threatSignals.isHeaderSender && !urlAnalysis.hasUrl) {
            if (action == VerdictAction.BLOCK) {
                action = VerdictAction.REVIEW
            }
        }

        val latency = System.currentTimeMillis() - startTime

        return SpamVerdict(
            action = action,
            spamProbability = probability,
            confidence = confidence,
            topSpamTerms = topSpam,
            topHamTerms = topHam,
            threatSignals = threatSignals,
            scamType = scamType,
            latencyMs = latency
        )
    }

    private fun isNonAsciiMessage(text: String): Boolean {
        if (text.isEmpty()) return false
        var nonAsciiCount = 0
        for (ch in text) {
            if (ch.code > 127) nonAsciiCount++
        }
        return (nonAsciiCount.toFloat() / text.length) > 0.40f
    }
}
