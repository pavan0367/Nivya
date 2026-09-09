package com.nivya.notification

import com.google.gson.Gson
import com.nivya.core.network.dto.PushTokenResponseDto
import com.nivya.core.network.dto.RegisterPushTokenRequestDto
import com.nivya.core.network.dto.UnregisterPushTokenRequestDto
import com.nivya.services.notification.AlertNotificationManager
import com.nivya.services.notification.ConvocationNotificationManager
import org.junit.Assert.*
import org.junit.Test

class FcmNotificationUnitTest {

    private val gson = Gson()

    @Test
    fun testRegisterPushTokenRequestDto_Serialization() {
        val dto = RegisterPushTokenRequestDto(
            deviceUuid = "device-uuid-abc",
            pushToken = "fcm-registration-token-123",
            platform = "ANDROID",
            deviceName = "Pixel 8 Pro"
        )

        val json = gson.toJson(dto)
        val deserialized = gson.fromJson(json, RegisterPushTokenRequestDto::class.java)

        assertEquals("device-uuid-abc", deserialized.deviceUuid)
        assertEquals("fcm-registration-token-123", deserialized.pushToken)
        assertEquals("ANDROID", deserialized.platform)
        assertEquals("Pixel 8 Pro", deserialized.deviceName)
    }

    @Test
    fun testUnregisterPushTokenRequestDto_Serialization() {
        val dto = UnregisterPushTokenRequestDto(
            deviceUuid = "device-uuid-logout",
            pushToken = "fcm-token-to-purge"
        )

        val json = gson.toJson(dto)
        val deserialized = gson.fromJson(json, UnregisterPushTokenRequestDto::class.java)

        assertEquals("device-uuid-logout", deserialized.deviceUuid)
        assertEquals("fcm-token-to-purge", deserialized.pushToken)
    }

    @Test
    fun testPushTokenResponseDto_Properties() {
        val dto = PushTokenResponseDto(
            deviceUuid = "device-uuid-789",
            maskedToken = "fcm-...-123",
            status = "REGISTERED",
            registeredAt = "2026-09-09T10:00:00Z"
        )

        assertEquals("device-uuid-789", dto.deviceUuid)
        assertEquals("fcm-...-123", dto.maskedToken)
        assertEquals("REGISTERED", dto.status)
        assertEquals("2026-09-09T10:00:00Z", dto.registeredAt)
    }

    @Test
    fun testConvocationDecoyNotification_ExactRequirements() {
        // STRICT REQUIREMENT 1: EXACT notification text must be "Check your battery status"
        assertEquals(
            "Convocation decoy notification message must match exact requirement",
            "Check your battery status",
            ConvocationNotificationManager.DECOY_MESSAGE
        )

        // STRICT REQUIREMENT 2: Title must be generic
        assertEquals("Device Update", ConvocationNotificationManager.DECOY_TITLE)

        // Channel ID must be dedicated to status sync
        assertEquals("nivya_device_status_sync", ConvocationNotificationManager.CHANNEL_ID)
        assertEquals(8801, ConvocationNotificationManager.NOTIFICATION_ID)
    }

    @Test
    fun testAlertNotificationManager_Channels() {
        assertEquals("nivya_safety_critical", AlertNotificationManager.CHANNEL_SAFETY_CRITICAL)
        assertEquals("nivya_general_alerts", AlertNotificationManager.CHANNEL_GENERAL_ALERTS)
    }
}
