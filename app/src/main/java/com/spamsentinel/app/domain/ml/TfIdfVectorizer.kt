package com.spamsentinel.app.domain.ml

import kotlin.math.ln
import kotlin.math.sqrt

data class SparseVector(
    val indices: IntArray,
    val values: FloatArray
)

class TfIdfVectorizer(
    private val vocab: Map<String, Int>,
    private val idf: FloatArray
) {
    private val tokenRegex = Regex("""[a-z0-9_]{2,}|[£$€]""")

    fun transform(cleanedText: String): SparseVector {
        val tokens = tokenRegex.findAll(cleanedText).map { it.value }.toList()
        if (tokens.isEmpty()) {
            return SparseVector(IntArray(0), FloatArray(0))
        }

        val termCounts = HashMap<String, Int>()

        // Unigrams
        for (token in tokens) {
            termCounts[token] = (termCounts[token] ?: 0) + 1
        }

        // Bigrams
        for (i in 0 until tokens.size - 1) {
            val bigram = "${tokens[i]} ${tokens[i + 1]}"
            termCounts[bigram] = (termCounts[bigram] ?: 0) + 1
        }

        val matchedFeatures = ArrayList<Pair<Int, Float>>()
        var sumSquared = 0.0

        for ((term, count) in termCounts) {
            val featureIdx = vocab[term] ?: continue
            if (featureIdx < 0 || featureIdx >= idf.size) continue

            // Sublinear TF: 1 + ln(tf)
            val sublinearTf = 1.0 + ln(count.toDouble())
            val tfidfVal = (sublinearTf * idf[featureIdx]).toFloat()

            matchedFeatures.add(Pair(featureIdx, tfidfVal))
            sumSquared += (tfidfVal * tfidfVal).toDouble()
        }

        if (matchedFeatures.isEmpty() || sumSquared == 0.0) {
            return SparseVector(IntArray(0), FloatArray(0))
        }

        // L2 normalization
        val l2Norm = sqrt(sumSquared).toFloat()
        matchedFeatures.sortBy { it.first }

        val indices = IntArray(matchedFeatures.size)
        val values = FloatArray(matchedFeatures.size)

        for (i in matchedFeatures.indices) {
            indices[i] = matchedFeatures[i].first
            values[i] = matchedFeatures[i].second / l2Norm
        }

        return SparseVector(indices, values)
    }
}
