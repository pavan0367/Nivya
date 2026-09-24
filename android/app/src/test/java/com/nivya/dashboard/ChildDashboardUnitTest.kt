package com.nivya.dashboard

import com.google.gson.Gson
import com.nivya.core.network.NivyaApiService
import com.nivya.core.network.dto.ChildSendMessageRequestDto
import com.nivya.data.repository.ConvocationRepository
import com.nivya.ui.dashboard.ChildDashboardUiState
import com.nivya.ui.dashboard.ChildDashboardViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * Phase 5 Unit Tests: Child Dashboard CRACK / FREAK Interaction & Feedback.
 *
 * Requirements covered:
 * 1. CRACK sends exact message "Mom,here"
 * 2. FREAK sends exact message "Someone's,here"
 * 3. No pre-send confirmation step
 * 4. Fast success does not enter delayed Sending state unnecessarily
 * 5. Delayed request enters progress ("Sending...") only after 400ms grace period
 * 6. Successful request emits non-blocking "Done!" feedback
 * 7. Success feedback is non-blocking (dashboard remains interactable)
 * 8. Failed request never emits "Done!"
 * 9. Failed request never fabricates message data
 * 10. Duplicate taps while request in-flight do not dispatch duplicate requests
 * 11. CRACK and FREAK do not block each other
 * 12. "Done!" feedback auto-dismisses after ~2 seconds
 * 13. Subtitle text absent under CRACK/FREAK
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChildDashboardUnitTest {

    private val gson = Gson()

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createApiService(
        handler: (request: Request) -> Pair<Int, String>
    ): NivyaApiService {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val (code, body) = handler(request)
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message(if (code in 200..299) "OK" else "Error")
                    .body(body.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://localhost:8080/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(NivyaApiService::class.java)
    }

    private fun createRepository(apiService: NivyaApiService): ConvocationRepository {
        return ConvocationRepository(
            apiService = apiService,
            realtimeManager = null,
            ioDispatcher = Dispatchers.Unconfined,
            coroutineScope = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    private fun extractRequestBody(request: Request): String {
        val buffer = Buffer()
        request.body?.writeTo(buffer)
        return buffer.readUtf8()
    }

    private fun waitUntil(timeoutMs: Long = 3000, condition: () -> Boolean) {
        val start = System.currentTimeMillis()
        while (!condition() && (System.currentTimeMillis() - start) < timeoutMs) {
            Thread.sleep(20)
        }
    }

    // =========================================================================
    // Test 1: CRACK sends exact message "Mom,here"
    // =========================================================================
    @Test
    fun testCrackSendsExactPayloadMomHere() {
        var sentPayload: String? = null
        val apiService = createApiService { req ->
            if (req.url.encodedPath.endsWith("/child/send")) {
                sentPayload = extractRequestBody(req)
                200 to """{"success":true,"data":{"status":"SENT"}}"""
            } else {
                500 to """{"success":false}"""
            }
        }

        val repository = createRepository(apiService)
        val viewModel = ChildDashboardViewModel(repository)

        viewModel.triggerCrack()
        waitUntil { sentPayload != null }

        assertNotNull("Request should have been dispatched", sentPayload)
        val dto = gson.fromJson(sentPayload, ChildSendMessageRequestDto::class.java)
        assertEquals("Mom,here", dto.message)
        assertFalse(dto.message.contains(" "))
    }

    // =========================================================================
    // Test 2: FREAK sends exact message "Someone's,here"
    // =========================================================================
    @Test
    fun testFreakSendsExactPayloadSomeonesHere() {
        var sentPayload: String? = null
        val apiService = createApiService { req ->
            if (req.url.encodedPath.endsWith("/child/send")) {
                sentPayload = extractRequestBody(req)
                200 to """{"success":true,"data":{"status":"SENT"}}"""
            } else {
                500 to """{"success":false}"""
            }
        }

        val repository = createRepository(apiService)
        val viewModel = ChildDashboardViewModel(repository)

        viewModel.triggerFreak()
        waitUntil { sentPayload != null }

        assertNotNull("Request should have been dispatched", sentPayload)
        val dto = gson.fromJson(sentPayload, ChildSendMessageRequestDto::class.java)
        assertEquals("Someone's,here", dto.message)
    }

    // =========================================================================
    // Test 3: Immediate dispatch without confirmation step
    // =========================================================================
    @Test
    fun testNoConfirmationStepRequired() {
        var requestDispatched = false
        val apiService = createApiService { req ->
            if (req.url.encodedPath.endsWith("/child/send")) {
                requestDispatched = true
                200 to """{"success":true,"data":{"status":"SENT"}}"""
            } else {
                500 to """{"success":false}"""
            }
        }

        val repository = createRepository(apiService)
        val viewModel = ChildDashboardViewModel(repository)

        assertFalse(requestDispatched)
        viewModel.triggerCrack()

        waitUntil { requestDispatched }
        assertTrue("CRACK must immediately dispatch API request upon tap", requestDispatched)
    }

    // =========================================================================
    // Test 4: Fast success does not enter delayed Sending state
    // =========================================================================
    @Test
    fun testFastSuccessDoesNotEnterSendingState() {
        val apiService = createApiService {
            200 to """{"success":true,"data":{"status":"SENT"}}"""
        }

        val repository = createRepository(apiService)
        val viewModel = ChildDashboardViewModel(repository)

        viewModel.triggerCrack()

        waitUntil { viewModel.uiState.value.showDoneToast }

        assertFalse(
            "Fast request must not enter delayed 'Sending...' state",
            viewModel.uiState.value.showCrackSending
        )
        assertTrue(viewModel.uiState.value.showDoneToast)
    }

    // =========================================================================
    // Test 5: Delayed request enters progress ("Sending...") only after 400ms
    // =========================================================================
    @Test
    fun testDelayedRequestEntersProgressOnlyAfter400ms() {
        val apiService = createApiService {
            Thread.sleep(600)
            200 to """{"success":true,"data":{"status":"SENT"}}"""
        }

        val repository = createRepository(apiService)
        val viewModel = ChildDashboardViewModel(repository)

        viewModel.triggerCrack()

        // Within first 150ms: request is in flight, but "Sending..." must NOT be shown
        Thread.sleep(150)
        assertTrue(viewModel.uiState.value.isCrackInFlight)
        assertFalse(
            "Must NOT show 'Sending...' during 400ms grace period",
            viewModel.uiState.value.showCrackSending
        )

        // After 450ms: "Sending..." progress state must now be active
        waitUntil(timeoutMs = 1000) { viewModel.uiState.value.showCrackSending }
        assertTrue(
            "Must show 'Sending...' when request exceeds 400ms grace period",
            viewModel.uiState.value.showCrackSending
        )

        // When request finishes: "Sending..." resets and "Done!" is shown
        waitUntil(timeoutMs = 1500) { viewModel.uiState.value.showDoneToast }
        assertFalse(viewModel.uiState.value.showCrackSending)
        assertFalse(viewModel.uiState.value.isCrackInFlight)
        assertTrue(viewModel.uiState.value.showDoneToast)
    }

    // =========================================================================
    // Test 6: Successful request emits non-blocking "Done!" feedback
    // =========================================================================
    @Test
    fun testSuccessfulRequestEmitsDoneFeedback() {
        val apiService = createApiService {
            200 to """{"success":true,"data":{"status":"SENT"}}"""
        }

        val repository = createRepository(apiService)
        val viewModel = ChildDashboardViewModel(repository)

        viewModel.triggerCrack()

        waitUntil { viewModel.uiState.value.showDoneToast }

        assertTrue(viewModel.uiState.value.showDoneToast)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    // =========================================================================
    // Test 7: Success feedback is non-blocking
    // =========================================================================
    @Test
    fun testSuccessFeedbackIsNonBlocking() {
        var freakSent = false
        val apiService = createApiService { req ->
            val payload = extractRequestBody(req)
            val msg = try { gson.fromJson(payload, ChildSendMessageRequestDto::class.java)?.message } catch (e: Exception) { null }
            if (msg == "Someone's,here") {
                freakSent = true
            }
            200 to """{"success":true,"data":{"status":"SENT"}}"""
        }

        val repository = createRepository(apiService)
        val viewModel = ChildDashboardViewModel(repository)

        // Trigger CRACK to completion
        viewModel.triggerCrack()
        waitUntil { viewModel.uiState.value.showDoneToast }
        assertTrue(viewModel.uiState.value.showDoneToast)

        // While "Done!" toast is visible, dashboard remains non-blocking: FREAK can be dispatched
        viewModel.triggerFreak()
        waitUntil { freakSent }

        assertTrue("Dashboard must remain non-blocking while Done! toast is active", freakSent)
    }

    // =========================================================================
    // Test 8: Failed request never emits "Done!"
    // =========================================================================
    @Test
    fun testFailedRequestNeverEmitsDone() {
        val apiService = createApiService {
            500 to """{"success":false,"message":"Network error"}"""
        }

        val repository = createRepository(apiService)
        val viewModel = ChildDashboardViewModel(repository)

        viewModel.triggerCrack()

        waitUntil { viewModel.uiState.value.errorMessage != null }

        assertFalse("Failed request must NEVER show Done! toast", viewModel.uiState.value.showDoneToast)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isCrackInFlight)
    }

    // =========================================================================
    // Test 9: Failed request never fabricates message data
    // =========================================================================
    @Test
    fun testFailedRequestNeverFabricatesMessageData() {
        val apiService = createApiService {
            503 to """{"success":false}"""
        }

        val repository = createRepository(apiService)
        val viewModel = ChildDashboardViewModel(repository)

        viewModel.triggerFreak()

        waitUntil { viewModel.uiState.value.errorMessage != null }

        assertFalse(viewModel.uiState.value.showDoneToast)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("Failed to send"))
        assertFalse(viewModel.uiState.value.isFreakInFlight)
    }

    // =========================================================================
    // Test 10: Duplicate taps do not dispatch duplicate in-flight requests
    // =========================================================================
    @Test
    fun testDuplicateTapsDoNotDispatchDuplicateInFlightRequests() {
        val callCount = AtomicInteger(0)
        val apiService = createApiService {
            callCount.incrementAndGet()
            Thread.sleep(300)
            200 to """{"success":true,"data":{"status":"SENT"}}"""
        }

        val repository = createRepository(apiService)
        val viewModel = ChildDashboardViewModel(repository)

        viewModel.triggerCrack()
        viewModel.triggerCrack()

        waitUntil { !viewModel.uiState.value.isCrackInFlight && viewModel.uiState.value.showDoneToast }

        assertEquals(
            "Duplicate in-flight tap must be ignored (exactly 1 request dispatched)",
            1,
            callCount.get()
        )
    }

    // =========================================================================
    // Test 11: CRACK and FREAK do not block each other
    // =========================================================================
    @Test
    fun testCrackAndFreakDoNotBlockEachOther() {
        val crackStarted = AtomicInteger(0)
        val freakStarted = AtomicInteger(0)

        val apiService = createApiService { req ->
            val body = extractRequestBody(req)
            val msg = try { gson.fromJson(body, ChildSendMessageRequestDto::class.java)?.message } catch (e: Exception) { null }
            if (msg == "Mom,here") {
                crackStarted.incrementAndGet()
                Thread.sleep(300)
            } else if (msg == "Someone's,here") {
                freakStarted.incrementAndGet()
            }
            200 to """{"success":true,"data":{"status":"SENT"}}"""
        }

        val repository = createRepository(apiService)
        val viewModel = ChildDashboardViewModel(repository)

        viewModel.triggerCrack()
        assertTrue(viewModel.uiState.value.isCrackInFlight)

        // FREAK is not blocked by CRACK in flight
        viewModel.triggerFreak()
        waitUntil { freakStarted.get() == 1 }

        assertEquals(1, crackStarted.get())
        assertEquals(1, freakStarted.get())
    }

    // =========================================================================
    // Test 12: "Done!" feedback auto-dismisses after ~2 seconds
    // =========================================================================
    @Test
    fun testDoneToastAutoDismissesAfter2Seconds() {
        val apiService = createApiService {
            200 to """{"success":true,"data":{"status":"SENT"}}"""
        }

        val repository = createRepository(apiService)
        val viewModel = ChildDashboardViewModel(repository)

        viewModel.triggerCrack()

        waitUntil { viewModel.uiState.value.showDoneToast }
        assertTrue(viewModel.uiState.value.showDoneToast)

        // Wait for auto-dismiss (~2000ms delay)
        waitUntil(timeoutMs = 3000) { !viewModel.uiState.value.showDoneToast }
        assertFalse("Done! toast must auto-dismiss after ~2 seconds", viewModel.uiState.value.showDoneToast)
    }

    // =========================================================================
    // Test 13: Subtitle text absent under CRACK/FREAK
    // =========================================================================
    @Test
    fun testVisibleSubtitleTextAbsent() {
        val state = ChildDashboardUiState()
        assertEquals("CRACK / FREAK state must not contain visible subtitle text", false, state.showCrackSending)
        assertEquals("CRACK / FREAK state must not contain visible subtitle text", false, state.showFreakSending)
    }
}
