package com.nivya.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nivya.ui.common.*
import com.nivya.ui.theme.*

/**
 * Parent Live Activity Screen presenting real-time telemetry stream overview.
 */
@Composable
fun ParentLiveActivityScreen(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(title = "Live Device Telemetry")

        StatusSummaryCard(
            deviceName = "Alex's Galaxy A54",
            isOnline = true,
            batteryPct = 78,
            networkType = "Wi-Fi (Home)",
            isStale = false,
            lastSeen = "Updated seconds ago"
        )

        SectionHeader(title = "Real-time Metrics")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Battery Drain Rate",
                value = "-4.2%",
                unit = "/ hr",
                icon = Icons.Default.BatteryChargingFull,
                accentColor = SuccessGreen,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Network Bandwidth",
                value = "1.4",
                unit = "Mbps",
                icon = Icons.Default.Wifi,
                accentColor = BluePrimary,
                modifier = Modifier.weight(1f)
            )
        }

        NivyaCard {
            Text(
                text = "Active Foreground Application",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Duolingo — Learn Languages",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Active for 18 minutes • Educational category",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        StaleDataIndicator(reason = "Telemetry heartbeat active: reporting every 60s over WebSocket")
    }
}
