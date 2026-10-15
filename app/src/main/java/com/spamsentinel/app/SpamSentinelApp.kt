package com.spamsentinel.app

import android.app.Application
import com.spamsentinel.app.di.AppContainer

class SpamSentinelApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
