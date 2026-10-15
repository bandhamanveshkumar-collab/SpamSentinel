package com.spamsentinel.app.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spamsentinel.app.data.db.ModelHistoryEntity
import com.spamsentinel.app.data.repo.SpamRepository
import com.spamsentinel.app.domain.model.BenchmarkData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

enum class InsightsTab {
    BENCHMARK,
    AGENT_ROUTING,
    FEEDBACK_LEARNING,
    ABLATION,
    GLOBAL_TERMS
}

data class InsightsUiState(
    val selectedTab: InsightsTab = InsightsTab.BENCHMARK,
    val benchmarkData: BenchmarkData? = null,
    val modelHistory: List<ModelHistoryEntity> = emptyList(),
    // Interactive Cost Simulator State
    val cFP: Float = 10f, // Cost of False Positive
    val cFN: Float = 50f, // Cost of False Negative
    val cR: Float = 1f,   // Cost of Review
    val tauSim: Float = 0.80f,
    val computedCost: Double = 0.0,
    val isLoading: Boolean = true
)

class InsightsViewModel(
    private val repository: SpamRepository
) : ViewModel() {

    private val selectedTab = MutableStateFlow(InsightsTab.BENCHMARK)
    private val benchmarkData = MutableStateFlow<BenchmarkData?>(null)
    private val cFP = MutableStateFlow(10f)
    private val cFN = MutableStateFlow(50f)
    private val cR = MutableStateFlow(1f)
    private val tauSim = MutableStateFlow(0.80f)

    val uiState: StateFlow<InsightsUiState> = combine(
        selectedTab,
        benchmarkData,
        cFP,
        cFN,
        cR,
        tauSim,
        repository.modelHistory
    ) { flows ->
        val tab = flows[0] as InsightsTab
        val bData = flows[1] as? BenchmarkData
        val cfp = flows[2] as Float
        val cfn = flows[3] as Float
        val cr = flows[4] as Float
        val tau = flows[5] as Float
        @Suppress("UNCHECKED_CAST")
        val history = flows[6] as List<ModelHistoryEntity>

        val cost = calculateCost(bData, tau, cfp, cfn, cr)

        InsightsUiState(
            selectedTab = tab,
            benchmarkData = bData,
            modelHistory = history,
            cFP = cfp,
            cFN = cfn,
            cR = cr,
            tauSim = tau,
            computedCost = cost,
            isLoading = bData == null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InsightsUiState()
    )

    fun loadBenchmarkData(context: Context) {
        if (benchmarkData.value != null) return
        viewModelScope.launch {
            try {
                val jsonString = context.assets.open("benchmark.json").bufferedReader().use { it.readText() }
                val parsed = Json { ignoreUnknownKeys = true }.decodeFromString<BenchmarkData>(jsonString)
                benchmarkData.value = parsed
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setTab(tab: InsightsTab) {
        selectedTab.value = tab
    }

    fun setCFP(value: Float) {
        cFP.value = value
    }

    fun setCFN(value: Float) {
        cFN.value = value
    }

    fun setCR(value: Float) {
        cR.value = value
    }

    fun setTauSim(value: Float) {
        tauSim.value = value
    }

    private fun calculateCost(data: BenchmarkData?, tau: Float, cfp: Float, cfn: Float, cr: Float): Double {
        if (data == null || data.threshold.isEmpty()) return 0.0
        val closestRow = data.threshold.minByOrNull { Math.abs(it.tau - tau) } ?: return 0.0
        val n = closestRow.n.coerceAtLeast(1)
        val fpCost = closestRow.auto_fp * cfp
        val fnCost = closestRow.auto_fn * cfn
        val rCost = closestRow.n_review * cr
        return (fpCost + fnCost + rCost) / n.toDouble()
    }
}
