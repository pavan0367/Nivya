package com.nivya.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nivya.core.navigation.NavigationDestination
import com.nivya.data.repository.ConvocationRepository
import com.nivya.ui.common.ActionCard
import com.nivya.ui.theme.*

/**
 * Child Dashboard strictly implementing the final required 9-item order:
 * 1. Battery
 * 2. Screen Time
 * 3. CRACK (immediate send of "Mom,here" -> non-blocking "Done!" feedback)
 * 4. FREAK (immediate send of "Someone's,here" -> non-blocking "Done!" feedback)
 * 5. Network
 * 6. Network Quality
 * 7. Location
 * 8. Device Health
 * 9. Alerts
 *
 * Invariant: Zero explanatory subtext under CRACK or FREAK. Immediate dispatch with no pre-send confirmation.
 * CRACK/FREAK 400ms grace period before showing progress, non-blocking ~2s "Done!" toast.
 */
@Composable
fun ChildDashboardScreen(
    onNavigateTo: (NavigationDestination) -> Unit,
    convocationRepository: ConvocationRepository,
    modifier: Modifier = Modifier,
    viewModel: ChildDashboardViewModel = viewModel(
        factory = ChildDashboardViewModel.provideFactory(convocationRepository)
    )
) {
    val scrollState = rememberScrollState()
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            uiState.errorMessage?.let { error ->
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

            // 3 & 4. CRACK & FREAK (Equal side-by-side layout, immediate send, zero subtext)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 3. CRACK (Immediate send of exact "Mom,here")
                Card(
                    onClick = { viewModel.triggerCrack() },
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444)),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.showCrackSending) {
                            Text(
                                text = "Sending...",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
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
                    onClick = { viewModel.triggerFreak() },
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF8B5CF6)),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.showFreakSending) {
                            Text(
                                text = "Sending...",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = "FREAK",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                        }
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

        // Non-blocking Top-Center Done! Toast
        if (uiState.showDoneToast) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFA0F172A),
                border = BorderStroke(1.dp, SuccessGreen),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Done!",
                        style = MaterialTheme.typography.labelLarge,
                        color = SuccessGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
