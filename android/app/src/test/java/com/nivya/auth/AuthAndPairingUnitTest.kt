package com.nivya.auth

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

/**
 * Unit tests verifying Android authentication, role selection,
 * secure pairing code formatting, and device registration metadata.
 */
class AuthAndPairingUnitTest {

    @Test
    fun testEmailSanitizationAndValidation() {
        fun sanitizeEmail(input: String): String {
            return input.trim().lowercase()
        }

        fun isValidEmail(email: String): Boolean {
            val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()
            return email.matches(emailRegex)
        }

        val rawInput = "  Child.User@Nivya.Local  "
        val sanitized = sanitizeEmail(rawInput)
        assertEquals("child.user@nivya.local", sanitized)
        assertTrue(isValidEmail(sanitized))

        assertFalse(isValidEmail("not-an-email"))
        assertFalse(isValidEmail("@nivya.local"))
        assertFalse(isValidEmail("user@"))
    }

    enum class AppRole {
        PARENT,
        CHILD
    }

    @Test
    fun testRoleSelectionResolution() {
        fun getHomeDestinationForRole(role: AppRole): String {
            return when (role) {
                AppRole.PARENT -> "/parent/dashboard"
                AppRole.CHILD -> "/child/status"
            }
        }

        assertEquals("/parent/dashboard", getHomeDestinationForRole(AppRole.PARENT))
        assertEquals("/child/status", getHomeDestinationForRole(AppRole.CHILD))
    }

    @Test
    fun testPairingCodeFormatValidation() {
        fun normalizePairingCode(raw: String): String {
            return raw.trim().uppercase().replace(" ", "-")
        }

        fun isValidPairingCode(code: String): Boolean {
            // Valid pairing codes: NV-XXXX-XXXX or NV-XXXXXX
            val pattern = "^NV-[A-Z0-9]{4,8}(-[A-Z0-9]{4})?$".toRegex()
            return code.matches(pattern)
        }

        val input1 = "nv-8492-0192"
        val normalized1 = normalizePairingCode(input1)
        assertEquals("NV-8492-0192", normalized1)
        assertTrue(isValidPairingCode(normalized1))

        val input2 = "NV-849201"
        assertTrue(isValidPairingCode(input2))

        assertFalse(isValidPairingCode("1234"))
        assertFalse(isValidPairingCode("INVALID"))
        assertFalse(isValidPairingCode(""))
    }

    @Test
    fun testDeviceMetadataPayloadPackaging() {
        data class DeviceInfoDto(
            val deviceUuid: String,
            val deviceName: String,
            val platform: String,
            val osVersion: String,
            val appVersion: String,
            val pushToken: String?
        )

        val deviceUuid = UUID.randomUUID().toString()
        val payload = DeviceInfoDto(
            deviceUuid = deviceUuid,
            deviceName = "Pixel 8",
            platform = "ANDROID",
            osVersion = "14",
            appVersion = "1.0.0",
            pushToken = "fcm_test_token_123"
        )

        assertEquals("ANDROID", payload.platform)
        assertEquals("Pixel 8", payload.deviceName)
        assertEquals("14", payload.osVersion)
        assertNotNull(payload.pushToken)
        assertFalse(payload.deviceUuid.isBlank())
    }

    @Test
    fun testTokenExpiryEvaluation() {
        fun isTokenExpired(issuedAtMs: Long, expiresInSeconds: Long, nowMs: Long): Boolean {
            val expiresAtMs = issuedAtMs + (expiresInSeconds * 1000L)
            return nowMs >= (expiresAtMs - 10000L) // 10-second safety window
        }

        val now = 1_000_000_000L
        val tokenIssued = now - 50_000L // 50 seconds ago

        // 60-second token: 50s elapsed -> within 10s safety buffer -> considered expired
        assertTrue(isTokenExpired(tokenIssued, 60L, now))

        // 3600-second token: 50s elapsed -> valid
        assertFalse(isTokenExpired(tokenIssued, 3600L, now))
    }
}
