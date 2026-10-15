package com.spamsentinel.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "model_history")
data class ModelHistoryEntity(
    @PrimaryKey val version: Int,
    val timestamp: Long,
    val trainingSamplesCount: Int,
    val valF1Before: Float,
    val valF1After: Float,
    val status: String, // "PROMOTED", "ROLLED_BACK", "ACTIVE"
    val rollbackCount: Int = 0,
    val weightsJsonPath: String = ""
)
