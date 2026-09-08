package com.nivya.services.telemetry

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

data class NetworkSnapshot(
    val networkType: String,
    val connectionType: String,
    val isNetworkAvailable: Boolean,
    val isInternetAvailable: Boolean,
    val signalLevel: Int?,
    val signalDbm: Int?,
    val quality: String, // EXCELLENT, GOOD, WEAK, UNAVAILABLE
    val ssid: String? = null,
    val ipAddress: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Official Android Network Telemetry Collector using standard ConnectivityManager and NetworkCapabilities.
 * Collects connection type, network availability, validated internet reachability, and signal metrics.
 */
object NetworkTelemetryCollector {

    fun collect(context: Context): NetworkSnapshot {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return NetworkSnapshot(
                networkType = "NONE",
                connectionType = "Offline",
                isNetworkAvailable = false,
                isInternetAvailable = false,
                signalLevel = 0,
                signalDbm = null,
                quality = "UNAVAILABLE"
            )

        val activeNetwork = cm.activeNetwork
        val capabilities = if (activeNetwork != null) cm.getNetworkCapabilities(activeNetwork) else null

        if (activeNetwork == null || capabilities == null) {
            return NetworkSnapshot(
                networkType = "NONE",
                connectionType = "Offline",
                isNetworkAvailable = false,
                isInternetAvailable = false,
                signalLevel = 0,
                signalDbm = null,
                quality = "UNAVAILABLE"
            )
        }

        val isNetworkAvailable = true
        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val isInternetAvailable = hasInternet && isValidated

        // Identify Network & Connection Type
        val networkType: String
        val connectionType: String
        when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                networkType = "WIFI"
                connectionType = "Wi-Fi (Active)"
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                networkType = "CELLULAR"
                connectionType = "Mobile 5G / LTE"
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                networkType = "ETHERNET"
                connectionType = "Ethernet (Wired)"
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> {
                networkType = "VPN"
                connectionType = "Secure VPN"
            }
            else -> {
                networkType = "OTHER"
                connectionType = "Connected"
            }
        }

        // Measure Signal Metrics (using modern non-deprecated capabilities)
        var signalDbm: Int? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rawStrength = capabilities.signalStrength
            if (rawStrength != NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED) {
                signalDbm = rawStrength
            }
        }

        // Calculate Signal Level (0 to 4 scale)
        val downstreamBandwidth = capabilities.linkDownstreamBandwidthKbps
        val signalLevel = when {
            signalDbm != null -> {
                when {
                    signalDbm > -65 -> 4
                    signalDbm > -75 -> 3
                    signalDbm > -85 -> 2
                    signalDbm > -95 -> 1
                    else -> 0
                }
            }
            downstreamBandwidth > 25000 -> 4
            downstreamBandwidth > 10000 -> 3
            downstreamBandwidth > 2000 -> 2
            downstreamBandwidth > 0 -> 1
            else -> 2 // Default moderate level if online
        }

        // Compute Quality Indicator: EXCELLENT, GOOD, WEAK, UNAVAILABLE
        val quality = when {
            !isNetworkAvailable || !isInternetAvailable -> "UNAVAILABLE"
            signalLevel >= 4 -> "EXCELLENT"
            signalLevel >= 2 -> "GOOD"
            else -> "WEAK"
        }

        return NetworkSnapshot(
            networkType = networkType,
            connectionType = connectionType,
            isNetworkAvailable = isNetworkAvailable,
            isInternetAvailable = isInternetAvailable,
            signalLevel = signalLevel,
            signalDbm = signalDbm,
            quality = quality,
            timestamp = System.currentTimeMillis()
        )
    }
}
