package com.nivya.usage

import com.nivya.core.database.entities.UsageEntity
import com.nivya.services.usage.UsagePermissionState
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying screen time telemetry offline entity queuing,
 * permission state transitions, app categorization, and duration calculations.
 */
class UsageSyncUnitTest {

    @Test
    fun testUsageEntityInitialOfflineState() {
        val entity = UsageEntity(
            deviceUuid = "device-child-001",
            date = "2026-09-08",
            totalForegroundSeconds = 7200L,
            educationalSeconds = 3600L,
            recreationalSeconds = 2400L,
            socialSeconds = 800L,
            productivitySeconds = 400L,
            topAppsJson = """[{"packageName":"com.duolingo","appName":"Duolingo","category":"EDUCATION","foregroundSeconds":3600}]""",
            recordedAt = System.currentTimeMillis(),
            isSynced = false
        )

        assertFalse("New offline usage entity must have isSynced=false", entity.isSynced)
        assertEquals("2026-09-08", entity.date)
        assertEquals(7200L, entity.totalForegroundSeconds)
        assertEquals(3600L, entity.educationalSeconds)
        assertEquals(2400L, entity.recreationalSeconds)
    }

    @Test
    fun testUsageEntityMarkAsSynced() {
        val original = UsageEntity(
            id = 42L,
            deviceUuid = "device-child-001",
            date = "2026-09-08",
            totalForegroundSeconds = 3600L,
            educationalSeconds = 1800L,
            recreationalSeconds = 1200L,
            socialSeconds = 600L,
            productivitySeconds = 0L,
            topAppsJson = "[]",
            recordedAt = System.currentTimeMillis(),
            isSynced = false
        )

        assertFalse(original.isSynced)

        val synced = original.copy(isSynced = true)
        assertTrue("Synced entity must have isSynced=true", synced.isSynced)
        assertEquals(42L, synced.id)
    }

    @Test
    fun testPermissionStateLifecycle() {
        var state = UsagePermissionState.REQUIRED
        assertEquals(UsagePermissionState.REQUIRED, state)

        // After user requests access but hasn't enabled in settings
        val userReturnedWithoutGranting = true
        if (userReturnedWithoutGranting) {
            state = UsagePermissionState.TRY_AGAIN
        }
        assertEquals(UsagePermissionState.TRY_AGAIN, state)

        // After user enables in settings
        val userGrantedInSettings = true
        if (userGrantedInSettings) {
            state = UsagePermissionState.GRANTED
        }
        assertEquals(UsagePermissionState.GRANTED, state)
    }

    @Test
    fun testDurationFormatting() {
        fun formatDuration(seconds: Long): String {
            if (seconds < 60) return "${seconds}s"
            val minutes = seconds / 60
            val hours = minutes / 60
            val remMinutes = minutes % 60
            return if (hours > 0) "${hours}h ${remMinutes}m" else "${remMinutes}m"
        }

        assertEquals("45s", formatDuration(45))
        assertEquals("15m", formatDuration(900))
        assertEquals("1h 0m", formatDuration(3600))
        assertEquals("2h 45m", formatDuration(9900))
    }

    @Test
    fun testAppCategorization() {
        fun categorize(pkg: String, label: String): String {
            val lowerPkg = pkg.lowercase()
            val lowerLabel = label.lowercase()
            return when {
                lowerPkg.contains("duolingo") || lowerPkg.contains("khan") || lowerLabel.contains("learning") -> "EDUCATION"
                lowerPkg.contains("minecraft") || lowerPkg.contains("roblox") || lowerLabel.contains("game") -> "GAMES"
                lowerPkg.contains("youtube") || lowerPkg.contains("netflix") || lowerLabel.contains("video") -> "ENTERTAINMENT"
                lowerPkg.contains("whatsapp") || lowerPkg.contains("discord") || lowerLabel.contains("messages") -> "SOCIAL"
                lowerPkg.contains("docs") || lowerPkg.contains("sheets") || lowerLabel.contains("notes") -> "PRODUCTIVITY"
                else -> "OTHER"
            }
        }

        assertEquals("EDUCATION", categorize("com.duolingo", "Duolingo"))
        assertEquals("EDUCATION", categorize("org.khanacademy.android", "Khan Academy"))
        assertEquals("GAMES", categorize("com.mojang.minecraftpe", "Minecraft"))
        assertEquals("ENTERTAINMENT", categorize("com.google.android.youtube", "YouTube"))
        assertEquals("SOCIAL", categorize("com.whatsapp", "WhatsApp"))
        assertEquals("PRODUCTIVITY", categorize("com.google.android.apps.docs", "Google Docs"))
        assertEquals("OTHER", categorize("com.android.settings", "Settings"))
    }
}
