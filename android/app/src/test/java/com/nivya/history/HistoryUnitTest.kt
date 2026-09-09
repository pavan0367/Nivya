package com.nivya.history

import com.nivya.core.database.entities.HistoryEntity
import com.nivya.core.database.entities.toDto
import com.nivya.core.network.dto.HistoryEventDetailDto
import com.nivya.core.network.dto.HistoryEventDto
import com.nivya.core.network.dto.HistoryPageResponseDto
import com.nivya.core.network.dto.RecordHistoryRequestDto
import com.nivya.ui.dashboard.DatePreset
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class HistoryUnitTest {

    @Test
    fun testHistoryEntityToDtoMapping() {
        val now = Instant.now().toString()
        val entity = HistoryEntity(
            id = 42L,
            deviceId = 5L,
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            broadActivity = "Chatting with Arun",
            activityLabel = "Arun",
            category = "COMMUNICATION",
            durationSeconds = 840,
            durationFormatted = "14m",
            eventTimestamp = now,
            details = "Consented communication telemetry"
        )

        val dto = entity.toDto()

        assertEquals(42L, dto.id)
        assertEquals(5L, dto.deviceId)
        assertEquals("com.whatsapp", dto.packageName)
        assertEquals("WhatsApp", dto.appName)
        assertEquals("Chatting with Arun", dto.broadActivity)
        assertEquals("Arun", dto.activityLabel)
        assertEquals("COMMUNICATION", dto.category)
        assertEquals(840, dto.durationSeconds)
        assertEquals("14m", dto.durationFormatted)
        assertEquals(now, dto.eventTimestamp)
    }

    @Test
    fun testPaginationInvariants() {
        val events = (1..15).map { idx ->
            HistoryEventDto(
                id = idx.toLong(),
                deviceId = 1L,
                packageName = "com.app$idx",
                appName = "App$idx",
                broadActivity = "Active in App$idx",
                activityLabel = null,
                category = "GENERAL",
                durationSeconds = idx * 60,
                durationFormatted = "${idx}m",
                eventTimestamp = Instant.now().toString()
            )
        }

        val pageResponse = HistoryPageResponseDto(
            items = events,
            currentPage = 0,
            totalPages = 3,
            totalElements = 45L,
            pageSize = 15,
            hasNext = true,
            hasPrevious = false
        )

        assertEquals(15, pageResponse.items.size)
        assertEquals(0, pageResponse.currentPage)
        assertEquals(3, pageResponse.totalPages)
        assertEquals(45L, pageResponse.totalElements)
        assertEquals(15, pageResponse.pageSize)
        assertTrue(pageResponse.hasNext)
        assertFalse(pageResponse.hasPrevious)
    }

    @Test
    fun testRecordHistoryRequestProperties() {
        val now = Instant.now().toString()
        val request = RecordHistoryRequestDto(
            deviceUuid = "child-uuid-test",
            packageName = "com.google.android.youtube",
            appName = "YouTube",
            broadActivity = "Watching",
            activityLabel = "Science Documentary",
            category = "ENTERTAINMENT",
            durationSeconds = 1800,
            eventTimestamp = now,
            details = "Authorized video entertainment"
        )

        assertEquals("child-uuid-test", request.deviceUuid)
        assertEquals("com.google.android.youtube", request.packageName)
        assertEquals("YouTube", request.appName)
        assertEquals("Watching", request.broadActivity)
        assertEquals("Science Documentary", request.activityLabel)
        assertEquals("ENTERTAINMENT", request.category)
        assertEquals(1800, request.durationSeconds)
        assertEquals(now, request.eventTimestamp)
        assertEquals("Authorized video entertainment", request.details)
    }

    @Test
    fun testHistoryEventDetailDtoProperties() {
        val now = Instant.now().toString()
        val detail = HistoryEventDetailDto(
            id = 101L,
            deviceId = 3L,
            deviceName = "Alex's Galaxy A54",
            packageName = "com.google.android.apps.docs",
            appName = "Files",
            broadActivity = "Viewing report.pdf",
            activityLabel = "report.pdf",
            category = "PRODUCTIVITY",
            durationSeconds = 300,
            durationFormatted = "5m",
            details = "Consented productivity activity within authorized scope.",
            eventTimestamp = now,
            recordedAt = now
        )

        assertEquals(101L, detail.id)
        assertEquals(3L, detail.deviceId)
        assertEquals("Alex's Galaxy A54", detail.deviceName)
        assertEquals("report.pdf", detail.activityLabel)
        assertEquals("5m", detail.durationFormatted)
        assertTrue(detail.details?.contains("Consented") == true)
    }

    @Test
    fun testDatePresetsDefinition() {
        val presets = DatePreset.values()
        assertEquals(4, presets.size)
        assertTrue(presets.contains(DatePreset.ALL_TIME))
        assertTrue(presets.contains(DatePreset.TODAY))
        assertTrue(presets.contains(DatePreset.LAST_7_DAYS))
        assertTrue(presets.contains(DatePreset.LAST_30_DAYS))
        assertEquals("All Time", DatePreset.ALL_TIME.label)
        assertEquals("Today", DatePreset.TODAY.label)
        assertEquals("Last 7 Days", DatePreset.LAST_7_DAYS.label)
        assertEquals("Last 30 Days", DatePreset.LAST_30_DAYS.label)
    }

    @Test
    fun testDurationFormatting() {
        fun formatDuration(seconds: Int): String {
            if (seconds < 60) return "${seconds}s"
            val minutes = seconds / 60
            if (minutes < 60) return "${minutes}m"
            val hours = minutes / 60
            val remMinutes = minutes % 60
            return if (remMinutes > 0) "${hours}h ${remMinutes}m" else "${hours}h"
        }

        assertEquals("45s", formatDuration(45))
        assertEquals("14m", formatDuration(840))
        assertEquals("1h", formatDuration(3600))
        assertEquals("1h 15m", formatDuration(4500))
    }
}
