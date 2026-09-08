package com.nivya.ui.location

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nivya.ui.common.*
import com.nivya.ui.theme.*

/**
 * Parent Location Screen showing safe zone membership and location transparency.
 */
@Composable
fun ParentLocationScreen(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(title = "Location & Safe Zones")

        NivyaCard {
            Text(
                text = "Current Status: Inside Safe Zone",
                style = MaterialTheme.typography.titleMedium,
                color = SuccessGreen
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Home Safe Zone (Radius 250m)",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Last verified: 12 minutes ago • GPS accuracy: ±15m",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }

        SectionHeader(title = "Configured Safe Zones")

        SafeZoneItem(name = "Home", address = "Designated Home Location", isCurrent = true)
        SafeZoneItem(name = "School", address = "Designated Campus Area", isCurrent = false)
        SafeZoneItem(name = "Community Center", address = "Activity Hub", isCurrent = false)

        NivyaCard {
            Text(
                text = "Consent & Transparency",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Both Parent and Child have explicitly consented to geofence and location visibility under Nivya Family Terms v1.0.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun SafeZoneItem(
    name: String,
    address: String,
    isCurrent: Boolean
) {
    NivyaCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            if (isCurrent) {
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                    color = SuccessGreen.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "Current",
                        style = MaterialTheme.typography.labelSmall,
                        color = SuccessGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
