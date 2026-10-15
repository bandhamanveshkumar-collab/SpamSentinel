package com.spamsentinel.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class VerdictAction {
    BLOCK,
    REVIEW,
    ALLOW
}
