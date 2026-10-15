package com.spamsentinel.app.di

import android.content.Context
import com.spamsentinel.app.data.db.AppDatabase
import com.spamsentinel.app.data.prefs.AppPreferences
import com.spamsentinel.app.data.repo.SpamRepository

class AppContainer(private val context: Context) {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    val preferences: AppPreferences by lazy {
        AppPreferences(context)
    }

    val repository: SpamRepository by lazy {
        SpamRepository(database, preferences)
    }
}
