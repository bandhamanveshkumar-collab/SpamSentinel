package com.spamsentinel.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ThreatSignals(
    val isHeaderSender: Boolean = false,
    val isPersonalNumberPromo: Boolean = false,
    val isOtpMessage: Boolean = false,
    val hasRiskyUrl: Boolean = false,
    val flaggedUrlHost: String? = null,
    val urlWarning: String? = null,
    val isNonAscii: Boolean = false
)
