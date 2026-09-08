package com.nivya.ui.device_health

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nivya.ui.common.*
import com.nivya.ui.theme.*

/**
 * Parent Device Health Screen showing hardware and storage diagnostics for linked devices.
 */
@Composable
fun ParentDeviceHealthScreen(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(title = "Hardware Diagnostics")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Health Score",
                value = "94/100",
                unit = "optimal",
                icon = Icons.Default.Favorite,
                accentColor = SuccessGreen,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Battery Level",
                value = "78%",
                unit = "good",
                icon = Icons.Default.BatteryChargingFull,
                accentColor = BluePrimary,
                modifier = Modifier.weight(1f)
            )
        }

        SectionHeader(title = "Storage & Memory Breakdown")

        NivyaCard {
            Text(
                text = "Internal Storage",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "48.2 GB Used of 128 GB", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(text = "38%", style = MaterialTheme.typography.bodySmall, color = BlueLight)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { 0.38f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = BluePrimary,
                trackColor = SurfaceVariantDark
            )
        }

        NivyaCard {
            Text(
                text = "RAM (Memory)",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "3.4 GB Used of 6 GB", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(text = "56%", style = MaterialTheme.typography.bodySmall, color = SuccessGreen)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { 0.56f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = SuccessGreen,
                trackColor = SurfaceVariantDark
            )
        }
    }
}
