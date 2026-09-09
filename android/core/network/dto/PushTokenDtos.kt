package com.nivya.core.network.dto

import com.google.gson.annotations.SerializedName

/**
 * DTOs for device FCM push token registration and unregistration.
 */
data class RegisterPushTokenRequestDto(
    @SerializedName("deviceUuid") val deviceUuid: String?,
    @SerializedName("pushToken") val pushToken: String,
    @SerializedName("platform") val platform: String? = "ANDROID",
    @SerializedName("deviceName") val deviceName: String? = null
)

data class UnregisterPushTokenRequestDto(
    @SerializedName("deviceUuid") val deviceUuid: String? = null,
    @SerializedName("pushToken") val pushToken: String? = null
)

data class PushTokenResponseDto(
    @SerializedName("deviceUuid") val deviceUuid: String?,
    @SerializedName("maskedToken") val maskedToken: String?,
    @SerializedName("status") val status: String,
    @SerializedName("registeredAt") val registeredAt: String? = null
)
