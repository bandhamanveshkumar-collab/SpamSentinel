package com.spamsentinel.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spamsentinel.app.ui.theme.DeepIndigoPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatbotScreen(
    uiState: ChatbotUiState,
    promptCategories: List<PromptCategory>,
    onInputTextChanged: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size, uiState.streamingText) {
        if (uiState.messages.isNotEmpty() || uiState.streamingText.isNotEmpty()) {
            val totalItems = uiState.messages.size + (if (uiState.isTyping) 1 else 0)
            if (totalItems > 0) {
                listState.animateScrollToItem(totalItems - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(DeepIndigoPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "Sentinel AI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = DeepIndigoPrimary, modifier = Modifier.size(16.dp))
                            }
                            Text(text = "Model: Sentinel-1.0-OnDevice • 100% Offline", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onClearChat) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Chat", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Chat Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                items(uiState.messages, key = { it.id }) { msg ->
                    ChatBubble(message = msg, context = context)
                }

                // Streaming Token Answer Effect (ChatGPT / Gemini style)
                if (uiState.isTyping) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Card(
                                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth(0.88f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = uiState.streamingText.ifEmpty { "Sentinel AI is thinking..." },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "Streaming answer...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            // Prompt Category Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                promptCategories.forEach { category ->
                    category.prompts.forEach { prompt ->
                        item {
                            FilterChip(
                                selected = false,
                                onClick = { onSendMessage(prompt) },
                                label = { Text(prompt, style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )
                        }
                    }
                }
            }

            // Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.inputText,
                    onValueChange = onInputTextChanged,
                    placeholder = { Text("Ask Sentinel AI anything...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { onSendMessage(uiState.inputText) },
                    enabled = uiState.inputText.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (uiState.inputText.isNotBlank()) DeepIndigoPrimary else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (uiState.inputText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, context: Context) {
    val isUser = message.sender == ChatSender.USER
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) DeepIndigoPrimary else MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth(if (isUser) 0.82f else 0.90f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isUser) {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Sentinel AI Answer", message.text)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Text(
                        text = dateFormat.format(Date(message.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
