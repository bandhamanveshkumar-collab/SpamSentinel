package com.spamsentinel.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.spamsentinel.app.domain.model.VerdictAction
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE action = :action ORDER BY timestamp DESC")
    fun getMessagesByAction(action: VerdictAction): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessageById(id: Long): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("UPDATE messages SET userLabel = :userLabel, action = :newAction WHERE id = :id")
    suspend fun updateMessageLabel(id: Long, userLabel: VerdictAction, newAction: VerdictAction)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: Long)

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()

    @Query("SELECT COUNT(*) FROM messages")
    fun getCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM messages WHERE action = 'BLOCK'")
    fun getBlockedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM messages WHERE action = 'REVIEW'")
    fun getReviewCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM messages WHERE action = 'ALLOW'")
    fun getAllowCount(): Flow<Int>

    @Query("SELECT AVG(latencyMs) FROM messages")
    fun getAvgLatency(): Flow<Double?>

    @Query("SELECT * FROM messages ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMessages(limit: Int): Flow<List<MessageEntity>>
}
