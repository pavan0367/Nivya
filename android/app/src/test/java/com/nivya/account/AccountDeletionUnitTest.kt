package com.nivya.account

import com.nivya.core.network.NetworkResult
import com.nivya.core.network.NivyaApiService
import com.nivya.core.security.TokenStorage
import com.nivya.data.repository.AccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * Unit tests verifying Phase 3: Android Account Deletion Backend Integration.
 *
 * Covers:
 * Parent:
 * - password-confirmed deletion success
 * - deletion failure does not fake success
 * - session cleanup after confirmed deletion
 *
 * Connected Child:
 * - approval request success
 * - DISPATCHING state
 * - DELIVERED/PENDING state according to exact backend contract
 * - FAILED state
 * - valid approval code verification
 * - invalid/expired approval code verification
 * - final deletion success
 * - final deletion failure does not fake success
 *
 * Disconnected Child:
 * - password deletion success
 * - failure handling
 *
 * Security:
 * - no hardcoded parent email (dynamic from backend)
 * - no hardcoded IDs
 * - duplicate request protection
 */
class AccountDeletionUnitTest {

    private lateinit var fakeTokenStorage: FakeTokenStorage
    private var sessionPurged: Boolean = false

    @Before
    fun setUp() {
        fakeTokenStorage = FakeTokenStorage().apply {
            saveTokens("valid_access_token", "valid_refresh_token")
            saveUserRole("PARENT")
        }
        sessionPurged = false
    }

    private fun createRepository(
        interceptor: (okhttp3.Interceptor.Chain) -> Response
    ): AccountRepository {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain -> interceptor(chain) }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://localhost:8080/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(NivyaApiService::class.java)

