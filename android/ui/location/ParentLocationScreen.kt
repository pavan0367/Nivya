package com.nivya.ui.location

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nivya.core.network.dto.LocationPointDto
import com.nivya.ui.common.*
import com.nivya.ui.theme.*

/**
 * Parent Location Screen presenting real-time/last-known child coordinates,
 * stale indication when offline or GPS is inactive, and consented location breadcrumbs.
 */
@Composable
fun ParentLocationScreen(
    viewModel: ParentLocationViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                SectionHeader(title = "Location & Safe Zones")
                Text(
                    text = uiState.deviceName,
                    style = MaterialTheme.typography.bodySmall,
                    color = BlueLight
                )
            }

            IconButton(
                onClick = { viewModel.loadData() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh location",
                    tint = BlueLight
                )
            }
        }

        if (uiState.isLoading && uiState.currentLocation == null) {
            LoadingState(message = "Locating child device...")
            return
        }

        uiState.errorMessage?.let { error ->
            ErrorState(
                message = error,
                onRetry = { viewModel.loadData() }
            )
        }

        val loc = uiState.currentLocation

        // Stale Location Alert when Device is Offline or GPS Disabled
        if (loc != null && loc.isStale) {
            StaleDataIndicator(
                reason = loc.staleDescription ?: "Child device offline — showing last known coordinates (${loc.lastUpdateAgo})"
            )
        }

        // Current / Last-Known Location Card
        NivyaCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (loc?.isStale == true) "Last Known Location" else "Current Location",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Updated: ${loc?.lastUpdateAgo ?: "Unknown"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (loc?.isStale == true) WarningAmber.copy(alpha = 0.15f) else SuccessGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (loc?.isStale == true) "Stale" else "Live",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (loc?.isStale == true) WarningAmber else SuccessGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (loc != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Latitude",
                        value = String.format("%.4f°", loc.latitude),
                        unit = "North",
                        icon = Icons.Default.Place,
                        accentColor = BluePrimary,
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "Longitude",
                        value = String.format("%.4f°", loc.longitude),
                        unit = "West",
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
                        text = "Accuracy: ${loc.accuracyMeters?.let { "±${it.toInt()}m" } ?: "Normal"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "Provider: ${loc.provider.uppercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            } else {
                Text(
                    text = "No location fix reported yet by child device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }

        // Diagnostics & Provider Availability
        SectionHeader(title = "Device Location Telemetry")
        NivyaCard {
            DiagnosticItem(
                label = "GPS Hardware Signal",
                status = if (loc?.isGpsAvailable == true) "Active & Locked" else "Disabled / Unlocked",
                isOk = loc?.isGpsAvailable == true
            )
            Spacer(modifier = Modifier.height(10.dp))
            DiagnosticItem(
                label = "Network Connectivity",
                status = if (loc?.isNetworkAvailable == true) "Online" else "Offline",
                isOk = loc?.isNetworkAvailable == true
            )
            Spacer(modifier = Modifier.height(10.dp))
            DiagnosticItem(
                label = "Background Tracking Consent",
                status = if (loc?.isBackgroundConsented == true) "Explicitly Granted" else "Foreground Only",
                isOk = loc?.isBackgroundConsented == true
            )
        }

        // Location History Breadcrumbs Timeline
        SectionHeader(title = "Location History (Past 24h)")

        if (!uiState.isConsentGranted) {
            NivyaCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Location History Not Consented",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = WarningAmber
                        )
                        Text(
                            text = uiState.consentMessage ?: "Historical movement breadcrumbs require explicit location consent under family terms.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else if (uiState.historyPoints.isEmpty()) {
            NivyaCard {
                Text(
                    text = "No movements recorded yet today. Stationary filtering is active.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        } else {
            uiState.historyPoints.takeLast(10).reversed().forEach { point ->
                BreadcrumbCard(point = point)
            }
        }
    }
}

@Composable
private fun BreadcrumbCard(point: LocationPointDto) {
    NivyaCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = BluePrimary.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = null,
                            tint = BluePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "${String.format("%.4f", point.latitude)}°, ${String.format("%.4f", point.longitude)}°",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Accuracy: ${point.accuracyMeters?.let { "±${it.toInt()}m" } ?: "Normal"} • ${point.sourceMode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            Text(
                text = point.recordedAt.substringAfter('T').take(5),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = BlueLight
            )
        }
    }
}

@Composable
private fun DiagnosticItem(
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
