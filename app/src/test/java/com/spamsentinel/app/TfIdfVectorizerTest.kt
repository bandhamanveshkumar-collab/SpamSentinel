package com.spamsentinel.app

import com.spamsentinel.app.domain.ml.TfIdfVectorizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TfIdfVectorizerTest {

    @Test
    fun testVectorizationAndL2Normalization() {
        val vocab = mapOf("free" to 0, "claim" to 1, "free claim" to 2)
        val idf = floatArrayOf(2.0f, 3.0f, 4.0f)
        val vectorizer = TfIdfVectorizer(vocab, idf)

        val sparse = vectorizer.transform("free claim")
        assertEquals(3, sparse.indices.size)

        // Verify L2 norm = 1.0
        var sumSq = 0.0
        for (v in sparse.values) {
            sumSq += (v * v).toDouble()
        }
        assertEquals(1.0, sumSq, 1e-4)
    }
}
