package com.spamsentinel.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spamsentinel.app.ui.theme.DeepIndigoPrimary
import com.spamsentinel.app.ui.theme.SafeGreen
import com.spamsentinel.app.ui.theme.SpamRed

@Composable
fun Heatmap2x2(
    cm: List<List<Int>>, // [[TN, FP], [FN, TP]]
    modifier: Modifier = Modifier
) {
    if (cm.size < 2 || cm[0].size < 2) return

    val tn = cm[0][0]
    val fp = cm[0][1]
    val fn = cm[1][0]
    val tp = cm[1][1]
    val total = (tn + fp + fn + tp).coerceAtLeast(1)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Confusion Matrix",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            // TN
            HeatmapCell(
                title = "TN (True Ham)",
                count = tn,
                percentage = (tn.toFloat() / total) * 100f,
                baseColor = SafeGreen,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            // FP
            HeatmapCell(
                title = "FP (False Alarm)",
                count = fp,
                percentage = (fp.toFloat() / total) * 100f,
                baseColor = SpamRed,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            // FN
            HeatmapCell(
                title = "FN (Missed Spam)",
                count = fn,
                percentage = (fn.toFloat() / total) * 100f,
                baseColor = SpamRed,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            // TP
            HeatmapCell(
                title = "TP (True Spam)",
                count = tp,
                percentage = (tp.toFloat() / total) * 100f,
                baseColor = DeepIndigoPrimary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HeatmapCell(
    title: String,
    count: Int,
    percentage: Float,
    baseColor: Color,
    modifier: Modifier = Modifier
) {
    val alpha = (percentage / 100f).coerceIn(0.15f, 0.85f)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(baseColor.copy(alpha = alpha))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.9f)
            )
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = String.format("%.1f%%", percentage),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}
