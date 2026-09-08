package com.nivya.ui.network

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nivya.ui.common.*
import com.nivya.ui.theme.*

/**
 * Parent Network Screen showing network status, signal quality, and connectivity health.
 */
@Composable
fun ParentNetworkScreen(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(title = "Network Connectivity")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Current Connection",
                value = "Wi-Fi",
                unit = "Home-5G",
                icon = Icons.Default.Wifi,
                accentColor = BluePrimary,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Signal Quality",
                value = "Strong",
                unit = "-58 dBm",
                icon = Icons.Default.SignalCellularAlt,
                accentColor = SuccessGreen,
                modifier = Modifier.weight(1f)
            )
        }

        SectionHeader(title = "Connection Details")

        NivyaCard {
            Text(text = "SSID / Network Name", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(text = "Home-5G-Network", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "Link Speed", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(text = "433 Mbps (802.11ac)", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "WebSocket Sync State", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(text = "Connected • Heartbeat active", style = MaterialTheme.typography.bodyMedium, color = SuccessGreen)
        }
    }
}
