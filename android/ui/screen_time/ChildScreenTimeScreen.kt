package com.nivya.ui.screen_time

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.nivya.services.usage.AppUsageRecord
import com.nivya.services.usage.UsagePermissionHelper
import com.nivya.services.usage.UsagePermissionState
import com.nivya.ui.common.*
import com.nivya.ui.theme.*

/**
 * Child Screen Time Screen presenting transparent permission lifecycle state machine
 * (REQUIRED -> Settings -> TRY_AGAIN -> GRANTED) and child digital well-being stats.
 */
@Composable
fun ChildScreenTimeScreen(
    viewModel: ChildScreenTimeViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollState = rememberScrollState()

    // Automatically verify permission whenever returning to Nivya from Android Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header with Refresh Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(title = "My Screen Time")

            if (uiState.permissionState == UsagePermissionState.GRANTED) {
                IconButton(
                    onClick = { viewModel.refresh() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh screen time",
                        tint = BlueLight
                    )
                }
            }
        }

        // --- Permission State Machine Rendering ---
        when (uiState.permissionState) {
            UsagePermissionState.REQUIRED -> {
                PermissionRequiredCard(
                    title = "Usage Access Required",
                    description = "To calculate your daily screen time, educational app balance, and healthy habit goals, Nivya requires Android Usage Access permission.\n\nNivya strictly respects your privacy: it never accesses private chat messages, web history, or passwords.",
                    buttonLabel = "Grant Access in Settings",
                    onGrantClick = {
                        viewModel.markPermissionRequested()
                        context.startActivity(UsagePermissionHelper.createUsageAccessSettingsIntent(context))
                    }
                )
            }

            UsagePermissionState.TRY_AGAIN -> {
                PermissionTryAgainCard(
                    onTryAgainClick = {
                        viewModel.markPermissionRequested()
                        context.startActivity(UsagePermissionHelper.createUsageAccessSettingsIntent(context))
                    },
                    onCheckAgainClick = {
                        viewModel.checkPermission()
                    }
                )
            }

            UsagePermissionState.GRANTED -> {
                GrantedUsageContent(uiState = uiState)
            }

        }
    }
}

@Composable
private fun PermissionRequiredCard(
    title: String,
    description: String,
    buttonLabel: String,
    onGrantClick: () -> Unit
) {
    NivyaCard(
        containerColor = SurfaceDark,
        borderColor = BluePrimary.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = BluePrimary.copy(alpha = 0.15f),
                modifier = Modifier.size(60.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = BluePrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onGrantClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = buttonLabel, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PermissionTryAgainCard(
    onTryAgainClick: () -> Unit,
    onCheckAgainClick: () -> Unit
) {
    NivyaCard(
        containerColor = SurfaceDark,
        borderColor = WarningAmber.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = WarningAmber.copy(alpha = 0.15f),
                modifier = Modifier.size(60.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Usage Access Not Granted",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Nivya was unable to detect Usage Access. Without this permission, Android prevents screen time statistics from being displayed. Please enable Nivya under Usage Access in Android Settings.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(0.95f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCheckAgainClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Verify")
                }

                Button(
                    onClick = onTryAgainClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Try Again", color = BackgroundDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun GrantedUsageContent(
    uiState: ChildScreenTimeUiState
) {

    if (uiState.isLoading && uiState.totalForegroundSeconds == 0L) {
        LoadingState(message = "Reading daily screen time...")
        return
    }

    if (!uiState.isSynced) {
        OfflineBanner(lastSyncTime = "Pending network sync")
    }

    // Top Summary Stat Cards
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            title = "Total Time Today",
            value = uiState.formattedTotalTime,
            unit = "screen time",
            icon = Icons.Default.Timer,
            accentColor = BluePrimary,
            modifier = Modifier.weight(1f)
        )

        StatCard(
            title = "Learning Apps",
            value = uiState.formattedEducationalTime,
            unit = "great job!",
            icon = Icons.Default.School,
            accentColor = SuccessGreen,
            modifier = Modifier.weight(1f)
        )
    }

    // Category Balance Card
    NivyaCard {
        Text(
            text = "Activity Breakdown",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        val total = if (uiState.totalForegroundSeconds > 0) uiState.totalForegroundSeconds.toFloat() else 1f

        CategoryRow(
            label = "Learning & Education",
            duration = uiState.formattedEducationalTime,
            fraction = (uiState.educationalSeconds.toFloat() / total).coerceIn(0f, 1f),
            color = SuccessGreen
        )

        Spacer(modifier = Modifier.height(10.dp))

        CategoryRow(
            label = "Games & Recreation",
            duration = uiState.formattedRecreationalTime,
            fraction = (uiState.recreationalSeconds.toFloat() / total).coerceIn(0f, 1f),
            color = BluePrimary
        )

        if (uiState.socialSeconds > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            CategoryRow(
                label = "Social & Communication",
                duration = formatSeconds(uiState.socialSeconds),
                fraction = (uiState.socialSeconds.toFloat() / total).coerceIn(0f, 1f),
                color = WarningAmber
            )
        }
    }

    // Top Applications Breakdown
    SectionHeader(title = "App Breakdown")

    if (uiState.topApps.isEmpty()) {
        NivyaCard {
            Text(
                text = "No foreground applications recorded yet today.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    } else {
        val maxAppSeconds = uiState.topApps.firstOrNull()?.foregroundSeconds?.toFloat() ?: 1f
        uiState.topApps.take(10).forEach { app ->
            AppItemCard(app = app, maxDurationSeconds = maxAppSeconds)
        }
    }

    // Digital Well-being Tips Card
    NivyaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = BlueLight,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Healthy Screen Balance",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Remember the 20-20-20 rule: every 20 minutes, take a 20-second break to look at something 20 feet away to rest your eyes.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun CategoryRow(
    label: String,
    duration: String,
    fraction: Float,
    color: androidx.compose.ui.graphics.Color
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
private fun AppItemCard(
    app: AppUsageRecord,
    maxDurationSeconds: Float
) {
    val durationText = formatSeconds(app.foregroundSeconds)
    val fraction = if (maxDurationSeconds > 0) (app.foregroundSeconds / maxDurationSeconds).coerceIn(0f, 1f) else 0f

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
                    text = "$durationText • ${app.category.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            Text(
                text = durationText,
                style = MaterialTheme.typography.titleSmall,
                color = BlueLight
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { fraction },
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
