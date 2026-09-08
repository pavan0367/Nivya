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
import com.nivya.core.navigation.NavigationDestination
import com.nivya.ui.common.*
import com.nivya.ui.theme.*

/**
 * Summary-oriented Parent Dashboard providing immediate insight into family devices and health.
 */
@Composable
fun ParentDashboardScreen(
    onNavigateTo: (NavigationDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

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

        StatusSummaryCard(
            deviceName = "Alex's Galaxy A54",
            isOnline = true,
            batteryPct = 78,
            networkType = "Wi-Fi (Home)",
            isStale = false,
            lastSeen = "Just now",
            onClick = { onNavigateTo(NavigationDestination.ParentLiveActivity) }
        )

        // Section 2: Key Metric Cards (2x2 Grid)
        SectionHeader(title = "Health & Safety Overview")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Screen Time",
                value = "2h 45m",
                unit = "today",
                icon = Icons.Default.Timer,
                accentColor = BluePrimary,
                onClick = { onNavigateTo(NavigationDestination.ParentAppUsage) },
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Device Health",
                value = "94%",
                unit = "good",
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
                value = "2",
                unit = "attention",
                icon = Icons.Default.Notifications,
                accentColor = WarningAmber,
                onClick = { onNavigateTo(NavigationDestination.ParentAlerts) },
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Convocation",
                value = "1",
                unit = "unread",
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
