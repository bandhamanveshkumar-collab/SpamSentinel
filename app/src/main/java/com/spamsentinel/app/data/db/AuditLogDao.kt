package com.spamsentinel.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {

    @Query("SELECT * FROM audit_log ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogEntity): Long

    @Query("DELETE FROM audit_log")
    suspend fun clearLogs()
}
