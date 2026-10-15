package com.spamsentinel.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spamsentinel.app.domain.model.VerdictAction
import com.spamsentinel.app.ui.theme.ReviewAmber
import com.spamsentinel.app.ui.theme.ReviewAmberContainer
import com.spamsentinel.app.ui.theme.ReviewAmberText
import com.spamsentinel.app.ui.theme.SafeGreen
import com.spamsentinel.app.ui.theme.SafeGreenContainer
import com.spamsentinel.app.ui.theme.SafeGreenText
import com.spamsentinel.app.ui.theme.SpamRed
import com.spamsentinel.app.ui.theme.SpamRedContainer
import com.spamsentinel.app.ui.theme.SpamRedText

@Composable
fun VerdictChip(
    action: VerdictAction,
    spamProbability: Float? = null,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, icon, label) = when (action) {
        VerdictAction.BLOCK -> Quadruple(
            SpamRedContainer,
            SpamRedText,
            Icons.Default.GppBad,
            "SPAM"
        )
        VerdictAction.REVIEW -> Quadruple(
            ReviewAmberContainer,
            ReviewAmberText,
            Icons.Default.Warning,
            "REVIEW"
        )
        VerdictAction.ALLOW -> Quadruple(
            SafeGreenContainer,
            SafeGreenText,
            Icons.Default.CheckCircle,
            "SAFE"
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = textColor,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (spamProbability != null) "$label (${(spamProbability * 100).toInt()}%)" else label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
