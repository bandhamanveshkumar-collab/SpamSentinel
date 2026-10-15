package com.spamsentinel.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.spamsentinel.app.ui.theme.DeepIndigoPrimary

@Composable
fun BarChart(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    barColor: Color = DeepIndigoPrimary
) {
    if (values.isEmpty()) return

    val progress = remember { Animatable(0f) }

    LaunchedEffect(values) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    val maxVal = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f

    Box(modifier = modifier.fillMaxWidth().height(160.dp).padding(8.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
            val width = size.width
            val height = size.height - 24.dp.toPx()
            val numBars = values.size
            val totalSpacing = width * 0.3f
            val barWidth = (width - totalSpacing) / numBars
            val spacing = totalSpacing / (numBars + 1)

            for (i in values.indices) {
                val x = spacing + i * (barWidth + spacing)
                val barHeight = (values[i] / maxVal) * height * progress.value

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }
    }
}
