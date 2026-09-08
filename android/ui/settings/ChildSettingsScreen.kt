package com.nivya.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nivya.ui.common.*
import com.nivya.ui.theme.*

/**
 * Child Settings Screen showing personal preferences and transparency consent.
 * Invariant: Child settings strictly exclude parent administration and surveillance controls.
 */
@Composable
fun ChildSettingsScreen(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(title = "Account & Family Sharing")

        NivyaCard {
            Text(text = "Family Unit Link", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Linked as Child Account under FAM-NIVYA-01", style = MaterialTheme.typography.bodyMedium, color = BlueLight)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Device battery level and usage balance are shared transparently with your parents.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        SectionHeader(title = "Privacy & Data Protection")

        NivyaCard {
            Row(modifier = Modifier.fillMaxWidth()) {
                Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = SuccessGreen)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = "Your Privacy is Protected", style = MaterialTheme.typography.titleSmall, color = SuccessGreen)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Nivya is built on mutual consent. Private messages, search queries, and audio recordings are never tracked or intercepted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        SectionHeader(title = "App Information")

        NivyaCard {
            Text(text = "Nivya Child Companion v1.0.0", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = "Built for healthy digital habits and family well-being.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
    }
}
