package com.nivya.core.network.dto

import com.google.gson.annotations.SerializedName

/**
 * Location telemetry and response DTO models matching the Spring Boot backend API.
 */
data class LocationTelemetryRequestDto(
    @SerializedName("deviceUuid") val deviceUuid: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("accuracyMeters") val accuracyMeters: Float? = null,
    @SerializedName("altitudeMeters") val altitudeMeters: Double? = null,
    @SerializedName("speedMetersPerSec") val speedMetersPerSec: Float? = null,
    @SerializedName("bearingDegrees") val bearingDegrees: Float? = null,
    @SerializedName("provider") val provider: String = "gps",
    @SerializedName("isGpsAvailable") val isGpsAvailable: Boolean = true,
    @SerializedName("isNetworkAvailable") val isNetworkAvailable: Boolean = true,
    @SerializedName("permissionState") val permissionState: String = "GRANTED",
    @SerializedName("isBackgroundConsented") val isBackgroundConsented: Boolean = false,
    @SerializedName("sourceMode") val sourceMode: String = "FOREGROUND",
    @SerializedName("recordedAt") val recordedAt: String? = null
)

data class LocationStatusResponseDto(
    @SerializedName("deviceId") val deviceId: Long,
    @SerializedName("deviceUuid") val deviceUuid: String,
    @SerializedName("deviceName") val deviceName: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("accuracyMeters") val accuracyMeters: Float? = null,
    @SerializedName("altitudeMeters") val altitudeMeters: Double? = null,
    @SerializedName("speedMetersPerSec") val speedMetersPerSec: Float? = null,
    @SerializedName("bearingDegrees") val bearingDegrees: Float? = null,
    @SerializedName("provider") val provider: String,
    @SerializedName("isGpsAvailable") val isGpsAvailable: Boolean,
    @SerializedName("isNetworkAvailable") val isNetworkAvailable: Boolean,
    @SerializedName("permissionState") val permissionState: String,
    @SerializedName("isBackgroundConsented") val isBackgroundConsented: Boolean,
    @SerializedName("isStale") val isStale: Boolean,
    @SerializedName("staleDescription") val staleDescription: String? = null,
    @SerializedName("recordedAt") val recordedAt: String,
    @SerializedName("updatedAt") val updatedAt: String,
    @SerializedName("lastUpdateAgo") val lastUpdateAgo: String? = null
)

data class LocationPointDto(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("accuracyMeters") val accuracyMeters: Float? = null,
    @SerializedName("provider") val provider: String,
    @SerializedName("sourceMode") val sourceMode: String,
    @SerializedName("recordedAt") val recordedAt: String
)

data class LocationHistoryResponseDto(
    @SerializedName("deviceId") val deviceId: Long,
    @SerializedName("deviceUuid") val deviceUuid: String,
    @SerializedName("points") val points: List<LocationPointDto> = emptyList(),
    @SerializedName("totalPoints") val totalPoints: Int,
    @SerializedName("consentGranted") val consentGranted: Boolean,
    @SerializedName("message") val message: String? = null
)
