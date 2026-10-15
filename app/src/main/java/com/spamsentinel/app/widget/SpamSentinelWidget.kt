package com.spamsentinel.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.spamsentinel.app.ui.theme.DarkBackground
import com.spamsentinel.app.ui.theme.SafeGreen

class SpamSentinelWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent()
        }
    }

    @Composable
    private fun WidgetContent() {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(DarkBackground))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SpamSentinel Active",
                style = TextStyle(
                    color = ColorProvider(SafeGreen),
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = "100% On-Device AI Protection",
                style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)))
            )
        }
    }
}

class SpamSentinelWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SpamSentinelWidget()
}
