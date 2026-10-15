package com.spamsentinel.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.spamsentinel.app.domain.model.ModelMetrics
import com.spamsentinel.app.ui.theme.DeepIndigoPrimary
import com.spamsentinel.app.ui.theme.ReviewAmber
import com.spamsentinel.app.ui.theme.SafeGreen

@Composable
fun GroupedBarChart(
    modelsData: Map<String, ModelMetrics>,
    modifier: Modifier = Modifier
) {
    if (modelsData.isEmpty()) return

    val progress = remember { Animatable(0f) }

    LaunchedEffect(modelsData) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    val modelNames = modelsData.keys.toList()
    val modelColors = listOf(
        ReviewAmber,
        DeepIndigoPrimary,
        Color(0xFF8B5CF6),
        SafeGreen
    )

    val metrics = listOf("Acc", "Prec", "Rec", "F1", "MCC")

    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp).padding(8.dp)) {
            Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                val width = size.width
                val height = size.height - 24.dp.toPx()

                val numGroups = metrics.size
                val groupWidth = width / numGroups

                for (g in 0 until numGroups) {
                    val groupX = g * groupWidth
                    val groupInnerWidth = groupWidth * 0.85f
                    val subBarWidth = groupInnerWidth / modelNames.size

                    for (m in modelNames.indices) {
                        val modelName = modelNames[m]
                        val metricVal = when (g) {
                            0 -> modelsData[modelName]?.acc ?: 0.0
                            1 -> modelsData[modelName]?.prec ?: 0.0
                            2 -> modelsData[modelName]?.rec ?: 0.0
                            3 -> modelsData[modelName]?.f1 ?: 0.0
                            else -> modelsData[modelName]?.mcc ?: 0.0
                        }.toFloat().coerceIn(0f, 1f)

                        val barX = groupX + (groupWidth - groupInnerWidth) / 2f + m * subBarWidth
                        val barHeight = metricVal * height * progress.value

                        drawRoundRect(
                            color = modelColors[m % modelColors.size],
                            topLeft = Offset(barX, height - barHeight),
                            size = Size(subBarWidth * 0.85f, barHeight),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            modelNames.forEachIndexed { idx, name ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(color = modelColors[idx % modelColors.size], shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
