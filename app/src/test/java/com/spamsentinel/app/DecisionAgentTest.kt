package com.spamsentinel.app

import com.spamsentinel.app.domain.agents.DecisionAgent
import com.spamsentinel.app.domain.ml.LinearSpamModel
import com.spamsentinel.app.domain.ml.ModelJson
import com.spamsentinel.app.domain.model.VerdictAction
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.InputStreamReader

class DecisionAgentTest {

    private val model = LinearSpamModel.getInstance()

    @Before
    fun setUp() {
        val modelStream = FileStreamHelper.getStream("model.json")
        val modelText = InputStreamReader(modelStream).readText()
        val modelData = Json { ignoreUnknownKeys = true }.decodeFromString<ModelJson>(modelText)
        model.loadFromData(modelData)
    }

    @Test
    fun testThresholdDecisions() {
        // High probability + strict mode / URL signal -> BLOCK
        val spamVerdict = DecisionAgent.decide(
            sender = "AX-CLAIM",
            messageBody = "WINNER! You won £900 cash reward! Claim now at http://claim.xyz or call longnum",
            model = model,
            tauLow = 0.20f,
            tauHigh = 0.70f,
            strictMode = true
        )
        assertEquals(VerdictAction.BLOCK, spamVerdict.action)

        // Low probability -> ALLOW
        val hamVerdict = DecisionAgent.decide(
            sender = "9876543210",
            messageBody = "Hey mate, see you tomorrow at work for lunch. Good night!",
            model = model,
            tauLow = 0.20f,
            tauHigh = 0.70f
        )
        assertEquals(VerdictAction.ALLOW, hamVerdict.action)
    }

    @Test
    fun testOtpSafetyGuard() {
        val otpMessage = "Your OTP for HDFC Bank transaction is 482910. Valid for 10 mins."
        val verdict = DecisionAgent.decide(
            sender = "AX-HDFCBK",
            messageBody = otpMessage,
            model = model,
            tauLow = 0.20f,
            tauHigh = 0.70f
        )

        // OTP without link from header sender must never be BLOCKED
        assert(verdict.action != VerdictAction.BLOCK)
        assert(verdict.threatSignals.isOtpMessage)
    }
}
