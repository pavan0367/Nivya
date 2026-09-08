package com.nivya.core.network.dto

import com.google.gson.annotations.SerializedName

/**
 * Pairing and Connection DTO models matching the Spring Boot backend API.
 */
data class GenerateCodeRequestDto(
    @SerializedName("deviceInfo") val deviceInfo: DeviceInfoDto? = null
)

data class PairingCodeResponseDto(
    @SerializedName("code") val code: String,
    @SerializedName("myRole") val myRole: String,
    @SerializedName("targetRole") val targetRole: String,
    @SerializedName("expiresAt") val expiresAt: String?,
    @SerializedName("ttlSeconds") val ttlSeconds: Long
)

data class ConnectPairingRequestDto(
    @SerializedName("code") val code: String,
    @SerializedName("deviceInfo") val deviceInfo: DeviceInfoDto? = null
)

data class PairingStatusResponseDto(
    @SerializedName("paired") val paired: Boolean,
    @SerializedName("familyId") val familyId: Long?,
    @SerializedName("familyCode") val familyCode: String?,
    @SerializedName("familyName") val familyName: String?,
    @SerializedName("userRole") val userRole: String?,
    @SerializedName("members") val members: List<LinkedMemberDto> = emptyList(),
    @SerializedName("devices") val devices: List<DeviceStatusDto> = emptyList()
)

data class DeviceInfoDto(
    @SerializedName("deviceUuid") val deviceUuid: String,
    @SerializedName("deviceName") val deviceName: String,
    @SerializedName("platform") val platform: String = "ANDROID",
    @SerializedName("osVersion") val osVersion: String? = null,
    @SerializedName("appVersion") val appVersion: String? = null,
    @SerializedName("pushToken") val pushToken: String? = null
)

data class DeviceStatusDto(
    @SerializedName("deviceId") val deviceId: Long,
    @SerializedName("deviceUuid") val deviceUuid: String,
    @SerializedName("deviceName") val deviceName: String,
    @SerializedName("platform") val platform: String,
    @SerializedName("isOnline") val isOnline: Boolean,
    @SerializedName("batteryPct") val batteryPct: Int?,
    @SerializedName("networkType") val networkType: String?,
    @SerializedName("networkQuality") val networkQuality: String?,
    @SerializedName("lastSyncAt") val lastSyncAt: String?,
    @SerializedName("lastSeenAt") val lastSeenAt: String?,
    @SerializedName("isStale") val isStale: Boolean
)

data class LinkedMemberDto(
    @SerializedName("userId") val userId: Long,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: String,
    @SerializedName("joinedAt") val joinedAt: String?
)

data class RevokePairingRequestDto(
    @SerializedName("targetDeviceId") val targetDeviceId: Long
)
