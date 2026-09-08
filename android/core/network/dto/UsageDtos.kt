package com.nivya.core.network.dto

import com.google.gson.annotations.SerializedName

/**
 * Screen time & app usage telemetry and response DTO models matching the Spring Boot backend API.
 */
data class UsageTelemetryRequestDto(
    @SerializedName("deviceUuid") val deviceUuid: String,
    @SerializedName("date") val date: String? = null,
    @SerializedName("totalForegroundSeconds") val totalForegroundSeconds: Long,
    @SerializedName("screenUnlocks") val screenUnlocks: Int = 0,
    @SerializedName("apps") val apps: List<AppUsageItemDto> = emptyList()
)

data class AppUsageItemDto(
    @SerializedName("packageName") val packageName: String,
    @SerializedName("appName") val appName: String,
    @SerializedName("category") val category: String,
    @SerializedName("foregroundSeconds") val foregroundSeconds: Long,
    @SerializedName("lastTimeUsed") val lastTimeUsed: String? = null
)

data class UsageSummaryResponseDto(
    @SerializedName("deviceId") val deviceId: Long,
    @SerializedName("deviceUuid") val deviceUuid: String,
    @SerializedName("deviceName") val deviceName: String,
    @SerializedName("date") val date: String,
    @SerializedName("totalForegroundSeconds") val totalForegroundSeconds: Long,
    @SerializedName("formattedTotalTime") val formattedTotalTime: String,
    @SerializedName("educationalSeconds") val educationalSeconds: Long,
    @SerializedName("recreationalSeconds") val recreationalSeconds: Long,
    @SerializedName("socialSeconds") val socialSeconds: Long,
    @SerializedName("productivitySeconds") val productivitySeconds: Long,
    @SerializedName("screenUnlocks") val screenUnlocks: Int,
    @SerializedName("categoryBreakdown") val categoryBreakdown: Map<String, Long> = emptyMap()
)

data class AppUsageResponseDto(
    @SerializedName("deviceId") val deviceId: Long,
    @SerializedName("date") val date: String,
    @SerializedName("apps") val apps: List<AppUsageDetailDto> = emptyList()
)

data class AppUsageDetailDto(
    @SerializedName("packageName") val packageName: String,
    @SerializedName("appName") val appName: String,
    @SerializedName("category") val category: String,
    @SerializedName("foregroundSeconds") val foregroundSeconds: Long,
    @SerializedName("formattedDuration") val formattedDuration: String,
    @SerializedName("percentageOfTotal") val percentageOfTotal: Double,
    @SerializedName("lastTimeUsed") val lastTimeUsed: String? = null
)

data class UsageTrendResponseDto(
    @SerializedName("deviceId") val deviceId: Long,
    @SerializedName("dailyPoints") val dailyPoints: List<DailyUsagePointDto> = emptyList(),
    @SerializedName("currentWeekTotalSeconds") val currentWeekTotalSeconds: Long,
    @SerializedName("previousWeekTotalSeconds") val previousWeekTotalSeconds: Long,
    @SerializedName("percentageChange") val percentageChange: Double,
    @SerializedName("trendDescription") val trendDescription: String
)

data class DailyUsagePointDto(
    @SerializedName("date") val date: String,
    @SerializedName("totalSeconds") val totalSeconds: Long,
    @SerializedName("formattedDuration") val formattedDuration: String,
    @SerializedName("educationalSeconds") val educationalSeconds: Long
)
