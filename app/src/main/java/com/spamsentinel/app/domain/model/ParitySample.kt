package com.spamsentinel.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ParitySample(
    val text: String,
    val p: Float
)
