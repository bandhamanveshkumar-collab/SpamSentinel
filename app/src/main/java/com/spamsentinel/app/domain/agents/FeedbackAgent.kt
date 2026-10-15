package com.spamsentinel.app.domain.agents

import android.content.Context
import com.spamsentinel.app.data.db.ModelHistoryEntity
import com.spamsentinel.app.data.repo.SpamRepository
import com.spamsentinel.app.domain.ml.LinearSpamModel
import com.spamsentinel.app.domain.ml.ModelJson
import com.spamsentinel.app.domain.ml.TextCleaner
import com.spamsentinel.app.domain.ml.TfIdfVectorizer
import com.spamsentinel.app.domain.model.VerdictAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.exp
import kotlin.math.ln

@Serializable
data class ValidationItem(
    val y: Int,
    val text: String
)

object FeedbackAgent {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun processFeedbackAndFineTune(
        context: Context,
        repository: SpamRepository
    ): Boolean = withContext(Dispatchers.IO) {
        val feedbackItems = repository.feedbackDao.getAllFeedback()
        if (feedbackItems.isEmpty()) return@withContext false

        val model = LinearSpamModel.getInstance()
        model.init(context)

        val currentWeights = model.getWeightsCopy()
        val currentBias = model.getBias()
        val vocab = model.getVocab()
        val idf = model.getIdf()
        val vectorizer = TfIdfVectorizer(vocab, idf)

        // Load validation dataset
        val valText = context.assets.open("validation.json").bufferedReader().use { it.readText() }
        val valItems = json.decodeFromString<List<ValidationItem>>(valText)

        // Compute validation F1 before fine-tuning
        val valF1Before = computeF1(valItems, model)

        // SGD Fine-Tuning
        val newWeights = currentWeights.copyOf()
        var newBias = currentBias
        val learningRate = 0.02f
        val epochs = 5

        // Prepare training samples from feedback buffer
        val trainingSamples = feedbackItems.map { fb ->
            val targetY = if (fb.correctedLabel == VerdictAction.BLOCK) 1f else 0f
            val sparse = vectorizer.transform(TextCleaner.clean(fb.body))
            Pair(sparse, targetY)
        }

        for (epoch in 0 until epochs) {
            for ((sparse, y) in trainingSamples) {
                if (sparse.indices.isEmpty()) continue

                var score = newBias.toDouble()
                for (i in sparse.indices.indices) {
                    val idx = sparse.indices[i]
                    if (idx in newWeights.indices) {
                        score += newWeights[idx].toDouble() * sparse.values[i].toDouble()
                    }
                }

                val prob = 1.0 / (1.0 + exp(-score))
                val error = (prob - y).toFloat()

                // SGD weight updates
                for (i in sparse.indices.indices) {
                    val idx = sparse.indices[i]
                    if (idx in newWeights.indices) {
                        val grad = error * sparse.values[i]
                        newWeights[idx] -= learningRate * grad
                    }
                }
                newBias -= learningRate * error
            }
        }

        val candidateVersion = model.version + 1
        val candidateModel = ModelJson(
            vocab = vocab,
            idf = idf.toList(),
            weights = newWeights.toList(),
            bias = newBias,
            plattA = model.getPlattA(),
            plattB = model.getPlattB(),
            version = candidateVersion
        )

        // Evaluate validation F1 after fine-tuning
        val tempModel = LinearSpamModel.getInstance()
        tempModel.loadFromData(candidateModel)
        val valF1After = computeF1(valItems, tempModel)

        // Safety Promotion Check: Promote only if F1 drop <= 1%
        val isPromoted = valF1After >= (valF1Before - 0.01f)

        if (isPromoted) {
            // Save active_model.json to internal storage
            val activeModelFile = File(context.filesDir, "active_model.json")
            activeModelFile.writeText(json.encodeToString(candidateModel))

            model.loadFromData(candidateModel)

            val maxVer = repository.modelHistoryDao.getMaxVersion() ?: 1
            repository.modelHistoryDao.insertHistory(
                ModelHistoryEntity(
                    version = candidateVersion,
                    timestamp = System.currentTimeMillis(),
                    trainingSamplesCount = feedbackItems.size,
                    valF1Before = valF1Before,
                    valF1After = valF1After,
                    status = "PROMOTED",
                    rollbackCount = 0,
                    weightsJsonPath = activeModelFile.absolutePath
                )
            )

            repository.logAuditEvent(
                eventType = "PROMOTION",
                description = "Model promoted to v$candidateVersion. Val F1: ${String.format("%.3f", valF1Before)} -> ${String.format("%.3f", valF1After)}",
                detailsJson = "{\"feedback_count\": ${feedbackItems.size}}"
            )

            repository.feedbackDao.clearBuffer()
            return@withContext true
        } else {
            // Rollback & Log audit
            repository.modelHistoryDao.insertHistory(
                ModelHistoryEntity(
                    version = candidateVersion,
                    timestamp = System.currentTimeMillis(),
                    trainingSamplesCount = feedbackItems.size,
                    valF1Before = valF1Before,
                    valF1After = valF1After,
                    status = "ROLLED_BACK",
                    rollbackCount = 1
                )
            )

            repository.logAuditEvent(
                eventType = "ROLLBACK",
                description = "Fine-tuning rejected due to validation F1 drop (>1%). Val F1: ${String.format("%.3f", valF1Before)} -> ${String.format("%.3f", valF1After)}",
                detailsJson = "{\"feedback_count\": ${feedbackItems.size}}"
            )

            repository.feedbackDao.clearBuffer()
            return@withContext false
        }
    }

    private fun computeF1(valItems: List<ValidationItem>, model: LinearSpamModel): Float {
        var tp = 0
        var fp = 0
        var fn = 0

        for (item in valItems) {
            val prob = model.predictProbability(item.text)
            val pred = if (prob >= 0.5f) 1 else 0

            if (pred == 1 && item.y == 1) tp++
            if (pred == 1 && item.y == 0) fp++
            if (pred == 0 && item.y == 1) fn++
        }

        val prec = if (tp + fp == 0) 0f else tp.toFloat() / (tp + fp)
        val rec = if (tp + fn == 0) 0f else tp.toFloat() / (tp + fn)
        return if (prec + rec == 0f) 0f else 2f * (prec * rec) / (prec + rec)
    }
}
