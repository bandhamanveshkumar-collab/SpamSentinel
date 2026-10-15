package com.spamsentinel.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spamsentinel.app.ui.theme.ReviewAmber
import com.spamsentinel.app.ui.theme.SafeGreen
import com.spamsentinel.app.ui.theme.SpamRed
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GaugeView(
    probability: Float, // 0.0 to 1.0
    modifier: Modifier = Modifier,
    tauLow: Float = 0.20f,
    tauHigh: Float = 0.80f
) {
    val animatedProb = remember { Animatable(0f) }

    LaunchedEffect(probability) {
        animatedProb.animateTo(
            targetValue = probability.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 1000)
        )
    }

    val currentProb = animatedProb.value
    val currentColor = when {
        currentProb >= tauHigh -> SpamRed
        currentProb <= tauLow -> SafeGreen
        else -> ReviewAmber
    }

    Box(
        modifier = modifier.size(200.dp, 120.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Canvas(modifier = Modifier.size(200.dp, 120.dp)) {
            val strokeWidth = 20.dp.toPx()
            val diameter = size.width - strokeWidth
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
            val arcSize = Size(diameter, diameter)

            // Background Track
            drawArc(
                color = Color.Gray.copy(alpha = 0.2f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Active Arc
            val sweep = currentProb * 180f
            drawArc(
                color = currentColor,
                startAngle = 180f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Draw Tau High / Low markers
            val center = Offset(size.width / 2f, size.width / 2f)
            val radius = diameter / 2f

            val tauLowAngle = Math.toRadians((180 + tauLow * 180).toDouble())
            val tauHighAngle = Math.toRadians((180 + tauHigh * 180).toDouble())

            val markerLen = 10.dp.toPx()

            // Tau Low Marker
            val t1X = center.x + (radius - markerLen) * cos(tauLowAngle).toFloat()
            val t1Y = center.y + (radius - markerLen) * sin(tauLowAngle).toFloat()
            val t2X = center.x + (radius + markerLen) * cos(tauLowAngle).toFloat()
            val t2Y = center.y + (radius + markerLen) * sin(tauLowAngle).toFloat()
            drawLine(Color.White, Offset(t1X, t1Y), Offset(t2X, t2Y), strokeWidth = 2.dp.toPx())

            // Tau High Marker
            val h1X = center.x + (radius - markerLen) * cos(tauHighAngle).toFloat()
            val h1Y = center.y + (radius - markerLen) * sin(tauHighAngle).toFloat()
            val h2X = center.x + (radius + markerLen) * cos(tauHighAngle).toFloat()
            val h2Y = center.y + (radius + markerLen) * sin(tauHighAngle).toFloat()
            drawLine(Color.White, Offset(h1X, h1Y), Offset(h2X, h2Y), strokeWidth = 2.dp.toPx())
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Text(
                text = "${(currentProb * 100).toInt()}%",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = currentColor
            )
            Text(
                text = "Spam Probability",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
