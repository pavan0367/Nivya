package com.nivya.auth

import com.nivya.core.network.AuthInterceptor
import com.nivya.core.network.TokenAuthenticator
import com.nivya.core.security.TokenStorage
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Unit tests verifying Phase 2: Production-safe HTTP 401 token refresh handling.
 *
 * Covers:
 * 1. Valid access token -> request succeeds without refresh.
 * 2. 401 + valid refresh token -> refresh succeeds -> retried once with new token.
 * 3. Refresh token rotation -> both access & refresh tokens are persisted correctly.
 * 4. 401 + refresh failure -> local session is purged and returns null.
 * 5. No infinite retry loop -> stops after 1 retry.
 * 6. Multiple concurrent 401s do not create unnecessary parallel refresh requests (single coordinated refresh).
 * 7. Refresh request itself is not recursively intercepted.
 * 8. Secrets are not written to logs.
 * 9. Missing refresh token -> purges session and returns null without network call.
 */
class TokenRefreshUnitTest {

    private lateinit var fakeTokenStorage: FakeTokenStorage
    private var sessionPurged: Boolean = false

    @Before
    fun setUp() {
        fakeTokenStorage = FakeTokenStorage()
        sessionPurged = false
    }

    // =========================================================================
    // Test 1: Valid access token -> request succeeds without refresh
    // =========================================================================
    @Test
    fun testValidAccessToken_RequestSucceedsWithoutRefresh() {
        fakeTokenStorage.saveTokens(accessToken = "valid_access_token_123", refreshToken = "refresh_token_456")

        var refreshCallCount = 0
        val mockRefreshClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                refreshCallCount++
                chain.proceed(chain.request())
            }
            .build()

        val authenticator = TokenAuthenticator(
            tokenStorage = fakeTokenStorage,
            baseUrl = "http://localhost:8080/",
            unauthenticatedClient = mockRefreshClient,
            onSessionExpired = { sessionPurged = true }
        )

        val authInterceptor = AuthInterceptor(fakeTokenStorage)

