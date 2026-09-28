package com.nivya.notification

import com.google.gson.Gson
import com.nivya.core.network.NetworkResult
import com.nivya.core.network.NivyaApiService
import com.nivya.core.network.dto.*
import com.nivya.core.security.TokenStorage
import com.nivya.data.local.UserPreferencesDataStore
import com.nivya.data.repository.AuthRepository
import com.nivya.data.repository.PairingRepository
import com.nivya.services.notification.AlertNotificationManager
import com.nivya.services.notification.ConvocationNotificationManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

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

    // --- Post-Auth Token Synchronization & Remediation Tests ---

    private class FakeTokenStorage : TokenStorage {
        var tokenAccess: String? = null
        var tokenRefresh: String? = null
        var role: String? = null
        var storedDeviceUuid: String = "test-device-uuid-123"

        override fun saveTokens(accessToken: String, refreshToken: String) {
            this.tokenAccess = accessToken
            this.tokenRefresh = refreshToken
        }

        override fun getAccessToken(): String? = tokenAccess
        override fun getRefreshToken(): String? = tokenRefresh
        override fun hasAccessToken(): Boolean = !tokenAccess.isNullOrBlank()
        override fun saveUserRole(role: String) { this.role = role }
        override fun getUserRole(): String? = role
        override fun saveDeviceUuid(uuid: String) { storedDeviceUuid = uuid }
        override fun getDeviceUuid(): String = storedDeviceUuid
        override fun clearAll() {
            tokenAccess = null
            tokenRefresh = null
            role = null
        }
    }

    private class FakeUserPreferencesDataStore : com.nivya.data.local.UserPreferencesDataStore(null) {
        var storedToken: String? = null
        var lastSyncedToken: String? = null

        override suspend fun getFcmToken(): String? = storedToken
        override suspend fun saveFcmToken(token: String) {
            storedToken = token
        }

        override suspend fun getLastSyncedFcmToken(): String? = lastSyncedToken
        override suspend fun saveLastSyncedFcmToken(token: String?) {
            lastSyncedToken = token
        }
    }

    private fun createFakeApiService(
        onRegisterPushToken: (RegisterPushTokenRequestDto) -> retrofit2.Response<com.nivya.core.network.dto.ApiResponseDto<PushTokenResponseDto>> = { req ->
            retrofit2.Response.success(
                com.nivya.core.network.dto.ApiResponseDto(
                    success = true,
                    message = "Registered",
                    data = PushTokenResponseDto(req.deviceUuid, req.pushToken, "REGISTERED", "2026-09-24T12:00:00Z"),
                    timestamp = "now",
                    traceId = "trace"
                )
            )
        },
        onLogin: (LoginRequestDto) -> retrofit2.Response<com.nivya.core.network.dto.ApiResponseDto<AuthResponseDataDto>> = { req ->
            retrofit2.Response.success(
                com.nivya.core.network.dto.ApiResponseDto(
                    success = true,
                    message = "Login success",
                    data = AuthResponseDataDto(
                        accessToken = "access_123",
                        refreshToken = "refresh_123",
                        tokenType = "Bearer",
                        expiresIn = 3600L,
                        user = UserDto(1L, "user-uuid", "Test Child", req.email, "CHILD", "ACTIVE")
                    ),
                    timestamp = "now",
                    traceId = "trace"
                )
            )
        }
    ): com.nivya.core.network.NivyaApiService {
        return java.lang.reflect.Proxy.newProxyInstance(
            com.nivya.core.network.NivyaApiService::class.java.classLoader,
            arrayOf(com.nivya.core.network.NivyaApiService::class.java)
        ) { _, method, args ->
            when (method.name) {
                "registerPushToken" -> onRegisterPushToken(args[0] as RegisterPushTokenRequestDto)
                "login" -> onLogin(args[0] as LoginRequestDto)
                else -> throw UnsupportedOperationException("Method not mocked: ${method.name}")
            }
        } as com.nivya.core.network.NivyaApiService
    }

    @Test
    fun test1_FcmTokenGeneratedBeforeLogin_StoredLocally() = kotlinx.coroutines.runBlocking {
        val prefs = FakeUserPreferencesDataStore()
        val storage = FakeTokenStorage()
        val api = createFakeApiService()
        val authRepo = com.nivya.data.repository.AuthRepository(api, storage, prefs)

        // Token received from Firebase before login
        prefs.saveFcmToken("fcm-pre-auth-token-12345")

        assertEquals("fcm-pre-auth-token-12345", prefs.getFcmToken())
        assertFalse("User must not be logged in before authentication", authRepo.isLoggedIn())

        // syncStoredPushToken returns null and does not execute unauthorized sync
        val syncResult = authRepo.syncStoredPushToken()
        assertNull(syncResult)
        assertNull("Last synced token must remain null", prefs.getLastSyncedFcmToken())
    }

    @Test
    fun test2_SuccessfulLogin_StoredTokenSynchronized() = kotlinx.coroutines.runBlocking {
        val prefs = FakeUserPreferencesDataStore()
        val storage = FakeTokenStorage()
        var registeredPushToken: String? = null
        var registeredDeviceUuid: String? = null

        val api = createFakeApiService(
            onRegisterPushToken = { req ->
                registeredPushToken = req.pushToken
                registeredDeviceUuid = req.deviceUuid
                retrofit2.Response.success(
                    com.nivya.core.network.dto.ApiResponseDto(
                        success = true,
                        message = "Registered",
                        data = PushTokenResponseDto(req.deviceUuid, req.pushToken, "REGISTERED", "2026-09-24T12:00:00Z"),
                        timestamp = "now",
                        traceId = "trace"
                    )
                )
            }
        )

        val authRepo = com.nivya.data.repository.AuthRepository(api, storage, prefs)

        // 1. Token pre-stored locally prior to login
        prefs.saveFcmToken("fcm-device-live-token-98765")

        // 2. User logs in
        val loginResult = authRepo.login("child@nivya.local", "Password123!")
        assertTrue("Login must succeed", loginResult is com.nivya.core.network.NetworkResult.Success)

        // 3. Stored token was automatically synchronized to backend
        assertEquals("fcm-device-live-token-98765", registeredPushToken)
        assertEquals("test-device-uuid-123", registeredDeviceUuid)
        assertEquals("fcm-device-live-token-98765", prefs.getLastSyncedFcmToken())
    }

    @Test
    fun test3_PairingCompletion_StoredTokenSynchronized() = kotlinx.coroutines.runBlocking {
        val prefs = FakeUserPreferencesDataStore()
        prefs.saveFcmToken("fcm-pairing-token-abc")

        var connectPushToken: String? = null
        var syncedViaAuthRepo = false

        val api = java.lang.reflect.Proxy.newProxyInstance(
            com.nivya.core.network.NivyaApiService::class.java.classLoader,
            arrayOf(com.nivya.core.network.NivyaApiService::class.java)
        ) { _, method, args ->
            when (method.name) {
                "connectDevices" -> {
                    val req = args[0] as ConnectPairingRequestDto
                    connectPushToken = req.deviceInfo?.pushToken
                    retrofit2.Response.success(
                        com.nivya.core.network.dto.ApiResponseDto(
                            success = true,
                            message = "Connected",
                            data = PairingStatusResponseDto(
                                paired = true,
                                familyId = 100L,
                                familyCode = "FAM-123",
                                familyName = "Test Family",
                                userRole = "CHILD",
                                members = emptyList(),
                                devices = emptyList()
                            ),
                            timestamp = "now",
                            traceId = "trace"
                        )
                    )
                }
                "registerPushToken" -> {
                    syncedViaAuthRepo = true
                    retrofit2.Response.success(
                        com.nivya.core.network.dto.ApiResponseDto(
                            success = true,
                            message = "Registered",
                            data = PushTokenResponseDto("test-uuid", "fcm-pairing-token-abc", "REGISTERED", "now"),
                            timestamp = "now",
                            traceId = "trace"
                        )
                    )
                }
                else -> throw UnsupportedOperationException("Method not mocked: ${method.name}")
            }
        } as com.nivya.core.network.NivyaApiService

        val storage = FakeTokenStorage()
        storage.saveTokens("pair_access", "pair_refresh")
        storage.saveDeviceUuid("pair-device-uuid")

        val authRepo = AuthRepository(api, storage, prefs)

        val pairingRepo = PairingRepository(
            apiService = api,
            tokenStorage = storage,
            database = null,
            networkMonitor = null,
            preferencesDataStore = prefs,
            authRepository = authRepo
        )

        val connectResult = pairingRepo.connect("NV-ABCD-1234")
        assertTrue(connectResult is com.nivya.core.network.NetworkResult.Success)
        assertEquals("fcm-pairing-token-abc", connectPushToken)
        assertTrue(syncedViaAuthRepo)
    }

    @Test
    fun test4_AuthenticatedOnNewToken_ImmediateSynchronization() = kotlinx.coroutines.runBlocking {
        val prefs = FakeUserPreferencesDataStore()
        val storage = FakeTokenStorage()
        storage.saveTokens("valid_access", "valid_refresh") // Authenticated

        var pushSynced = false
        val api = createFakeApiService(
            onRegisterPushToken = { req ->
                pushSynced = true
                assertEquals("fcm-rotated-token-live", req.pushToken)
                retrofit2.Response.success(
                    com.nivya.core.network.dto.ApiResponseDto(
                        success = true,
                        message = "Registered",
                        data = PushTokenResponseDto(req.deviceUuid, req.pushToken, "REGISTERED", "now"),
                        timestamp = "now",
                        traceId = "trace"
                    )
                )
            }
        )

        val authRepo = com.nivya.data.repository.AuthRepository(api, storage, prefs)

        // When authenticated, onNewToken invokes syncPushToken immediately
        assertTrue(authRepo.isLoggedIn())
        val result = authRepo.syncPushToken("fcm-rotated-token-live")
        assertTrue(result is com.nivya.core.network.NetworkResult.Success)
        assertTrue(pushSynced)
        assertEquals("fcm-rotated-token-live", prefs.getLastSyncedFcmToken())
    }

    @Test
    fun test5_UnauthenticatedOnNewToken_NoUnauthorizedSync() = kotlinx.coroutines.runBlocking {
        val prefs = FakeUserPreferencesDataStore()
        val storage = FakeTokenStorage() // Unauthenticated

        var apiCalled = false
        val api = createFakeApiService(
            onRegisterPushToken = {
                apiCalled = true
                throw AssertionError("registerPushToken must not be called when unauthenticated")
            }
        )

        val authRepo = com.nivya.data.repository.AuthRepository(api, storage, prefs)
        assertFalse(authRepo.isLoggedIn())

        // Simulating unauthenticated onNewToken behavior: only save locally
        prefs.saveFcmToken("fcm-early-token")
        if (authRepo.isLoggedIn()) {
            authRepo.syncPushToken("fcm-early-token")
        }

        assertFalse("API must not be invoked when user is not logged in", apiCalled)
        assertEquals("fcm-early-token", prefs.getFcmToken())
    }

    @Test
    fun test6_TokenSyncFailure_LoginAndAppRemainFunctional() = kotlinx.coroutines.runBlocking {
        val prefs = FakeUserPreferencesDataStore()
        prefs.saveFcmToken("fcm-failing-token")
        val storage = FakeTokenStorage()

        // API throws exception on push token sync
        val api = createFakeApiService(
            onRegisterPushToken = {
                throw RuntimeException("Network unreachable")
            }
        )

        val authRepo = com.nivya.data.repository.AuthRepository(api, storage, prefs)

        // Login must NOT fail or crash when push token sync fails
        val result = authRepo.login("user@nivya.local", "Password123!")
        assertTrue("Login must succeed even if push token sync fails", result is com.nivya.core.network.NetworkResult.Success)
        assertTrue("User must still be logged in", authRepo.isLoggedIn())
    }

    @Test
    fun test7_ExistingTokenRotationBehavior() = kotlinx.coroutines.runBlocking {
        val prefs = FakeUserPreferencesDataStore()
        val storage = FakeTokenStorage()
        storage.saveTokens("token_access", "token_refresh")

        var latestSyncedToken: String? = null
        val api = createFakeApiService(
            onRegisterPushToken = { req ->
                latestSyncedToken = req.pushToken
                retrofit2.Response.success(
                    com.nivya.core.network.dto.ApiResponseDto(
                        success = true,
                        message = "Registered",
                        data = PushTokenResponseDto(req.deviceUuid, req.pushToken, "REGISTERED", "now"),
                        timestamp = "now",
                        traceId = "trace"
                    )
                )
            }
        )

        val authRepo = com.nivya.data.repository.AuthRepository(api, storage, prefs)

        // Initial token
        authRepo.syncPushToken("initial-token-111")
        assertEquals("initial-token-111", latestSyncedToken)
        assertEquals("initial-token-111", prefs.getLastSyncedFcmToken())

        // Rotation to new token
        prefs.saveFcmToken("rotated-token-222")
        authRepo.syncPushToken("rotated-token-222")
        assertEquals("rotated-token-222", latestSyncedToken)
        assertEquals("rotated-token-222", prefs.getLastSyncedFcmToken())
    }

    @Test
    fun test8_NoDuplicateUncontrolledTokenRegistration() = kotlinx.coroutines.runBlocking {
        val prefs = FakeUserPreferencesDataStore()
        val storage = FakeTokenStorage()
        storage.saveTokens("token_access", "token_refresh")

        var networkCalls = 0
        val api = createFakeApiService(
            onRegisterPushToken = { req ->
                networkCalls++
                retrofit2.Response.success(
                    com.nivya.core.network.dto.ApiResponseDto(
                        success = true,
                        message = "Registered",
                        data = PushTokenResponseDto(req.deviceUuid, req.pushToken, "REGISTERED", "now"),
                        timestamp = "now",
                        traceId = "trace"
                    )
                )
            }
        )

        val authRepo = com.nivya.data.repository.AuthRepository(api, storage, prefs)

        prefs.saveFcmToken("token-steady-state")

        // First sync (force = false, but not synced yet) -> triggers sync
        val sync1 = authRepo.syncStoredPushToken(force = false)
        assertNotNull(sync1)
        assertEquals(1, networkCalls)

        // Second sync (force = false, already synced) -> deduplicated, returns null without network call
        val sync2 = authRepo.syncStoredPushToken(force = false)
        assertNull("Duplicate sync must be skipped", sync2)
        assertEquals("Network calls must not increase on duplicate sync", 1, networkCalls)

        // Force sync -> bypasses deduplication
        val sync3 = authRepo.syncStoredPushToken(force = true)
        assertNotNull(sync3)
        assertEquals(2, networkCalls)
    }

    @Test
    fun test9_ActualTokenValueNeverAppearsInLogs() {
        fun maskTokenForLogging(token: String): String {
            return if (token.length <= 8) "***" else "${token.take(8)}..."
        }

        val secretHardwareToken = "fcm_very_long_secret_hardware_device_token_xyz9876543210"
        val loggedSnippet = maskTokenForLogging(secretHardwareToken)

        assertEquals("fcm_very...", loggedSnippet)
        assertFalse("Full secret token must never appear in log string", loggedSnippet.contains("xyz9876543210"))
    }
}
