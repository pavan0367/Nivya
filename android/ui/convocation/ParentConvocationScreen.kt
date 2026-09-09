package com.nivya.ui.convocation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nivya.core.network.dto.ParentConvocationMessageDto
import com.nivya.ui.common.*
import com.nivya.ui.theme.*

/**
 * Parent Convocation Screen displaying permanent retained conversation history,
 * Sent guidance messages with "Seen" indicators, and Child-originated notes.
 */
@Composable
fun ParentConvocationScreen(
    viewModel: ParentConvocationViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Convocation Guidance",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Permanent retained history with Alex",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            IconButton(onClick = { viewModel.loadHistory() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = PurpleLight
                )
            }
        }

        // Info Banner
        ConvocationParentBanner()

        // Message Stream Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (uiState.isLoading && uiState.messages.isEmpty()) {
                LoadingState(message = "Loading convocation history...")
            } else if (uiState.messages.isEmpty()) {
                EmptyState(
                    title = "No Convocation Messages",
                    description = "Send a priority message to your child. It will appear on their device when they open Convocation.",
                    icon = Icons.Default.Mail
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(uiState.messages) { message ->
                        ParentMessageBubble(message = message)
                    }
                }
            }
        }

        // Compose and Send Bar
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = SurfaceDark,
            border = BorderStroke(1.dp, OutlineDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.inputMessage,
                    onValueChange = { viewModel.updateInput(it) },
                    placeholder = { Text("Send priority message to child...", color = TextMuted) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurpleAccent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { viewModel.sendMessage() },
                    enabled = uiState.inputMessage.isNotBlank() && !uiState.isSending,
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = if (uiState.inputMessage.isNotBlank()) PurpleAccent else SurfaceVariantDark,
                            shape = CircleShape
                        )
                ) {
                    if (uiState.isSending) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (uiState.inputMessage.isNotBlank()) Color.White else TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConvocationParentBanner() {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = PurpleAccent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = PurpleLight,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Child receives a decoy reminder. Messages expire from child view after 2 minutes of opening.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun ParentMessageBubble(message: ParentConvocationMessageDto) {
    val isChild = message.childOriginated

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isChild) Arrangement.Start else Arrangement.End
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isChild) 4.dp else 16.dp,
                bottomEnd = if (isChild) 16.dp else 4.dp
            ),
            color = if (isChild) SurfaceVariantDark else PurpleAccent,
            border = if (isChild) BorderStroke(1.dp, OutlineDark) else null,
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (isChild) {
                    Text(
                        text = "From ${message.senderName}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = BlueLight
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(message.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isChild) TextMuted else Color.White.copy(alpha = 0.7f)
                    )

                    // Seen status appears ONLY on Parent side!
                    if (!isChild) {
                        Spacer(modifier = Modifier.width(6.dp))
                        if (message.seen) {
                            Text(
                                text = "Seen ✓",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        } else {
                            Text(
                                text = "Delivered",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(isoTime: String?): String {
    if (isoTime.isNullOrBlank()) return ""
    return try {
        if (isoTime.contains('T')) {
            isoTime.substringAfter('T').take(5)
        } else {
            isoTime
        }
    } catch (_: Exception) {
        ""
    }
}
