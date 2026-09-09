package com.nivya.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nivya.core.network.dto.HistoryEventDetailDto
import com.nivya.core.network.dto.HistoryEventDto
import com.nivya.ui.common.*
import com.nivya.ui.theme.*

/**
 * Parent-only History Screen.
 * Displays chronological activity records, broad activity classifications,
 * contact/document activity labels, and session durations with multi-dimensional
 * date and application filtering, bounded pagination, and granular event details.
 */
@Composable
fun ParentHistoryScreen(
    viewModel: ParentHistoryViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Activity History",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Chronological audit log • Alex's Galaxy A54",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            IconButton(onClick = { viewModel.refresh() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = BluePrimary
                )
            }
        }

        // Privacy Shield Reassurance Banner
        HistoryPrivacyBanner()

        // Stale Data Indicator
        if (uiState.isStale) {
            StaleDataIndicator(
                reason = "Displaying cached / offline history events",
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Date Filter Chips
        DateFilterSection(
            selectedPreset = uiState.dateFilter,
            onSelectPreset = { viewModel.filterByDate(it) }
        )

        // Application Filter Chips
        ApplicationFilterSection(
            availableApps = uiState.availableApps,
            selectedApp = uiState.selectedApp,
            onSelectApp = { viewModel.filterByApplication(it) }
        )

        // Content Area
        if (uiState.isLoading) {
            LoadingState(message = "Loading activity history...")
        } else if (uiState.errorMessage != null) {
            ErrorState(
                message = uiState.errorMessage ?: "Failed to load history",
                onRetry = { viewModel.refresh() }
            )
        } else if (uiState.items.isEmpty()) {
            EmptyState(
                title = "No Activity Records",
                description = "No activity events recorded for the selected date range and application filters.",
                icon = Icons.Default.History
            )
        } else {
            // Chronological Event Cards
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                uiState.items.forEach { event ->
                    HistoryEventCard(
                        event = event,
                        onClick = { viewModel.selectEvent(event.id) }
                    )
                }
            }

            // Pagination Controls
            PaginationControls(
                currentPage = uiState.currentPage,
                totalPages = uiState.totalPages,
                totalElements = uiState.totalElements,
                hasPrevious = uiState.hasPrevious,
                hasNext = uiState.hasNext,
                onPreviousClick = { viewModel.previousPage() },
                onNextClick = { viewModel.nextPage() }
            )
        }
    }

    // Event Detail Dialog
    uiState.selectedEventDetail?.let { detail ->
        HistoryDetailDialog(
            detail = detail,
            onDismiss = { viewModel.dismissDetail() }
        )
    }
}

/**
 * Privacy Shield explaining legitimate telemetry and zero message/call eavesdropping.
 */
@Composable
private fun HistoryPrivacyBanner() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = BluePrimary.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, BluePrimary.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = BluePrimary.copy(alpha = 0.2f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = BluePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Parent-Only Audit Log",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = BlueLight
                )
                Text(
                    text = "Only authorized activity categories and durations are collected. Message contents, passwords, and calls are never stored.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

/**
 * Filter row for presets: All Time, Today, Last 7 Days, Last 30 Days.
 */
@Composable
private fun DateFilterSection(
    selectedPreset: DatePreset,
    onSelectPreset: (DatePreset) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Time Range",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DatePreset.values().forEach { preset ->
                FilterChip(
                    selected = selectedPreset == preset,
                    onClick = { onSelectPreset(preset) },
                    label = { Text(preset.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BluePrimary,
                        selectedLabelColor = Color.White,
                        containerColor = SurfaceDark,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedPreset == preset,
                        borderColor = OutlineDark,
                        selectedBorderColor = BluePrimary
                    )
                )
            }
        }
    }
}

/**
 * Filter row for dynamic applications (All + WhatsApp, Chrome, YouTube, Files, Phone, etc.).
 */
@Composable
private fun ApplicationFilterSection(
    availableApps: List<String>,
    selectedApp: String?,
    onSelectApp: (String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Application",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // "All Apps" option
            FilterChip(
                selected = selectedApp == null,
                onClick = { onSelectApp(null) },
                label = { Text("All Apps") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PurpleAccent,
                    selectedLabelColor = Color.White,
                    containerColor = SurfaceDark,
                    labelColor = TextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedApp == null,
                    borderColor = OutlineDark,
                    selectedBorderColor = PurpleAccent
                )
            )

            availableApps.forEach { app ->
                FilterChip(
                    selected = selectedApp.equals(app, ignoreCase = true),
                    onClick = {
                        if (selectedApp.equals(app, ignoreCase = true)) {
                            onSelectApp(null)
                        } else {
                            onSelectApp(app)
                        }
                    },
                    label = { Text(app) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PurpleAccent,
                        selectedLabelColor = Color.White,
                        containerColor = SurfaceDark,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedApp.equals(app, ignoreCase = true),
                        borderColor = OutlineDark,
                        selectedBorderColor = PurpleAccent
                    )
                )
            }
        }
    }
}

