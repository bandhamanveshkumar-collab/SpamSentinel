package com.spamsentinel.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spamsentinel.app.domain.model.VerdictAction
import com.spamsentinel.app.ui.components.SectionHeader
import com.spamsentinel.app.ui.theme.DeepIndigoPrimary
import com.spamsentinel.app.ui.theme.SafeGreen
import com.spamsentinel.app.ui.theme.SpamRed

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onProtectionToggle: (Boolean) -> Unit,
    onTauLowChanged: (Float) -> Unit,
    onTauHighChanged: (Float) -> Unit,
    onStrictModeToggle: (Boolean) -> Unit,
    onRetrainOnChargingToggle: (Boolean) -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    onPrivacyBlurToggle: (Boolean) -> Unit,
    onTrustContactsToggle: (Boolean) -> Unit,
    onWeeklyDigestToggle: (Boolean) -> Unit,
    onAddRule: (String, VerdictAction) -> Unit,
    onRemoveRule: (String) -> Unit,
    onRetrainNow: (android.content.Context) -> Unit,
    onRunSelfTest: (android.content.Context) -> Unit,
    onClearAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var newSenderInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SectionHeader(title = "Settings & Security")

        Spacer(modifier = Modifier.height(12.dp))

        // Model Self-Test Banner
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "On-Device Model Self-Test", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = "Active Model v${uiState.modelVersion}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Button(onClick = { onRunSelfTest(context) }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Run Test")
                    }
                }

                if (uiState.parityCheckResult != null) {
                    val result = uiState.parityCheckResult
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (result.isPassed) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (result.isPassed) SafeGreen else SpamRed
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (result.isPassed) "Self-Test PASSED (${result.sampleCount} parity samples)" else "Self-Test FAILED",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (result.isPassed) SafeGreen else SpamRed
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Detection Threshold Sliders
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                SectionHeader(title = "Decision Thresholds")

                Text(text = "Upper Threshold (tau High = ${String.format("%.2f", uiState.tauHigh)}): Block Above")
                Slider(value = uiState.tauHigh, onValueChange = onTauHighChanged, valueRange = 0.60f..0.95f)

                Spacer(modifier = Modifier.height(8.dp))

                Text(text = "Lower Threshold (tau Low = ${String.format("%.2f", uiState.tauLow)}): Allow Below")
                Slider(value = uiState.tauLow, onValueChange = onTauLowChanged, valueRange = 0.05f..0.40f)

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Strict Mode (Upgrade Review -> Block on Threat)", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = uiState.strictMode, onCheckedChange = onStrictModeToggle)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Security & Privacy Toggles
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                SectionHeader(title = "Privacy & App Security")

                SettingSwitchRow("Biometric Lock (Fingerprint/Face)", uiState.biometricEnabled, onBiometricToggle)
                SettingSwitchRow("Privacy Blur Message Snippets", uiState.privacyBlur, onPrivacyBlurToggle)
                SettingSwitchRow("Trust Saved Contacts", uiState.trustContacts, onTrustContactsToggle)
                SettingSwitchRow("Weekly Protection Summary Digest", uiState.weeklyDigestEnabled, onWeeklyDigestToggle)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Allowlist / Blocklist Rule Manager
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                SectionHeader(title = "Sender Rules Manager")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newSenderInput,
                        onValueChange = { newSenderInput = it },
                        placeholder = { Text("Sender ID e.g. AX-HDFCBK") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = {
                        onAddRule(newSenderInput, VerdictAction.BLOCK)
                        newSenderInput = ""
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Rule", tint = SpamRed)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                uiState.senderRules.forEach { rule ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = rule.sender, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = rule.ruleType.name,
                                color = if (rule.ruleType == VerdictAction.BLOCK) SpamRed else SafeGreen,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { onRemoveRule(rule.sender) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Rule", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Adaptive Learning & Manual Retrain
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                SectionHeader(title = "Adaptive Learning Fine-Tuning")

                SettingSwitchRow("Retrain Only While Charging", uiState.retrainOnCharging, onRetrainOnChargingToggle)

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { onRetrainNow(context) },
                    enabled = !uiState.isRetraining,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (uiState.isRetraining) "Fine-Tuning SGD Epochs..." else "Retrain Model Now")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Danger Zone
        OutlinedButton(
            onClick = onClearAllData,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SpamRed),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Delete, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Clear All Messages & Data")
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun SettingSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
