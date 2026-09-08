package com.nivya.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nivya.ui.common.*
import com.nivya.ui.theme.*

/**
 * Parent Family & Devices Screen managing linked family members and hardware endpoints.
 */
@Composable
fun ParentFamilyDevicesScreen(
    onPairNewDevice: () -> Unit = {},
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
        SectionHeader(title = "Family Unit: FAM-NIVYA-01")

        NivyaCard {
            Row(modifier = Modifier.fillMaxWidth()) {
                Icon(imageVector = Icons.Default.Group, contentDescription = null, tint = BluePrimary)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = "The Nivya Family", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(text = "2 Active Members • Mutual Consent Active", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }

        SectionHeader(
            title = "Enrolled Devices",
            actionLabel = "+ Pair Device",
            onActionClick = onPairNewDevice
        )

        StatusSummaryCard(
            deviceName = "Alex's Galaxy A54 (Child)",
            isOnline = true,
            batteryPct = 78,
            networkType = "Wi-Fi (Home)",
            isStale = false,
            lastSeen = "Online now"
        )

        StatusSummaryCard(
            deviceName = "Sarah's Pixel 8 (Parent - This Device)",
            isOnline = true,
            batteryPct = 92,
            networkType = "Cellular 5G",
            isStale = false,
            lastSeen = "Active now"
        )

        Button(
            onClick = onPairNewDevice,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Pair Additional Device")
        }
    }
}
