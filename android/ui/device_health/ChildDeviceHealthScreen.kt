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
import com.nivya.ui.common.*
import com.nivya.ui.theme.*

/**
 * Simplified Child Device Health screen showing friendly, positive status,
 * safe storage indicators, battery readiness, and protection health.
 */
@Composable
fun ChildDeviceHealthScreen(
    viewModel: ChildDeviceHealthViewModel? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val uiState by viewModel?.uiState?.collectAsState() ?: androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(ChildDeviceHealthUiState())
    }

    val score = uiState.healthScore
    val isGood = score >= 70

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
            SectionHeader(title = "My Device Health")
            IconButton(onClick = { viewModel?.loadData() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = BluePrimary
                )
            }
        }

        // 1. Friendly Condition Hero Card
        NivyaCard(borderColor = if (isGood) SuccessGreen else WarningAmber) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isGood) "Condition: Great" else "Needs Attention",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isGood) SuccessGreen else WarningAmber
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = uiState.conditionSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
                Icon(
                    imageVector = if (isGood) Icons.Default.Favorite else Icons.Default.Info,
                    contentDescription = null,
                    tint = if (isGood) SuccessGreen else WarningAmber,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // 2. Storage Health Card
        SectionHeader(title = "Storage Status")
        NivyaCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Free Storage Space",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    text = "${String.format("%.1f", uiState.freeStorageGb)} GB Free",
                    style = MaterialTheme.typography.titleSmall,
                    color = BlueLight
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            val progress = (uiState.storageUsedPct / 100.0).toFloat().coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = if (uiState.freeStorageGb < 5.0) WarningAmber else SuccessGreen,
                trackColor = SurfaceVariantDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (uiState.freeStorageGb >= 10.0) {
                    "Plenty of room for your favorite apps, games, and photos!"
                } else {
                    "Storage is getting a bit full. Consider removing old videos or unused apps."
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        // 3. Battery Readiness Card
        SectionHeader(title = "Battery & Power")
        NivyaCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${uiState.batteryPct}% Battery",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (uiState.isCharging) "⚡ Charging right now" else "Ready to use",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (uiState.isCharging) BlueLight else TextSecondary
                    )
                }
                Icon(
                    imageVector = if (uiState.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                    contentDescription = null,
                    tint = if (uiState.batteryPct <= 20) WarningAmber else SuccessGreen,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // 4. Protection Health
        SectionHeader(title = "Protection Health")
        NivyaCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (uiState.allPermissionsHealthy) Icons.Default.Shield else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (uiState.allPermissionsHealthy) SuccessGreen else WarningAmber,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (uiState.allPermissionsHealthy) "All Protections Active" else "Permission Check Needed",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (uiState.allPermissionsHealthy) {
                            "Nivya is keeping your device connected and family safety features on."
                        } else {
                            "Some safety permissions need to be turned on in Android settings."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
