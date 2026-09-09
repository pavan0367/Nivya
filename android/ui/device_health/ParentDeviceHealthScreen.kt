package com.nivya.ui.device_health

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.nivya.core.network.dto.DeviceHealthResponseDto
import com.nivya.ui.common.*
import com.nivya.ui.theme.*

/**
 * Detailed Parent Device Health screen displaying comprehensive system diagnostics:
 * hardware specs, storage progress, RAM usage, battery telemetry, connectivity, and permission health audits.
 */
@Composable
fun ParentDeviceHealthScreen(
    viewModel: ParentDeviceHealthViewModel? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val uiState by viewModel?.uiState?.collectAsState() ?: androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(ParentDeviceHealthUiState())
    }

    val health = uiState.healthData

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
            Column {
                SectionHeader(title = "Device Health & Diagnostics")
                Text(
                    text = health?.deviceName ?: "Child Device",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            IconButton(onClick = { viewModel?.loadData() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh telemetry",
                    tint = BluePrimary
                )
            }
        }

        // 1. Overall Health Condition Card
        val score = health?.healthScore ?: 96
        val scoreColor = when {
            score >= 85 -> SuccessGreen
            score >= 70 -> BlueLight
            score >= 50 -> WarningAmber
            else -> ErrorRed
        }

        NivyaCard(borderColor = scoreColor) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "System Health Score",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$score / 100",
                        style = MaterialTheme.typography.displaySmall,
                        color = scoreColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = health?.healthStatus ?: "EXCELLENT",
                        style = MaterialTheme.typography.titleMedium,
                        color = scoreColor
                    )
                }
                Icon(
                    imageVector = if (score >= 70) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = scoreColor,
                    modifier = Modifier.size(52.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = health?.conditionSummary ?: "All device diagnostics are optimal.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        // 2. Hardware & OS Specifications
        SectionHeader(title = "Hardware & Operating System")
        NivyaCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Device Model", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(text = health?.deviceModel ?: "Unknown Model", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
                HorizontalDivider(color = OutlineDark)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Manufacturer", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(text = health?.deviceManufacturer ?: "Android OEM", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
                HorizontalDivider(color = OutlineDark)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Android OS", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(text = health?.osVersion ?: "Android 14", style = MaterialTheme.typography.bodyMedium, color = BlueLight)
                }
                HorizontalDivider(color = OutlineDark)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "API SDK Level", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(text = "SDK ${health?.sdkVersion ?: 34}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        }

        // 3. Storage Diagnostics
        val storage = health?.storage
        val totalStorageGb = (storage?.totalBytes ?: 128_000_000_000L) / (1024.0 * 1024.0 * 1024.0)
        val usedStorageGb = (storage?.usedBytes ?: 48_200_000_000L) / (1024.0 * 1024.0 * 1024.0)
        val freeStorageGb = (storage?.freeBytes ?: 79_800_000_000L) / (1024.0 * 1024.0 * 1024.0)
        val storageProgress = if (totalStorageGb > 0) (usedStorageGb / totalStorageGb).toFloat().coerceIn(0f, 1f) else 0.4f
        val isLowStorage = storage?.isLowStorage == true

        SectionHeader(title = "Internal Storage Health")
        NivyaCard(borderColor = if (isLowStorage) WarningAmber else OutlineDark) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${String.format("%.1f", usedStorageGb)} GB used of ${String.format("%.1f", totalStorageGb)} GB",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    text = "${String.format("%.1f", storage?.usedPct ?: 37.6)}%",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isLowStorage) WarningAmber else BlueLight
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { storageProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = if (isLowStorage) WarningAmber else BluePrimary,
                trackColor = SurfaceVariantDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${String.format("%.1f", freeStorageGb)} GB free space available.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        // 4. Memory (RAM) Diagnostics
        val memory = health?.memory
        val totalRamGb = (memory?.totalBytes ?: 8_000_000_000L) / (1024.0 * 1024.0 * 1024.0)
        val usedRamGb = (memory?.usedBytes ?: 4_100_000_000L) / (1024.0 * 1024.0 * 1024.0)
        val ramProgress = if (totalRamGb > 0) (usedRamGb / totalRamGb).toFloat().coerceIn(0f, 1f) else 0.5f
        val isLowRam = memory?.isLowRam == true

        SectionHeader(title = "RAM & System Memory")
        NivyaCard(borderColor = if (isLowRam) WarningAmber else OutlineDark) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${String.format("%.1f", usedRamGb)} GB used of ${String.format("%.1f", totalRamGb)} GB RAM",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    text = "${String.format("%.1f", memory?.usedPct ?: 51.2)}%",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isLowRam) WarningAmber else SuccessGreen
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { ramProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = if (isLowRam) WarningAmber else SuccessGreen,
                trackColor = SurfaceVariantDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isLowRam) "System is experiencing low memory pressure." else "Memory headroom is healthy for multitasking.",
                style = MaterialTheme.typography.bodySmall,
                color = if (isLowRam) WarningAmber else TextSecondary
            )
        }

        // 5. Battery Snapshot
        val batteryPct = health?.batteryPct ?: 82
        val isCharging = health?.chargingState?.contains("CHARGING", ignoreCase = true) == true

        SectionHeader(title = "Power & Battery Status")
        NivyaCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Battery Level: $batteryPct%",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isCharging) "⚡ Charging (${health?.chargingState})" else "Discharging (Unplugged)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCharging) BlueLight else TextSecondary
                    )
                }
                Text(
                    text = "${health?.batteryTempCelsius ?: 31.5}°C",
                    style = MaterialTheme.typography.titleSmall,
                    color = BlueLight
                )
            }
        }

        // 6. Permission Health Reporting
        val perm = health?.permissionHealth
        val allPermsHealthy = perm?.allHealthy ?: true

        SectionHeader(title = "Permission Health Reporting")
        if (!allPermsHealthy) {
            NivyaCard(borderColor = WarningAmber) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "One or more safety permissions require attention on the child's device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarningAmber
                    )
                }
            }
        }

        NivyaCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PermissionStatusRow(
                    name = "Location Access",
                    status = perm?.locationPermission ?: "GRANTED",
                    isHealthy = perm?.locationPermission?.contains("GRANTED") == true
                )
                HorizontalDivider(color = OutlineDark)
                PermissionStatusRow(
                    name = "Usage & Screen Time",
                    status = perm?.usagePermission ?: "GRANTED",
                    isHealthy = "GRANTED".equals(perm?.usagePermission, ignoreCase = true)
                )
                HorizontalDivider(color = OutlineDark)
                PermissionStatusRow(
                    name = "Safety Notifications",
                    status = perm?.notificationPermission ?: "GRANTED",
                    isHealthy = perm?.notificationPermission?.contains("GRANTED") == true
                )
                HorizontalDivider(color = OutlineDark)
                PermissionStatusRow(
                    name = "Battery Optimization",
                    status = perm?.batteryOptimization ?: "OPTIMIZED",
                    isHealthy = true
                )
            }
        }

        // 7. Connectivity & Telemetry Sync State
        SectionHeader(title = "Connectivity & Synchronization")
        NivyaCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Connection: ${health?.networkType ?: "WIFI"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Sync State: ${health?.syncState ?: "SYNCED"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Text(
                    text = if (health?.isOnline == true) "● Online" else "○ Offline",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (health?.isOnline == true) SuccessGreen else ErrorRed
                )
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    name: String,
    status: String,
    isHealthy: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isHealthy) SuccessGreen else WarningAmber,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$status • " + if (isHealthy) "Healthy" else "Action Needed",
                style = MaterialTheme.typography.bodySmall,
                color = if (isHealthy) SuccessGreen else WarningAmber
            )
        }
    }
}
