package com.spamsentinel.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spamsentinel.app.data.db.MessageEntity
import com.spamsentinel.app.domain.model.ScamType
import com.spamsentinel.app.ui.components.DonutChart
import com.spamsentinel.app.ui.components.HorizontalBarChart
import com.spamsentinel.app.ui.components.LineChart
import com.spamsentinel.app.ui.components.SectionHeader
import com.spamsentinel.app.ui.components.StatCard
import com.spamsentinel.app.ui.components.VerdictChip
import com.spamsentinel.app.ui.theme.DeepIndigoPrimary
import com.spamsentinel.app.ui.theme.ReviewAmber
import com.spamsentinel.app.ui.theme.SafeGreen
import com.spamsentinel.app.ui.theme.SpamRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onProtectionToggle: (Boolean) -> Unit,
    onNavigateToChecker: () -> Unit,
    onNavigateToMessages: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToChatbot: () -> Unit,
    onMessageClick: (Long) -> Unit,
    onMarkSafe: (Long) -> Unit,
    onBlockSender: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToChecker,
                containerColor = DeepIndigoPrimary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Live Checker")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Hero Protection Card with Pulsing Dot
            item {
                val transition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by transition.animateFloat(
                    initialValue = 0.9f,
                    targetValue = 1.25f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(
                        brush = Brush.horizontalGradient(
                            colors = if (uiState.protectionActive) listOf(DeepIndigoPrimary, Color(0xFF6366F1))
                            else listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.outline)
                        )
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (uiState.protectionActive) "Protection Active" else "Protection Paused",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        if (uiState.protectionActive) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .scale(pulseScale)
                                                    .clip(CircleShape)
                                                    .background(SafeGreen)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "On-device AI Shield • ${uiState.avgLatencyMs}ms latency",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }

                            Switch(
                                checked = uiState.protectionActive,
                                onCheckedChange = onProtectionToggle
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${uiState.protectionScore}",
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "/ 100",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "Security Index",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
            }

            // Sentinel AI Assistant Featured Banner
            item {
                Card(
                    onClick = onNavigateToChatbot,
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(DeepIndigoPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.SmartToy, contentDescription = "AI Assistant", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Sentinel AI Assistant",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = DeepIndigoPrimary, modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = "Ask questions, verify scams & get 100% offline security tips",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = "Open Chatbot", tint = DeepIndigoPrimary)
                    }
                }
            }

            // Quick Action Shortcuts Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionButton(
                        title = "AI Chatbot",
                        icon = Icons.Default.SmartToy,
                        onClick = onNavigateToChatbot,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionButton(
                        title = "Live Checker",
                        icon = Icons.Default.FlashOn,
                        onClick = onNavigateToChecker,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionButton(
                        title = "Settings",
                        icon = Icons.Default.Settings,
                        onClick = onNavigateToSettings,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Stat Cards Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Scanned",
                        count = uiState.scannedCount,
                        icon = Icons.Default.Security,
                        iconColor = DeepIndigoPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Blocked",
                        count = uiState.blockedCount,
                        icon = Icons.Default.GppBad,
                        iconColor = SpamRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Review Queue",
                        count = uiState.reviewCount,
                        icon = Icons.Default.GppMaybe,
                        iconColor = ReviewAmber,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Safe SMS",
                        count = uiState.safeCount,
                        icon = Icons.Default.CheckCircle,
                        iconColor = SafeGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Donut Chart Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader(title = "Detection Breakdown")
                        Spacer(modifier = Modifier.height(8.dp))
                        DonutChart(
                            slices = uiState.donutSlices,
                            centerTitle = "${uiState.scannedCount}",
                            centerSubtitle = "Total SMS"
                        )
                    }
                }
            }

            // 7-Day Spam Activity Line Chart
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader(title = "7-Day Spam Activity")
                        Spacer(modifier = Modifier.height(8.dp))
                        LineChart(
                            dataPoints = uiState.weeklyTrendPoints,
                            labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
                            lineColor = SpamRed
                        )
                    }
                }
            }

            // Top Scam Categories
            if (uiState.topScamTypes.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SectionHeader(title = "Top Scam Categories")
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalBarChart(items = uiState.topScamTypes)
                        }
                    }
                }
            }

            // Recent Detections List
            item {
                SectionHeader(
                    title = "Recent Detections",
                    actionText = "See All",
                    onActionClick = onNavigateToMessages
                )
            }

            items(uiState.recentDetections, key = { it.id }) { msg ->
                RecentDetectionRow(
                    message = msg,
                    onClick = { onMessageClick(msg.id) },
                    onMarkSafe = { onMarkSafe(msg.id) },
                    onBlockSender = { onBlockSender(msg.id) }
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun QuickActionButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = DeepIndigoPrimary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RecentDetectionRow(
    message: MessageEntity,
    onClick: () -> Unit,
    onMarkSafe: () -> Unit,
    onBlockSender: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message.sender,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dateFormat.format(Date(message.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message.body,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    VerdictChip(action = message.action, spamProbability = message.spamProbability)
                    if (message.scamType != ScamType.NONE) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${message.scamType.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
