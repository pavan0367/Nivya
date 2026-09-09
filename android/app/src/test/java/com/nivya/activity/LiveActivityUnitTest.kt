package com.nivya.activity

import com.nivya.core.network.dto.ActivityEventDto
import com.nivya.core.network.dto.LiveActivityRequestDto
import com.nivya.core.network.dto.LiveActivityResponseDto
import com.nivya.services.activity.LiveActivityCollector
import org.junit.Assert.*
import org.junit.Test

class LiveActivityUnitTest {

    @Test
    fun testBroadActivityMapping_KnownApps() {
        // WhatsApp
        val (waActivity, waCat) = LiveActivityCollector.mapBroadActivity("com.whatsapp", "WhatsApp")
        assertEquals("Chatting with Arun", waActivity)
        assertEquals("COMMUNICATION", waCat)

        // Files / Documents
        val (filesActivity, filesCat) = LiveActivityCollector.mapBroadActivity("com.google.android.apps.docs", "Files")
        assertEquals("Viewing report.pdf", filesActivity)
        assertEquals("PRODUCTIVITY", filesCat)

        // Chrome Browsing
        val (chromeActivity, chromeCat) = LiveActivityCollector.mapBroadActivity("com.android.chrome", "Chrome")
        assertEquals("Browsing", chromeActivity)
        assertEquals("BROWSING", chromeCat)

        // YouTube Watching
        val (ytActivity, ytCat) = LiveActivityCollector.mapBroadActivity("com.google.android.youtube", "YouTube")
        assertEquals("Watching", ytActivity)
        assertEquals("ENTERTAINMENT", ytCat)

        // Phone call
        val (phoneActivity, phoneCat) = LiveActivityCollector.mapBroadActivity("com.google.android.dialer", "Phone")
        assertEquals("In call with Mom", phoneActivity)
        assertEquals("COMMUNICATION", phoneCat)
    }

    @Test
    fun testBroadActivityMapping_FallbackForUnknownApp() {
        val (activity, category) = LiveActivityCollector.mapBroadActivity("com.example.game", "Chess Master")
        assertEquals("Active in Chess Master", activity)
        assertEquals("GENERAL", category)
    }

    @Test
    fun testPrivacySanitizer_RemovesSensitiveTokens() {
        val sanitizedPwd = LiveActivityCollector.sanitizeBroadActivity("Entering password for account", "SafeApp")
        assertEquals("Active in SafeApp", sanitizedPwd)

        val sanitizedOtp = LiveActivityCollector.sanitizeBroadActivity("Viewing OTP code 882910", "Messenger")
        assertEquals("Active in Messenger", sanitizedOtp)

        val sanitizedPin = LiveActivityCollector.sanitizeBroadActivity("ATM secret pin verification", "BankApp")
        assertEquals("Active in BankApp", sanitizedPin)

        val legitimate = LiveActivityCollector.sanitizeBroadActivity("Chatting with Arun", "WhatsApp")
        assertEquals("Chatting with Arun", legitimate)
    }

    @Test
    fun testLiveActivityDtoProperties() {
        val request = LiveActivityRequestDto(
            deviceUuid = "child-uuid-1",
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            broadActivity = "Chatting with Arun",
            category = "COMMUNICATION",
            durationSeconds = 600,
            isCurrent = true
        )

        assertEquals("child-uuid-1", request.deviceUuid)
        assertEquals("WhatsApp", request.appName)
        assertEquals("Chatting with Arun", request.broadActivity)
        assertTrue(request.isCurrent)

        val event = ActivityEventDto(
            id = 1L,
            deviceId = 10L,
            packageName = "com.android.chrome",
            appName = "Chrome",
            broadActivity = "Browsing",
            category = "BROWSING",
            durationSeconds = 900,
            durationFormatted = "15m",
            isCurrent = true,
            startedAt = "2026-09-09T09:00:00Z",
            endedAt = null,
            createdAt = "2026-09-09T09:00:00Z"
        )

        val response = LiveActivityResponseDto(
            deviceId = 10L,
            deviceUuid = "child-uuid-1",
            deviceName = "Galaxy A54",
            isOnline = true,
            currentActivity = event,
            recentActivities = listOf(event)
        )

        assertEquals(10L, response.deviceId)
        assertTrue(response.isOnline)
        assertNotNull(response.currentActivity)
        assertEquals("Browsing", response.currentActivity?.broadActivity)
        assertEquals(1, response.recentActivities.size)
    }
}
