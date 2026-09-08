package com.nivya.ui.location

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.nivya.permissions.location.LocationPermissionHelper
import com.nivya.permissions.location.LocationPermissionState
import com.nivya.ui.common.*
import com.nivya.ui.theme.*

/**
 * Child Location Screen providing transparent visibility into own coordinates,
 * official permission state, hardware GPS/network availability, and privacy disclosures.
 */
@Composable
fun ChildLocationScreen(
    viewModel: ChildLocationViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollState = rememberScrollState()

    // Automatically recheck permissions & GPS availability upon returning from Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissionsAndProvider()
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header with Refresh Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(title = "Location Sharing")

            IconButton(
                onClick = { viewModel.refresh() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh location",
                    tint = BlueLight
                )
            }
        }

        // --- Hardware / Permission Availability Warning States ---
        if (uiState.permissionState == LocationPermissionState.DENIED ||
            uiState.permissionState == LocationPermissionState.REVOKED) {
            PermissionRequiredState(
                permissionName = "Location Access",
                rationale = "Nivya needs location access so your parents can confirm you have arrived safely. Nivya never shares your location outside your verified family.",
                onGrantClick = {
                    context.startActivity(LocationPermissionHelper.createAppSettingsIntent(context))
                }
            )
        }

        if (!uiState.isGpsAvailable) {
            Surface(
                color = WarningAmber.copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsOff,
                            contentDescription = null,
                            tint = WarningAmber,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Device GPS is Turned Off",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = WarningAmber
                            )
                            Text(
                                text = "Turn on device location in Android settings for accurate positioning.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            context.startActivity(LocationPermissionHelper.createLocationSettingsIntent())
                        }
                    ) {
                        Text(text = "Enable", color = WarningAmber, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (uiState.isStale && uiState.permissionState != LocationPermissionState.DENIED) {
            StaleDataIndicator(reason = "GPS inactive or device stationary — showing last known coordinates (${uiState.lastUpdatedText})")
        }

        // --- Current Location Card ---
        NivyaCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "My Current Location",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Updated ${uiState.lastUpdatedText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                val badgeColor = when (uiState.permissionState) {
                    LocationPermissionState.GRANTED_BACKGROUND -> SuccessGreen
                    LocationPermissionState.GRANTED_FOREGROUND_ONLY -> BluePrimary
                    else -> ErrorRed
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = when (uiState.permissionState) {
                            LocationPermissionState.GRANTED_BACKGROUND -> "All Time"
                            LocationPermissionState.GRANTED_FOREGROUND_ONLY -> "While Using"
                            else -> "Denied"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (uiState.latitude != 0.0 || uiState.longitude != 0.0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Latitude",
                        value = String.format("%.4f°", uiState.latitude),
                        unit = "coord",
                        icon = Icons.Default.Place,
                        accentColor = BluePrimary,
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "Longitude",
                        value = String.format("%.4f°", uiState.longitude),
                        unit = "coord",
                        icon = Icons.Default.Place,
                        accentColor = BlueLight,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Accuracy: ${uiState.accuracyMeters?.let { "±${it.toInt()}m" } ?: "Normal"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "Provider: ${uiState.provider.uppercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            } else {
                Text(
                    text = "Awaiting initial GPS location fix...",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // --- Hardware & Diagnostics Status ---
        SectionHeader(title = "Hardware & Status")
        NivyaCard {
            DiagnosticRow(
                label = "GPS Receiver",
                status = if (uiState.isGpsAvailable) "Enabled" else "Disabled",
                isOk = uiState.isGpsAvailable
            )
            Spacer(modifier = Modifier.height(10.dp))
            DiagnosticRow(
                label = "Network Assisted Location",
                status = if (uiState.isNetworkAvailable) "Connected" else "Unavailable",
                isOk = uiState.isNetworkAvailable
            )
            Spacer(modifier = Modifier.height(10.dp))
            DiagnosticRow(
                label = "Background Location Sharing",
                status = if (uiState.isBackgroundConsented) "Consented" else "Foreground Only",
                isOk = uiState.isBackgroundConsented
            )
        }

        // --- Privacy Guarantee Card ---
        SectionHeader(title = "Safety & Consent Disclosure")
        NivyaCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Encrypted Family Location",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Your location is only accessible to your linked family parents. Nivya never shares or sells your location to advertising networks or external third parties.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun DiagnosticRow(
    label: String,
    status: String,
    isOk: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = if (isOk) SuccessGreen else WarningAmber,
                modifier = Modifier.size(6.dp)
            ) {}
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isOk) SuccessGreen else WarningAmber
            )
        }
    }
}
