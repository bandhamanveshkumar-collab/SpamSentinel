package com.spamsentinel.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spamsentinel.app.data.db.MessageEntity
import com.spamsentinel.app.domain.model.ScamType
import com.spamsentinel.app.domain.model.VerdictAction
import com.spamsentinel.app.ui.components.EmptyState
import com.spamsentinel.app.ui.components.VerdictChip
import com.spamsentinel.app.ui.theme.DeepIndigoPrimary
import com.spamsentinel.app.ui.theme.ReviewAmber
import com.spamsentinel.app.ui.theme.SafeGreen
import com.spamsentinel.app.ui.theme.SpamRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessagesScreen(
    uiState: MessagesUiState,
    onTabSelected: (MessagesTab) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onScamTypeFilterChanged: (ScamType?) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onClearSelection: () -> Unit,
    onBulkMarkSafe: () -> Unit,
    onBulkBlockSender: () -> Unit,
    onBulkDelete: () -> Unit,
    onMessageClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Multi-select bar or Search bar
        if (uiState.isMultiSelectMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepIndigoPrimary)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${uiState.selectedIds.size} Selected",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Row {
                    IconButton(onClick = onBulkMarkSafe) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Mark Safe", tint = Color.White)
                    }
                    IconButton(onClick = onBulkBlockSender) {
                        Icon(Icons.Default.Block, contentDescription = "Block Sender", tint = Color.White)
                    }
                    IconButton(onClick = onBulkDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search sender or content...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
        }

        // Tabs Row
        ScrollableTabRow(
            selectedTabIndex = uiState.selectedTab.ordinal,
            edgePadding = 16.dp
        ) {
            MessagesTab.entries.forEach { tab ->
                Tab(
                    selected = uiState.selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    text = {
                        Text(
                            text = when (tab) {
                                MessagesTab.ALL -> "All"
                                MessagesTab.SPAM -> "Spam"
                                MessagesTab.SAFE -> "Safe"
                                MessagesTab.REVIEW -> "Review"
                                MessagesTab.QUARANTINE -> "Quarantine"
                            },
                            fontWeight = if (uiState.selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // Scam Type Filter Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = uiState.scamTypeFilter == null,
                    onClick = { onScamTypeFilterChanged(null) },
                    label = { Text("All Categories") }
                )
            }
            items(ScamType.entries.filter { it != ScamType.NONE }) { scam ->
                FilterChip(
                    selected = uiState.scamTypeFilter == scam,
                    onClick = { onScamTypeFilterChanged(if (uiState.scamTypeFilter == scam) null else scam) },
                    label = { Text(scam.displayName) }
                )
            }
        }

        // Messages List
        if (uiState.messages.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Inbox,
                title = "No Messages Found",
                description = "No SMS messages match the selected filters or search query."
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.messages, key = { it.id }) { msg ->
                    MessageRowItem(
                        message = msg,
                        isSelected = uiState.selectedIds.contains(msg.id),
                        isMultiSelectMode = uiState.isMultiSelectMode,
                        privacyBlur = uiState.privacyBlur,
                        onClick = {
                            if (uiState.isMultiSelectMode) onToggleSelection(msg.id)
                            else onMessageClick(msg.id)
                        },
                        onLongClick = { onToggleSelection(msg.id) }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun MessageRowItem(
    message: MessageEntity,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    privacyBlur: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    val avatarColor = when (message.action) {
        VerdictAction.BLOCK -> SpamRed
        VerdictAction.REVIEW -> ReviewAmber
        VerdictAction.ALLOW -> SafeGreen
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isMultiSelectMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() }
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(avatarColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = message.sender.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = avatarColor,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = if (privacyBlur) Modifier.blur(8.dp) else Modifier
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    VerdictChip(action = message.action, spamProbability = message.spamProbability)
                    if (message.scamType != ScamType.NONE) {
                        Spacer(modifier = Modifier.width(8.dp))
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