/**
 * Card representing an individual chronological history event with app icon,
 * broad activity title, duration tag, contact/document label, and timestamp.
 */
@Composable
private fun HistoryEventCard(
    event: HistoryEventDto,
    onClick: () -> Unit
) {
    val icon = resolveAppIcon(event.packageName, event.appName)
    val appColor = resolveAppColor(event.packageName, event.appName)

    NivyaCard(
        containerColor = SurfaceDark,
        borderColor = OutlineDark,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon Surface
            Surface(
                shape = CircleShape,
                color = appColor.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = appColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Main Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = event.appName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    // Duration pill
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SurfaceVariantDark
                    ) {
                        Text(
                            text = "⏱ ${event.durationFormatted}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = BlueLight,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Broad Activity
                Text(
                    text = event.broadActivity,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Footer row with activity label and timestamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!event.activityLabel.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = BluePrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "🏷 ${event.activityLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = BlueLight,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Text(
                            text = event.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }

                    Text(
                        text = formatHistoryTimestamp(event.eventTimestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Details",
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Bounded pagination controls with Prev/Next buttons, current page, and total elements.
 */
@Composable
private fun PaginationControls(
    currentPage: Int,
    totalPages: Int,
    totalElements: Long,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, OutlineDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onPreviousClick,
                enabled = hasPrevious,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = BlueLight,
                    disabledContentColor = TextMuted
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Previous", style = MaterialTheme.typography.labelMedium)
            }

            Text(
                text = "Page ${currentPage + 1} of ${maxOf(1, totalPages)} ($totalElements items)",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )

            OutlinedButton(
                onClick = onNextClick,
                enabled = hasNext,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = BlueLight,
                    disabledContentColor = TextMuted
                )
            ) {
                Text("Next", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Granular detail dialog for an activity history event.
 */
@Composable
private fun HistoryDetailDialog(
    detail: HistoryEventDetailDto,
    onDismiss: () -> Unit
) {
    val icon = resolveAppIcon(detail.packageName, detail.appName)
    val appColor = resolveAppColor(detail.packageName, detail.appName)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = appColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = appColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = detail.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = detail.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                DetailRow(label = "Broad Activity", value = detail.broadActivity)

                if (!detail.activityLabel.isNullOrBlank()) {
                    DetailRow(label = "Activity Label", value = detail.activityLabel)
                }

                DetailRow(
                    label = "Duration",
                    value = "${detail.durationFormatted} (${detail.durationSeconds} seconds)"
                )

                DetailRow(label = "Category", value = detail.category)
                DetailRow(label = "Recorded At", value = detail.eventTimestamp)

                if (!detail.details.isNullOrBlank()) {
                    HorizontalDivider(color = OutlineDark, modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Consented Scope",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = detail.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) {
                Text("Close")
            }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}

private fun resolveAppIcon(packageName: String, appName: String): ImageVector {
    val lower = (packageName + " " + appName).lowercase()
    return when {
        lower.contains("whatsapp") || lower.contains("chat") || lower.contains("message") -> Icons.Default.Message
        lower.contains("chrome") || lower.contains("browser") || lower.contains("firefox") -> Icons.Default.Language
        lower.contains("youtube") || lower.contains("video") || lower.contains("netflix") -> Icons.Default.PlayCircle
        lower.contains("file") || lower.contains("docs") || lower.contains("pdf") -> Icons.Default.Description
        lower.contains("phone") || lower.contains("call") || lower.contains("dialer") -> Icons.Default.Phone
        else -> Icons.Default.Apps
    }
}

private fun resolveAppColor(packageName: String, appName: String): Color {
    val lower = (packageName + " " + appName).lowercase()
    return when {
        lower.contains("whatsapp") -> Color(0xFF25D366)
        lower.contains("chrome") -> Color(0xFF4285F4)
        lower.contains("youtube") -> Color(0xFFFF0000)
        lower.contains("file") || lower.contains("docs") -> Color(0xFFFBBC05)
        lower.contains("phone") -> Color(0xFF34A853)
        else -> BluePrimary
    }
}

private fun formatHistoryTimestamp(isoTime: String?): String {
    if (isoTime.isNullOrBlank()) return "Recent"
    return try {
        if (isoTime.contains('T')) {
            val datePart = isoTime.substringBefore('T')
            val timePart = isoTime.substringAfter('T').take(5)
            "$datePart $timePart"
        } else {
            isoTime
        }
    } catch (_: Exception) {
        "Recent"
    }
}
