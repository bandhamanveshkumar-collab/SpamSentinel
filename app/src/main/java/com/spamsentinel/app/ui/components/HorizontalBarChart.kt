package com.spamsentinel.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.spamsentinel.app.ui.theme.SpamRed

data class HorizontalBarItem(
    val label: String,
    val value: Float,
    val color: Color = SpamRed
)

@Composable
fun HorizontalBarChart(
    items: List<HorizontalBarItem>,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val progress = remember { Animatable(0f) }

    LaunchedEffect(items) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    val maxVal = items.maxOfOrNull { it.value }?.coerceAtLeast(1f) ?: 1f

    Column(modifier = modifier.fillMaxWidth()) {
        items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(110.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f).height(16.dp)) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) {
                        val barWidth = (item.value / maxVal) * size.width * progress.value
                        drawRoundRect(
                            color = item.color,
                            topLeft = Offset(0f, 0f),
                            size = Size(barWidth, size.height),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${item.value.toInt()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
