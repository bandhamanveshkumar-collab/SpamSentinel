package com.spamsentinel.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.spamsentinel.app.domain.model.VerdictAction

@Entity(tableName = "feedback_buffer")
data class FeedbackBufferEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messageId: Long,
    val sender: String,
    val body: String,
    val originalProbability: Float,
    val correctedLabel: VerdictAction, // BLOCK (spam = 1) or ALLOW (ham = 0)
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = "USER_NOTIFICATION"
)
