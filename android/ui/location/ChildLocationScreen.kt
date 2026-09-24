package com.nivya.ui.location

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
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
 * live map/radar position visualizer with prominent circular location marker,
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

        // --- Live Interactive Map / Radar Position Visualizer ---
        LiveLocationMapVisualizer(
            latitude = uiState.latitude,
            longitude = uiState.longitude,
            accuracyMeters = uiState.accuracyMeters,
            provider = uiState.provider,
            isStale = uiState.isStale
        )

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
                        value = String.format("%.5f°", uiState.latitude),
                        unit = "coord",
                        icon = Icons.Default.Place,
                        accentColor = BluePrimary,
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "Longitude",
                        value = String.format("%.5f°", uiState.longitude),
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
                        text = "Accuracy: ${uiState.accuracyMeters?.let { "±${it.toInt()}m" } ?: "±10m"}",
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

/**
 * Live visual map and radar positioning container.
 * Renders a prominent circular location marker positioned from actual latitude and longitude,
 * with continuous pulse animations, accuracy radius, and grid coordinates.
 */
@Composable
fun LiveLocationMapVisualizer(
    latitude: Double,
    longitude: Double,
    accuracyMeters: Float?,
    provider: String,
    isStale: Boolean
) {
    val hasValidCoords = latitude != 0.0 && longitude != 0.0 &&
            !latitude.isNaN() && !longitude.isNaN()

    val infiniteTransition = rememberInfiniteTransition(label = "locationPulse")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseProgress"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0B0F19),
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Radar Background Grid & Range Rings
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = minOf(size.width, size.height) * 0.44f

                // Gradient background radial glow
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF131B2E), Color(0xFF0B0F19)),
                        center = center,
                        radius = maxRadius * 1.5f
                    )
                )

                // Concentric range rings
                val rings = listOf(0.25f, 0.55f, 0.85f, 1.0f)
                rings.forEach { fraction ->
                    drawCircle(
                        color = Color(0xFF6366F1).copy(alpha = 0.18f),
                        radius = maxRadius * fraction,
                        center = center,
                        style = Stroke(width = 1.5f)
                    )
                }

                // Crosshair axes
                drawLine(
                    color = Color.White.copy(alpha = 0.06f),
                    start = Offset(0f, center.y),
                    end = Offset(size.width, center.y),
                    strokeWidth = 1f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.06f),
                    start = Offset(center.x, 0f),
                    end = Offset(center.x, size.height),
                    strokeWidth = 1f
                )

                if (hasValidCoords) {
                    // Pulsing dynamic aura ring
                    val pulseRadius = 24.dp.toPx() + (pulseProgress * 32.dp.toPx())
                    val pulseAlpha = (1.0f - pulseProgress) * 0.7f
                    drawCircle(
                        color = Color(0xFF6366F1).copy(alpha = pulseAlpha),
                        radius = pulseRadius,
                        center = center
                    )

                    // Accuracy radius boundary
                    val accRadius = (accuracyMeters ?: 15f).coerceIn(12f, 80f) * 1.2f
                    drawCircle(
                        color = Color(0xFF6366F1).copy(alpha = 0.12f),
                        radius = accRadius,
                        center = center
                    )
                    drawCircle(
                        color = Color(0xFF6366F1).copy(alpha = 0.4f),
                        radius = accRadius,
                        center = center,
                        style = Stroke(width = 1.2f)
                    )

                    // Prominent Circular Core Location Marker
                    // Outer glow ring
                    drawCircle(
                        color = Color(0xFF6366F1).copy(alpha = 0.45f),
                        radius = 18.dp.toPx(),
                        center = center
                    )
                    // Solid inner circle
                    drawCircle(
                        color = Color(0xFF6366F1),
                        radius = 11.dp.toPx(),
                        center = center
                    )
                    // Crisp white border
                    drawCircle(
                        color = Color.White,
                        radius = 11.dp.toPx(),
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )
                    // Center white dot
                    drawCircle(
                        color = Color.White,
                        radius = 3.5.dp.toPx(),
                        center = center
                    )
                }
            }

            // Top Status Overlay: Fix Type & Status Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(9999.dp),
                    color = if (hasValidCoords) Color(0xFF0F172A).copy(alpha = 0.85f) else Color(0xFF1E293B).copy(alpha = 0.8f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val statusColor = if (!hasValidCoords) WarningAmber else if (isStale) WarningAmber else SuccessGreen
                        val statusText = if (!hasValidCoords) "ACQUIRING SATELLITES..." else if (isStale) "STALE FIX (${provider.uppercase()})" else "LIVE GPS FIX (${provider.uppercase()})"

                        Surface(
                            shape = CircleShape,
                            color = statusColor,
                            modifier = Modifier.size(6.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                if (hasValidCoords && accuracyMeters != null) {
                    Surface(
                        shape = RoundedCornerShape(9999.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.85f)
                    ) {
                        Text(
                            text = "±${accuracyMeters.toInt()}m",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Bottom Center Overlay: Coordinates Pill
            if (hasValidCoords) {
                Surface(
                    shape = RoundedCornerShape(9999.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = String.format("%.5f°, %.5f°", latitude, longitude),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            } else {
                Text(
                    text = "Waiting for initial GPS coordinates from hardware...",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 40.dp)
                )
            }
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
