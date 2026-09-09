package com.nivya.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.nivya.core.network.NetworkResult
import com.nivya.data.repository.PairingRepository
import com.nivya.ui.common.*
import com.nivya.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Parent Family & Devices Screen managing linked family members, hardware endpoints,
 * and protected one-time disconnect code generation.
 */
@Composable
fun ParentFamilyDevicesScreen(
    onPairNewDevice: () -> Unit = {},
    pairingRepository: PairingRepository? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var showDisconnectDialog by remember { mutableStateOf(false) }
    var disconnectCode by remember { mutableStateOf<String?>(null) }
    var isGeneratingCode by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var copiedAlert by remember { mutableStateOf(false) }

    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = {
                Text(
                    text = "One-Time Disconnect Code",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Share this code with your child to authorize disconnection. It can only be used once and expires in 10 minutes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceCard, RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = disconnectCode ?: "------",
                            style = MaterialTheme.typography.headlineMedium,
                            color = PurpleAccent
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                disconnectCode?.let {
                                    clipboardManager.setText(AnnotatedString(it))
                                    copiedAlert = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BlueSecondary)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (copiedAlert) "Copied!" else "Copy Code")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text("Close")
                }
            },
            containerColor = SurfaceDark
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        errorMessage?.let { error ->
            Card(
                colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = error,
                    color = ErrorRed,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        SectionHeader(title = "Family Unit: FAM-NIVYA-01")

        NivyaCard {
            Row(modifier = Modifier.fillMaxWidth()) {
                Icon(imageVector = Icons.Default.Group, contentDescription = null, tint = BluePrimary)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = "The Nivya Family", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(text = "2 Active Members • Mutual Consent Active", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }

        SectionHeader(
            title = "Enrolled Devices",
            actionLabel = "+ Pair Device",
            onActionClick = onPairNewDevice
        )

        StatusSummaryCard(
            deviceName = "Alex's Galaxy A54 (Child)",
            isOnline = true,
            batteryPct = 78,
            networkType = "Wi-Fi (Home)",
            isStale = false,
            lastSeen = "Online now"
        )

        StatusSummaryCard(
            deviceName = "Sarah's Pixel 8 (Parent - This Device)",
            isOnline = true,
            batteryPct = 92,
            networkType = "Cellular 5G",
            isStale = false,
            lastSeen = "Active now"
        )

        Button(
            onClick = onPairNewDevice,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Pair Additional Device")
        }

        // Section 5.2: Protected Disconnect Code Generation for Parent
        SectionHeader(title = "Connection Management")

        ActionCard(
            title = "Generate Disconnect Code",
            description = "Generate a secure, single-use 10-minute code to allow your child device to unlink",
            icon = Icons.Default.Key,
            accentColor = WarningYellow,
            onClick = {
                if (pairingRepository == null || isGeneratingCode) return@ActionCard
                isGeneratingCode = true
                errorMessage = null
                copiedAlert = false

                coroutineScope.launch {
                    val result = pairingRepository.generateDisconnectCode()
                    isGeneratingCode = false
                    when (result) {
                        is NetworkResult.Success -> {
                            disconnectCode = result.data.code
                            showDisconnectDialog = true
                        }
                        is NetworkResult.Error -> {
                            errorMessage = result.message
                        }
                        is NetworkResult.Exception -> {
                            errorMessage = result.throwable.message ?: "Failed to generate code"
                        }
                    }
                }
            }
        )
    }
}
