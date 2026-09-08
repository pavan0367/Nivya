package com.nivya.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nivya.ui.common.*
import com.nivya.ui.theme.*

/**
 * Parent Settings Screen providing family controls, telemetry sync settings, and consent terms.
 */
@Composable
fun ParentSettingsScreen(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    var alertsEnabled by remember { mutableStateOf(true) }
    var locationSharing by remember { mutableStateOf(true) }
    var syncInterval by remember { mutableStateOf("Every 5 minutes") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(title = "Monitoring & Alerts")

        NivyaCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Safety Push Notifications", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Text(text = "Receive alerts for low battery & geofence events", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Switch(checked = alertsEnabled, onCheckedChange = { alertsEnabled = it })
            }

            HorizontalDivider(color = OutlineDark, modifier = Modifier.padding(vertical = 10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Safe Zone Alerts", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Text(text = "Notify when child enters or leaves Safe Zones", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Switch(checked = locationSharing, onCheckedChange = { locationSharing = it })
            }
        }

        SectionHeader(title = "Background Synchronization")

        NivyaCard {
            Text(text = "Telemetry Sync Frequency", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Current: $syncInterval (Adjusts automatically on battery saver)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }

        SectionHeader(title = "Privacy & Consent Agreement")

        NivyaCard {
            Text(text = "Mutual Consent Agreement v1.0", style = MaterialTheme.typography.titleSmall, color = SuccessGreen)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Signed on account pairing. Both parties have consented to shared status, device diagnostics, and location visibility.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
