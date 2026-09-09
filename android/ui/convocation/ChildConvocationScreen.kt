package com.nivya.ui.convocation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nivya.core.network.dto.ChildConvocationMessageDto
import com.nivya.ui.common.*
import com.nivya.ui.theme.*

/**
 * Child Convocation Screen.
 * Default view: Empty note-pad / text area.
 * No persistent conversation history is visible to the Child.
 * Top-right 3-dot options menu contains ONE single On/Off toggle.
 * When ON: all currently unread Parent messages become visible together with a 2-minute countdown.
 * Child NEVER sees the "Seen" status.
 * Child-sent messages disappear immediately upon server acknowledgement.
 */
@Composable
fun ChildConvocationScreen(
    viewModel: ChildConvocationViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top App Bar / Header with 3-dot Options Menu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "My Notepad",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Personal notes & quick memo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = TextPrimary
                    )
                }

                // Options Dropdown with ONE single On/Off toggle
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(SurfaceDark)
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Convocation Mode",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = if (uiState.isConvocationEnabled) "Active (2m timer)" else "Disabled",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (uiState.isConvocationEnabled) PurpleLight else TextMuted
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Switch(
                                    checked = uiState.isConvocationEnabled,
                                    onCheckedChange = { checked ->
                                        viewModel.toggleConvocationMode(checked)
                                        showMenu = false
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = PurpleAccent,
                                        checkedTrackColor = PurpleLight.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        },
                        onClick = {
                            viewModel.toggleConvocationMode(!uiState.isConvocationEnabled)
                            showMenu = false
                        }
                    )
                }
            }
        }

        // Active Viewing Session (Only visible when single toggle is ON and timer > 0)
        if (uiState.isViewingActive && uiState.remainingSeconds > 0 && uiState.activeMessages.isNotEmpty()) {
            ActiveViewingSection(
                remainingSeconds = uiState.remainingSeconds,
                messages = uiState.activeMessages
            )
        }

        // Default Main Area: EMPTY NOTE-PAD / TEXT AREA
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = SurfaceDark,
            border = BorderStroke(1.dp, OutlineDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Quick Memo & Notes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Type a note or message. Notes sent to family clear automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = uiState.noteText,
                    onValueChange = { viewModel.updateNoteText(it) },
                    placeholder = {
                        Text(
                            text = "Write your note here...",
                            color = TextMuted
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurpleAccent,
                        unfocusedBorderColor = OutlineDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.noteSentFeedback) {
                        Text(
                            text = "✓ Note sent to family",
                            style = MaterialTheme.typography.labelMedium,
                            color = SuccessGreen
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Button(
                        onClick = { viewModel.sendChildNote() },
                        enabled = uiState.noteText.isNotBlank() && !uiState.isSendingNote,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                    ) {
                        if (uiState.isSendingNote) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(text = "Send Note to Family")
                    }
                }
            }
        }
    }
}

/**
 * Ephemeral section displaying unread messages together with 2-minute countdown timer.
 * Note: Child NEVER sees the "Seen" status!
 */
@Composable
private fun ActiveViewingSection(
    remainingSeconds: Long,
    messages: List<ChildConvocationMessageDto>
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = PurpleAccent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Countdown Timer Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = PurpleLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Priority Messages",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PurpleLight
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = WarningAmber.copy(alpha = 0.2f)
                ) {
                    val minutes = remainingSeconds / 60
                    val seconds = remainingSeconds % 60
                    Text(
                        text = String.format("⏱ %02d:%02d", minutes, seconds),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = WarningAmber,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Message Cards (All unread messages visible together)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                messages.forEach { msg ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceDark,
                        border = BorderStroke(1.dp, OutlineDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = msg.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Delivered today",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}
