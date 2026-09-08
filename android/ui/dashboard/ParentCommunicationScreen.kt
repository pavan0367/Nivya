package com.nivya.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nivya.ui.common.*
import com.nivya.ui.theme.*

/**
 * Parent Communication Status Screen providing high-level family reachability status.
 * Invariant: Private message contents and audio calls are never intercepted or recorded.
 */
@Composable
fun ParentCommunicationScreen(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(title = "Reachability & Safety")

        NivyaCard {
            Text(
                text = "Emergency Contact Reachability",
                style = MaterialTheme.typography.titleMedium,
                color = SuccessGreen
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Device can reach designated emergency family contacts over cellular and Wi-Fi.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        SectionHeader(title = "Communication Channels")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Cellular Voice",
                value = "Active",
                unit = "4G VoLTE",
                icon = Icons.Default.Call,
                accentColor = BluePrimary,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Family Convocation",
                value = "Online",
                unit = "WebSocket",
                icon = Icons.Default.Shield,
                accentColor = PurpleAccent,
                modifier = Modifier.weight(1f)
            )
        }

        NivyaCard {
            Text(
                text = "Privacy & Consent Invariant",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Nivya preserves personal message confidentiality. Only reachability, emergency contact readiness, and explicit Convocation messages are monitored.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
    }
}
