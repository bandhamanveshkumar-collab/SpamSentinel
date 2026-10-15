package com.spamsentinel.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.spamsentinel.app.domain.ml.LinearSpamModel
import com.spamsentinel.app.receiver.NotificationHelper
import com.spamsentinel.app.service.SmsObserverService
import com.spamsentinel.app.ui.navigation.MainAppNavigation
import com.spamsentinel.app.ui.navigation.Screen
import com.spamsentinel.app.ui.theme.SpamSentinelTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        startSmsObserverService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val app = applicationContext as SpamSentinelApp
        val repository = app.container.repository

        // Preload ML model & create notification channels
        CoroutineScope(Dispatchers.IO).launch {
            LinearSpamModel.getInstance().init(applicationContext)
            NotificationHelper.createChannels(applicationContext)
        }

        // Request SMS & Notification runtime permissions
        requestRequiredPermissions()
        requestBatteryOptimizationExemption()
        startSmsObserverService()

        setContent {
            val onboardingCompleted by repository.preferences.onboardingCompleted.collectAsState(initial = false)
            val themeMode by repository.preferences.themeMode.collectAsState(initial = "SYSTEM")

            val darkTheme = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            SpamSentinelTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val startDestination = if (onboardingCompleted) Screen.Home.route else Screen.Onboarding.route
                    MainAppNavigation(startDestination = startDestination)
                }
            }
        }
    }

    private fun startSmsObserverService() {
        try {
            val intent = Intent(this, SmsObserverService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as? PowerManager
            if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun requestRequiredPermissions() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingPermissions = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }
}
