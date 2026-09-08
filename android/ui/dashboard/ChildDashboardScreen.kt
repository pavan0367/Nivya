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
 * Summary-oriented Child Dashboard focusing on personal device care, screen balance, and parent guidance.
 * Strict Invariant: Zero parent administration, surveillance controls, or hidden settings.
 */
@Composable
fun ChildDashboardScreen(
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
        // Section 1: Device Health & Battery Overview
        SectionHeader(
            title = "My Device Status",
            actionLabel = "Battery Details",
            onActionClick = { onNavigateTo(NavigationDestination.ChildBattery) }
        )

        StatusSummaryCard(
            deviceName = "This Device (Connected)",
            isOnline = true,
            batteryPct = 78,
            networkType = "Home Wi-Fi",
            isStale = false,
            lastSeen = "Syncing live",
            onClick = { onNavigateTo(NavigationDestination.ChildBattery) }
        )

        // Section 2: Metric Overview (2x2 Grid)
        SectionHeader(title = "Daily Well-being")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Today's Usage",
                value = "2h 45m",
                unit = "total",
                icon = Icons.Default.Timer,
                accentColor = BluePrimary,
                onClick = { onNavigateTo(NavigationDestination.ChildScreenTime) },
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Storage Health",
                value = "88%",
                unit = "optimal",
                icon = Icons.Default.Delete,
                accentColor = SuccessGreen,
                onClick = { onNavigateTo(NavigationDestination.ChildCleanUp) },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Family Messages",
                value = "1",
                unit = "new",
                icon = Icons.Default.Mail,
                accentColor = PurpleAccent,
                onClick = { onNavigateTo(NavigationDestination.ChildConvocation) },
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Sharing Status",
                value = "Active",
                unit = "safe",
                icon = Icons.Default.Security,
                accentColor = BlueSecondary,
                onClick = { onNavigateTo(NavigationDestination.ChildSettings) },
                modifier = Modifier.weight(1f)
            )
        }

        // Section 3: Child Tools & Guidance Actions
        SectionHeader(title = "My Tools")

        ActionCard(
            title = "Clean Up Temporary Storage",
            description = "Clear cache and reclaim device storage safely",
            icon = Icons.Default.Delete,
            accentColor = SuccessGreen,
            onClick = { onNavigateTo(NavigationDestination.ChildCleanUp) }
        )

        ActionCard(
            title = "View Screen Time Balance",
            description = "Track app usage categories and daily goals",
            icon = Icons.Default.Timer,
            accentColor = BluePrimary,
            onClick = { onNavigateTo(NavigationDestination.ChildScreenTime) }
        )

        ActionCard(
            title = "Family Convocation Messages",
            description = "Read messages and priority reminders from parents",
            icon = Icons.Default.Mail,
            accentColor = PurpleAccent,
            onClick = { onNavigateTo(NavigationDestination.ChildConvocation) }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
