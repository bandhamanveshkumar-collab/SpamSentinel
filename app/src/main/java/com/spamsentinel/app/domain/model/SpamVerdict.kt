package com.spamsentinel.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TermWeight(
    val term: String,
    val weight: Float
)

@Serializable
data class SpamVerdict(
    val action: VerdictAction,
    val spamProbability: Float,
    val confidence: Float,
    val topSpamTerms: List<TermWeight> = emptyList(),
    val topHamTerms: List<TermWeight> = emptyList(),
    val threatSignals: ThreatSignals = ThreatSignals(),
    val scamType: ScamType = ScamType.NONE,
    val latencyMs: Long = 0L
)
