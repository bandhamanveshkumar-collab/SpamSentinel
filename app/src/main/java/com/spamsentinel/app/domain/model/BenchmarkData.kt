package com.spamsentinel.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ModelMetrics(
    val acc: Double = 0.0,
    val prec: Double = 0.0,
    val rec: Double = 0.0,
    val f1: Double = 0.0,
    val mcc: Double = 0.0,
    val cm: List<List<Int>> = listOf(listOf(0, 0), listOf(0, 0)),
    val train_s: Double = 0.0,
    val infer_ms: Double = 0.0,
    val cv_f1_mean: Double = 0.0,
    val cv_f1_std: Double = 0.0
)

@Serializable
data class ThresholdRow(
    val tau: Double,
    val review_pct: Double,
    val auto_fp: Int,
    val auto_fn: Int,
    val n_review: Int,
    val n: Int,
    val auto_acc: Double
)

@Serializable
data class FeedbackStep(
    val k: Int,
    val prec: Double,
    val rec: Double,
    val f1: Double
)

@Serializable
data class BenchmarkData(
    val models: Map<String, ModelMetrics> = emptyMap(),
    val threshold: List<ThresholdRow> = emptyList(),
    val feedback: List<FeedbackStep> = emptyList(),
    val ablation: Map<String, ModelMetrics> = emptyMap(),
    val top_spam: List<List<String>> = emptyList(),
    val top_ham: List<List<String>> = emptyList(),
    val n_total: Int = 0,
    val n_spam: Int = 0
)
