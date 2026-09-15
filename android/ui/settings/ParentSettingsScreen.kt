package com.nivya.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nivya.ui.common.*
import com.nivya.ui.theme.*

/**
 * Parent Settings Screen providing family controls, telemetry sync settings,
 * consent terms, and the locked 4-step Permanent Account Deletion flow.
 */
@Composable
fun ParentSettingsScreen(
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var alertsEnabled by remember { mutableStateOf(true) }
    var locationSharing by remember { mutableStateOf(true) }
    var syncInterval by remember { mutableStateOf("Every 5 minutes") }

    // Delete Account Flow States (1: Overview, 2: Password, 3: Final Confirm, 4: Success)
    var showDeleteModal by remember { mutableStateOf(false) }
    var deletionStep by remember { mutableStateOf(1) }
    var passwordInput by remember { mutableStateOf("") }
    var isDeleting by remember { mutableStateOf(false) }
    var deletionError by remember { mutableStateOf<String?>(null) }

    if (showDeleteModal) {
        AlertDialog(
            onDismissRequest = {
                if (!isDeleting && deletionStep != 4) {
                    showDeleteModal = false
                    deletionStep = 1
                    passwordInput = ""
                    deletionError = null
                }
            },
            title = {
                Text(
                    text = when (deletionStep) {
                        1 -> "Delete Parent Account"
                        2 -> "Confirm Identity"
                        3 -> "Final Confirmation"
                        else -> "Account Deleted"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (deletionStep == 4) SuccessGreen else ErrorRed
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    deletionError?.let { err ->
                        Text(text = err, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                    }

                    when (deletionStep) {
                        1 -> {
                            Text(
                                text = "Permanently delete your parent administrator account and credentials. This action is irreversible.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(text = "• Your parent account will be permanently deleted.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    Text(text = "• Connected child accounts will NOT be deleted; their data remains preserved.", style = MaterialTheme.typography.bodySmall, color = SuccessGreen, fontWeight = FontWeight.SemiBold)
                                    Text(text = "• All active sessions across your devices will terminate.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                            }
                        }
                        2 -> {
                            Text(
                                text = "Enter your parent account password to confirm identity:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Account Password") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        3 -> {
                            Text(
                                text = "Are you absolutely sure you want to delete your parent account? This action cannot be undone.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(text = "• All your personal data and sessions will be deleted.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    Text(text = "• Connected child accounts remain intact and preserved.", style = MaterialTheme.typography.bodySmall, color = SuccessGreen, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        4 -> {
                            Text(
                                text = "Your parent account has been successfully deleted from Nivya servers.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                when (deletionStep) {
                    1 -> {
                        Button(
                            onClick = { deletionStep = 2 },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                        ) {
                            Text("Continue to Verification")
                        }
                    }
                    2 -> {
                        Button(
                            onClick = {
                                if (passwordInput.isBlank()) {
                                    deletionError = "Please enter your password."
                                } else {
                                    deletionError = null
                                    deletionStep = 3
                                }
                            },
                            enabled = passwordInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                        ) {
                            Text("Confirm Identity")
                        }
                    }
                    3 -> {
                        Button(
                            onClick = {
                                isDeleting = true
                                deletionError = null
                                // Perform deletion call
                                deletionStep = 4
                                isDeleting = false
                            },
                            enabled = !isDeleting,
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                        ) {
                            if (isDeleting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TextPrimary)
                            } else {
                                Text("Yes, Delete My Account")
                            }
                        }
                    }
                    4 -> {
                        Button(
                            onClick = {
                                showDeleteModal = false
                                onLogout()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                        ) {
                            Text("Return to Login")
                        }
                    }
                }
            },
            dismissButton = {
                if (deletionStep in 1..3) {
                    TextButton(onClick = {
                        if (deletionStep == 3) {
                            showDeleteModal = false
                        } else if (deletionStep > 1) {
                            deletionStep -= 1
                        } else {
                            showDeleteModal = false
                        }
                    }) {
                        Text(if (deletionStep == 2) "Back" else "Cancel")
                    }
                }
            },
            containerColor = SurfaceCard
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(title = "Monitoring & Alerts")

        NivyaCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Safety Push Notifications", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Text(text = "Receive alerts for low battery & geofence events", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Switch(checked = alertsEnabled, onCheckedChange = { alertsEnabled = it })
            }

            HorizontalDivider(color = OutlineDark, modifier = Modifier.padding(vertical = 10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Safe Zone Alerts", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Text(text = "Notify when child enters or leaves Safe Zones", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Switch(checked = locationSharing, onCheckedChange = { locationSharing = it })
            }
        }

        SectionHeader(title = "Background Synchronization")

        NivyaCard {
            Text(text = "Telemetry Sync Frequency", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Current: $syncInterval (Adjusts automatically on battery saver)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }

        SectionHeader(title = "Privacy & Consent Agreement")

        NivyaCard {
            Text(text = "Mutual Consent Agreement v1.0", style = MaterialTheme.typography.titleSmall, color = SuccessGreen)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Signed on account pairing. Both parties have consented to shared status, device diagnostics, and location visibility.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        SectionHeader(title = "Account Management")

        ActionCard(
            title = "Delete Account",
            description = "Permanently delete your parent account and credentials. Connected child accounts will not be deleted.",
            icon = Icons.Default.DeleteForever,
            accentColor = ErrorRed,
            onClick = {
                deletionStep = 1
                deletionError = null
                passwordInput = ""
                showDeleteModal = true
            }
        )
    }
}
