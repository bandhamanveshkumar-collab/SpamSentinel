package com.spamsentinel.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_log")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String, // "PROMOTION", "ROLLBACK", "CORRECTION_RATE_LIMITED", "FLIP_FLOP_IGNORED"
    val description: String,
    val detailsJson: String = "{}"
)
