package com.spamsentinel.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.spamsentinel.app.ui.theme.DeepIndigoPrimary

@Composable
fun Sparkline(
    values: List<Float>,
    modifier: Modifier = Modifier.size(60.dp, 24.dp),
    color: Color = DeepIndigoPrimary
) {
    if (values.size < 2) return

    val min = values.minOrNull() ?: 0f
    val max = (values.maxOrNull() ?: 1f).coerceAtLeast(min + 1f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val stepX = width / (values.size - 1)

        val path = Path()
        values.forEachIndexed { index, valX ->
            val x = index * stepX
            val normY = (valX - min) / (max - min)
            val y = height - (normY * height)

            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
