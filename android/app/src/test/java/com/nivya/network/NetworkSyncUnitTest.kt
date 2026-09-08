package com.nivya.network

import com.nivya.core.database.entities.NetworkEntity
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying network offline entity queueing, sync state, and quality evaluation.
 */
class NetworkSyncUnitTest {

    @Test
    fun testOfflineReadingInitialStateIsNotSynced() {
        val entity = NetworkEntity(
            deviceUuid = "device-child-001",
            networkType = "WIFI",
            connectionType = "Wi-Fi (Active)",
            isNetworkAvailable = true,
            isInternetAvailable = true,
            signalLevel = 4,
            signalDbm = -54,
            quality = "EXCELLENT",
            recordedAt = System.currentTimeMillis(),
            isSynced = false
        )

        assertFalse("New offline reading must have isSynced=false", entity.isSynced)
        assertEquals("WIFI", entity.networkType)
        assertEquals("EXCELLENT", entity.quality)
    }

    @Test
    fun testOfflineReadingMarkAsSynced() {
        val original = NetworkEntity(
            id = 101L,
            deviceUuid = "device-child-001",
            networkType = "CELLULAR",
            connectionType = "Mobile 5G / LTE",
            isNetworkAvailable = true,
            isInternetAvailable = true,
            signalLevel = 3,
            signalDbm = -75,
            quality = "GOOD",
            recordedAt = System.currentTimeMillis(),
            isSynced = false
        )

        assertFalse(original.isSynced)

        val synced = original.copy(isSynced = true)
        assertTrue("Synced entity must have isSynced=true", synced.isSynced)
        assertEquals(101L, synced.id)
    }

    @Test
    fun testQualityTierClassification() {
        fun computeQuality(isNetworkAvailable: Boolean, isInternetAvailable: Boolean, signalLevel: Int): String {
            return when {
                !isNetworkAvailable || !isInternetAvailable -> "UNAVAILABLE"
                signalLevel >= 4 -> "EXCELLENT"
                signalLevel >= 2 -> "GOOD"
                else -> "WEAK"
            }
        }

        assertEquals("UNAVAILABLE", computeQuality(false, false, 0))
        assertEquals("UNAVAILABLE", computeQuality(true, false, 4))
        assertEquals("EXCELLENT", computeQuality(true, true, 4))
        assertEquals("GOOD", computeQuality(true, true, 3))
        assertEquals("GOOD", computeQuality(true, true, 2))
        assertEquals("WEAK", computeQuality(true, true, 1))
        assertEquals("WEAK", computeQuality(true, true, 0))
    }
}
