package com.spamsentinel.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        MessageEntity::class,
        FeedbackBufferEntity::class,
        ModelHistoryEntity::class,
        SenderRuleEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao
    abstract fun feedbackDao(): FeedbackDao
    abstract fun modelHistoryDao(): ModelHistoryDao
    abstract fun senderRuleDao(): SenderRuleDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "spam_sentinel_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
