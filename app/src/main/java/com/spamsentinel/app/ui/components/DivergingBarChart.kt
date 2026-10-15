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
import com.spamsentinel.app.domain.model.TermWeight
import com.spamsentinel.app.ui.theme.SafeGreen
import com.spamsentinel.app.ui.theme.SpamRed
import kotlin.math.abs

@Composable
fun DivergingBarChart(
    spamTerms: List<TermWeight>,
    hamTerms: List<TermWeight>,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(spamTerms, hamTerms) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    val maxAbsWeight = (spamTerms.map { abs(it.weight) } + hamTerms.map { abs(it.weight) })
        .maxOrNull()?.coerceAtLeast(0.1f) ?: 1.0f

    val allTerms = (spamTerms + hamTerms).sortedByDescending { abs(it.weight) }

    Column(modifier = modifier.fillMaxWidth()) {
        allTerms.forEach { termWeight ->
            val isSpam = termWeight.weight > 0f
            val absVal = abs(termWeight.weight)

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = termWeight.term,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(90.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))

                Box(modifier = Modifier.weight(1f).height(18.dp)) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(18.dp)) {
                        val centerX = size.width / 2f
                        val barWidth = (absVal / maxAbsWeight) * (size.width / 2f) * progress.value

                        // Draw center axis
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.4f),
                            start = Offset(centerX, 0f),
                            end = Offset(centerX, size.height),
                            strokeWidth = 1.dp.toPx()
                        )

                        if (isSpam) {
                            drawRoundRect(
                                color = SpamRed,
                                topLeft = Offset(centerX, 2.dp.toPx()),
                                size = Size(barWidth, size.height - 4.dp.toPx()),
                                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                            )
                        } else {
                            drawRoundRect(
                                color = SafeGreen,
                                topLeft = Offset(centerX - barWidth, 2.dp.toPx()),
                                size = Size(barWidth, size.height - 4.dp.toPx()),
                                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = String.format("%.2f", termWeight.weight),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSpam) SpamRed else SafeGreen,
                    modifier = Modifier.width(45.dp)
                )
            }
        }
    }
}
