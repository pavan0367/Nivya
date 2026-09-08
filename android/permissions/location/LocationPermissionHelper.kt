package com.nivya.permissions.location

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

enum class LocationPermissionState {
    GRANTED_BACKGROUND,
    GRANTED_FOREGROUND_ONLY,
    DENIED,
    REVOKED
}

enum class LocationAvailabilityState {
    AVAILABLE,
    GPS_DISABLED,
    NETWORK_DISABLED,
    PERMISSION_DENIED,
    STALE
}

/**
 * Official helper for Android Location runtime permissions, background location consent,
 * and GPS / Network hardware provider status.
 * Strictly adheres to Android security guidelines and never attempts permission workarounds.
 */
object LocationPermissionHelper {

    fun hasForegroundLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun hasFineLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    fun hasBackgroundLocationPermission(context: Context): Boolean {
        if (!hasForegroundLocationPermission(context)) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Prior to Android 10 (Q), background location was bundled with foreground location
        }
    }

    fun getLocationPermissionState(context: Context): LocationPermissionState {
        return when {
            hasBackgroundLocationPermission(context) -> LocationPermissionState.GRANTED_BACKGROUND
            hasForegroundLocationPermission(context) -> LocationPermissionState.GRANTED_FOREGROUND_ONLY
            else -> LocationPermissionState.DENIED
        }
    }

    fun isGpsProviderEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return try {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (_: Exception) {
            false
        }
    }

    fun isNetworkProviderEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return try {
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {
            false
        }
    }

    fun isLocationProviderAvailable(context: Context): Boolean {
        return isGpsProviderEnabled(context) || isNetworkProviderEnabled(context)
    }

    fun createLocationSettingsIntent(): Intent {
        return Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun createAppSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
