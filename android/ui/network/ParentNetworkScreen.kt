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
 * Parent Network Screen displaying child device network connectivity:
 * network type, connection details, signal strength, internet availability, and last sync timestamp.
 */
@Composable
fun ParentNetworkScreen(
    viewModel: ParentNetworkViewModel? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val uiState by viewModel?.uiState?.collectAsState() ?: androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(ParentNetworkUiState())
    }

    val net = uiState.networkStatus
    val isInternetAvailable = net?.isInternetAvailable == true
    val quality = net?.quality ?: "UNAVAILABLE"

    val qualityColor = when (quality.uppercase()) {
        "EXCELLENT" -> SuccessGreen
        "GOOD" -> BluePrimary
        "WEAK" -> WarningAmber
        else -> ErrorRed
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
            SectionHeader(title = "Network Connectivity")
            IconButton(onClick = { viewModel?.loadData() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh telemetry",
                    tint = BluePrimary
                )
            }
        }

        if (uiState.isLoading) {
            LoadingState(message = "Reading child device network status...")
        }

        // Hero Connection Card
        NivyaCard(borderColor = qualityColor) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.deviceName,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = net?.connectionType ?: net?.networkType ?: "Offline",
                        style = MaterialTheme.typography.displaySmall,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Quality: $quality",
                            style = MaterialTheme.typography.bodySmall,
                            color = qualityColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• Type: ${net?.networkType ?: "NONE"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Icon(
                    imageVector = if (net?.networkType == "WIFI") Icons.Default.Wifi else Icons.Default.CellTower,
                    contentDescription = null,
                    tint = qualityColor,
                    modifier = Modifier.size(52.dp)
                )
            }
        }

        // Key Required Parameters: Signal, Internet Availability, Last Sync
        SectionHeader(title = "Connection Diagnostics")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Signal Strength",
                value = net?.signalLevel?.let { "$it / 4" } ?: "N/A",
                unit = net?.signalDbm?.let { "$it dBm" } ?: "optimal",
                icon = Icons.Default.SignalCellularAlt,
                accentColor = if ((net?.signalLevel ?: 0) >= 3) SuccessGreen else WarningAmber,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Internet Status",
                value = if (isInternetAvailable) "Available" else "No Internet",
                unit = if (isInternetAvailable) "verified" else "unreachable",
                icon = if (isInternetAvailable) Icons.Default.CheckCircle else Icons.Default.Cancel,
                accentColor = if (isInternetAvailable) SuccessGreen else ErrorRed,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Network Type",
                value = net?.networkType ?: "NONE",
                unit = "carrier/local",
                icon = Icons.Default.Router,
                accentColor = BluePrimary,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Last Sync",
                value = net?.lastSyncAt ?: "Just now",
                unit = "telemetry",
                icon = Icons.Default.Schedule,
                accentColor = PurpleAccent,
                modifier = Modifier.weight(1f)
            )
        }

        // Connection Details Breakdown
        NivyaCard {
            Text(text = "Link & Socket State", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Network Available", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(
                    text = if (net?.isNetworkAvailable == true) "Yes (Physical Link)" else "No",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (net?.isNetworkAvailable == true) SuccessGreen else ErrorRed
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Internet Validation", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(
                    text = if (isInternetAvailable) "Validated (DNS/HTTP)" else "Unreachable",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isInternetAvailable) SuccessGreen else ErrorRed
                )
            }
            if (net?.ipAddress != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Local IP Address", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(text = net.ipAddress, style = MaterialTheme.typography.bodySmall, color = BlueLight)
                }
            }
        }

        // Recent Network History
        val points = uiState.historyPoints.take(5)
        if (points.isNotEmpty()) {
            SectionHeader(title = "Recent Connectivity Transitions")
            NivyaCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (i in points.indices) {
                        val point = points[i]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${point.networkType} • ${point.connectionType ?: "Connected"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Quality: ${point.quality} | Signal: ${point.signalLevel ?: "N/A"}/4",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                            Text(
                                text = point.recordedAt ?: "Recent",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        if (i < points.size - 1) {
                            HorizontalDivider(color = OutlineDark)
                        }
                    }
                }
            }
        }
    }
}
