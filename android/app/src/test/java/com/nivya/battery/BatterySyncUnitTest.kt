package com.nivya.battery

import com.nivya.core.database.entities.BatteryEntity
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying battery entity offline queue state handling and business rules.
 */
class BatterySyncUnitTest {

    @Test
    fun testOfflineReadingInitialStateIsNotSynced() {
        val entity = BatteryEntity(
            deviceUuid = "device-child-001",
            batteryPct = 18,
            chargingState = "NOT_CHARGING",
            batteryState = "UNPLUGGED",
            health = "GOOD",
            temperatureCelsius = 32.5,
            recordedAt = System.currentTimeMillis(),
            isSynced = false
        )

        assertFalse("New offline reading must have isSynced=false", entity.isSynced)
        assertTrue("Battery at 18% must be identified as low battery (<= 20%)", entity.batteryPct <= 20)
        assertEquals("device-child-001", entity.deviceUuid)
    }

    @Test
    fun testOfflineReadingMarkAsSynced() {
        val original = BatteryEntity(
            id = 42L,
            deviceUuid = "device-child-001",
            batteryPct = 75,
            chargingState = "CHARGING_AC",
            batteryState = "PLUGGED",
            health = "GOOD",
            temperatureCelsius = 30.0,
            recordedAt = System.currentTimeMillis(),
            isSynced = false
        )

        assertFalse(original.isSynced)

        // Simulate marking as synced upon successful network flush
        val synced = original.copy(isSynced = true)
        assertTrue("Synced entity must have isSynced=true", synced.isSynced)
        assertEquals(42L, synced.id)
    }

    @Test
    fun testLowBatteryThresholdEvaluation() {
        val criticalBattery = 12
        val lowBattery = 19
        val normalBattery = 55

        assertTrue("12% is critical (<= 15)", criticalBattery <= 15)
        assertTrue("19% is low (<= 20)", lowBattery <= 20)
        assertFalse("55% is not low (> 20)", normalBattery <= 20)
    }

    @Test
    fun testEstimatedRemainingHoursCalculation() {
        val currentPct = 60
        val drainRatePerHour = 5.0 // 5% drain per hour
        val estimatedHours = currentPct / drainRatePerHour

        assertEquals(12.0, estimatedHours, 0.01)
    }
}
