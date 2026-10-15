package com.spamsentinel.app.domain.ml

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Locale
import kotlin.math.exp

@Serializable
data class ModelJson(
    val vocab: Map<String, Int> = emptyMap(),
    val idf: List<Float> = emptyList(),
    val weights: List<Float> = emptyList(),
    val bias: Float = 0f,
    val plattA: Float = 1f,
    val plattB: Float = 0f,
    val version: Int = 1
)

class LinearSpamModel private constructor() {

    @Volatile
    private var isLoaded = false

    private var vocab: Map<String, Int> = emptyMap()
    private var idf: FloatArray = FloatArray(0)
    private var weights: FloatArray = FloatArray(0)
    private var bias: Float = 0f
    private var plattA: Float = 1f
    private var plattB: Float = 0f
    var version: Int = 1
        private set

    private var vectorizer: TfIdfVectorizer? = null
    private val jsonParser = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    companion object {
        @Volatile
        private var instance: LinearSpamModel? = null

        fun getInstance(): LinearSpamModel {
            return instance ?: synchronized(this) {
                instance ?: LinearSpamModel().also { instance = it }
            }
        }
    }

    suspend fun init(context: Context) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (isLoaded) return@withLock
            try {
                // Try reading updated model file from internal storage first
                val customModelFile = File(context.filesDir, "active_model.json")
                val jsonString = if (customModelFile.exists()) {
                    customModelFile.readText()
                } else {
                    context.assets.open("model.json").bufferedReader().use { it.readText() }
                }

                val modelData = jsonParser.decodeFromString<ModelJson>(jsonString)
                loadFromData(modelData)
            } catch (e: Exception) {
                e.printStackTrace()
                // Use default fallback if assets or json parsing fails
            } finally {
                isLoaded = true
            }
        }
    }

    fun loadFromData(modelData: ModelJson) {
        vocab = modelData.vocab
        idf = modelData.idf.toFloatArray()
        weights = modelData.weights.toFloatArray()
        bias = modelData.bias
        plattA = modelData.plattA
        plattB = modelData.plattB
        version = modelData.version
        vectorizer = TfIdfVectorizer(vocab, idf)
        isLoaded = true
    }

    suspend fun awaitReady(timeoutMs: Long = 2000L): Boolean {
        return withTimeoutOrNull(timeoutMs) {
            while (!isLoaded) {
                kotlinx.coroutines.delay(10)
            }
            true
        } ?: false
    }

    fun predictProbability(text: String): Float {
        val cleaned = TextCleaner.clean(text)
        val vec = vectorizer ?: return fallbackScorer(text)

        val sparse = vec.transform(cleaned)
        if (sparse.indices.isEmpty()) {
            return fallbackScorer(text)
        }

        var decisionScore = bias.toDouble()
        for (i in sparse.indices.indices) {
            val idx = sparse.indices[i]
            if (idx in weights.indices) {
                decisionScore += weights[idx].toDouble() * sparse.values[i].toDouble()
            }
        }

        val calibratedZ = plattA.toDouble() * decisionScore + plattB.toDouble()
        val probability = 1.0 / (1.0 + exp(-calibratedZ))
        return probability.toFloat().coerceIn(0.0001f, 0.9999f)
    }

    fun decisionScore(text: String): Double {
        val cleaned = TextCleaner.clean(text)
        val vec = vectorizer ?: return 0.0
        val sparse = vec.transform(cleaned)
        var decisionScore = bias.toDouble()
        for (i in sparse.indices.indices) {
            val idx = sparse.indices[i]
            if (idx in weights.indices) {
                decisionScore += weights[idx].toDouble() * sparse.values[i].toDouble()
            }
        }
        return decisionScore
    }

    fun getTermContributions(text: String): List<Pair<String, Float>> {
        val cleaned = TextCleaner.clean(text)
        val vec = vectorizer ?: return emptyList()
        val sparse = vec.transform(cleaned)

        val invVocab = vocab.entries.associate { (k, v) -> v to k }
        val contributions = ArrayList<Pair<String, Float>>()

        for (i in sparse.indices.indices) {
            val featureIdx = sparse.indices[i]
            val term = invVocab[featureIdx] ?: continue
            val weight = if (featureIdx in weights.indices) weights[featureIdx] else 0f
            val phi = weight * sparse.values[i]
            contributions.add(Pair(term, phi))
        }

        return contributions
    }

    private fun fallbackScorer(text: String): Float {
        val t = text.lowercase(Locale.ROOT)
        val spamKeywords = listOf("free", "win", "prize", "claim", "urgent", "urltoken", "longnum", "click here", "call now", "verify", "kyc", "lottery")
        var matchCount = 0
        for (kw in spamKeywords) {
            if (t.contains(kw)) matchCount++
        }
        return (matchCount * 0.22f).coerceIn(0.05f, 0.95f)
    }

    fun getWeightsCopy(): FloatArray = weights.copyOf()
    fun getBias(): Float = bias
    fun getPlattA(): Float = plattA
    fun getPlattB(): Float = plattB
    fun getVocab(): Map<String, Int> = vocab
    fun getIdf(): FloatArray = idf.copyOf()
}
