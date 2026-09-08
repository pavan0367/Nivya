package com.nivya.ui.device_health

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nivya.ui.common.*
import com.nivya.ui.theme.*

/**
 * Child Device Health Screen showing hardware performance and storage health.
 */
@Composable
fun ChildDeviceHealthScreen(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(title = "My Device Health")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Device Health",
                value = "94%",
                unit = "great condition",
                icon = Icons.Default.Favorite,
                accentColor = SuccessGreen,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Free Storage",
                value = "79.8 GB",
                unit = "available",
                icon = Icons.Default.Storage,
                accentColor = BluePrimary,
                modifier = Modifier.weight(1f)
            )
        }

        SectionHeader(title = "Storage Status")

        NivyaCard {
            Text(text = "Internal Storage (38% Used)", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { 0.38f },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = SuccessGreen,
                trackColor = SurfaceVariantDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Plenty of storage available for photos, games, and apps.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
