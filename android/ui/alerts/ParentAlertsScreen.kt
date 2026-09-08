package com.nivya.ui.alerts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nivya.ui.common.*
import com.nivya.ui.theme.*

/**
 * Parent Alerts Screen presenting priority family alerts and safety notifications.
 */
@Composable
fun ParentAlertsScreen(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(title = "Active Safety Alerts")

        NivyaCard(borderColor = WarningAmber) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Icon(imageVector = Icons.Default.BatteryAlert, contentDescription = null, tint = WarningAmber)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = "Low Battery Warning", style = MaterialTheme.typography.titleSmall, color = WarningAmber)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Alex's Galaxy A54 is at 18% battery. Encourage device charging.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Triggered 25m ago", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }
        }

        NivyaCard(borderColor = BluePrimary) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Icon(imageVector = Icons.Default.Timer, contentDescription = null, tint = BlueLight)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = "Screen Time Threshold", style = MaterialTheme.typography.titleSmall, color = BlueLight)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Daily recreation goal of 2 hours exceeded by 15 minutes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Triggered 1h ago", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }
        }

        SectionHeader(title = "Alert Rules Summary")

        NivyaCard {
            Text(text = "Configured Alert Rules", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "• Battery critical threshold: ≤ 15%", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text(text = "• Screen time limit threshold: ≥ 3 hours", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text(text = "• Offline duration warning: ≥ 30 minutes", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}
