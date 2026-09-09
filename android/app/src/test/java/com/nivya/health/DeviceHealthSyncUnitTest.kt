package com.nivya.health

import com.nivya.core.database.entities.DeviceHealthEntity
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying Device Health entity offline queue state,
 * storage and memory diagnostics calculations, and permission health evaluation.
 */
class DeviceHealthSyncUnitTest {

    @Test
    fun testDeviceHealthEntityInitialOfflineState() {
        val now = System.currentTimeMillis()
        val entity = DeviceHealthEntity(
            deviceUuid = "dev-child-001",
            deviceModel = "Samsung Galaxy A54",
            deviceManufacturer = "Samsung",
            osVersion = "14",
            sdkVersion = 34,
            storageTotalBytes = 128_000_000_000L,
            storageUsedBytes = 48_000_000_000L,
            storageFreeBytes = 80_000_000_000L,
            ramTotalBytes = 8_000_000_000L,
            ramUsedBytes = 4_000_000_000L,
            ramFreeBytes = 4_000_000_000L,
            isLowRam = false,
            batteryPct = 85,
            chargingState = "NOT_CHARGING",
            batteryHealth = "GOOD",
            batteryTempCelsius = 31.0,
            networkType = "WIFI",
            isOnline = true,
            locationPermission = "GRANTED",
            usagePermission = "GRANTED",
            notificationPermission = "GRANTED",
            batteryOptimization = "OPTIMIZED",
            allPermissionsHealthy = true,
            recordedAt = now,
            isSynced = false
        )

        assertFalse("New offline health reading must have isSynced=false", entity.isSynced)
        assertEquals("Samsung Galaxy A54", entity.deviceModel)
        assertEquals(85, entity.batteryPct)
        assertTrue(entity.allPermissionsHealthy)
        assertEquals(80_000_000_000L, entity.storageFreeBytes)
    }

    @Test
    fun testDeviceHealthEntityMarkAsSynced() {
        val original = DeviceHealthEntity(
            id = 42L,
            deviceUuid = "dev-child-001",
            deviceModel = "Google Pixel 8",
            deviceManufacturer = "Google",
            osVersion = "14",
            sdkVersion = 34,
            storageTotalBytes = 256_000_000_000L,
            storageUsedBytes = 100_000_000_000L,
            storageFreeBytes = 156_000_000_000L,
            ramTotalBytes = 12_000_000_000L,
            ramUsedBytes = 5_000_000_000L,
            ramFreeBytes = 7_000_000_000L,
            isLowRam = false,
            batteryPct = 90,
            chargingState = "CHARGING_AC",
            batteryHealth = "GOOD",
            batteryTempCelsius = 29.5,
            networkType = "WIFI",
            isOnline = true,
            locationPermission = "GRANTED",
            usagePermission = "GRANTED",
            notificationPermission = "GRANTED",
            batteryOptimization = "OPTIMIZED",
            allPermissionsHealthy = true,
            recordedAt = System.currentTimeMillis(),
            isSynced = false
        )

        assertFalse(original.isSynced)
        val synced = original.copy(isSynced = true)
        assertTrue("Synced entity must have isSynced=true", synced.isSynced)
        assertEquals(42L, synced.id)
    }

    @Test
    fun testStorageUsageAndLowStorageThreshold() {
        fun isLowStorage(total: Long, free: Long): Boolean {
            return total > 0 && (free < (total * 0.10) || free < 2_000_000_000L)
        }

        val totalStorage = 128_000_000_000L

        // Ample storage (80 GB free > 10%)
        val healthyFree = 80_000_000_000L
        assertFalse("80GB free out of 128GB should not trigger low storage", isLowStorage(totalStorage, healthyFree))

        // Tight storage (8 GB free < 10% of 128GB = 12.8GB)
        val tightFree = 8_000_000_000L
        assertTrue("8GB free out of 128GB must trigger low storage warning", isLowStorage(totalStorage, tightFree))

        // Critical storage (<2GB)
        val criticalFree = 1_200_000_000L
        assertTrue("1.2GB free must trigger low storage warning", isLowStorage(totalStorage, criticalFree))
    }

