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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spamsentinel.app.domain.model.TermWeight
import com.spamsentinel.app.ui.components.DivergingBarChart
import com.spamsentinel.app.ui.components.GroupedBarChart
import com.spamsentinel.app.ui.components.Heatmap2x2
import com.spamsentinel.app.ui.components.HorizontalBarChart
import com.spamsentinel.app.ui.components.HorizontalBarItem
import com.spamsentinel.app.ui.components.LineChart
import com.spamsentinel.app.ui.components.SectionHeader
import com.spamsentinel.app.ui.theme.DeepIndigoPrimary
import com.spamsentinel.app.ui.theme.SafeGreen
import com.spamsentinel.app.ui.theme.SpamRed

@Composable
fun InsightsScreen(
    uiState: InsightsUiState,
    onTabSelected: (InsightsTab) -> Unit,
    onLoadData: (android.content.Context) -> Unit,
    onCFPChanged: (Float) -> Unit,
    onCFNChanged: (Float) -> Unit,
    onCRChanged: (Float) -> Unit,
    onTauSimChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        onLoadData(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScrollableTabRow(
            selectedTabIndex = uiState.selectedTab.ordinal,
            edgePadding = 16.dp
        ) {
            InsightsTab.entries.forEach { tab ->
                Tab(
                    selected = uiState.selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    text = {
                        Text(
                            text = when (tab) {
                                InsightsTab.BENCHMARK -> "Benchmark"
                                InsightsTab.AGENT_ROUTING -> "Agent Routing"
                                InsightsTab.FEEDBACK_LEARNING -> "Feedback Learning"
                                InsightsTab.ABLATION -> "Ablation"
                                InsightsTab.GLOBAL_TERMS -> "Global Terms"
                            },
                            fontWeight = if (uiState.selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        if (uiState.isLoading || uiState.benchmarkData == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return
        }

        val data = uiState.benchmarkData

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (uiState.selectedTab) {
                InsightsTab.BENCHMARK -> {
                    SectionHeader(
                        title = "Model Benchmark Comparison",
                        subtitle = "Evaluated on SMS Spam Collection dataset"
                    )

                    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            GroupedBarChart(modelsData = data.models)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    data.models["Linear SVM"]?.let { svmMetrics ->
                        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                SectionHeader(title = "Deployed Model: Linear SVM Heatmap")
                                Spacer(modifier = Modifier.height(8.dp))
                                Heatmap2x2(cm = svmMetrics.cm)
                            }
                        }
                    }
                }

                InsightsTab.AGENT_ROUTING -> {
                    SectionHeader(
                        title = "Cost-Optimal Agent Routing",
                        subtitle = "Interactive Simulator: J(tau) = (cFP*FP + cFN*FN + cR*nR)/n"
                    )

                    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Expected Cost J(tau): ${String.format("%.3f", uiState.computedCost)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = DeepIndigoPrimary
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(text = "Threshold tau: ${String.format("%.2f", uiState.tauSim)}")
                            Slider(value = uiState.tauSim, onValueChange = onTauSimChanged, valueRange = 0.60f..0.95f)

                            Text(text = "cFP (False Positive Cost): ${uiState.cFP.toInt()}")
                            Slider(value = uiState.cFP, onValueChange = onCFPChanged, valueRange = 1f..50f)

                            Text(text = "cFN (False Negative Cost): ${uiState.cFN.toInt()}")
                            Slider(value = uiState.cFN, onValueChange = onCFNChanged, valueRange = 10f..100f)

                            Text(text = "cR (Manual Review Cost): ${uiState.cR.toInt()}")
                            Slider(value = uiState.cR, onValueChange = onCRChanged, valueRange = 0.5f..10f)
                        }
                    }
                }

                InsightsTab.FEEDBACK_LEARNING -> {
                    SectionHeader(
                        title = "Adaptive Learning Dynamics",
                        subtitle = "F1 Score improvement vs user corrections"
                    )

                    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val f1Points = data.feedback.map { it.f1.toFloat() }
                            LineChart(
                                dataPoints = f1Points,
                                labels = data.feedback.map { "${it.k}" },
                                lineColor = SafeGreen
                            )
                        }
                    }
                }

                InsightsTab.ABLATION -> {
                    SectionHeader(
                        title = "Ablation Study",
                        subtitle = "F1 Score impact across model variations"
                    )

                    val ablationItems = data.ablation.map { (name, metrics) ->
                        HorizontalBarItem(name, (metrics.f1 * 100).toFloat(), DeepIndigoPrimary)
                    }

                    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            HorizontalBarChart(items = ablationItems)
                        }
                    }
                }

                InsightsTab.GLOBAL_TERMS -> {
                    SectionHeader(
                        title = "Global Term Weights",
                        subtitle = "Top Linear SVM model feature coefficients"
                    )

                    val topSpamTerms = data.top_spam.take(5).map { TermWeight(it[0], it[1].toFloat()) }
                    val topHamTerms = data.top_ham.take(5).map { TermWeight(it[0], it[1].toFloat()) }

                    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            DivergingBarChart(spamTerms = topSpamTerms, hamTerms = topHamTerms)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Research Attribution Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "About the Research Paper",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Implemented from: 'An Agentic AI Framework for SMS Spam Detection with Explainable, Confidence-Aware and Feedback-Driven Adaptive Learning'\nAuthors: E. Goutami, S. Sabir Hussain, B. Anvesh Kumar, S. Fahad\nSanthiram Engineering College (Autonomous), Nandyal.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
