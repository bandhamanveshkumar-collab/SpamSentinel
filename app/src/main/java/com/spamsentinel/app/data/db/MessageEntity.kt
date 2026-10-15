package com.spamsentinel.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.spamsentinel.app.domain.model.ScamType
import com.spamsentinel.app.domain.model.VerdictAction

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val senderType: String, // "ALPHANUMERIC", "MOBILE_10", "SHORT_CODE", "INTERNATIONAL"
    val body: String,
    val timestamp: Long,
    val spamProbability: Float,
    val confidence: Float,
    val action: VerdictAction,
    val userLabel: VerdictAction? = null,
    val topTermsJson: String = "[]",
    val signalsJson: String = "{}",
    val scamType: ScamType = ScamType.NONE,
    val modelVersion: Int = 1,
    val latencyMs: Long = 0L
)
