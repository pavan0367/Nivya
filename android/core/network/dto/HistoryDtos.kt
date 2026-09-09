package com.nivya.core.network.dto

import com.google.gson.annotations.SerializedName

data class HistoryEventDto(
    @SerializedName("id") val id: Long,
    @SerializedName("deviceId") val deviceId: Long? = null,
    @SerializedName("packageName") val packageName: String,
    @SerializedName("appName") val appName: String,
    @SerializedName("broadActivity") val broadActivity: String,
    @SerializedName("activityLabel") val activityLabel: String? = null,
    @SerializedName("category") val category: String = "GENERAL",
    @SerializedName("durationSeconds") val durationSeconds: Int? = null,
    @SerializedName("durationFormatted") val durationFormatted: String? = null,
    @SerializedName("eventTimestamp") val eventTimestamp: String
)

data class HistoryEventDetailDto(
    @SerializedName("id") val id: Long,
    @SerializedName("deviceId") val deviceId: Long? = null,
    @SerializedName("deviceName") val deviceName: String? = null,
    @SerializedName("packageName") val packageName: String,
    @SerializedName("appName") val appName: String,
    @SerializedName("broadActivity") val broadActivity: String,
    @SerializedName("activityLabel") val activityLabel: String? = null,
    @SerializedName("category") val category: String = "GENERAL",
    @SerializedName("durationSeconds") val durationSeconds: Int? = null,
    @SerializedName("durationFormatted") val durationFormatted: String? = null,
    @SerializedName("details") val details: String? = null,
    @SerializedName("eventTimestamp") val eventTimestamp: String,
    @SerializedName("recordedAt") val recordedAt: String? = null
)

data class HistoryPageResponseDto(
    @SerializedName("items") val items: List<HistoryEventDto> = emptyList(),
    @SerializedName("currentPage") val currentPage: Int = 0,
    @SerializedName("totalPages") val totalPages: Int = 0,
    @SerializedName("totalElements") val totalElements: Long = 0,
    @SerializedName("pageSize") val pageSize: Int = 20,
    @SerializedName("hasNext") val hasNext: Boolean = false,
    @SerializedName("hasPrevious") val hasPrevious: Boolean = false
)

data class RecordHistoryRequestDto(
    @SerializedName("deviceUuid") val deviceUuid: String,
    @SerializedName("packageName") val packageName: String,
    @SerializedName("appName") val appName: String,
    @SerializedName("broadActivity") val broadActivity: String,
    @SerializedName("activityLabel") val activityLabel: String? = null,
    @SerializedName("category") val category: String = "GENERAL",
    @SerializedName("durationSeconds") val durationSeconds: Int? = null,
    @SerializedName("eventTimestamp") val eventTimestamp: String? = null,
    @SerializedName("details") val details: String? = null
)
