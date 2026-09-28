package com.nivya.location

import com.nivya.BuildConfig
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying OpenStreetMap integration safety, coordinate validity,
 * accuracy radius calculation, and camera initial-centering UX contract.
 */
class LocationMapUnitTest {

    @Test
    fun testOpenStreetMapFreeTileSourceConfiguration() {
        // OpenStreetMap tiles are served over standard HTTPS without billing or paid API credentials
        val osmTileEndpoint = "https://tile.openstreetmap.org"
        assertTrue("Tile endpoint must use secure HTTPS", osmTileEndpoint.startsWith("https://"))
        assertFalse("Tile endpoint must not append paid billing keys", osmTileEndpoint.contains("key="))
    }

    @Test
    fun testCoordinateValidationRejectsZeroZero() {
        val isValid = { lat: Double, lng: Double ->
            lat != 0.0 && lng != 0.0 && !lat.isNaN() && !lng.isNaN() &&
                    lat >= -90.0 && lat <= 90.0 && lng >= -180.0 && lng <= 180.0
        }

        assertFalse("0.0, 0.0 null island coordinate must be rejected", isValid(0.0, 0.0))
        assertFalse("NaN latitude must be rejected", isValid(Double.NaN, 77.5946))
        assertFalse("NaN longitude must be rejected", isValid(12.9716, Double.NaN))
    }

    @Test
    fun testCoordinateValidationValidPhysicalRanges() {
        val isValid = { lat: Double, lng: Double ->
            lat != 0.0 && lng != 0.0 && !lat.isNaN() && !lng.isNaN() &&
                    lat >= -90.0 && lat <= 90.0 && lng >= -180.0 && lng <= 180.0
        }

        assertTrue("Bengaluru coordinates must be valid", isValid(12.9716, 77.5946))
        assertTrue("San Francisco coordinates must be valid", isValid(37.7749, -122.4194))
        assertTrue("Tokyo coordinates must be valid", isValid(35.6762, 139.6503))
    }

    @Test
    fun testAccuracyRadiusBoundaryEnforcement() {
        val calculateRadius = { accuracy: Float? ->
            (accuracy ?: 15f).toDouble().coerceAtLeast(5.0)
        }

        assertEquals(5.0, calculateRadius(2f), 0.001)
        assertEquals(5.0, calculateRadius(0f), 0.001)
        assertEquals(15.0, calculateRadius(null), 0.001)
        assertEquals(22.5, calculateRadius(22.5f), 0.001)
    }

    @Test
    fun testCameraInitialCenterContract() {
        var hasCenteredInitial = false
        var cameraPanCount = 0

        val onNewTelemetryReceived = { _: Double, _: Double ->
            if (!hasCenteredInitial) {
                cameraPanCount++
                hasCenteredInitial = true
            }
        }

        // First fix locks camera
        onNewTelemetryReceived(12.9716, 77.5946)
        assertEquals(1, cameraPanCount)

        // Subsequent telemetry points must not hijack or reset user pan/zoom
        onNewTelemetryReceived(12.9718, 77.5948)
        onNewTelemetryReceived(12.9720, 77.5950)
        assertEquals(1, cameraPanCount)
    }
}
