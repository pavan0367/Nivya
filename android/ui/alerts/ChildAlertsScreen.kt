package com.nivya.ui.alerts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nivya.ui.common.*
import com.nivya.ui.theme.*

/**
 * Child Alerts Screen displaying personal device notifications and reminders.
 */
@Composable
fun ChildAlertsScreen(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(title = "My Notifications")

        NivyaCard(borderColor = WarningAmber) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Icon(imageVector = Icons.Default.BatteryAlert, contentDescription = null, tint = WarningAmber)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = "Charge Device Reminder", style = MaterialTheme.typography.titleSmall, color = WarningAmber)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "Battery reached 18%. Plug in your charger soon.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "25m ago", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }
        }

        NivyaCard(borderColor = BluePrimary) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Icon(imageVector = Icons.Default.Timer, contentDescription = null, tint = BlueLight)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = "Screen Time Check-In", style = MaterialTheme.typography.titleSmall, color = BlueLight)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "You've been on screen for 45 minutes straight. Take a quick eye break!", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "1h ago", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }
        }
    }
}
