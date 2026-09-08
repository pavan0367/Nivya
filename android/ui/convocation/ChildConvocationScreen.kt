package com.nivya.ui.convocation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nivya.ui.common.*
import com.nivya.ui.theme.*

/**
 * Child Convocation Screen displaying incoming priority family messages.
 */
@Composable
fun ChildConvocationScreen(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    var acknowledged by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(title = "Family Convocation Messages")

        NivyaCard(borderColor = PurpleAccent) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Icon(imageVector = Icons.Default.Mail, contentDescription = null, tint = PurpleLight)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = "Message from Mom & Dad", style = MaterialTheme.typography.titleMedium, color = PurpleLight)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "\"Please pack your bag for school tomorrow and wrap up device time by 9:00 PM.\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "Sent today at 07:45 PM • High Priority", style = MaterialTheme.typography.labelSmall, color = TextMuted)

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { acknowledged = true },
                        enabled = !acknowledged,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (acknowledged) SuccessGreen else PurpleAccent)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if (acknowledged) "Acknowledged ✓" else "I Understood!")
                    }
                }
            }
        }

        SectionHeader(title = "Previous Messages")

        NivyaCard {
            Text(text = "Weekend Study Reminder", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "\"Good job finishing math homework today!\"", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Yesterday at 04:30 PM • Read", style = MaterialTheme.typography.labelSmall, color = SuccessGreen)
        }
    }
}
