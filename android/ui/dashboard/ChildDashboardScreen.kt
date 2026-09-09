package com.nivya.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nivya.core.navigation.NavigationDestination
import com.nivya.data.repository.ConvocationRepository
import com.nivya.ui.common.ActionCard
import com.nivya.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Child Dashboard strictly implementing the final required 9-item order:
 * 1. Battery
 * 2. Screen Time
 * 3. CRACK (immediate send of "Mom,here" -> "Message sent" popup with "DONE")
 * 4. FREAK (immediate send of "Someone's,here" -> "Message sent" popup with "DONE")
 * 5. Network
 * 6. Network Quality
 * 7. Location
 * 8. Device Health
 * 9. Alerts
 *
 * Invariant: Zero explanatory subtext under CRACK or FREAK. Immediate dispatch with no pre-send confirmation.
 */
@Composable
fun ChildDashboardScreen(
    onNavigateTo: (NavigationDestination) -> Unit,
    convocationRepository: ConvocationRepository,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var showSuccessDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }

    fun sendImmediateNote(exactMessage: String) {
        if (isSending) return
        isSending = true
        errorMessage = null

        coroutineScope.launch {
            val result = convocationRepository.childSendMessage(exactMessage)
            isSending = false
            if (result.isSuccess) {
                showSuccessDialog = true
            } else {
                errorMessage = "Failed to send message. Please check connection."
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = {
                Text(
                    text = "Message sent",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = { showSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                ) {
                    Text("DONE")
                }
            },
            containerColor = SurfaceCard
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        errorMessage?.let { error ->
            Card(
                colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = error,
                    color = ErrorRed,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // 1. Battery
        ActionCard(
            title = "Battery",
            description = "Battery level & charging status",
            icon = Icons.Default.BatteryChargingFull,
            accentColor = SuccessGreen,
            onClick = { onNavigateTo(NavigationDestination.ChildBattery) }
        )

        // 2. Screen Time
        ActionCard(
            title = "Screen Time",
            description = "Daily screen time & app limits",
            icon = Icons.Default.Timer,
            accentColor = BluePrimary,
            onClick = { onNavigateTo(NavigationDestination.ChildScreenTime) }
        )

        // 3. CRACK (Immediate send of exact "Mom,here")
        Card(
            onClick = { sendImmediateNote("Mom,here") },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444)),
            shape = MaterialTheme.shapes.medium
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isSending) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = "CRACK",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }
            }
        }

        // 4. FREAK (Immediate send of exact "Someone's,here")
        Card(
            onClick = { sendImmediateNote("Someone's,here") },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF8B5CF6)),
            shape = MaterialTheme.shapes.medium
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isSending) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = "FREAK",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }
            }
        }

        // 5. Network
        ActionCard(
            title = "Network",
            description = "Wi-Fi & cellular connections",
            icon = Icons.Default.Wifi,
            accentColor = BlueSecondary,
            onClick = { onNavigateTo(NavigationDestination.ChildNetwork) }
        )

        // 6. Network Quality
        ActionCard(
            title = "Network Quality",
            description = "Connection latency & signal strength",
            icon = Icons.Default.Speed,
            accentColor = BlueLight,
            onClick = { onNavigateTo(NavigationDestination.ChildNetwork) }
        )

        // 7. Location
        ActionCard(
            title = "Location",
            description = "Current GPS location & coordinates",
            icon = Icons.Default.LocationOn,
            accentColor = WarningYellow,
            onClick = { onNavigateTo(NavigationDestination.ChildLocation) }
        )

        // 8. Device Health
        ActionCard(
            title = "Device Health",
            description = "System storage, RAM & hardware health",
            icon = Icons.Default.HealthAndSafety,
            accentColor = TealAccent,
            onClick = { onNavigateTo(NavigationDestination.ChildDeviceHealth) }
        )

        // 9. Alerts
        ActionCard(
            title = "Alerts",
            description = "Safety alerts & parent notifications",
            icon = Icons.Default.Notifications,
            accentColor = PurpleAccent,
            onClick = { onNavigateTo(NavigationDestination.ChildAlerts) }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