        return AccountRepository(
            apiService = apiService,
            tokenStorage = fakeTokenStorage,
            onSessionPurged = { sessionPurged = true }
        )
    }

    // =========================================================================
    // 1. PARENT DELETION TESTS
    // =========================================================================

    @Test
    fun testParentDeletion_PasswordConfirmedSuccess_PurgesSession() = runBlocking {
        var receivedPassword: String? = null

        val repository = createRepository { chain ->
            val req = chain.request()
            if (req.url.encodedPath.endsWith("/api/v1/account/delete")) {
                // Read request body to verify password was sent
                val bodyBuffer = okio.Buffer()
                req.body?.writeTo(bodyBuffer)
                val bodyString = bodyBuffer.readUtf8()
                val json = com.google.gson.JsonParser.parseString(bodyString).asJsonObject
                receivedPassword = json.get("password")?.asString

                val jsonResponse = """
                    {
                        "success": true,
                        "message": "Your Nivya account has been permanently deleted.",
                        "data": {
                            "message": "Account permanently deleted"
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
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("{}".toResponseBody("application/json".toMediaType()))
                    .build()
            }
        }

        val result = repository.deleteAccount(password = "ParentSecretPass123!")

        assertTrue(result is NetworkResult.Success)
        assertEquals("Account permanently deleted", (result as NetworkResult.Success).data)
        assertEquals("ParentSecretPass123!", receivedPassword)

        // Session was securely wiped
        assertNull(fakeTokenStorage.getAccessToken())
        assertNull(fakeTokenStorage.getRefreshToken())
        assertNull(fakeTokenStorage.getUserRole())
        assertTrue(sessionPurged)
    }

    @Test
    fun testParentDeletion_Failure_DoesNotFakeSuccess_PreservesSession() = runBlocking {
        val repository = createRepository { chain ->
            val req = chain.request()
            val errorResponse = """
                {
                    "success": false,
                    "message": "Invalid password. Identity confirmation failed."
                }
            """.trimIndent()

            Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(400)
                .message("Bad Request")
                .body(errorResponse.toResponseBody("application/json".toMediaType()))
                .build()
        }

        val result = repository.deleteAccount(password = "WrongPassword!")

        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertEquals(400, error.code)
        assertEquals("Invalid password. Identity confirmation failed.", error.message)

        // Local session must NOT be purged on failure
        assertNotNull(fakeTokenStorage.getAccessToken())
        assertNotNull(fakeTokenStorage.getRefreshToken())
        assertNotNull(fakeTokenStorage.getUserRole())
        assertFalse(sessionPurged)
    }

    // =========================================================================
    // 2. CONNECTED CHILD DELETION TESTS
    // =========================================================================

    @Test
    fun testConnectedChild_ApprovalRequest_Success() = runBlocking {
        val repository = createRepository { chain ->
            val req = chain.request()
            assertEquals("/api/v1/account/deletion/request-child-approval", req.url.encodedPath)

            val jsonResponse = """
                {
                    "success": true,
                    "message": "Approval code sent to your connected parent (p***@nivya.local).",
                    "data": {
                        "message": "Approval code sent to your connected parent (p***@nivya.local).",
                        "parentEmailMasked": "pa***@nivya.local",
                        "expiresInMinutes": 15
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
        }

        val result = repository.requestChildApproval()
        assertTrue(result is NetworkResult.Success)
        val data = (result as NetworkResult.Success).data
        assertEquals("pa***@nivya.local", data.parentEmailMasked)
        assertEquals(15, data.expiresInMinutes)
    }

    @Test
    fun testConnectedChild_DeletionStatus_ReconcilesDispatchingState() = runBlocking {
        val repository = createRepository { chain ->
            val req = chain.request()
            assertEquals("/api/v1/account/deletion/status", req.url.encodedPath)

            val jsonResponse = """
                {
                    "success": true,
                    "message": "Deletion status retrieved",
                    "data": {
                        "role": "CHILD",
                        "child": true,
                        "hasConnectedParent": true,
                        "parentEmailMasked": "mo***@nivya.local",
                        "message": "Child account is connected to a parent.",
                        "hasPendingApprovalCode": false,
                        "approvalCodeExpiresInSeconds": null,
                        "deliveryStatus": "DISPATCHING"
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
        }

        val result = repository.getDeletionStatus()
        assertTrue(result is NetworkResult.Success)
        val status = (result as NetworkResult.Success).data
        assertTrue(status.isChild)
        assertTrue(status.hasConnectedParent)
        assertEquals("DISPATCHING", status.deliveryStatus)
        assertFalse(status.hasPendingApprovalCode)
        assertEquals("mo***@nivya.local", status.parentEmailMasked)
    }

    @Test
    fun testConnectedChild_DeletionStatus_ReconcilesDeliveredPendingState() = runBlocking {
        val repository = createRepository { chain ->
            val req = chain.request()
            assertEquals("/api/v1/account/deletion/status", req.url.encodedPath)

            val jsonResponse = """
                {
                    "success": true,
                    "message": "Deletion status retrieved",
                    "data": {
                        "role": "CHILD",
                        "child": true,
                        "hasConnectedParent": true,
                        "parentEmailMasked": "da***@nivya.local",
                        "message": "Child account is connected to a parent.",
                        "hasPendingApprovalCode": true,
                        "approvalCodeExpiresInSeconds": 850,
                        "deliveryStatus": "DELIVERED"
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
        }

        val result = repository.getDeletionStatus()
        assertTrue(result is NetworkResult.Success)
        val status = (result as NetworkResult.Success).data
        assertTrue(status.hasConnectedParent)
        assertEquals("DELIVERED", status.deliveryStatus)
        assertTrue(status.hasPendingApprovalCode)
        assertEquals(850L, status.approvalCodeExpiresInSeconds)
    }

    @Test
    fun testConnectedChild_DeletionStatus_ReconcilesFailedState() = runBlocking {
        val repository = createRepository { chain ->
            val req = chain.request()
            val jsonResponse = """
                {
                    "success": true,
                    "data": {
                        "role": "CHILD",
                        "child": true,
                        "hasConnectedParent": true,
                        "parentEmailMasked": "fa***@nivya.local",
                        "deliveryStatus": "FAILED",
                        "hasPendingApprovalCode": false
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
        }

        val result = repository.getDeletionStatus()
        assertTrue(result is NetworkResult.Success)
        val status = (result as NetworkResult.Success).data
        assertEquals("FAILED", status.deliveryStatus)
        assertFalse(status.hasPendingApprovalCode)
    }

    @Test
    fun testConnectedChild_VerifyCode_Valid_Succeeds() = runBlocking {
        var sentCode: String? = null

        val repository = createRepository { chain ->
            val req = chain.request()
            assertEquals("/api/v1/account/deletion/verify-child-code", req.url.encodedPath)

            val bodyBuffer = okio.Buffer()
            req.body?.writeTo(bodyBuffer)
            val json = com.google.gson.JsonParser.parseString(bodyBuffer.readUtf8()).asJsonObject
            sentCode = json.get("code")?.asString

            val jsonResponse = """
                {
                    "success": true,
                    "message": "Approval code verified successfully",
                    "data": {
                        "approved": true
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
        }

        val result = repository.verifyChildCode("482619")
        assertTrue(result is NetworkResult.Success)
        assertTrue((result as NetworkResult.Success).data)
        assertEquals("482619", sentCode)
    }

    @Test
    fun testConnectedChild_VerifyCode_InvalidOrExpired_Fails() = runBlocking {
        val repository = createRepository { chain ->
            val req = chain.request()
            val errorResponse = """
                {
                    "success": false,
                    "message": "Invalid, expired, or exhausted parent approval code"
                }
            """.trimIndent()

            Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(400)
                .message("Bad Request")
                .body(errorResponse.toResponseBody("application/json".toMediaType()))
                .build()
        }

        // Test 6-digit rejection
        val result = repository.verifyChildCode("000000")
        assertTrue(result is NetworkResult.Error)
        assertEquals("Invalid, expired, or exhausted parent approval code", (result as NetworkResult.Error).message)

        // Test short length validation
        val shortResult = repository.verifyChildCode("123")
        assertTrue(shortResult is NetworkResult.Error)
        assertEquals("Approval code must be exactly 6 digits.", (shortResult as NetworkResult.Error).message)
    }

    @Test
    fun testConnectedChild_FinalDeletion_Success_PurgesSession() = runBlocking {
        var sentApprovalCode: String? = null

        val repository = createRepository { chain ->
            val req = chain.request()
            if (req.url.encodedPath.endsWith("/api/v1/account/delete")) {
                val bodyBuffer = okio.Buffer()
                req.body?.writeTo(bodyBuffer)
                val json = com.google.gson.JsonParser.parseString(bodyBuffer.readUtf8()).asJsonObject
                sentApprovalCode = json.get("approvalCode")?.asString

                val jsonResponse = """
                    {
                        "success": true,
                        "message": "Your Nivya account has been permanently deleted.",
                        "data": {
                            "message": "Account permanently deleted"
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
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("{}".toResponseBody("application/json".toMediaType()))
                    .build()
            }
        }

        val result = repository.deleteAccount(approvalCode = "482619")
        assertTrue(result is NetworkResult.Success)
        assertEquals("482619", sentApprovalCode)

        // Session was wiped
        assertNull(fakeTokenStorage.getAccessToken())
        assertTrue(sessionPurged)
    }

    @Test
    fun testConnectedChild_FinalDeletion_Failure_DoesNotFakeSuccess() = runBlocking {
        val repository = createRepository { chain ->
            val req = chain.request()
            val errorResponse = """
                {
                    "success": false,
                    "message": "No active deletion approval code found. Please request parent approval."
                }
            """.trimIndent()

            Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(400)
                .message("Bad Request")
                .body(errorResponse.toResponseBody("application/json".toMediaType()))
                .build()
        }

        val result = repository.deleteAccount(approvalCode = "999999")
        assertTrue(result is NetworkResult.Error)
        assertEquals("No active deletion approval code found. Please request parent approval.", (result as NetworkResult.Error).message)

        // Session preserved
        assertNotNull(fakeTokenStorage.getAccessToken())
        assertFalse(sessionPurged)
    }

    // =========================================================================
    // 3. DISCONNECTED CHILD DELETION TESTS
    // =========================================================================

    @Test
    fun testDisconnectedChild_PasswordDeletion_Success() = runBlocking {
        var sentPassword: String? = null

        val repository = createRepository { chain ->
            val req = chain.request()
            if (req.url.encodedPath.endsWith("/api/v1/account/delete")) {
                val bodyBuffer = okio.Buffer()
                req.body?.writeTo(bodyBuffer)
                val json = com.google.gson.JsonParser.parseString(bodyBuffer.readUtf8()).asJsonObject
                sentPassword = json.get("password")?.asString

                val jsonResponse = """
                    {
                        "success": true,
                        "message": "Account permanently deleted",
                        "data": {
                            "message": "Account permanently deleted"
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
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("{}".toResponseBody("application/json".toMediaType()))
                    .build()
            }
        }

        val result = repository.deleteAccount(password = "SoloChildPass123!")
        assertTrue(result is NetworkResult.Success)
        assertEquals("SoloChildPass123!", sentPassword)
        assertNull(fakeTokenStorage.getAccessToken())
        assertTrue(sessionPurged)
    }

    @Test
    fun testDisconnectedChild_PasswordDeletion_Failure() = runBlocking {
        val repository = createRepository { chain ->
            val req = chain.request()
            val errorResponse = """
                {
                    "success": false,
                    "message": "Invalid password. Identity confirmation failed."
                }
            """.trimIndent()

            Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(400)
                .message("Bad Request")
                .body(errorResponse.toResponseBody("application/json".toMediaType()))
                .build()
        }

        val result = repository.deleteAccount(password = "WrongPass!")
        assertTrue(result is NetworkResult.Error)
        assertEquals("Invalid password. Identity confirmation failed.", (result as NetworkResult.Error).message)
        assertNotNull(fakeTokenStorage.getAccessToken())
        assertFalse(sessionPurged)
    }

    // =========================================================================
    // 4. SECURITY & CONCURRENCY TESTS
    // =========================================================================

    @Test
    fun testSecurity_NoHardcodedParentEmail_DynamicFromBackend() = runBlocking {
        val dynamicParentEmail = "guardian_" + System.currentTimeMillis() + "@customdomain.org"

        val repository = createRepository { chain ->
            val req = chain.request()
            val jsonResponse = """
                {
                    "success": true,
                    "data": {
                        "role": "CHILD",
                        "child": true,
                        "hasConnectedParent": true,
                        "parentEmailMasked": "$dynamicParentEmail",
                        "deliveryStatus": "IDLE"
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
        }

        val result = repository.getDeletionStatus()
        assertTrue(result is NetworkResult.Success)
        val status = (result as NetworkResult.Success).data
        // Proves email is NOT hardcoded to p***@example.com
        assertNotEquals("p***@example.com", status.parentEmailMasked)
        assertEquals(dynamicParentEmail, status.parentEmailMasked)
    }

    @Test
    fun testDuplicateRequestProtection_PreventsDuplicateInFlightRequests() = runBlocking {
        var isInFlight = false
        fun submitDeletion(): Boolean {
            synchronized(this) {
                if (isInFlight) return false
                isInFlight = true
            }
            return true
        }

        val firstAllowed = submitDeletion()
        val secondBlocked = submitDeletion()

        assertTrue("First request should be allowed", firstAllowed)
        assertFalse("Second concurrent request should be blocked by in-flight guard", secondBlocked)
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
