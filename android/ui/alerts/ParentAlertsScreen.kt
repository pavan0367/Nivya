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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nivya.core.network.dto.AlertDto
import com.nivya.core.network.dto.AlertRuleDto
import com.nivya.ui.common.*
import com.nivya.ui.theme.*

/**
 * Parent Alerts Screen presenting priority family alerts, resolution controls,
 * and configured threshold policies.
 */
@Composable
fun ParentAlertsScreen(
    viewModel: ParentAlertsViewModel? = null,
    modifier: Modifier = Modifier
) {
    if (viewModel == null) {
        ParentAlertsPlaceholder(modifier)
        return
    }

    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Overview & Unread Header Card
        item {
            NivyaCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Safety & Status Alerts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Real-time child device safety notifications",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    if (uiState.unreadCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ErrorRed.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(ErrorRed)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${uiState.unreadCount} Unread",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ErrorRed
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AlertFilter.values().forEach { filter ->
                    val isSelected = uiState.filter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setFilter(filter) },
                        label = {
                            Text(
                                text = filter.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BluePrimary,
                            selectedLabelColor = Color.White,
                            containerColor = SurfaceVariantDark,
                            labelColor = TextSecondary
                        )
                    )
                }
            }
        }

        // 3. Alerts List
        if (uiState.isLoading) {
            item {
                LoadingState(message = "Loading safety alerts...")
            }
        } else if (uiState.alerts.isEmpty()) {
            item {
                EmptyState(
                    title = "No Alerts Found",
                    description = "All child devices are within healthy parameters."
                )
            }
        } else {
            items(uiState.alerts, key = { it.id }) { alert ->
                ParentAlertCard(
                    alert = alert,
                    onMarkRead = { viewModel.markAsRead(alert.id) },
                    onResolve = { viewModel.resolveAlert(alert.id) }
                )
            }
        }

        // 4. Alert Rules Configuration Summary
        item {
            SectionHeader(title = "Threshold Policy Rules")
        }

        if (uiState.rules.isNotEmpty()) {
            items(uiState.rules, key = { it.id }) { rule ->
                AlertRuleRow(
                    rule = rule,
                    onToggle = { viewModel.toggleRule(rule) }
                )
            }
        } else {
            item {
                NivyaCard {
                    Text(
                        text = "• Low battery threshold: ≤ 15%\n• Offline threshold: ≥ 30m\n• Stale location threshold: ≥ 15m\n• Critical permissions: Immediate",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun ParentAlertCard(
    alert: AlertDto,
    onMarkRead: () -> Unit,
    onResolve: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCritical = "CRITICAL".equals(alert.severity, ignoreCase = true)
    val isWarning = "WARNING".equals(alert.severity, ignoreCase = true)
    val borderColor = if (isCritical) ErrorRed else if (isWarning) WarningAmber else BluePrimary
    val icon = getAlertIcon(alert.alertType)

    NivyaCard(borderColor = borderColor, modifier = modifier) {
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
                        tint = borderColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = alert.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = borderColor
                            )
                            if (!alert.isRead) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(borderColor)
                                )
                            }
                        }
                        if (alert.deviceName != null) {
                            Text(
                                text = alert.deviceName,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = borderColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = alert.severity.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = borderColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = alert.message,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Action Row
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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!alert.isRead) {
                        TextButton(
                            onClick = onMarkRead,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Mark Read", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }

                    if (!alert.resolved) {
                        OutlinedButton(
                            onClick = onResolve,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SuccessGreen)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resolve", style = MaterialTheme.typography.labelSmall)
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SuccessGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Resolved ✓",
                                style = MaterialTheme.typography.labelSmall,
                                color = SuccessGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlertRuleRow(
    rule: AlertRuleDto,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    NivyaCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatRuleName(rule.ruleType),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "Threshold: ${rule.thresholdValue} • Severity: ${rule.severity}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            Switch(
                checked = rule.enabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = BluePrimary,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = SurfaceVariantDark
                )
            )
        }
    }
}

private fun getAlertIcon(alertType: String): ImageVector {
    return when (alertType.uppercase()) {
        "LOW_BATTERY" -> Icons.Default.BatteryAlert
        "OFFLINE" -> Icons.Default.WifiOff
        "DEVICE_ONLINE", "RECONNECTED" -> Icons.Default.Wifi
        "STALE_LOCATION" -> Icons.Default.LocationOff
        "SECURITY_ALERT" -> Icons.Default.Security
        "PERMISSION_REVOKED", "PERMISSION_CHANGED" -> Icons.Default.Lock
        "LOW_STORAGE", "DEVICE_STATUS_ALERT" -> Icons.Default.SdStorage
        else -> Icons.Default.NotificationsActive
    }
}

private fun formatRuleName(ruleType: String): String {
    return when (ruleType.uppercase()) {
        "LOW_BATTERY" -> "Low Battery Protection"
        "OFFLINE" -> "Device Offline Monitor"
        "STALE_LOCATION" -> "Stale GPS Location"
        "PERMISSION_REVOKED" -> "Permission Health Guard"
        "LOW_STORAGE" -> "Storage Space Threshold"
        "SECURITY_ALERT" -> "Security & Pairing Monitor"
        else -> ruleType.replace("_", " ")
    }
}

@Composable
private fun ParentAlertsPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(title = "Active Safety Alerts")
        NivyaCard(borderColor = WarningAmber) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Icon(imageVector = Icons.Default.BatteryAlert, contentDescription = null, tint = WarningAmber)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = "Low Battery Warning", style = MaterialTheme.typography.titleSmall, color = WarningAmber)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "Alex's Galaxy A54 is at 18% battery. Encourage device charging.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }
    }
}
