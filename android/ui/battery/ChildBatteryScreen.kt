package com.nivya.ui.battery

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nivya.ui.common.*
import com.nivya.ui.theme.*

/**
 * Child Battery Screen showing personal battery level, charging state,
 * official hardware health status, and practical power preservation tips.
 */
@Composable
fun ChildBatteryScreen(
    viewModel: ChildBatteryViewModel? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val uiState by viewModel?.uiState?.collectAsState() ?: androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(ChildBatteryUiState())
    }

    val batteryPct = uiState.percentage
    val isCharging = uiState.chargingState.contains("CHARGING", ignoreCase = true)
    val isCritical = batteryPct <= 15
    val isLow = batteryPct <= 20

    val accentColor = when {
        isCritical -> ErrorRed
        isLow -> WarningAmber
        isCharging -> BluePrimary
        else -> SuccessGreen
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
            SectionHeader(title = "Battery & Charging")
            IconButton(onClick = { viewModel?.refresh() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh battery",
                    tint = BluePrimary
                )
            }
        }

        if (uiState.isLoading) {
            LoadingState(message = "Reading hardware battery status...")
        }

        // Hero Battery Level Card
        NivyaCard(borderColor = accentColor) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Current Charge",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$batteryPct%",
                        style = MaterialTheme.typography.displaySmall,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isCharging) "⚡ ${uiState.chargingState}" else "Discharging",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isCharging) BlueLight else TextSecondary
                    )
                }

                Icon(
                    imageVector = if (isCharging) Icons.Default.BatteryChargingFull else if (isLow) Icons.Default.BatteryAlert else Icons.Default.BatteryFull,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { (batteryPct / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = accentColor,
                trackColor = SurfaceVariantDark
            )
        }

        // Key Metric Grid (Health & Temperature)
        SectionHeader(title = "Diagnostics")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Hardware Health",
                value = uiState.health,
                unit = "state",
                icon = Icons.Default.Favorite,
                accentColor = if (uiState.health.equals("GOOD", ignoreCase = true)) SuccessGreen else WarningAmber,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Temperature",
                value = uiState.temperatureCelsius?.let { "${String.format("%.1f", it)}°C" } ?: "N/A",
                unit = "celsius",
                icon = Icons.Default.Thermostat,
                accentColor = BluePrimary,
                modifier = Modifier.weight(1f)
            )
        }

        // Sync & Offline Status Indicator
        if (!uiState.isSynced) {
            NivyaCard(borderColor = WarningAmber) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = WarningAmber
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Queued for Sync",
                            style = MaterialTheme.typography.titleSmall,
                            color = WarningAmber
                        )
                        Text(
                            text = "Reading will synchronize automatically once reconnected.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        SectionHeader(title = "Battery Preservation Tips")

        NivyaCard {
            Text(
                text = "💡 Optimal Charge Range",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Keep your battery between 20% and 80% to prolong lithium-ion battery lifespan and reduce charging degradation.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        NivyaCard {
            Text(
                text = "❄️ Avoid Extreme Temperatures",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Do not leave your device in direct sunlight or inside a hot car. Overheating can permanently decrease battery health.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
