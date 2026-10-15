package com.spamsentinel.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.spamsentinel.app.domain.model.VerdictAction

@Entity(tableName = "sender_rules")
data class SenderRuleEntity(
    @PrimaryKey val sender: String,
    val ruleType: VerdictAction, // ALLOW or BLOCK
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)
