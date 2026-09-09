package com.nivya.core.network.dto

import com.google.gson.annotations.SerializedName

/**
 * Activity telemetry request payload sent from child device.
 * Strictest privacy standard: NEVER sends message bodies, passwords, or call audio.
 */
data class LiveActivityRequestDto(
    @SerializedName("deviceUuid") val deviceUuid: String,
    @SerializedName("packageName") val packageName: String,
    @SerializedName("appName") val appName: String,
    @SerializedName("broadActivity") val broadActivity: String,
    @SerializedName("category") val category: String = "GENERAL",
    @SerializedName("durationSeconds") val durationSeconds: Int? = null,
    @SerializedName("isCurrent") val isCurrent: Boolean = true,
    @SerializedName("startedAt") val startedAt: String? = null,
    @SerializedName("endedAt") val endedAt: String? = null
)

/**
 * High-level activity event received by Parent accounts.
 */
data class ActivityEventDto(
    @SerializedName("id") val id: Long,
    @SerializedName("deviceId") val deviceId: Long? = null,
    @SerializedName("packageName") val packageName: String,
    @SerializedName("appName") val appName: String,
    @SerializedName("broadActivity") val broadActivity: String,
    @SerializedName("category") val category: String = "GENERAL",
    @SerializedName("durationSeconds") val durationSeconds: Int? = null,
    @SerializedName("durationFormatted") val durationFormatted: String? = null,
    @SerializedName("isCurrent") val isCurrent: Boolean = true,
    @SerializedName("startedAt") val startedAt: String,
    @SerializedName("endedAt") val endedAt: String? = null,
    @SerializedName("createdAt") val createdAt: String
)

/**
 * Real-time live activity response with current activity and chronological timeline.
 */
data class LiveActivityResponseDto(
    @SerializedName("deviceId") val deviceId: Long,
    @SerializedName("deviceUuid") val deviceUuid: String? = null,
    @SerializedName("deviceName") val deviceName: String? = null,
    @SerializedName("isOnline") val isOnline: Boolean = false,
    @SerializedName("currentActivity") val currentActivity: ActivityEventDto? = null,
    @SerializedName("recentActivities") val recentActivities: List<ActivityEventDto> = emptyList(),
    @SerializedName("lastUpdatedAt") val lastUpdatedAt: String? = null
)
