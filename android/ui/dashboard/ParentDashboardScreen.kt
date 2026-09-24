package com.nivya.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nivya.core.navigation.NavigationDestination
import com.nivya.ui.common.*
import com.nivya.ui.theme.*

/**
 * Summary-oriented Parent Dashboard providing immediate insight into family devices and health.
 * Bound to real data via ParentDashboardViewModel with authoritative Room and API state.
 */
@Composable
fun ParentDashboardScreen(
    onNavigateTo: (NavigationDestination) -> Unit,
    viewModel: ParentDashboardViewModel? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val uiState by viewModel?.uiState?.collectAsState() ?: remember {
        mutableStateOf(ParentDashboardUiState())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Child Device Status Overview
        SectionHeader(
            title = "Family Devices",
            actionLabel = "All Devices",
            onActionClick = { onNavigateTo(NavigationDestination.ParentFamilyDevices) }
        )

        if (uiState.hasChildDevice) {
            StatusSummaryCard(
                deviceName = uiState.childDeviceName,
                isOnline = uiState.isOnline,
                batteryPct = uiState.batteryPct,
                networkType = uiState.networkType,
                isStale = uiState.isStale,
                lastSeen = uiState.lastSeen,
                onClick = { onNavigateTo(NavigationDestination.ParentLiveActivity) }
            )
        } else {
            StatusSummaryCard(
                deviceName = uiState.childDeviceName,
                isOnline = false,
                batteryPct = null,
                networkType = "No connection",
                isStale = false,
                lastSeen = uiState.lastSeen,
                onClick = { onNavigateTo(NavigationDestination.ParentFamilyDevices) }
            )
        }

        // Section 2: Key Metric Cards (2x2 Grid)
        SectionHeader(title = "Health & Safety Overview")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Screen Time",
                value = uiState.screenTimeFormatted,
                unit = uiState.screenTimeUnit,
                icon = Icons.Default.Timer,
                accentColor = BluePrimary,
                onClick = { onNavigateTo(NavigationDestination.ParentAppUsage) },
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Device Health",
                value = uiState.deviceHealthFormatted,
                unit = uiState.deviceHealthUnit,
                icon = Icons.Default.Favorite,
                accentColor = SuccessGreen,
                onClick = { onNavigateTo(NavigationDestination.ParentDeviceHealth) },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Active Alerts",
                value = uiState.activeAlertsCount,
                unit = uiState.activeAlertsUnit,
                icon = Icons.Default.Notifications,
                accentColor = WarningAmber,
                onClick = { onNavigateTo(NavigationDestination.ParentAlerts) },
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Convocation",
                value = uiState.convocationCount,
                unit = uiState.convocationUnit,
                icon = Icons.Default.Mail,
                accentColor = PurpleAccent,
                onClick = { onNavigateTo(NavigationDestination.ParentConvocation) },
                modifier = Modifier.weight(1f)
            )
        }

        // Section 3: Quick Action Shortcuts
        SectionHeader(title = "Quick Actions")

        ActionCard(
            title = "Send Convocation Guidance",
            description = "Deliver high-priority family messages or instructions",
            icon = Icons.Default.Mail,
            accentColor = PurpleAccent,
            onClick = { onNavigateTo(NavigationDestination.ParentConvocation) }
        )

        ActionCard(
            title = "Live Activity Telemetry",
            description = "Real-time battery, screen time, and network telemetry",
            icon = Icons.Default.Timeline,
            accentColor = BluePrimary,
            onClick = { onNavigateTo(NavigationDestination.ParentLiveActivity) }
        )

        ActionCard(
            title = "View Safe Zone & Location",
            description = "Check child last verified location and safe zone status",
            icon = Icons.Default.Place,
            accentColor = SuccessGreen,
            onClick = { onNavigateTo(NavigationDestination.ParentLocation) }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
