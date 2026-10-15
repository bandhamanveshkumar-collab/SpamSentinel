package com.spamsentinel.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedbackDao {

    @Query("SELECT * FROM feedback_buffer ORDER BY timestamp ASC")
    suspend fun getAllFeedback(): List<FeedbackBufferEntity>

    @Query("SELECT COUNT(*) FROM feedback_buffer")
    fun getFeedbackCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM feedback_buffer")
    suspend fun getFeedbackCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: FeedbackBufferEntity): Long

    @Query("DELETE FROM feedback_buffer")
    suspend fun clearBuffer()

    @Query("SELECT COUNT(*) FROM feedback_buffer WHERE timestamp >= :sinceTimestamp")
    suspend fun getRecentCorrectionCount(sinceTimestamp: Long): Int

    @Query("SELECT * FROM feedback_buffer WHERE messageId = :messageId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastFeedbackForMessage(messageId: Long): FeedbackBufferEntity?
}
