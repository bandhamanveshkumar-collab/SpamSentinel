package com.spamsentinel.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spamsentinel.app.SpamSentinelApp
import com.spamsentinel.app.ui.theme.DeepIndigoPrimary
import com.spamsentinel.app.worker.InboxScanProgress
import com.spamsentinel.app.worker.InboxScanner
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as SpamSentinelApp
    val repository = app.container.repository
    val scope = rememberCoroutineScope()

    var isScanning by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableStateOf<InboxScanProgress?>(null) }
    var showRestrictedSettingsHelp by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(DeepIndigoPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "SpamSentinel Shield",
                tint = DeepIndigoPrimary,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Welcome to SpamSentinel",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "On-device Agentic AI Protection for SMS Spam & Scams",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = DeepIndigoPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "100% On-Device & Offline Privacy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No internet required. Your messages never leave this phone. Processing happens strictly on-device using a calibrated SVM Machine Learning model.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = DeepIndigoPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SMS Filtering Permission Note",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Android allows SpamSentinel to scan incoming SMS and display security notifications. Non-default SMS apps move flagged messages to in-app quarantine.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { showRestrictedSettingsHelp = !showRestrictedSettingsHelp },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Info, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Android 13+ 'Allow Restricted Settings' Help")
        }

        AnimatedVisibility(visible = showRestrictedSettingsHelp) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Sideloaded Installation Steps:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "If Android shows 'Restricted setting':\n1. Open Settings > Apps > SpamSentinel.\n2. Tap 3 dots (top right) > 'Allow restricted settings'.\n3. Authenticate with PIN/Fingerprint and grant SMS permissions.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isScanning && scanProgress != null) {
            val prog = scanProgress!!
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Scanning Inbox: ${prog.scannedCount} / ${prog.totalCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { if (prog.totalCount > 0) prog.scannedCount.toFloat() / prog.totalCount else 0f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Button(
                onClick = {
                    isScanning = true
                    scope.launch {
                        val scanner = InboxScanner(context, repository)
                        scanner.scanInbox().collect { progress ->
                            scanProgress = progress
                            if (progress.isComplete) {
                                repository.preferences.setOnboardingCompleted(true)
                                onComplete()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Scan Inbox & Enable Protection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    scope.launch {
                        repository.preferences.setOnboardingCompleted(true)
                        onComplete()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = "Skip First Scan")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
