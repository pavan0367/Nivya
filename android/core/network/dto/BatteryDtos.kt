package com.nivya.core.network.dto

import com.google.gson.annotations.SerializedName

/**
 * Battery telemetry and status DTO models matching the Spring Boot backend API.
 */
data class BatteryTelemetryRequestDto(
    @SerializedName("deviceUuid") val deviceUuid: String,
    @SerializedName("batteryPct") val batteryPct: Int,
    @SerializedName("chargingState") val chargingState: String = "DISCHARGING",
    @SerializedName("batteryState") val batteryState: String = "UNPLUGGED",
    @SerializedName("health") val health: String = "GOOD",
    @SerializedName("temperatureCelsius") val temperatureCelsius: Double? = null,
    @SerializedName("recordedAt") val recordedAt: String? = null
)

data class BatteryStatusResponseDto(
    @SerializedName("deviceId") val deviceId: Long,
    @SerializedName("deviceUuid") val deviceUuid: String,
    @SerializedName("deviceName") val deviceName: String,
    @SerializedName("batteryPct") val batteryPct: Int,
    @SerializedName("chargingState") val chargingState: String,
    @SerializedName("batteryState") val batteryState: String,
    @SerializedName("health") val health: String,
    @SerializedName("temperatureCelsius") val temperatureCelsius: Double?,
    @SerializedName("lowBattery") val isLowBattery: Boolean,
    @SerializedName("updatedAt") val updatedAt: String?
)

data class BatteryHistoryResponseDto(
    @SerializedName("deviceId") val deviceId: Long,
    @SerializedName("deviceUuid") val deviceUuid: String,
    @SerializedName("points") val points: List<BatteryDataPointDto> = emptyList()
)

data class BatteryDataPointDto(
    @SerializedName("batteryPct") val batteryPct: Int,
    @SerializedName("chargingState") val chargingState: String,
    @SerializedName("batteryState") val batteryState: String,
    @SerializedName("temperatureCelsius") val temperatureCelsius: Double?,
    @SerializedName("recordedAt") val recordedAt: String?
)

data class BatteryTrendResponseDto(
    @SerializedName("deviceId") val deviceId: Long,
    @SerializedName("currentBatteryPct") val currentBatteryPct: Int,
    @SerializedName("chargingState") val chargingState: String,
    @SerializedName("drainRatePctPerHour") val drainRatePctPerHour: Double?,
    @SerializedName("estimatedHoursRemaining") val estimatedHoursRemaining: Double?,
    @SerializedName("averageTemperatureCelsius") val averageTemperatureCelsius: Double?
)
