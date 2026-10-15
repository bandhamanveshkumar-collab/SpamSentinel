package com.spamsentinel.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SenderRuleDao {

    @Query("SELECT * FROM sender_rules ORDER BY timestamp DESC")
    fun getAllRules(): Flow<List<SenderRuleEntity>>

    @Query("SELECT * FROM sender_rules WHERE sender = :sender")
    suspend fun getRuleForSender(sender: String): SenderRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: SenderRuleEntity)

    @Query("DELETE FROM sender_rules WHERE sender = :sender")
    suspend fun deleteRule(sender: String)

    @Query("DELETE FROM sender_rules")
    suspend fun deleteAllRules()
}
