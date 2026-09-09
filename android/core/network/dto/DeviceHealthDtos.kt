package com.nivya.core.network.dto

import com.google.gson.annotations.SerializedName

data class StorageHealthDto(
    @SerializedName("totalBytes") val totalBytes: Long,
    @SerializedName("usedBytes") val usedBytes: Long,
    @SerializedName("freeBytes") val freeBytes: Long,
    @SerializedName("usedPct") val usedPct: Double,
    @SerializedName("isLowStorage") val isLowStorage: Boolean
)

data class MemoryHealthDto(
    @SerializedName("totalBytes") val totalBytes: Long,
    @SerializedName("usedBytes") val usedBytes: Long,
    @SerializedName("freeBytes") val freeBytes: Long,
    @SerializedName("usedPct") val usedPct: Double,
    @SerializedName("isLowRam") val isLowRam: Boolean
)

data class PermissionHealthDto(
    @SerializedName("locationPermission") val locationPermission: String,
    @SerializedName("usagePermission") val usagePermission: String,
    @SerializedName("notificationPermission") val notificationPermission: String,
    @SerializedName("batteryOptimization") val batteryOptimization: String,
    @SerializedName("allHealthy") val allHealthy: Boolean
)

data class DeviceHealthResponseDto(
    @SerializedName("deviceId") val deviceId: Long,
    @SerializedName("deviceUuid") val deviceUuid: String,
    @SerializedName("deviceName") val deviceName: String,
    @SerializedName("deviceModel") val deviceModel: String?,
    @SerializedName("deviceManufacturer") val deviceManufacturer: String?,
    @SerializedName("osVersion") val osVersion: String?,
    @SerializedName("sdkVersion") val sdkVersion: Int?,
    @SerializedName("storage") val storage: StorageHealthDto,
    @SerializedName("memory") val memory: MemoryHealthDto,
    @SerializedName("batteryPct") val batteryPct: Int?,
    @SerializedName("chargingState") val chargingState: String?,
    @SerializedName("batteryHealth") val batteryHealth: String?,
    @SerializedName("batteryTempCelsius") val batteryTempCelsius: Double?,
    @SerializedName("networkType") val networkType: String?,
    @SerializedName("isOnline") val isOnline: Boolean,
    @SerializedName("syncState") val syncState: String,
    @SerializedName("permissionHealth") val permissionHealth: PermissionHealthDto,
    @SerializedName("healthScore") val healthScore: Int,
    @SerializedName("healthStatus") val healthStatus: String,
    @SerializedName("conditionSummary") val conditionSummary: String?,
    @SerializedName("storageSummary") val storageSummary: String?,
    @SerializedName("batterySummary") val batterySummary: String?,
    @SerializedName("protectionSummary") val protectionSummary: String?,
    @SerializedName("recordedAt") val recordedAt: String?,
    @SerializedName("updatedAt") val updatedAt: String?
)

data class DeviceHealthTelemetryRequestDto(
    @SerializedName("deviceUuid") val deviceUuid: String,
    @SerializedName("deviceModel") val deviceModel: String?,
    @SerializedName("deviceManufacturer") val deviceManufacturer: String?,
    @SerializedName("osVersion") val osVersion: String?,
    @SerializedName("sdkVersion") val sdkVersion: Int?,
    @SerializedName("batteryPct") val batteryPct: Int?,
    @SerializedName("chargingState") val chargingState: String?,
    @SerializedName("batteryHealth") val batteryHealth: String?,
    @SerializedName("batteryTempCelsius") val batteryTempCelsius: Double?,
    @SerializedName("storageTotalBytes") val storageTotalBytes: Long?,
    @SerializedName("storageUsedBytes") val storageUsedBytes: Long?,
    @SerializedName("storageFreeBytes") val storageFreeBytes: Long?,
    @SerializedName("ramTotalBytes") val ramTotalBytes: Long?,
    @SerializedName("ramUsedBytes") val ramUsedBytes: Long?,
    @SerializedName("ramFreeBytes") val ramFreeBytes: Long?,
    @SerializedName("isLowRam") val isLowRam: Boolean?,
    @SerializedName("networkType") val networkType: String?,
    @SerializedName("isOnline") val isOnline: Boolean?,
    @SerializedName("locationPermission") val locationPermission: String?,
    @SerializedName("usagePermission") val usagePermission: String?,
    @SerializedName("notificationPermission") val notificationPermission: String?,
    @SerializedName("batteryOptimization") val batteryOptimization: String?,
    @SerializedName("syncState") val syncState: String?,
    @SerializedName("recordedAt") val recordedAt: String?
)
