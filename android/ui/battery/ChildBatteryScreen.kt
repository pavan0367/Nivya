package com.nivya.ui.battery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nivya.ui.common.*
import com.nivya.ui.theme.*

/**
 * Child Battery Screen showing personal battery level, health, and tips.
 */
@Composable
fun ChildBatteryScreen(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(title = "Battery & Charging")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Battery Level",
                value = "78%",
                unit = "charging",
                icon = Icons.Default.BatteryChargingFull,
                accentColor = SuccessGreen,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Est. Remaining",
                value = "6h 20m",
                unit = "usage",
                icon = Icons.Default.Eco,
                accentColor = BluePrimary,
                modifier = Modifier.weight(1f)
            )
        }

        SectionHeader(title = "Battery Health")

        NivyaCard {
            Text(text = "Health Status: Excellent", style = MaterialTheme.typography.titleSmall, color = SuccessGreen)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Maximum capacity is 96%. Battery temperature is normal (31°C).",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        SectionHeader(title = "Battery Saving Tips")

        NivyaCard {
            Text(text = "💡 Keep battery between 20% and 80%", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Avoid letting your phone completely discharge to preserve battery lifespan.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
    }
}
