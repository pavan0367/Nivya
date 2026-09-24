package com.nivya.auth

import com.nivya.core.network.NetworkResult
import com.nivya.core.network.NivyaApiService
import com.nivya.core.network.dto.*
import com.nivya.core.security.TokenStorage
import com.nivya.data.repository.AuthRepository
import com.nivya.ui.auth.EmailVerificationViewModel
import com.nivya.ui.auth.LoginViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import com.nivya.core.navigation.NavigationDestination
import com.nivya.core.navigation.UriPathEncoder
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class EmailVerificationUnitTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        testDispatcher.scheduler.advanceUntilIdle()
        Dispatchers.resetMain()
    }

    // --- Lightweight Test Doubles ---

    private class FakeTokenStorage : TokenStorage {
        var savedAccessToken: String? = null
        var savedRefreshToken: String? = null
        var savedRole: String? = null
        var cleared: Boolean = false

        override fun saveTokens(accessToken: String, refreshToken: String) {
            savedAccessToken = accessToken
            savedRefreshToken = refreshToken
        }

        override fun getAccessToken(): String? = savedAccessToken
        override fun getRefreshToken(): String? = savedRefreshToken
        override fun saveUserRole(role: String) { savedRole = role }
        override fun getUserRole(): String? = savedRole
        override fun saveDeviceUuid(uuid: String) {}
        override fun getDeviceUuid(): String = "test-device-uuid"
        override fun clearAll() {
            cleared = true
            savedAccessToken = null
            savedRefreshToken = null
        }
        override fun hasAccessToken(): Boolean = savedAccessToken != null
    }

    private class FakeAuthRepository : AuthRepository() {
        var registerResult: NetworkResult<AuthResponseDataDto>? = null
        var loginResult: NetworkResult<AuthResponseDataDto>? = null
        var sendCodeResult: NetworkResult<String>? = null
        var confirmCodeResult: NetworkResult<Boolean>? = null
        var sendCodeCallCount: Int = 0
        var lastConfirmedEmail: String? = null
        var lastConfirmedCode: String? = null

        override suspend fun register(name: String, email: String, password: String, role: String): NetworkResult<AuthResponseDataDto> {
            return registerResult ?: NetworkResult.Error(500, "No register result configured")
        }

        override suspend fun login(email: String, password: String): NetworkResult<AuthResponseDataDto> {
            return loginResult ?: NetworkResult.Error(500, "No login result configured")
        }

        override suspend fun sendVerificationCode(email: String): NetworkResult<String> {
            sendCodeCallCount++
            return sendCodeResult ?: NetworkResult.Error(500, "No send result configured")
        }

        override suspend fun confirmVerificationCode(email: String, code: String): NetworkResult<Boolean> {
            lastConfirmedEmail = email
            lastConfirmedCode = code
            return confirmCodeResult ?: NetworkResult.Error(500, "No confirm result configured")
        }
    }

    // --- Tests ---

    @Test
    fun testRegistrationRequiringVerificationRoutesToVerificationState() = runTest {
        val fakeRepo = FakeAuthRepository()
        val pendingUser = UserDto(
            id = 1001L,
            uuid = "user-uuid-1",
            name = "Test Parent",
            email = "parent@example.com",
            role = "PARENT",
            status = "PENDING"
        )
        fakeRepo.registerResult = NetworkResult.Success(
            AuthResponseDataDto(
                accessToken = "pending-access-token",
                refreshToken = "pending-refresh-token",
                tokenType = "Bearer",
                expiresIn = 3600L,
                user = pendingUser
            )
        )

        val viewModel = LoginViewModel(fakeRepo)
        viewModel.toggleMode() // switch to register mode
        viewModel.onNameChanged("Test Parent")
        viewModel.onEmailChanged("parent@example.com")
        viewModel.onPasswordChanged("Secret123!")
        viewModel.onRegisterRoleChanged("PARENT")

        viewModel.submit()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("Unverified registration must not set isSuccess=true", state.isSuccess)
        assertTrue("Unverified registration must require verification", state.requiresVerification)
        assertEquals("parent@example.com", state.unverifiedEmail)
        assertNull(state.errorMessage)
    }

    @Test
    fun testUnverifiedUserIsNotTreatedAsAuthenticatedInRepository() = runTest {
        val fakeTokenStorage = FakeTokenStorage()
        fakeTokenStorage.saveTokens("old-access", "old-refresh")

        val pendingUser = UserDto(
            id = 1002L,
            uuid = "user-uuid-2",
            name = "Pending User",
            email = "pending@example.com",
            role = "PARENT",
            status = "PENDING"
        )
        val authData = AuthResponseDataDto(
            accessToken = "pending-token",
            refreshToken = "pending-refresh",
            tokenType = "Bearer",
            expiresIn = 3600L,
            user = pendingUser
        )

        val fakeApiService = Proxy.newProxyInstance(
            NivyaApiService::class.java.classLoader,
            arrayOf(NivyaApiService::class.java)
        ) { _, method, _ ->
            if (method.name == "register") {
                retrofit2.Response.success(
                    ApiResponseDto(
                        success = true,
                        message = "User registered successfully",
                        data = authData,
                        timestamp = "2026-09-20T12:00:00Z",
                        traceId = "trace-test-123"
                    )
                )
            } else {
                null
            }
        } as NivyaApiService

        val repo = AuthRepository(
            apiService = fakeApiService,
            tokenStorage = fakeTokenStorage,
            preferencesDataStore = null,
            database = null
        )

        val result = repo.register("Pending User", "pending@example.com", "Secret123!", "PARENT")

        assertTrue(result is NetworkResult.Success<*>)
        // Verify tokens are NOT saved to tokenStorage for unverified pending account
        assertNull("Pending account access token must not be saved", fakeTokenStorage.savedAccessToken)
        assertNull("Pending account refresh token must not be saved", fakeTokenStorage.savedRefreshToken)
        assertTrue("Existing/lingering tokens must be cleared", fakeTokenStorage.cleared)
    }

    @Test
    fun testValid6DigitVerificationSucceeds() = runTest {
        val fakeRepo = FakeAuthRepository()
        fakeRepo.confirmCodeResult = NetworkResult.Success(true)

        val viewModel = EmailVerificationViewModel("user@example.com", fakeRepo, initialCooldownSeconds = 0)
        viewModel.onOtpChanged("654321")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Valid code must set isVerified=true", state.isVerified)
        assertNull(state.errorMessage)
        assertNotNull(state.infoMessage)
        viewModel.onCleared()
    }

    @Test
    fun testInvalidCodeFails() = runTest {
        val fakeRepo = FakeAuthRepository()
        fakeRepo.confirmCodeResult = NetworkResult.Error(code = 400, message = "Invalid, expired, or exhausted verification code")

        val viewModel = EmailVerificationViewModel("user@example.com", fakeRepo, initialCooldownSeconds = 0)
        viewModel.onOtpChanged("000000")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("Invalid code must not set isVerified=true", state.isVerified)
        assertEquals("Invalid, expired, or exhausted verification code", state.errorMessage)
        viewModel.onCleared()
    }

    @Test
    fun testExpiredCodeFails() = runTest {
        val fakeRepo = FakeAuthRepository()
        fakeRepo.confirmCodeResult = NetworkResult.Error(code = 400, message = "Verification code expired")

        val viewModel = EmailVerificationViewModel("user@example.com", fakeRepo, initialCooldownSeconds = 0)
        viewModel.onOtpChanged("111111")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isVerified)
        assertEquals("Verification code expired", state.errorMessage)
        viewModel.onCleared()
    }

    @Test
    fun testResendSuccessSetsInfoMessageAndCooldown() = runTest {
        val fakeRepo = FakeAuthRepository()
        fakeRepo.sendCodeResult = NetworkResult.Success("Verification code dispatched")

        val viewModel = EmailVerificationViewModel("user@example.com", fakeRepo, initialCooldownSeconds = 0)
        assertTrue("Can resend when cooldown is 0", viewModel.uiState.value.canResend)

        viewModel.resendCode()
        testDispatcher.scheduler.advanceTimeBy(50L) // Execute network call, before 1-second delay tick

        val state = viewModel.uiState.value
        assertEquals("Verification code dispatched", state.infoMessage)
        assertFalse(state.isResending)
        assertTrue("Cooldown must be restarted after successful resend", state.resendCooldown > 0)
        assertEquals(1, fakeRepo.sendCodeCallCount)
        viewModel.onCleared()
    }

    @Test
    fun testResendFailureShowsErrorMessage() = runTest {
        val fakeRepo = FakeAuthRepository()
        fakeRepo.sendCodeResult = NetworkResult.Error(code = 429, message = "Too many resend attempts. Please wait.")

        val viewModel = EmailVerificationViewModel("user@example.com", fakeRepo, initialCooldownSeconds = 0)
        viewModel.resendCode()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Too many resend attempts. Please wait.", state.errorMessage)
        viewModel.onCleared()
    }

    @Test
    fun testDuplicateResendPreventionDuringCooldown() = runTest {
        val fakeRepo = FakeAuthRepository()
        val viewModel = EmailVerificationViewModel("user@example.com", fakeRepo, initialCooldownSeconds = 60)

        // Right after initialization, cooldown is 60 seconds
        val state = viewModel.uiState.value
        assertFalse("Cannot resend while cooldown is active", state.canResend)

        viewModel.resendCode()
        testDispatcher.scheduler.advanceTimeBy(50L)

        // Verify repository was never called due to cooldown guard
        assertEquals(0, fakeRepo.sendCodeCallCount)
        viewModel.onCleared()
    }

    @Test
    fun testUnverifiedLogin401RoutesToVerification() = runTest {
        val fakeRepo = FakeAuthRepository()
        fakeRepo.loginResult = NetworkResult.Error(
            code = 401,
            message = "Account email is not verified. Please verify your email before logging in."
        )

        val viewModel = LoginViewModel(fakeRepo)
        viewModel.onEmailChanged("unverified@example.com")
        viewModel.onPasswordChanged("Password123!")
        viewModel.submit()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSuccess)
        assertTrue("401 unverified login must require verification", state.requiresVerification)
        assertEquals("unverified@example.com", state.unverifiedEmail)
        assertEquals("Account email is not verified. Please verify your email before logging in.", state.errorMessage)
    }

    // --- Email Route Parameter Encoding / Decoding Unit Tests ---

    @Test
    fun testNormalEmailRoundTripEncoding() {
        val email = "pavan@gmail.com"
        val encoded = UriPathEncoder.encode(email)
        assertEquals("pavan%40gmail.com", encoded)
        val decoded = UriPathEncoder.decode(encoded)
        assertEquals("Normal email must exactly match original after round-trip", email, decoded)
    }

    @Test
    fun testPlusAddressRoundTripEncoding() {
        val email = "pavan+test@gmail.com"
        val encoded = UriPathEncoder.encode(email)
        assertEquals("pavan%2Btest%40gmail.com", encoded)
        assertTrue("Plus sign must be encoded as %2B in route parameter", encoded.contains("%2B"))
        assertFalse("Raw '+' must not appear unencoded in route parameter", encoded.contains("+"))

        val decoded = UriPathEncoder.decode(encoded)
        assertEquals("Plus-addressed email must preserve '+' without becoming a space", email, decoded)
        assertFalse("Decoded email must never contain space", decoded.contains(" "))
    }

    @Test
    fun testDotHyphenUnderscoreRoundTripEncoding() {
        val email = "pavan.test_01@example-domain.com"
        val encoded = UriPathEncoder.encode(email)
        assertEquals("pavan.test_01%40example-domain.com", encoded)
        val decoded = UriPathEncoder.decode(encoded)
        assertEquals("Email with dot, hyphen, underscore must round-trip exactly", email, decoded)
    }

    @Test
    fun testNoDoubleDecodeOrSpaceConversionForPreDecodedRouteArgument() {
        // If Jetpack Navigation already decoded the route argument or if a plus-addressed email is passed to decode
        val preDecoded = "pavan+test@gmail.com"
        val result = UriPathEncoder.decode(preDecoded)
        assertEquals("Pre-decoded email with '+' must not be transformed into space", "pavan+test@gmail.com", result)
        assertNotEquals("Must not convert '+' to space", "pavan test@gmail.com", result)
    }

    @Test
    fun testVerificationRequestUsesDecodedOriginalEmailExactly() = runTest {
        val originalEmail = "pavan+test@gmail.com"
        // Simulate Navigation round-trip: createRoute -> decode route arg -> pass to ViewModel
        val route = NavigationDestination.EmailVerification.createRoute(originalEmail)
        val routeArg = route.removePrefix("auth/verify_email/")
        val decodedEmail = UriPathEncoder.decode(routeArg)

        assertEquals("Decoded route email must match original", originalEmail, decodedEmail)

        val fakeRepo = FakeAuthRepository()
        fakeRepo.confirmCodeResult = NetworkResult.Success(true)

        val viewModel = EmailVerificationViewModel(decodedEmail, fakeRepo, initialCooldownSeconds = 0)
        viewModel.onOtpChanged("123456")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Verification request must send exact decoded original email with '+'", originalEmail, fakeRepo.lastConfirmedEmail)
        assertEquals("123456", fakeRepo.lastConfirmedCode)
        viewModel.onCleared()
    }

    @Test
    fun testExistingAuthRoutesRemainUnchanged() {
        assertEquals("auth/login", NavigationDestination.Login.route)
        assertEquals("auth/role_selection", NavigationDestination.RoleSelection.route)
        assertEquals("auth/verify_email/{email}", NavigationDestination.EmailVerification.route)
        assertEquals("auth/verify_email/test%40example.com", NavigationDestination.EmailVerification.createRoute("test@example.com"))
    }
}

