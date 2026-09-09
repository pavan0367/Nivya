package com.nivya.ui.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nivya.core.network.dto.AlertDto
import com.nivya.ui.common.*
import com.nivya.ui.theme.*

/**
 * Child Alerts Screen displaying personal notifications and friendly reminders.
 * Strictly adheres to child UI constraints: exposes NO parent-only resolution controls
 * or configuration settings.
 */
@Composable
fun ChildAlertsScreen(
    viewModel: ChildAlertsViewModel? = null,
    modifier: Modifier = Modifier
) {
    if (viewModel == null) {
        ChildAlertsPlaceholder(modifier)
        return
    }

    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Card
        item {
            NivyaCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = BlueLight,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "My Device Reminders",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Helpful battery, screen time, and status reminders",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // 2. Notifications List
        if (uiState.isLoading) {
            item {
                LoadingState(message = "Checking for reminders...")
            }
        } else if (uiState.alerts.isEmpty()) {
            item {
                EmptyState(
                    title = "All Caught Up! 🎉",
                    description = "You have no active reminders right now. Have a great day!"
                )
            }
        } else {
            items(uiState.alerts, key = { it.id }) { alert ->
                ChildAlertCard(
                    alert = alert,
                    onMarkRead = { viewModel.markAsRead(alert.id) }
                )
            }
        }
    }
}

@Composable
fun ChildAlertCard(
    alert: AlertDto,
    onMarkRead: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isWarning = "WARNING".equals(alert.severity, ignoreCase = true) || "CRITICAL".equals(alert.severity, ignoreCase = true)
    val accentColor = if (isWarning) WarningAmber else BlueLight
    val icon = getChildAlertIcon(alert.alertType)

    NivyaCard(borderColor = accentColor, modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = alert.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                            if (!alert.isRead) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(accentColor)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = alert.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = alert.createdAt.take(16).replace("T", " "),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )

                if (!alert.isRead) {
                    TextButton(
                        onClick = onMarkRead,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Dismiss", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
            }
        }
    }
}

private fun getChildAlertIcon(alertType: String): ImageVector {
    return when (alertType.uppercase()) {
        "LOW_BATTERY" -> Icons.Default.BatteryAlert
        "OFFLINE", "DEVICE_OFFLINE" -> Icons.Default.WifiOff
        "DEVICE_ONLINE", "RECONNECTED" -> Icons.Default.Wifi
        "LOW_STORAGE" -> Icons.Default.CleaningServices
        "PERMISSION_REVOKED" -> Icons.Default.Lock
        else -> Icons.Default.Notifications
    }
}

@Composable
private fun ChildAlertsPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(title = "My Notifications")
        NivyaCard(borderColor = WarningAmber) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Icon(imageVector = Icons.Default.BatteryAlert, contentDescription = null, tint = WarningAmber)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = "Charge Device Reminder", style = MaterialTheme.typography.titleSmall, color = WarningAmber)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "Battery reached 18%. Plug in your charger soon.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }
    }
}
