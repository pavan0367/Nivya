package com.nivya.ui.network

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nivya.ui.common.*
import com.nivya.ui.theme.*

/**
 * Child Network Screen showing personal connection quality and sync status.
 */
@Composable
fun ChildNetworkScreen(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(title = "My Network Status")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Network",
                value = "Connected",
                unit = "Wi-Fi",
                icon = Icons.Default.Wifi,
                accentColor = SuccessGreen,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Signal",
                value = "Strong",
                unit = "optimal",
                icon = Icons.Default.SignalWifi4Bar,
                accentColor = BluePrimary,
                modifier = Modifier.weight(1f)
            )
        }

        NivyaCard {
            Text(text = "Active Wi-Fi Network", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Home-5G-Network (Connected)", style = MaterialTheme.typography.bodyMedium, color = BlueLight)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Family status updates and Convocation messages sync automatically while connected.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
