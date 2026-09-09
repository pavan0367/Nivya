package com.nivya.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nivya.core.network.dto.ActivityEventDto
import com.nivya.ui.common.*
import com.nivya.ui.theme.*

/**
 * Parent-only Live Activity Screen.
 * Displays legitimate real-time application activity, broad contextual states,
 * active elapsed duration, and chronological activity history with strict privacy guardrails.
 */
@Composable
fun ParentLiveActivityScreen(
    viewModel: ParentLiveActivityViewModel,
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
        // Top Header with Device Name & Refresh
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Live Activity",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = uiState.deviceName,
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
        PrivacyShieldBanner()

        if (uiState.isLoading) {
            LoadingState(message = "Synchronizing live activity...")
        } else if (uiState.errorMessage != null) {
            ErrorState(
                message = uiState.errorMessage ?: "Failed to load live activity",
                onRetry = { viewModel.refresh() }
            )
        } else {
            // Live Pulsing Indicator & Status Overview
            LivePulseHeader(isOnline = uiState.isOnline)

            // Current Foreground Activity Hero Card
            val current = uiState.currentActivity
            if (current != null) {
                CurrentActivityHeroCard(activity = current)
            } else {
                EmptyState(
                    title = "No Active Application",
                    description = "Device is idle or screen is currently locked.",
                    icon = Icons.Default.Apps
                )
            }

            // Chronological Activity Timeline
            SectionHeader(title = "Chronological Activity Timeline")

            if (uiState.recentActivities.isEmpty()) {
                EmptyState(
                    title = "No Recent Activities",
                    description = "Recent application activities will appear here in chronological order.",
                    icon = Icons.Default.Schedule
                )
            } else {
                    uiState.recentActivities.forEach { activity ->
                        ActivityTimelineItem(activity = activity)
                    }
            }

            if (uiState.isStale) {
                StaleDataIndicator(reason = "Showing cached activity history. Live stream reconnecting...")
            }
        }
    }
}

/**
 * Animated Live Pulsing Header badge indicating real-time stream status.
 */
@Composable
private fun LivePulseHeader(isOnline: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.dp, OutlineDark, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        if (isOnline) SuccessGreen.copy(alpha = alpha) else TextMuted
                    )
            )
            Text(
                text = if (isOnline) "LIVE NOW" else "OFFLINE",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isOnline) SuccessGreen else TextMuted
            )
        }

        Text(
            text = "Parent View Only",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

/**
 * Hero Card highlighting the current foreground application and broad activity.
 */
@Composable
private fun CurrentActivityHeroCard(activity: ActivityEventDto) {
    val categoryColor = getCategoryColor(activity.category)
    val appIcon = getAppIcon(activity.packageName, activity.appName)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            brush = Brush.horizontalGradient(
                listOf(categoryColor.copy(alpha = 0.8f), BluePrimary.copy(alpha = 0.4f))
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Category Badge & Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = categoryColor.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = activity.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = categoryColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (activity.durationFormatted != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Duration",
                            tint = BluePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Active for ${activity.durationFormatted}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = BluePrimary
                        )
                    }
                }
            }

            // App Name & Broad Activity
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(categoryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = appIcon,
                        contentDescription = activity.appName,
                        tint = categoryColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activity.appName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = activity.broadActivity,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = categoryColor
                    )
                }
            }

            HorizontalDivider(color = OutlineDark, thickness = 0.8.dp)

            // High-level context & legitimate guarantee
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "High-level broad context only",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Text(
                    text = formatShortTime(activity.startedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

/**
 * Item in the Chronological Activity Timeline.
 */
@Composable
private fun ActivityTimelineItem(
    activity: ActivityEventDto
) {
    val categoryColor = getCategoryColor(activity.category)
    val appIcon = getAppIcon(activity.packageName, activity.appName)

    NivyaCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = appIcon,
                    contentDescription = activity.appName,
                    tint = categoryColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activity.appName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = formatShortTime(activity.startedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activity.broadActivity,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    if (activity.durationFormatted != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SurfaceVariantDark
                        ) {
                            Text(
                                text = activity.durationFormatted,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Privacy Shield Reassurance Banner.
 */
@Composable
private fun PrivacyShieldBanner() {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = BluePrimary.copy(alpha = 0.10f),
        border = androidx.compose.foundation.BorderStroke(1.dp, BluePrimary.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Privacy Shield",
                tint = BluePrimary,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = "Privacy Safeguards Active",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
                )
                Text(
                    text = "Message bodies, passwords, calls, and microphone audio are never recorded or intercepted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

private fun getCategoryColor(category: String): Color {
    return when (category.uppercase()) {
        "COMMUNICATION" -> Color(0xFF25D366) // Green accent
        "BROWSING" -> BluePrimary
        "ENTERTAINMENT" -> Color(0xFFFF0033) // Red YouTube accent
        "PRODUCTIVITY" -> Color(0xFFFFB300) // Amber accent
        "EDUCATION" -> Color(0xFF58CC02) // Duolingo green
        else -> TextSecondary
    }
}

private fun getAppIcon(packageName: String, appName: String): ImageVector {
    val lower = (packageName + " " + appName).lowercase()
    return when {
        lower.contains("whatsapp") || lower.contains("chat") || lower.contains("message") -> Icons.Default.Chat
        lower.contains("chrome") || lower.contains("browser") || lower.contains("firefox") -> Icons.Default.Language
        lower.contains("youtube") || lower.contains("video") || lower.contains("netflix") -> Icons.Default.PlayCircle
        lower.contains("file") || lower.contains("docs") || lower.contains("pdf") -> Icons.Default.Description
        lower.contains("phone") || lower.contains("call") || lower.contains("dialer") -> Icons.Default.Phone
        else -> Icons.Default.Apps
    }
}

private fun formatShortTime(isoTime: String?): String {
    if (isoTime.isNullOrBlank()) return "Just now"
    return try {
        // Extract HH:mm from ISO timestamp e.g. 2026-09-09T09:25:00Z
        if (isoTime.contains('T')) {
            val timePart = isoTime.substringAfter('T').take(5)
            timePart
        } else {
            "Recent"
        }
    } catch (_: Exception) {
        "Recent"
    }
}