    @Test
    fun testMemoryHeadroomCalculation() {
        val totalRam = 8_000_000_000L
        val usedRam = 4_000_000_000L
        val freeRam = totalRam - usedRam
        val usedPct = (usedRam.toDouble() / totalRam.toDouble()) * 100.0

        assertEquals(50.0, usedPct, 0.01)
        assertEquals(4_000_000_000L, freeRam)

        fun isLowRam(total: Long, free: Long): Boolean {
            return total > 0 && free < (total * 0.10)
        }

        assertFalse(isLowRam(totalRam, freeRam))
        assertTrue("Free RAM < 10% must indicate memory pressure", isLowRam(totalRam, 500_000_000L))
    }

    @Test
    fun testPermissionHealthEvaluation() {
        fun evaluatePermissions(loc: String, usage: String, notif: String): Boolean {
            val locHealthy = loc.contains("GRANTED", ignoreCase = true)
            val usageHealthy = "GRANTED".equals(usage, ignoreCase = true)
            val notifHealthy = notif.contains("GRANTED", ignoreCase = true)
            return locHealthy && usageHealthy && notifHealthy
        }

        assertTrue("All granted must be healthy", evaluatePermissions("GRANTED", "GRANTED", "GRANTED"))
        assertFalse("Denied location must fail permission health", evaluatePermissions("DENIED", "GRANTED", "GRANTED"))
        assertFalse("Required usage stats must fail permission health", evaluatePermissions("GRANTED", "REQUIRED", "GRANTED"))
        assertFalse("Denied notifications must fail permission health", evaluatePermissions("GRANTED", "GRANTED", "DENIED"))
    }

    @Test
    fun testHealthScoreCalculation() {
        fun calculateHealthScore(
            storageTotal: Long,
            storageFree: Long,
            isLowRam: Boolean,
            batteryPct: Int,
            isCharging: Boolean,
            locHealthy: Boolean,
            usageHealthy: Boolean,
            notifHealthy: Boolean
        ): Int {
            var score = 100
            if (storageTotal > 0 && storageFree < (storageTotal * 0.05)) {
                score -= 30
            } else if (storageTotal > 0 && storageFree < (storageTotal * 0.10)) {
                score -= 15
            }

            if (isLowRam) score -= 15
            if (batteryPct <= 15 && !isCharging) score -= 20
            else if (batteryPct <= 25 && !isCharging) score -= 10

            if (!locHealthy) score -= 15
            if (!usageHealthy) score -= 15
            if (!notifHealthy) score -= 10

            return score.coerceIn(10, 100)
        }

        // Perfect device
        val perfect = calculateHealthScore(
            storageTotal = 128_000_000_000L,
            storageFree = 80_000_000_000L,
            isLowRam = false,
            batteryPct = 80,
            isCharging = false,
            locHealthy = true,
            usageHealthy = true,
            notifHealthy = true
        )
        assertEquals(100, perfect)

        // Degraded permissions
        val degradedPerms = calculateHealthScore(
            storageTotal = 128_000_000_000L,
            storageFree = 80_000_000_000L,
            isLowRam = false,
            batteryPct = 80,
            isCharging = false,
            locHealthy = false, // -15
            usageHealthy = false, // -15
            notifHealthy = true
        )
        assertEquals(70, degradedPerms)

        // Multiple critical issues
        val critical = calculateHealthScore(
            storageTotal = 100_000_000_000L,
            storageFree = 2_000_000_000L, // 2% free -> -30
            isLowRam = true, // -15
            batteryPct = 10,
            isCharging = false, // -20
            locHealthy = false, // -15
            usageHealthy = false, // -15
            notifHealthy = false // -10
        )
        // 100 - 30 - 15 - 20 - 15 - 15 - 10 = -5 -> coerced to 10
        assertEquals(10, critical)
    }
}
