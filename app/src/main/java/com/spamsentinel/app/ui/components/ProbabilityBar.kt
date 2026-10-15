package com.spamsentinel.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.spamsentinel.app.ui.theme.ReviewAmber
import com.spamsentinel.app.ui.theme.SafeGreen
import com.spamsentinel.app.ui.theme.SpamRed

@Composable
fun ProbabilityBar(
    probability: Float, // 0.0 to 1.0
    modifier: Modifier = Modifier,
    tauLow: Float = 0.20f,
    tauHigh: Float = 0.80f
) {
    val animatedProb = remember { Animatable(0f) }

    LaunchedEffect(probability) {
        animatedProb.animateTo(
            targetValue = probability.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 800)
        )
    }

    val currentProb = animatedProb.value
    val barColor = when {
        currentProb >= tauHigh -> SpamRed
        currentProb <= tauLow -> SafeGreen
        else -> ReviewAmber
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
    ) {
        val width = size.width
        val height = size.height

        // Track
        drawRoundRect(
            color = Color.Gray.copy(alpha = 0.2f),
            size = size,
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        // Active Bar
        drawRoundRect(
            color = barColor,
            topLeft = Offset(0f, 0f),
            size = Size(width * currentProb, height),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
    }
}
