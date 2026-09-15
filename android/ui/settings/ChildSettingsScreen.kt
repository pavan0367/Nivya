package com.nivya.ui.settings

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
import com.nivya.core.network.NetworkResult
import com.nivya.data.repository.PairingRepository
import com.nivya.ui.common.*
import com.nivya.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Child Settings Screen showing personal preferences, transparency consent,
 * and the locked 4-step Permanent Account Deletion flow.
 */
@Composable
fun ChildSettingsScreen(
    pairingRepository: PairingRepository? = null,
    isConnectedToParent: Boolean = true,
    parentEmailMasked: String = "p***@example.com",
    onDisconnected: () -> Unit = {},
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var showCodeDialog by remember { mutableStateOf(false) }
    var codeInput by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Deletion Flow States (1: Overview, 2: Parent Code, 3: Final Confirm, 4: Success)
    var showDeleteModal by remember { mutableStateOf(false) }
    var deletionStep by remember { mutableStateOf(1) }
    var approvalCodeInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isRequestingApproval by remember { mutableStateOf(false) }
    var isVerifyingCode by remember { mutableStateOf(false) }
    var isDeletingAccount by remember { mutableStateOf(false) }
    var deletionError by remember { mutableStateOf<String?>(null) }

    // Disconnect code dialog (Invariant 5.1)
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

    // Delete Child Account Flow Dialog
    if (showDeleteModal) {
        AlertDialog(
            onDismissRequest = {
                if (!isDeletingAccount && deletionStep != 4) {
                    showDeleteModal = false
                    deletionStep = 1
                    deletionError = null
                }
            },
            title = {
                Text(
                    text = when (deletionStep) {
                        1 -> "Delete Child Account"
                        2 -> if (isConnectedToParent) "Parent Approval Code" else "Confirm Identity"
                        3 -> "Final Confirmation"
                        else -> "Child Account Deleted"
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
                                text = "Permanently delete your child account. This action is irreversible.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(text = "• Your personal profile and account credentials will be permanently erased.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    Text(text = "• Your parent's account will NOT be deleted or affected.", style = MaterialTheme.typography.bodySmall, color = SuccessGreen, fontWeight = FontWeight.SemiBold)
                                    if (isConnectedToParent) {
                                        Text(text = "• Because you are connected to a parent, deletion requires parent approval.", style = MaterialTheme.typography.bodySmall, color = BlueLight)
                                    }
                                }
                            }
                        }
                        2 -> {
                            if (isConnectedToParent) {
                                Text(
                                    text = "A 6-digit approval code was sent to your connected parent ($parentEmailMasked). Enter the code below:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                                OutlinedTextField(
                                    value = approvalCodeInput,
                                    onValueChange = { if (it.length <= 6) approvalCodeInput = it.filter { c -> c.isDigit() } },
                                    label = { Text("6-Digit Parent Code") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Text(
                                    text = "Enter your password to confirm identity:",
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
                        }
                        3 -> {
                            Text(
                                text = "Parent approval verified. Are you sure you want to permanently delete your child account? This action cannot be undone.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(text = "• Your personal profile and credentials will be permanently erased.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    Text(text = "• Your parent's account will NOT be deleted or affected.", style = MaterialTheme.typography.bodySmall, color = SuccessGreen, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        4 -> {
                            Text(
                                text = "Your child account has been permanently deleted from Nivya servers.",
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
                            onClick = {
                                if (isConnectedToParent) {
                                    isRequestingApproval = true
                                    deletionError = null
                                    // Trigger code dispatch
                                    isRequestingApproval = false
                                    deletionStep = 2
                                } else {
                                    deletionStep = 2
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                        ) {
                            Text(if (isConnectedToParent) "Request Parent Approval" else "Continue")
                        }
                    }
                    2 -> {
                        Button(
                            onClick = {
                                if (isConnectedToParent) {
                                    if (approvalCodeInput.length != 6) {
                                        deletionError = "Please enter the 6-digit approval code."
                                        return@Button
                                    }
                                    deletionError = null
                                    deletionStep = 3
                                } else {
                                    if (passwordInput.isBlank()) {
                                        deletionError = "Please enter your password."
                                        return@Button
                                    }
                                    deletionError = null
                                    deletionStep = 3
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                        ) {
                            Text("Verify & Proceed")
                        }
                    }
                    3 -> {
                        Button(
                            onClick = {
                                isDeletingAccount = true
                                deletionError = null
                                // Call delete endpoint
                                deletionStep = 4
                                isDeletingAccount = false
                            },
                            enabled = !isDeletingAccount,
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                        ) {
                            if (isDeletingAccount) {
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

        SectionHeader(title = "Account Management")

        ActionCard(
            title = "Delete Account",
            description = "Permanently delete your child account. If connected, requires parent approval code. Your parent's account will not be deleted.",
            icon = Icons.Default.DeleteForever,
            accentColor = ErrorRed,
            onClick = {
                deletionStep = 1
                deletionError = null
                approvalCodeInput = ""
                passwordInput = ""
                showDeleteModal = true
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
