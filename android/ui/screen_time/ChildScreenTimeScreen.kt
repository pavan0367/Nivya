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
 * Child Screen Time Screen showing daily screen usage balance and self-regulation.
 */
@Composable
fun ChildScreenTimeScreen(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(title = "My Screen Time")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Total Time Today",
                value = "2h 45m",
                unit = "usage",
                icon = Icons.Default.Timer,
                accentColor = BluePrimary,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Learning Apps",
                value = "1h 10m",
                unit = "great job!",
                icon = Icons.Default.PieChart,
                accentColor = SuccessGreen,
                modifier = Modifier.weight(1f)
            )
        }

        SectionHeader(title = "App Breakdown")

        NivyaCard {
            Text(text = "Duolingo", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(text = "45m • Learning & Education", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { 0.45f },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = SuccessGreen,
                trackColor = SurfaceVariantDark
            )
        }

        NivyaCard {
            Text(text = "Minecraft", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(text = "40m • Games & Recreation", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { 0.40f },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = BluePrimary,
                trackColor = SurfaceVariantDark
            )
        }

        NivyaCard {
            Text(text = "Healthy Screen Balance", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Taking a 5-minute break every 30 minutes helps protect your eyes and improves focus.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
