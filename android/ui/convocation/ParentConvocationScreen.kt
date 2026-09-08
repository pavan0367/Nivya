package com.nivya.ui.convocation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nivya.ui.common.*
import com.nivya.ui.theme.*

/**
 * Parent Convocation Screen allowing parents to compose and send family guidance messages.
 */
@Composable
fun ParentConvocationScreen(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    var messageText by remember { mutableStateOf("") }
    var sentMessage by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(title = "Send Convocation Guidance")

        NivyaCard {
            Text(
                text = "Compose Priority Message",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Convocation messages are highlighted directly on your child's device dashboard.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text("e.g. Please wrap up gaming and join for dinner in 15 minutes.") },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    if (messageText.isNotBlank()) {
                        sentMessage = true
                        messageText = ""
                    }
                },
                enabled = messageText.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Send to Child Device")
            }
        }

        if (sentMessage) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SuccessGreen.copy(alpha = 0.2f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "✓ Convocation message delivered successfully.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SuccessGreen,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        SectionHeader(title = "Recent Convocation Messages")

        NivyaCard {
            Text(text = "Dinner Time Reminder", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "\"Please pack your bag for tomorrow and turn off devices before 9:00 PM.\"",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Delivered • Read by Alex at 08:15 PM", style = MaterialTheme.typography.labelSmall, color = SuccessGreen)
        }
    }
}
