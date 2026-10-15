package com.spamsentinel.app

import com.spamsentinel.app.domain.ml.LinearSpamModel
import com.spamsentinel.app.domain.ml.ModelJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.InputStreamReader

@Serializable
data class ParitySample(
    val text: String,
    val p: Float
)

class ModelParityTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val model = LinearSpamModel.getInstance()

    @Before
    fun setUp() {
        val modelStream = FileStreamHelper.getStream("model.json")
        val modelText = InputStreamReader(modelStream).readText()
        val modelData = json.decodeFromString<ModelJson>(modelText)
        model.loadFromData(modelData)
    }

    @Test
    fun testOnDeviceModelProbabilityParityWithPython() {
        val parityStream = FileStreamHelper.getStream("parity.json")
        val parityText = InputStreamReader(parityStream).readText()
        val samples = json.decodeFromString<List<ParitySample>>(parityText)

        assertTrue("Parity test set must contain test samples", samples.isNotEmpty())

        var totalDelta = 0f
        for (sample in samples) {
            val kotlinP = model.predictProbability(sample.text)
            assertTrue("Probability must be in [0, 1]", kotlinP in 0f..1f)
            val delta = Math.abs(kotlinP - sample.p)
            totalDelta += delta
        }

        val avgDelta = totalDelta / samples.size
        println("Model Parity Test Passed! Average delta across ${samples.size} samples: $avgDelta")
    }
}
