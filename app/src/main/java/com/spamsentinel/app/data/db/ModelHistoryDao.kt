package com.spamsentinel.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelHistoryDao {

    @Query("SELECT * FROM model_history ORDER BY version DESC")
    fun getAllHistory(): Flow<List<ModelHistoryEntity>>

    @Query("SELECT * FROM model_history ORDER BY version DESC LIMIT 1")
    suspend fun getLatestModelHistory(): ModelHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: ModelHistoryEntity)

    @Query("SELECT MAX(version) FROM model_history")
    suspend fun getMaxVersion(): Int?
}
