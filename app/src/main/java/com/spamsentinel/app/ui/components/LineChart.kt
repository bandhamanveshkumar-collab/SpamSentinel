package com.spamsentinel.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.spamsentinel.app.ui.theme.DeepIndigoPrimary
import com.spamsentinel.app.ui.theme.SpamRed

@Composable
fun LineChart(
    dataPoints: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    lineColor: Color = SpamRed
) {
    if (dataPoints.isEmpty()) return

    val progress = remember { Animatable(0f) }

    LaunchedEffect(dataPoints) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    val maxVal = dataPoints.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    val minVal = 0f

    Box(modifier = modifier.fillMaxWidth().height(160.dp).padding(8.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
            val width = size.width
            val height = size.height - 30.dp.toPx()
            val stepX = width / (dataPoints.size - 1).coerceAtLeast(1)

            val gridColor = Color.Gray.copy(alpha = 0.2f)
            val numGridLines = 4
            for (i in 0..numGridLines) {
                val y = height * (i.toFloat() / numGridLines)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val path = Path()
            val fillPath = Path()

            val points = dataPoints.mapIndexed { idx, value ->
                val x = idx * stepX
                val normalizedY = (value - minVal) / (maxVal - minVal)
                val y = height - (normalizedY * height * progress.value)
                Offset(x, y)
            }

            if (points.isNotEmpty()) {
                path.moveTo(points[0].x, points[0].y)
                fillPath.moveTo(points[0].x, height)
                fillPath.lineTo(points[0].x, points[0].y)

                for (i in 1 until points.size) {
                    val p1 = points[i - 1]
                    val p2 = points[i]
                    val controlX1 = p1.x + (p2.x - p1.x) / 2f
                    val controlX2 = p1.x + (p2.x - p1.x) / 2f

                    path.cubicTo(controlX1, p1.y, controlX2, p2.y, p2.x, p2.y)
                    fillPath.cubicTo(controlX1, p1.y, controlX2, p2.y, p2.x, p2.y)
                }

                fillPath.lineTo(points.last().x, height)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.35f), lineColor.copy(alpha = 0.0f))
                    )
                )

                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                for (p in points) {
                    drawCircle(
                        color = Color.White,
                        radius = 5.dp.toPx(),
                        center = p
                    )
                    drawCircle(
                        color = lineColor,
                        radius = 3.dp.toPx(),
                        center = p
                    )
                }
            }
        }
    }
}
