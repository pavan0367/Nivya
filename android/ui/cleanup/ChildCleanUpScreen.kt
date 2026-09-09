package com.nivya.ui.cleanup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nivya.services.cleanup.CleanUpCategory
import com.nivya.ui.common.*
import com.nivya.ui.theme.*

/**
 * Child Clean Up Screen allowing child users to safely purge temporary cache
 * with mandatory confirmation, real-time progress, and clear safety guardrails.
 */
@Composable
fun ChildCleanUpScreen(
    viewModel: ChildCleanUpViewModel? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val uiState by viewModel?.uiState?.collectAsState() ?: androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(ChildCleanUpUiState())
    }

    val selectedBytes = uiState.selectedEstimatedBytes

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with Refresh
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(title = "Safe Storage Clean Up")
            IconButton(
                onClick = { viewModel?.scanStorage() },
                enabled = !uiState.isCleaning && !uiState.isScanning
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh scan",
                    tint = BluePrimary
                )
            }
        }

        // Post-Cleanup Result Card if completed
        uiState.cleanUpResult?.let { result ->
            NivyaCard(borderColor = SuccessGreen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Clean Up Complete! 🎉",
                            style = MaterialTheme.typography.titleMedium,
                            color = SuccessGreen
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Safely removed ${formatBytes(result.freedBytes)} of temporary cache (${result.deletedFilesCount} files).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                    IconButton(onClick = { viewModel?.dismissResult() }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", tint = TextSecondary)
                    }
                }
            }
        }

        // Error message if any
        uiState.errorMessage?.let { error ->
            NivyaCard(borderColor = ErrorRed) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Error, contentDescription = null, tint = ErrorRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = error, style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                }
            }
        }

        // In-Progress Cleanup Card
        if (uiState.isCleaning) {
            NivyaCard(borderColor = BluePrimary) {
                Text(
                    text = "Cleaning up temporary files...",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { uiState.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = BluePrimary,
                    trackColor = SurfaceVariantDark
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${(uiState.progress * 100).toInt()}% completed",
                    style = MaterialTheme.typography.labelSmall,
                    color = BlueLight
                )
            }
        }

        // Cleanup Categories Header & List
        SectionHeader(title = "Removable Data Categories")

        if (uiState.isScanning) {
            NivyaCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = BluePrimary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "Scanning safe cache storage...", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        } else {
            for (category in uiState.categories) {
                CategoryItemCard(
                    category = category,
                    onToggle = { viewModel?.toggleCategory(category.id) },
                    onOpenSettings = { viewModel?.openSystemStorageSettings() },
                    isCleaning = uiState.isCleaning
                )
            }
        }

        // Main Clean Up Button
        val canClean = uiState.hasSelectedSupportedCategories && !uiState.isCleaning && !uiState.isScanning
        Button(
            onClick = { viewModel?.requestConfirmation() },
            enabled = canClean,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SuccessGreen,
                disabledContainerColor = SurfaceVariantDark
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Icon(imageVector = Icons.Default.CleaningServices, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (selectedBytes > 0) "Clean Up ${formatBytes(selectedBytes)}" else "Clean Up Temporary Cache",
                style = MaterialTheme.typography.titleSmall
            )
        }

        // Safety Guarantee Box
        SectionHeader(title = "Strict Safety Guarantee")
        NivyaCard {
            Text(
                text = "What is protected?",
                style = MaterialTheme.typography.titleSmall,
                color = SuccessGreen
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "✓ Photos, camera rolls, and videos are NEVER deleted", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text(text = "✓ Documents, downloads, and school files are NEVER touched", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text(text = "✓ Personal game saves, messages, and app logins remain 100% safe", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text(text = "✓ Only temporary cache buffers and scratch files are cleaned", style = MaterialTheme.typography.bodySmall, color = SuccessGreen)
        }
    }

    // Pre-Cleanup Confirmation Dialog
    if (uiState.showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel?.dismissConfirmation() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CleaningServices, contentDescription = null, tint = SuccessGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Confirm Clean Up")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Are you ready to clean ${formatBytes(selectedBytes)} of temporary cache?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "This will only remove temporary buffers. Your photos, videos, game saves, and personal files will NOT be touched.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel?.executeCleanUp() },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Text("Confirm & Clean")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel?.dismissConfirmation() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CategoryItemCard(
    category: CleanUpCategory,
    onToggle: () -> Unit,
    onOpenSettings: () -> Unit,
    isCleaning: Boolean
) {
    NivyaCard(
        borderColor = if (category.isSupported && category.isSelected) BluePrimary else OutlineDark
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (category.isSupported) {
                        Checkbox(
                            checked = category.isSelected,
                            onCheckedChange = { onToggle() },
                            enabled = !isCleaning,
                            colors = CheckboxDefaults.colors(checkedColor = BluePrimary)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Unsupported",
                            tint = WarningAmber,
                            modifier = Modifier
                                .padding(12.dp)
                                .size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = category.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                if (category.isSupported) {
                    Text(
                        text = formatBytes(category.estimatedBytes),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (category.estimatedBytes > 0) BlueLight else TextMuted
                    )
                } else {
                    SuggestionChip(
                        onClick = onOpenSettings,
                        label = { Text("Settings", style = MaterialTheme.typography.labelSmall) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = SurfaceVariantDark,
                            labelColor = BlueLight
                        )
                    )
                }
            }

            if (!category.isSupported && category.unsupportedReason != null) {
                Text(
                    text = category.unsupportedReason,
                    style = MaterialTheme.typography.labelSmall,
                    color = WarningAmber,
                    modifier = Modifier.padding(start = 40.dp, end = 8.dp)
                )
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format("%.2f GB", gb)
        mb >= 1.0 -> String.format("%.1f MB", mb)
        kb >= 1.0 -> String.format("%.1f KB", kb)
        else -> "$bytes B"
    }
}
