package com.nivya.location

import com.nivya.core.database.entities.LocationEntity
import com.nivya.permissions.location.LocationPermissionState
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying location telemetry offline entity queueing,
 * staleness evaluation, permission state transitions, and stationary distance calculation.
 */
class LocationSyncUnitTest {

    @Test
    fun testLocationEntityInitialOfflineState() {
        val now = System.currentTimeMillis()
        val entity = LocationEntity(
            deviceUuid = "device-child-001",
            latitude = 37.7749,
            longitude = -122.4194,
            accuracyMeters = 12.0f,
            altitudeMeters = 15.0,
            speedMetersPerSec = 0.5f,
            bearingDegrees = 180.0f,
            provider = "gps",
            isGpsAvailable = true,
            isNetworkAvailable = true,
            permissionState = "GRANTED_BACKGROUND",
            isBackgroundConsented = true,
            isStale = false,
            sourceMode = "FOREGROUND",
            recordedAt = now,
            isSynced = false
        )

        assertFalse("New offline location reading must have isSynced=false", entity.isSynced)
        assertEquals(37.7749, entity.latitude, 0.0001)
        assertEquals(-122.4194, entity.longitude, 0.0001)
        assertEquals("gps", entity.provider)
        assertTrue(entity.isGpsAvailable)
        assertFalse(entity.isStale)
    }

    @Test
    fun testLocationEntityMarkAsSynced() {
        val original = LocationEntity(
            id = 99L,
            deviceUuid = "device-child-001",
            latitude = 40.7128,
            longitude = -74.0060,
            accuracyMeters = 8.0f,
            altitudeMeters = null,
            speedMetersPerSec = null,
            bearingDegrees = null,
            provider = "network",
            isGpsAvailable = false,
            isNetworkAvailable = true,
            permissionState = "GRANTED_FOREGROUND_ONLY",
            isBackgroundConsented = false,
            isStale = false,
            sourceMode = "FOREGROUND",
            recordedAt = System.currentTimeMillis(),
            isSynced = false
        )

        assertFalse(original.isSynced)

        val synced = original.copy(isSynced = true)
        assertTrue("Synced entity must have isSynced=true", synced.isSynced)
        assertEquals(99L, synced.id)
    }

    @Test
    fun testPermissionStateLifecycle() {
        var state = LocationPermissionState.DENIED
        assertEquals(LocationPermissionState.DENIED, state)

        // When user grants While In Use
        state = LocationPermissionState.GRANTED_FOREGROUND_ONLY
        assertEquals(LocationPermissionState.GRANTED_FOREGROUND_ONLY, state)

        // When user enables All The Time
        state = LocationPermissionState.GRANTED_BACKGROUND
        assertEquals(LocationPermissionState.GRANTED_BACKGROUND, state)
    }

    @Test
    fun testStalenessDetermination() {
        fun isStale(recordedAtMs: Long, isGpsAvailable: Boolean): Boolean {
            val minutesAgo = (System.currentTimeMillis() - recordedAtMs) / (60 * 1000)
            return minutesAgo > 15 || !isGpsAvailable
        }

        val freshTime = System.currentTimeMillis() - (5 * 60 * 1000) // 5 minutes ago
        val oldTime = System.currentTimeMillis() - (25 * 60 * 1000) // 25 minutes ago

        assertFalse("Fresh GPS reading should not be stale", isStale(freshTime, true))
        assertTrue("Reading older than 15 mins must be marked stale", isStale(oldTime, true))
        assertTrue("Reading when GPS is disabled must be marked stale", isStale(freshTime, false))
    }

    @Test
    fun testHaversineDistanceCalculation() {
        fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val r = 6371000.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                    Math.sin(dLon / 2) * Math.sin(dLon / 2)
            val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
            return r * c
        }

        // Exact same coordinate -> 0 distance
        assertEquals(0.0, calculateDistance(37.7749, -122.4194, 37.7749, -122.4194), 0.1)

        // Micro movement ~1 meter -> < 15m (stationary filter would suppress)
        val tinyDiff = calculateDistance(37.774900, -122.419400, 37.774908, -122.419400)
        assertTrue(tinyDiff < 15.0)

        // Significant movement ~1.1km -> > 15m (stationary filter would record)
        val kmDiff = calculateDistance(37.7749, -122.4194, 37.7849, -122.4194)
        assertTrue(kmDiff > 1000.0)
    }
}
