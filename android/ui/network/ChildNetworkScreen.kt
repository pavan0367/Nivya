package com.nivya.ui.network

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
 * Child Network Screen showing personal connection quality and network availability
 * with distinct quality indicators: Excellent, Good, Weak, or Unavailable.
 */
@Composable
fun ChildNetworkScreen(
    viewModel: ChildNetworkViewModel? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val uiState by viewModel?.uiState?.collectAsState() ?: androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(ChildNetworkUiState())
    }

    val quality = uiState.quality.uppercase()
    val isOnline = uiState.isNetworkAvailable && uiState.isInternetAvailable

    val (qualityLabel, qualityColor, qualityIcon, qualityDescription) = when (quality) {
        "EXCELLENT" -> Quad(
            "Excellent",
            SuccessGreen,
            Icons.Default.SignalWifi4Bar,
            "Optimal internet reachability and strong connection signal."
        )
        "GOOD" -> Quad(
            "Good",
            BluePrimary,
            Icons.Default.Wifi,
            "Stable internet connection suitable for all family companion syncs."
        )
        "WEAK" -> Quad(
            "Weak",
            WarningAmber,
            Icons.Default.SignalCellularAlt,
            "Low signal or slower link speed. Synchronization may experience minor delays."
        )
        else -> Quad(
            "Unavailable",
            ErrorRed,
            Icons.Default.CloudOff,
            "No internet reachability detected. Telemetry and messages are safely queued locally."
        )
    }

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
            SectionHeader(title = "My Network Status")
            IconButton(onClick = { viewModel?.refresh() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh network state",
                    tint = BluePrimary
                )
            }
        }

        if (uiState.isLoading) {
            LoadingState(message = "Checking connectivity diagnostics...")
        }

        // Hero Network Quality Indicator
        NivyaCard(borderColor = qualityColor) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Network Quality",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = qualityLabel,
                        style = MaterialTheme.typography.displaySmall,
                        color = qualityColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = qualityDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Icon(
                    imageVector = qualityIcon,
                    contentDescription = qualityLabel,
                    tint = qualityColor,
                    modifier = Modifier.size(54.dp)
                )
            }
        }

        // Key Metrics
        SectionHeader(title = "Connection Details")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Connection",
                value = uiState.networkType,
                unit = uiState.connectionType,
                icon = if (uiState.networkType == "WIFI") Icons.Default.Wifi else Icons.Default.CellTower,
                accentColor = if (isOnline) BluePrimary else TextMuted,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Internet Access",
                value = if (uiState.isInternetAvailable) "Online" else "No Internet",
                unit = if (uiState.isInternetAvailable) "validated" else "offline",
                icon = if (uiState.isInternetAvailable) Icons.Default.CheckCircle else Icons.Default.Cancel,
                accentColor = if (uiState.isInternetAvailable) SuccessGreen else ErrorRed,
                modifier = Modifier.weight(1f)
            )
        }

        // Signal Metrics Card
        NivyaCard {
            Text(
                text = "Signal Diagnostics",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Signal Strength", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(
                    text = uiState.signalLevel?.let { "$it of 4 bars" } ?: "N/A",
                    style = MaterialTheme.typography.bodySmall,
                    color = BlueLight
                )
            }
            if (uiState.signalDbm != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Signal Level (dBm)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(
                        text = "${uiState.signalDbm} dBm",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                }
            }
        }

        // Sync & Offline State Badge
        if (!uiState.isSynced || !isOnline) {
            NivyaCard(borderColor = WarningAmber) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = null,
                        tint = WarningAmber
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (!isOnline) "Offline Mode" else "Readings Queued",
                            style = MaterialTheme.typography.titleSmall,
                            color = WarningAmber
                        )
                        Text(
                            text = "Telemetry is safely preserved in Room storage and will flush automatically upon reconnecting.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
