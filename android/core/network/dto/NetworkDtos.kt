package com.nivya.core.network.dto

import com.google.gson.annotations.SerializedName

/**
 * Network telemetry and status DTO models matching the Spring Boot backend API.
 */
data class NetworkTelemetryRequestDto(
    @SerializedName("deviceUuid") val deviceUuid: String,
    @SerializedName("networkType") val networkType: String = "NONE",
    @SerializedName("connectionType") val connectionType: String? = null,
    @SerializedName("isNetworkAvailable") val isNetworkAvailable: Boolean = false,
    @SerializedName("isInternetAvailable") val isInternetAvailable: Boolean = false,
    @SerializedName("signalLevel") val signalLevel: Int? = null,
    @SerializedName("signalDbm") val signalDbm: Int? = null,
    @SerializedName("quality") val quality: String = "UNAVAILABLE",
    @SerializedName("ssid") val ssid: String? = null,
    @SerializedName("ipAddress") val ipAddress: String? = null,
    @SerializedName("recordedAt") val recordedAt: String? = null
)

data class NetworkStatusResponseDto(
    @SerializedName("deviceId") val deviceId: Long,
    @SerializedName("deviceUuid") val deviceUuid: String,
    @SerializedName("deviceName") val deviceName: String,
    @SerializedName("networkType") val networkType: String,
    @SerializedName("connectionType") val connectionType: String?,
    @SerializedName("isNetworkAvailable") val isNetworkAvailable: Boolean,
    @SerializedName("isInternetAvailable") val isInternetAvailable: Boolean,
    @SerializedName("signalLevel") val signalLevel: Int?,
    @SerializedName("signalDbm") val signalDbm: Int?,
    @SerializedName("quality") val quality: String,
    @SerializedName("ssid") val ssid: String?,
    @SerializedName("ipAddress") val ipAddress: String?,
    @SerializedName("lastSyncAt") val lastSyncAt: String?
)

data class NetworkHistoryResponseDto(
    @SerializedName("deviceId") val deviceId: Long,
    @SerializedName("deviceUuid") val deviceUuid: String,
    @SerializedName("points") val points: List<NetworkPointDto> = emptyList()
)

data class NetworkPointDto(
    @SerializedName("networkType") val networkType: String,
    @SerializedName("connectionType") val connectionType: String?,
    @SerializedName("isNetworkAvailable") val isNetworkAvailable: Boolean,
    @SerializedName("isInternetAvailable") val isInternetAvailable: Boolean,
    @SerializedName("signalLevel") val signalLevel: Int?,
    @SerializedName("quality") val quality: String,
    @SerializedName("recordedAt") val recordedAt: String?
)
