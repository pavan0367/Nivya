package com.nivya.ui.screen_time

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nivya.ui.common.*
import com.nivya.ui.theme.*

/**
 * Parent App Usage Screen showing summary-oriented application usage breakdowns.
 */
@Composable
fun ParentAppUsageScreen(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(title = "App Usage Summary")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Total Screen Time",
                value = "2h 45m",
                unit = "today",
                icon = Icons.Default.Timer,
                accentColor = BluePrimary,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Educational",
                value = "1h 10m",
                unit = "42%",
                icon = Icons.Default.PieChart,
                accentColor = SuccessGreen,
                modifier = Modifier.weight(1f)
            )
        }

        SectionHeader(title = "Top Applications Today")

        AppUsageItem(
            appName = "Duolingo",
            category = "Education",
            duration = "45m",
            percentage = 0.35f
        )

        AppUsageItem(
            appName = "Khan Academy",
            category = "Education",
            duration = "25m",
            percentage = 0.20f
        )

        AppUsageItem(
            appName = "Minecraft",
            category = "Games",
            duration = "40m",
            percentage = 0.30f
        )

        AppUsageItem(
            appName = "Messages",
            category = "Communication",
            duration = "15m",
            percentage = 0.15f
        )
    }
}

@Composable
private fun AppUsageItem(
    appName: String,
    category: String,
    duration: String,
    percentage: Float
) {
    NivyaCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            Text(
                text = duration,
                style = MaterialTheme.typography.titleSmall,
                color = BlueLight
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { percentage },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = BluePrimary,
            trackColor = SurfaceVariantDark
        )
    }
}
