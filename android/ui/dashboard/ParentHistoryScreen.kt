package com.nivya.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nivya.ui.common.*
import com.nivya.ui.theme.*

/**
 * Parent History Screen displaying chronological activity logs, connection changes, and alerts.
 */
@Composable
fun ParentHistoryScreen(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    var selectedFilter by remember { mutableStateOf("All") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(title = "Activity History")

        // Filter chips row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Security", "Location", "Alerts").forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter) }
                )
            }
        }

        // Timeline items
        NivyaCard {
            Text(
                text = "Device Paired Successfully",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Alex's Galaxy A54 completed mutual code handshake with Parent device.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Today, 10:45 AM • Family Unit FAM-NIVYA-01",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }

        NivyaCard {
            Text(
                text = "Battery Threshold Reached",
                style = MaterialTheme.typography.titleSmall,
                color = WarningAmber
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Child battery level dipped below 20% (currently charging).",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Today, 08:30 AM • Telemetry Alert",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }

        NivyaCard {
            Text(
                text = "Safe Zone Entry: Home",
                style = MaterialTheme.typography.titleSmall,
                color = SuccessGreen
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Device arrived at designated Home Safe Zone.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Yesterday, 04:15 PM • Geofence Event",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}
