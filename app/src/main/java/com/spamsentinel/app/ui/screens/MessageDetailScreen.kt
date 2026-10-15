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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.spamsentinel.app.domain.model.VerdictAction
import com.spamsentinel.app.ui.components.DivergingBarChart
import com.spamsentinel.app.ui.components.GaugeView
import com.spamsentinel.app.ui.components.ProbabilityBar
import com.spamsentinel.app.ui.components.SectionHeader
import com.spamsentinel.app.ui.components.VerdictChip
import com.spamsentinel.app.ui.theme.ReviewAmber
import com.spamsentinel.app.ui.theme.SafeGreen
import com.spamsentinel.app.ui.theme.SpamRed

@Composable
fun MessageDetailScreen(
    uiState: MessageDetailUiState,
    onBackClick: () -> Unit,
    onMarkSafe: () -> Unit,
    onMarkSpam: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Message Analysis", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val msg = uiState.message
        val verdict = uiState.verdict

        if (msg == null || verdict == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Message details unavailable.")
            }
            return@Scaffold
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Gauge View
            GaugeView(probability = verdict.spamProbability)

            Spacer(modifier = Modifier.height(16.dp))

            // Probability Scale
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Calibrated Probability", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        VerdictChip(action = verdict.action)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    ProbabilityBar(probability = verdict.spamProbability)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Message Body with Highlighted Terms
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Sender: ${msg.sender}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    val spamTerms = verdict.topSpamTerms.map { it.term }
                    val hamTerms = verdict.topHamTerms.map { it.term }

                    val annotatedBody = buildAnnotatedString {
                        val words = msg.body.split(" ")
                        words.forEachIndexed { idx, word ->
                            val clean = word.lowercase().replace(Regex("[^a-z0-9]"), "")
                            when {
                                spamTerms.contains(clean) -> {
                                    withStyle(SpanStyle(color = SpamRed, fontWeight = FontWeight.Bold)) {
                                        append(word)
                                    }
                                }
                                hamTerms.contains(clean) -> {
                                    withStyle(SpanStyle(color = SafeGreen, fontWeight = FontWeight.Bold)) {
                                        append(word)
                                    }
                                }
                                else -> append(word)
                            }
                            if (idx < words.size - 1) append(" ")
                        }
                    }

                    Text(
                        text = annotatedBody,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Explainer Diverging Bar Chart
            if (verdict.topSpamTerms.isNotEmpty() || verdict.topHamTerms.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader(title = "Explainability (Feature Contributions)")
                        Spacer(modifier = Modifier.height(8.dp))
                        DivergingBarChart(
                            spamTerms = verdict.topSpamTerms,
                            hamTerms = verdict.topHamTerms
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Threat Signals Chips
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader(title = "Threat Signals")
                    Spacer(modifier = Modifier.height(8.dp))

                    val signals = verdict.threatSignals
                    if (signals.isHeaderSender) SignalRow("Sender Type", "Alphanumeric Header (Banking/Official)", SafeGreen)
                    if (signals.isPersonalNumberPromo) SignalRow("Sender Type", "10-digit Mobile Sending Promo", SpamRed)
                    if (signals.isOtpMessage) SignalRow("OTP Guard", "Verification / Transaction Code", SafeGreen)
                    if (signals.hasRiskyUrl) SignalRow("URL Guard", signals.urlWarning ?: "Risky Link Detected", SpamRed)
                    if (signals.isNonAscii) SignalRow("Text Guard", "High Non-ASCII Character Content", ReviewAmber)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Agent Timeline Stepper
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader(title = "Agent Pipeline Executed (${verdict.latencyMs}ms)")
                    Spacer(modifier = Modifier.height(8.dp))
                    AgentStep("1. Perceive", "Text preprocessed & TF-IDF vectorized")
                    AgentStep("2. Score", "Linear SVM + Platt Calibration evaluated")
                    AgentStep("3. Decide", "Symmetric threshold tau applied with Threat Signals")
                    AgentStep("4. Explain", "Top positive/negative term weights computed")
                    AgentStep("5. Learn", "User correction enqueued for adaptive SGD fine-tuning")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onMarkSafe,
                    colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Not Spam")
                }

                Button(
                    onClick = onMarkSpam,
                    colors = ButtonDefaults.buttonColors(containerColor = SpamRed),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Block, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Block Sender")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SignalRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(8.dp).background(color, shape = RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "$label: ", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

@Composable
private fun AgentStep(step: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = step, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(90.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
