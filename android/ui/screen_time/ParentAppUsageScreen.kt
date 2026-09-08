package com.nivya.ui.screen_time

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nivya.core.network.dto.AppUsageDetailDto
import com.nivya.core.network.dto.DailyUsagePointDto
import com.nivya.ui.common.*
import com.nivya.ui.theme.*

/**
 * Parent App Usage Screen presenting detailed application analytics,
 * category distribution, and weekly trends for the child device.
 */
@Composable
fun ParentAppUsageScreen(
    viewModel: ParentAppUsageViewModel,
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
                SectionHeader(title = "App Usage & Screen Time")
                Text(
                    text = uiState.deviceName,
                    style = MaterialTheme.typography.bodySmall,
                    color = BlueLight
                )
            }

            IconButton(
                onClick = { viewModel.loadData() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh analytics",
                    tint = BlueLight
                )
            }
        }

        if (uiState.isLoading && uiState.summary == null) {
            LoadingState(message = "Loading application usage insights...")
            return
        }

        uiState.errorMessage?.let { error ->
            ErrorState(
                message = error,
                onRetry = { viewModel.loadData() }
            )
        }

        val summary = uiState.summary

        // Summary Metric Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Total Screen Time",
                value = summary?.formattedTotalTime ?: "0m",
                unit = "today",
                icon = Icons.Default.Timer,
                accentColor = BluePrimary,
                modifier = Modifier.weight(1f)
            )

            val eduPct = if (summary != null && summary.totalForegroundSeconds > 0) {
                ((summary.educationalSeconds.toDouble() / summary.totalForegroundSeconds) * 100).toInt()
            } else 0

            StatCard(
                title = "Educational",
                value = formatSeconds(summary?.educationalSeconds ?: 0L),
                unit = "$eduPct% balance",
                icon = Icons.Default.School,
                accentColor = SuccessGreen,
                modifier = Modifier.weight(1f)
            )
        }

        // 7-Day Usage Trends Card
        SectionHeader(title = "Weekly Usage Trends")
        val trends = uiState.trends
        NivyaCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Weekly Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = trends?.trendDescription ?: "Comparing with previous 7 days",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                trends?.let { tr ->
                    val isIncrease = tr.percentageChange > 0
                    val badgeColor = if (isIncrease) WarningAmber else SuccessGreen
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = badgeColor.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isIncrease) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${Math.abs(tr.percentageChange)}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 7-day daily bars
            val dailyPoints = trends?.dailyPoints ?: emptyList()
            if (dailyPoints.isNotEmpty()) {
                val maxSeconds = (dailyPoints.maxOfOrNull { it.totalSeconds } ?: 1L).coerceAtLeast(1L).toFloat()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    dailyPoints.takeLast(7).forEach { point ->
                        DailyBar(point = point, maxSeconds = maxSeconds)
                    }
                }
            } else {
                Text(
                    text = "No previous week telemetry points available yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }

        // Category Usage Breakdown
        SectionHeader(title = "Category Breakdown")
        NivyaCard {
            val totalSec = summary?.totalForegroundSeconds ?: 1L
            val safeTotal = if (totalSec > 0) totalSec.toFloat() else 1f

            val edu = summary?.educationalSeconds ?: 0L
            val rec = summary?.recreationalSeconds ?: 0L
            val soc = summary?.socialSeconds ?: 0L
            val prod = summary?.productivitySeconds ?: 0L

            CategoryProgressRow("Educational Apps", formatSeconds(edu), (edu / safeTotal).coerceIn(0f, 1f), SuccessGreen)
            Spacer(modifier = Modifier.height(10.dp))
            CategoryProgressRow("Games & Entertainment", formatSeconds(rec), (rec / safeTotal).coerceIn(0f, 1f), BluePrimary)
            Spacer(modifier = Modifier.height(10.dp))
            CategoryProgressRow("Social & Messaging", formatSeconds(soc), (soc / safeTotal).coerceIn(0f, 1f), WarningAmber)
            Spacer(modifier = Modifier.height(10.dp))
            CategoryProgressRow("Productivity & Study", formatSeconds(prod), (prod / safeTotal).coerceIn(0f, 1f), Color(0xFFAB47BC))
        }

        // Detailed Application List
        SectionHeader(title = "Top Applications Today")
        val apps = uiState.appUsage?.apps ?: emptyList()
        if (apps.isEmpty()) {
            NivyaCard {
                Text(
                    text = "No applications reported foreground usage today.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        } else {
            apps.take(15).forEach { app ->
                ParentAppUsageCard(app = app)
            }
        }
    }
}

@Composable
private fun DailyBar(
    point: DailyUsagePointDto,
    maxSeconds: Float
) {
    val barHeightRatio = (point.totalSeconds.toFloat() / maxSeconds).coerceIn(0.1f, 1f)
    val dayLabel = point.date.substringAfterLast('-')

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.width(36.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
            color = if (point.educationalSeconds > (point.totalSeconds / 2)) SuccessGreen else BluePrimary,
            modifier = Modifier
                .width(18.dp)
                .height((80 * barHeightRatio).dp)
        ) {}

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = dayLabel,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun CategoryProgressRow(
    label: String,
    duration: String,
    fraction: Float,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            Text(text = duration, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = color,
            trackColor = SurfaceVariantDark
        )
    }
}

@Composable
private fun ParentAppUsageCard(app: AppUsageDetailDto) {
    val accentColor = when (app.category) {
        "EDUCATION" -> SuccessGreen
        "GAMES", "ENTERTAINMENT" -> BluePrimary
        "SOCIAL" -> WarningAmber
        else -> TextMuted
    }

    NivyaCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "${app.formattedDuration} • ${app.category.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = app.formattedDuration,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = BlueLight
                )
                Text(
                    text = "${app.percentageOfTotal.toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { (app.percentageOfTotal / 100.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = accentColor,
            trackColor = SurfaceVariantDark
        )
    }
}

private fun formatSeconds(seconds: Long): String {
    if (seconds < 60) return "${seconds}s"
    val minutes = seconds / 60
    val hours = minutes / 60
    val remMinutes = minutes % 60
    return if (hours > 0) "${hours}h ${remMinutes}m" else "${remMinutes}m"
}