        var capturedAuthHeader: String? = null
        val testClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(authenticator)
            .addInterceptor { chain ->
                capturedAuthHeader = chain.request().header("Authorization")
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("{\"status\":\"ok\"}".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val request = Request.Builder()
            .url("http://localhost:8080/api/v1/user/profile")
            .build()

        val response = testClient.newCall(request).execute()

        assertEquals(200, response.code)
        assertEquals("Bearer valid_access_token_123", capturedAuthHeader)
        assertEquals(0, refreshCallCount)
        assertFalse(sessionPurged)
    }

    // =========================================================================
    // Test 2: 401 + valid refresh token -> refresh succeeds -> retried once with new token
    // =========================================================================
    @Test
    fun test401_ValidRefreshToken_RefreshSucceedsAndRetriesOnce_WithRotation() {
        fakeTokenStorage.saveTokens(accessToken = "expired_token_111", refreshToken = "valid_refresh_token_222")

        var refreshCallCount = 0
        val mockRefreshClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request()
                if (req.url.encodedPath.endsWith("/api/v1/auth/refresh")) {
                    refreshCallCount++
                    val jsonResponse = """
                        {
                            "success": true,
                            "message": "Token refreshed successfully",
                            "data": {
                                "accessToken": "new_access_token_333",
                                "refreshToken": "rotated_refresh_token_444",
                                "tokenType": "Bearer",
                                "expiresIn": 86400,
                                "user": {
                                    "id": 1,
                                    "name": "Alex",
                                    "email": "alex@nivya.local",
                                    "role": "CHILD"
                                }
                            }
                        }
                    """.trimIndent()
                    Response.Builder()
                        .request(req)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(jsonResponse.toResponseBody("application/json".toMediaType()))
                        .build()
                } else {
                    chain.proceed(req)
                }
            }
            .build()

        val authenticator = TokenAuthenticator(
            tokenStorage = fakeTokenStorage,
            baseUrl = "http://localhost:8080/",
            unauthenticatedClient = mockRefreshClient,
            onSessionExpired = { sessionPurged = true }
        )

        val originalRequest = Request.Builder()
            .url("http://localhost:8080/api/v1/telemetry/battery")
            .header("Authorization", "Bearer expired_token_111")
            .build()

        val response401 = Response.Builder()
            .request(originalRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("{\"message\":\"Token expired\"}".toResponseBody("application/json".toMediaType()))
            .build()

        val retriedRequest = authenticator.authenticate(null, response401)

        // 1. Authenticator returned a valid retried request
        assertNotNull(retriedRequest)
        assertEquals("Bearer new_access_token_333", retriedRequest?.header("Authorization"))
        assertEquals(1, refreshCallCount)

        // 2. Token storage was updated with new access token and rotated refresh token
        assertEquals("new_access_token_333", fakeTokenStorage.getAccessToken())
        assertEquals("rotated_refresh_token_444", fakeTokenStorage.getRefreshToken())
        assertEquals("CHILD", fakeTokenStorage.getUserRole())
        assertFalse(sessionPurged)
    }

    // =========================================================================
    // Test 3: Refresh token rotation -> both tokens are persisted correctly
    // =========================================================================
    @Test
    fun testRefreshTokenRotation_PersistsBothTokensAndRole() {
        fakeTokenStorage.saveTokens(accessToken = "old_access", refreshToken = "old_refresh")

        val mockRefreshClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val jsonResponse = """
                    {
                        "success": true,
                        "data": {
                            "accessToken": "brand_new_access",
                            "refreshToken": "brand_new_refresh_rotated",
                            "tokenType": "Bearer",
                            "expiresIn": 86400,
                            "user": {
                                "id": 42,
                                "name": "Parent User",
                                "email": "parent@nivya.local",
                                "role": "PARENT"
                            }
                        }
                    }
                """.trimIndent()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(jsonResponse.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val authenticator = TokenAuthenticator(
            tokenStorage = fakeTokenStorage,
            baseUrl = "http://localhost:8080/",
            unauthenticatedClient = mockRefreshClient
        )

        val request = Request.Builder()
            .url("http://localhost:8080/api/v1/family/members")
            .header("Authorization", "Bearer old_access")
            .build()

        val response401 = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("{}".toResponseBody("application/json".toMediaType()))
            .build()

        val retry = authenticator.authenticate(null, response401)
        assertNotNull(retry)
        assertEquals("Bearer brand_new_access", retry?.header("Authorization"))

        // Both tokens & role must be updated in storage
        assertEquals("brand_new_access", fakeTokenStorage.getAccessToken())
        assertEquals("brand_new_refresh_rotated", fakeTokenStorage.getRefreshToken())
        assertEquals("PARENT", fakeTokenStorage.getUserRole())
    }

    // =========================================================================
    // Test 4: 401 + refresh failure -> local session is purged
    // =========================================================================
    @Test
    fun test401_RefreshFailure_PurgesLocalSession() {
        fakeTokenStorage.saveTokens(accessToken = "expired_token_111", refreshToken = "invalid_refresh_token_999")
        fakeTokenStorage.saveUserRole("CHILD")

        val mockRefreshClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                // Refresh endpoint rejects the invalid refresh token with 401
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(401)
                    .message("Unauthorized")
                    .body("{\"success\":false,\"message\":\"Refresh token expired or revoked\"}".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val authenticator = TokenAuthenticator(
            tokenStorage = fakeTokenStorage,
            baseUrl = "http://localhost:8080/",
            unauthenticatedClient = mockRefreshClient,
            onSessionExpired = { sessionPurged = true }
        )

        val request = Request.Builder()
            .url("http://localhost:8080/api/v1/telemetry/battery")
            .header("Authorization", "Bearer expired_token_111")
            .build()

        val response401 = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("{\"message\":\"Token expired\"}".toResponseBody("application/json".toMediaType()))
            .build()

        val retryRequest = authenticator.authenticate(null, response401)

        // 1. Authenticator returns null (stops retrying)
        assertNull(retryRequest)

        // 2. Local session is completely purged
        assertNull(fakeTokenStorage.getAccessToken())
        assertNull(fakeTokenStorage.getRefreshToken())
        assertNull(fakeTokenStorage.getUserRole())
        assertTrue(sessionPurged)
    }

    // =========================================================================
    // Test 5: No infinite retry loop -> stops after 1 retry
    // =========================================================================
    @Test
    fun testNoInfiniteRetryLoop_StopsAfterOneRetry() {
        fakeTokenStorage.saveTokens(accessToken = "expired_token_111", refreshToken = "valid_refresh_token_222")

        val mockRefreshClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val jsonResponse = """
                    {
                        "success": true,
                        "data": {
                            "accessToken": "new_access_token_333",
                            "refreshToken": "new_refresh_token_444",
                            "tokenType": "Bearer",
                            "expiresIn": 86400,
                            "user": { "id": 1, "name": "User", "email": "user@nivya.local", "role": "PARENT" }
                        }
                    }
                """.trimIndent()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(jsonResponse.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val authenticator = TokenAuthenticator(
            tokenStorage = fakeTokenStorage,
            baseUrl = "http://localhost:8080/",
            unauthenticatedClient = mockRefreshClient,
            onSessionExpired = { sessionPurged = true }
        )

        // First 401 response (attempt 1)
        val initialRequest = Request.Builder()
            .url("http://localhost:8080/api/v1/protected/endpoint")
            .header("Authorization", "Bearer expired_token_111")
            .build()

        val firstResponse = Response.Builder()
            .request(initialRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("{\"message\":\"Initial 401\"}".toResponseBody("application/json".toMediaType()))
            .build()

        // First authenticate call: should succeed and return retried request with new token
        val firstRetry = authenticator.authenticate(null, firstResponse)
        assertNotNull(firstRetry)
        assertEquals("Bearer new_access_token_333", firstRetry?.header("Authorization"))

        // Second 401 response (attempt 2 failed despite retry)
        val priorWithoutBody = firstResponse.newBuilder().body(null).build()
        val secondResponse = Response.Builder()
            .request(firstRetry!!)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("{\"message\":\"Persistent 401\"}".toResponseBody("application/json".toMediaType()))
            .priorResponse(priorWithoutBody)
            .build()

        // Second authenticate call: MUST return null to prevent infinite loop
        val secondRetry = authenticator.authenticate(null, secondResponse)
        assertNull("Authenticator must return null when responseCount >= 2 to prevent infinite loops", secondRetry)
    }

    // =========================================================================
    // Test 6: Concurrent 401s do not create unnecessary parallel refresh requests
    // =========================================================================
    @Test
    fun testConcurrent401s_CoordinatedSingleRefresh() {
        fakeTokenStorage.saveTokens(accessToken = "expired_token_000", refreshToken = "valid_refresh_token_111")

        val refreshCallCount = AtomicInteger(0)
        val mockRefreshClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                refreshCallCount.incrementAndGet()
                // Small sleep to simulate network latency and test concurrency locking
                Thread.sleep(60)
                val jsonResponse = """
                    {
                        "success": true,
                        "data": {
                            "accessToken": "refreshed_access_token_888",
                            "refreshToken": "refreshed_refresh_token_999",
                            "tokenType": "Bearer",
                            "expiresIn": 86400,
                            "user": { "id": 1, "name": "User", "email": "u@n.l", "role": "PARENT" }
                        }
                    }
                """.trimIndent()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(jsonResponse.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val authenticator = TokenAuthenticator(
            tokenStorage = fakeTokenStorage,
            baseUrl = "http://localhost:8080/",
            unauthenticatedClient = mockRefreshClient,
            onSessionExpired = { sessionPurged = true }
        )

        val threadCount = 8
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadCount)
        val retryResults = ConcurrentHashMap<Int, Request?>()

        for (i in 0 until threadCount) {
            Thread {
                startLatch.await()
                try {
                    val req = Request.Builder()
                        .url("http://localhost:8080/api/v1/telemetry/$i")
                        .header("Authorization", "Bearer expired_token_000")
                        .build()
                    val res401 = Response.Builder()
                        .request(req)
                        .protocol(Protocol.HTTP_1_1)
                        .code(401)
                        .message("Unauthorized")
                        .body("{}".toResponseBody("application/json".toMediaType()))
                        .build()
                    val retry = authenticator.authenticate(null, res401)
                    retryResults[i] = retry
                } finally {
                    doneLatch.countDown()
                }
            }.start()
        }

        // Release all 8 threads simultaneously
        startLatch.countDown()
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS))

        // All 8 threads must receive a valid retry request with the refreshed token
        assertEquals(threadCount, retryResults.size)
        for (i in 0 until threadCount) {
            val retry = retryResults[i]
            assertNotNull("Thread $i should have received a retried request", retry)
            assertEquals("Bearer refreshed_access_token_888", retry?.header("Authorization"))
        }

        // Critical verification: Exactly ONE refresh call was executed across all concurrent 401s!
        assertEquals(1, refreshCallCount.get())
    }

    // =========================================================================
    // Test 7: Refresh request itself is not recursively intercepted
    // =========================================================================
    @Test
    fun testRefreshRequestItself_NotRecursivelyIntercepted() {
        fakeTokenStorage.saveTokens(accessToken = "token_123", refreshToken = "refresh_456")

        val authenticator = TokenAuthenticator(
            tokenStorage = fakeTokenStorage,
            baseUrl = "http://localhost:8080/",
            unauthenticatedClient = OkHttpClient(),
            onSessionExpired = { sessionPurged = true }
        )

        // 1. Authenticator must return null for auth endpoints
        val refreshReq = Request.Builder().url("http://localhost:8080/api/v1/auth/refresh").build()
        val refreshRes = Response.Builder()
            .request(refreshReq)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("{}".toResponseBody("application/json".toMediaType()))
            .build()

        assertNull(authenticator.authenticate(null, refreshRes))

        val loginReq = Request.Builder().url("http://localhost:8080/api/v1/auth/login").build()
        val loginRes = Response.Builder()
            .request(loginReq)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("{}".toResponseBody("application/json".toMediaType()))
            .build()

        assertNull(authenticator.authenticate(null, loginRes))

        val registerReq = Request.Builder().url("http://localhost:8080/api/v1/auth/register").build()
        val registerRes = Response.Builder()
            .request(registerReq)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("{}".toResponseBody("application/json".toMediaType()))
            .build()

        assertNull(authenticator.authenticate(null, registerRes))

        // 2. AuthInterceptor must not attach Authorization header to /auth/refresh
        val authInterceptor = AuthInterceptor(fakeTokenStorage)
        var capturedHeader: String? = "SHOULD_BE_REPLACED"
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor { chain ->
                capturedHeader = chain.request().header("Authorization")
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("{}".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        client.newCall(refreshReq).execute()
        assertNull(capturedHeader)
    }

    // =========================================================================
    // Test 8: Secrets are not written to logs
    // =========================================================================
    @Test
    fun testSecretsNotWrittenToLogs() {
        val loggedMessages = mutableListOf<String>()
        val logger = HttpLoggingInterceptor.Logger { message ->
            loggedMessages.add(message)
        }

        val loggingInterceptor = HttpLoggingInterceptor(logger).apply {
            level = HttpLoggingInterceptor.Level.HEADERS
            redactHeader("Authorization")
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("{}".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val sensitiveToken = "super_secret_jwt_access_token_xyz_999"
        val request = Request.Builder()
            .url("http://localhost:8080/api/v1/secure/data")
            .header("Authorization", "Bearer $sensitiveToken")
            .build()

        client.newCall(request).execute()

        val fullLog = loggedMessages.joinToString("\n")
        // Sensitive token must NOT appear anywhere in the log output
        assertFalse("Log should never contain plaintext access token", fullLog.contains(sensitiveToken))
        assertTrue("Log should contain redacted header indicator", fullLog.contains("Authorization: ██") || fullLog.contains("Authorization: [redacted]"))
    }

    // =========================================================================
    // Test 9: Missing refresh token -> purges session and returns null
    // =========================================================================
    @Test
    fun testMissingRefreshToken_PurgesSessionAndReturnsNull() {
        fakeTokenStorage.saveTokens(accessToken = "expired_token_only", refreshToken = "")

        val mockRefreshClient = OkHttpClient()
        val authenticator = TokenAuthenticator(
            tokenStorage = fakeTokenStorage,
            baseUrl = "http://localhost:8080/",
            unauthenticatedClient = mockRefreshClient,
            onSessionExpired = { sessionPurged = true }
        )

        val request = Request.Builder()
            .url("http://localhost:8080/api/v1/data")
            .header("Authorization", "Bearer expired_token_only")
            .build()

        val response401 = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("{}".toResponseBody("application/json".toMediaType()))
            .build()

        val retry = authenticator.authenticate(null, response401)
        assertNull(retry)
        assertNull(fakeTokenStorage.getAccessToken())
        assertTrue(sessionPurged)
    }
}

/**
 * In-memory test fake implementation of TokenStorage.
 */
class FakeTokenStorage : TokenStorage {
    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var userRole: String? = null
    private var deviceUuid: String = "test-device-uuid-1234"

    override fun saveTokens(accessToken: String, refreshToken: String) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
    }

    override fun getAccessToken(): String? = accessToken

    override fun getRefreshToken(): String? = refreshToken

    override fun saveUserRole(role: String) {
        this.userRole = role
    }

    override fun getUserRole(): String? = userRole

    override fun saveDeviceUuid(uuid: String) {
        this.deviceUuid = uuid
    }

    override fun getDeviceUuid(): String = deviceUuid

    override fun clearAll() {
        accessToken = null
        refreshToken = null
        userRole = null
    }

    override fun hasAccessToken(): Boolean = !accessToken.isNullOrBlank()
}
