package com.nivya.services.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Tasks
import com.nivya.permissions.location.LocationPermissionHelper
import com.nivya.permissions.location.LocationPermissionState
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

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
) {
    val hasValidCoordinates: Boolean
        get() = latitude != 0.0 && longitude != 0.0 &&
                !latitude.isNaN() && !longitude.isNaN() &&
                latitude >= -90.0 && latitude <= 90.0 &&
                longitude >= -180.0 && longitude <= 180.0
}

/**
 * Production LocationTelemetryCollector leveraging Google Play Services FusedLocationProviderClient
 * with robust LocationManager fallback.
 * Ensures real-time continuous GPS tracking and strictly avoids dummy/seeded coordinates.
 */
object LocationTelemetryCollector {

    private val cachedLocation = AtomicReference<Location?>(null)
    private var isContinuousStarted = false
    private var fusedClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null

    private fun isValidLocation(loc: Location?): Boolean {
        if (loc == null) return false
        val lat = loc.latitude
        val lng = loc.longitude
        return lat != 0.0 && lng != 0.0 &&
                !lat.isNaN() && !lng.isNaN() &&
                lat >= -90.0 && lat <= 90.0 &&
                lng >= -180.0 && lng <= 180.0
    }

    @SuppressLint("MissingPermission")
    @Synchronized
    fun startContinuousUpdates(context: Context) {
        if (isContinuousStarted) return
        val appContext = context.applicationContext
        if (!LocationPermissionHelper.hasForegroundLocationPermission(appContext)) return

        try {
            fusedClient = LocationServices.getFusedLocationProviderClient(appContext)
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
                .setMinUpdateIntervalMillis(1000L)
                .setMinUpdateDistanceMeters(2.0f)
                .setWaitForAccurateLocation(false)
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val lastLoc = result.lastLocation
                    if (isValidLocation(lastLoc)) {
                        cachedLocation.set(lastLoc)
                    }
                }
            }

            fusedClient?.requestLocationUpdates(
                locationRequest,
                locationCallback as LocationCallback,
                Looper.getMainLooper()
            )
            isContinuousStarted = true
        } catch (_: Exception) {
            // Fallback to platform LocationManager
            startLocationManagerFallback(appContext)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationManagerFallback(context: Context) {
        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
            val listener = object : LocationListener {
                override fun onLocationChanged(loc: Location) {
                    if (isValidLocation(loc)) {
                        cachedLocation.set(loc)
                    }
                }
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            }

            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000L, 2.0f, listener, Looper.getMainLooper())
            }
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 5.0f, listener, Looper.getMainLooper())
            }
            isContinuousStarted = true
        } catch (_: Exception) {}
    }

    @Synchronized
    fun stopContinuousUpdates() {
        try {
            locationCallback?.let { fusedClient?.removeLocationUpdates(it) }
        } catch (_: Exception) {}
        locationCallback = null
        fusedClient = null
        isContinuousStarted = false
    }

    @SuppressLint("MissingPermission")
    fun collect(context: Context, sourceMode: String = "FOREGROUND"): LocationSnapshot {
        val appContext = context.applicationContext
        val permissionState = LocationPermissionHelper.getLocationPermissionState(appContext)
        val hasPermission = LocationPermissionHelper.hasForegroundLocationPermission(appContext)
        val isGpsAvailable = LocationPermissionHelper.isGpsProviderEnabled(appContext)
        val isNetworkAvailable = LocationPermissionHelper.isNetworkProviderEnabled(appContext)
        val isBackgroundConsented = LocationPermissionHelper.hasBackgroundLocationPermission(appContext)

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

        // Make sure continuous updates are initiated
        if (!isContinuousStarted) {
            startContinuousUpdates(appContext)
        }

        var bestLocation: Location? = cachedLocation.get()

        // If cached location is null or stale (> 30s), attempt synchronous fetch via FusedLocationClient
        val now = System.currentTimeMillis()
        if (bestLocation == null || (now - bestLocation.time) > 30_000L) {
            try {
                val client = LocationServices.getFusedLocationProviderClient(appContext)
                val task = client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                val freshLoc = Tasks.await(task, 2, TimeUnit.SECONDS)
                if (isValidLocation(freshLoc)) {
                    cachedLocation.set(freshLoc)
                    bestLocation = freshLoc
                }
            } catch (_: Exception) {}

            // Secondary fallback: FusedLocation lastLocation
            if (bestLocation == null) {
                try {
                    val client = LocationServices.getFusedLocationProviderClient(appContext)
                    val lastTask = client.lastLocation
                    val lastFused = Tasks.await(lastTask, 1, TimeUnit.SECONDS)
                    if (isValidLocation(lastFused)) {
                        cachedLocation.set(lastFused)
                        bestLocation = lastFused
                    }
                } catch (_: Exception) {}
            }

            // Tertiary fallback: LocationManager lastKnown
            if (bestLocation == null) {
                try {
                    val lm = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                    if (lm != null) {
                        val gpsLoc = if (isGpsAvailable) lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) else null
                        val netLoc = if (isNetworkAvailable) lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) else null
                        val candidate = when {
                            gpsLoc != null && netLoc != null -> if (gpsLoc.time >= netLoc.time) gpsLoc else netLoc
                            gpsLoc != null -> gpsLoc
                            else -> netLoc
                        }
                        if (isValidLocation(candidate)) {
                            cachedLocation.set(candidate)
                            bestLocation = candidate
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        if (bestLocation != null && isValidLocation(bestLocation)) {
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

        // When awaiting first GPS lock
        return LocationSnapshot(
            latitude = 0.0,
            longitude = 0.0,
            accuracyMeters = null,
            altitudeMeters = null,
            speedMetersPerSec = null,
            bearingDegrees = null,
            provider = if (isGpsAvailable) "gps_acquiring" else "none",
            isGpsAvailable = isGpsAvailable,
            isNetworkAvailable = isNetworkAvailable,
            permissionState = permissionState,
            isBackgroundConsented = isBackgroundConsented,
            isStale = true,
            timestamp = now,
            sourceMode = sourceMode
        )
    }
}
