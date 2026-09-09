package com.nivya.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nivya.core.network.NetworkResult
import com.nivya.data.repository.PairingRepository
import com.nivya.ui.common.*
import com.nivya.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Child Settings Screen showing personal preferences and transparency consent.
 * Strict Invariant 5.1: Zero Disconnect/Remove Parent buttons.
 * The only Child capability is the modal labeled "Enter Parent Disconnect Code".
 */
@Composable
fun ChildSettingsScreen(
    pairingRepository: PairingRepository? = null,
    onDisconnected: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var showCodeDialog by remember { mutableStateOf(false) }
    var codeInput by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    if (showCodeDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isSubmitting) {
                    showCodeDialog = false
                    errorMessage = null
                }
            },
            title = {
                Text(
                    text = "Enter Parent Disconnect Code",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "To unlink this device, enter the one-time disconnect code provided by your parent.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { codeInput = it.uppercase() },
                        label = { Text("Disconnect Code (DIS-XXXX-XXXX)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = ErrorRed,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (codeInput.isBlank() || pairingRepository == null) return@Button
                        isSubmitting = true
                        errorMessage = null
                        coroutineScope.launch {
                            val result = pairingRepository.verifyDisconnectCode(codeInput)
                            isSubmitting = false
                            when (result) {
                                is NetworkResult.Success -> {
                                    showCodeDialog = false
                                    successMessage = "Device unlinked successfully"
                                    onDisconnected()
                                }
                                is NetworkResult.Error -> {
                                    errorMessage = result.message
                                }
                                is NetworkResult.Exception -> {
                                    errorMessage = result.throwable.message ?: "Verification failed"
                                }
                            }
                        }
                    },
                    enabled = codeInput.isNotBlank() && !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TextPrimary)
                    } else {
                        Text("Verify Code")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCodeDialog = false },
                    enabled = !isSubmitting
                ) {
                    Text("Cancel")
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
        successMessage?.let { msg ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = msg,
                    color = SuccessGreen,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        SectionHeader(title = "Account & Family Sharing")

        NivyaCard {
            Text(text = "Family Unit Link", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Linked as Child Account", style = MaterialTheme.typography.bodyMedium, color = BlueLight)
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

        // Section 5.1: Enter Parent Disconnect Code dialog entry
        SectionHeader(title = "Family Connection")

        ActionCard(
            title = "Enter Parent Disconnect Code",
            description = "Enter the secure one-time code generated by your parent to unlink this device",
            icon = Icons.Default.Key,
            accentColor = PurpleAccent,
            onClick = {
                codeInput = ""
                errorMessage = null
                showCodeDialog = true
            }
        )

        SectionHeader(title = "App Information")

        NivyaCard {
            Text(text = "Nivya Child Companion v1.0.0", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = "Built for healthy digital habits and family well-being.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
    }
}
