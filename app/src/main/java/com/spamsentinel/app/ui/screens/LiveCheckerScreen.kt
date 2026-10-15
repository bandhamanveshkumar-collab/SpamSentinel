package com.spamsentinel.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spamsentinel.app.ui.components.DivergingBarChart
import com.spamsentinel.app.ui.components.GaugeView
import com.spamsentinel.app.ui.components.ProbabilityBar
import com.spamsentinel.app.ui.components.SectionHeader
import com.spamsentinel.app.ui.components.VerdictChip
import com.spamsentinel.app.ui.theme.DeepIndigoPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveCheckerScreen(
    uiState: LiveCheckerUiState,
    sampleSmsList: List<SampleSms>,
    onSenderChanged: (String) -> Unit,
    onTextChanged: (String) -> Unit,
    onLoadSample: (SampleSms) -> Unit,
    onSimulateIncomingSms: () -> Unit,
    onSeedDemoData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SectionHeader(
            title = "Live SMS Checker",
            subtitle = "Test any custom text or select real-world SMS samples"
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Sender & Text Inputs
        OutlinedTextField(
            value = uiState.senderInput,
            onValueChange = onSenderChanged,
            label = { Text("Sender Header / Number") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.textInput,
            onValueChange = onTextChanged,
            label = { Text("SMS Message Text") },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Preset Samples Chips
        Text(
            text = "Try Real-World Samples:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sampleSmsList.forEach { sample ->
                FilterChip(
                    selected = uiState.textInput == sample.text,
                    onClick = { onLoadSample(sample) },
                    label = { Text("${sample.title} (${sample.category})") }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Verdict Result Section
        if (uiState.verdict != null) {
            val verdict = uiState.verdict

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    GaugeView(probability = verdict.spamProbability)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VerdictChip(action = verdict.action, spamProbability = verdict.spamProbability)
                        Text(
                            text = "Latency: ${verdict.latencyMs}ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    ProbabilityBar(probability = verdict.spamProbability)

                    if (verdict.topSpamTerms.isNotEmpty() || verdict.topHamTerms.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionHeader(title = "Top Contributing Features")
                        DivergingBarChart(
                            spamTerms = verdict.topSpamTerms,
                            hamTerms = verdict.topHamTerms
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Interactive Action Buttons
        Button(
            onClick = onSimulateIncomingSms,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DeepIndigoPrimary)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Simulate Incoming SMS (System Notification)", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onSeedDemoData,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Storage, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = if (uiState.demoSeeded) "30-Day Demo History Seeded!" else "Seed 30-Day Demo History")
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
