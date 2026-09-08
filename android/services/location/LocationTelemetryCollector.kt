package com.nivya.services.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.nivya.permissions.location.LocationPermissionHelper
import com.nivya.permissions.location.LocationPermissionState

data class LocationSnapshot(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val altitudeMeters: Double?,
    val speedMetersPerSec: Float?,
    val bearingDegrees: Float?,
    val provider: String,
    val isGpsAvailable: Boolean,
    val isNetworkAvailable: Boolean,
    val permissionState: LocationPermissionState,
    val isBackgroundConsented: Boolean,
    val isStale: Boolean,
    val timestamp: Long,
    val sourceMode: String = "FOREGROUND"
)

/**
 * Official Android LocationTelemetryCollector using LocationManager.
 * Strictly uses Android platform location APIs without requiring proprietary binary dependencies.
 */
object LocationTelemetryCollector {

    @SuppressLint("MissingPermission")
    fun collect(context: Context, sourceMode: String = "FOREGROUND"): LocationSnapshot {
        val permissionState = LocationPermissionHelper.getLocationPermissionState(context)
        val hasPermission = LocationPermissionHelper.hasForegroundLocationPermission(context)
        val isGpsAvailable = LocationPermissionHelper.isGpsProviderEnabled(context)
        val isNetworkAvailable = LocationPermissionHelper.isNetworkProviderEnabled(context)
        val isBackgroundConsented = LocationPermissionHelper.hasBackgroundLocationPermission(context)

        if (!hasPermission) {
            return LocationSnapshot(
                latitude = 0.0,
                longitude = 0.0,
                accuracyMeters = null,
                altitudeMeters = null,
                speedMetersPerSec = null,
                bearingDegrees = null,
                provider = "none",
                isGpsAvailable = isGpsAvailable,
                isNetworkAvailable = isNetworkAvailable,
                permissionState = LocationPermissionState.DENIED,
                isBackgroundConsented = false,
                isStale = true,
                timestamp = System.currentTimeMillis(),
                sourceMode = sourceMode
            )
        }

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return LocationSnapshot(
                latitude = 0.0,
                longitude = 0.0,
                accuracyMeters = null,
                altitudeMeters = null,
                speedMetersPerSec = null,
                bearingDegrees = null,
                provider = "none",
                isGpsAvailable = false,
                isNetworkAvailable = false,
                permissionState = permissionState,
                isBackgroundConsented = isBackgroundConsented,
                isStale = true,
                timestamp = System.currentTimeMillis(),
                sourceMode = sourceMode
            )

        // Select the most recent and accurate available location fix
        var bestLocation: Location? = null

        try {
            if (isGpsAvailable) {
                val gpsLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (gpsLoc != null) {
                    bestLocation = gpsLoc
                }
            }
        } catch (_: Exception) {}

        try {
            if (isNetworkAvailable) {
                val netLoc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (netLoc != null) {
                    if (bestLocation == null || netLoc.time > bestLocation.time) {
                        bestLocation = netLoc
                    }
                }
            }
        } catch (_: Exception) {}

        try {
            val passiveLoc = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            if (passiveLoc != null && (bestLocation == null || passiveLoc.time > bestLocation.time)) {
                bestLocation = passiveLoc
            }
        } catch (_: Exception) {}

        if (bestLocation != null) {
            val now = System.currentTimeMillis()
            val timeDiffMs = now - bestLocation.time
            val isStale = timeDiffMs > 15 * 60 * 1000 // > 15 minutes is stale

            return LocationSnapshot(
                latitude = bestLocation.latitude,
                longitude = bestLocation.longitude,
                accuracyMeters = if (bestLocation.hasAccuracy()) bestLocation.accuracy else null,
                altitudeMeters = if (bestLocation.hasAltitude()) bestLocation.altitude else null,
                speedMetersPerSec = if (bestLocation.hasSpeed()) bestLocation.speed else null,
                bearingDegrees = if (bestLocation.hasBearing()) bestLocation.bearing else null,
                provider = bestLocation.provider ?: "fused",
                isGpsAvailable = isGpsAvailable,
                isNetworkAvailable = isNetworkAvailable,
                permissionState = permissionState,
                isBackgroundConsented = isBackgroundConsented,
                isStale = isStale,
                timestamp = bestLocation.time,
                sourceMode = sourceMode
            )
        }

        // When no previous location fix is stored yet in system
        return LocationSnapshot(
            latitude = 0.0,
            longitude = 0.0,
            accuracyMeters = null,
            altitudeMeters = null,
            speedMetersPerSec = null,
            bearingDegrees = null,
            provider = if (isGpsAvailable) "gps_waiting_fix" else "none",
            isGpsAvailable = isGpsAvailable,
            isNetworkAvailable = isNetworkAvailable,
            permissionState = permissionState,
            isBackgroundConsented = isBackgroundConsented,
            isStale = true,
            timestamp = System.currentTimeMillis(),
            sourceMode = sourceMode
        )
    }
}
