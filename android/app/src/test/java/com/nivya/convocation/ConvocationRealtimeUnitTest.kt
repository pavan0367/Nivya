package com.nivya.convocation

import com.google.gson.Gson
import com.nivya.core.network.NivyaApiService
import com.nivya.core.network.NivyaRealtimeManager
import com.nivya.core.network.dto.*
import com.nivya.data.repository.ConvocationRepository
import com.nivya.ui.convocation.ChildConvocationViewModel
import com.nivya.ui.convocation.ParentConvocationViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * Phase 4 Comprehensive Unit Test Suite:
 * Android Convocation Realtime Wiring.
 *
 * Verifies:
 * 1. Realtime manager connection lifecycle & duplicate prevention
 * 2. Subscription routing & STOMP frame dispatching
 * 3. Incoming message mapping & DTO parsing
 * 4. Parent receives Child note in realtime (chronological order)
 * 5. Child receives Parent message while viewing is active
 * 6. Child does NOT display Parent message when viewing is OFF (default OFF + EMPTY)
 * 7. CRACK message ("Mom,here") realtime delivery to Parent
 * 8. FREAK message ("Someone's,here") realtime delivery to Parent
 * 9. Authoritative message ID deduplication
 * 10. REST + WebSocket duplicate reconciliation
 * 11. Reconnect subscription restoration without duplicate subscriptions
 * 12. Backfill history merge on reconnect
 * 13. Seen/read event handling (updates seen status without duplicating message)
 * 14. Failed Child send does NOT fake success (no feedback, input preserved, error set)
 * 15. Failed Parent send does NOT create fake message (no System.currentTimeMillis, input preserved, error set)
 * 16. No demo fallback data when REST history fails
 * 17. No demo fallback data when Child viewing fails
 * 18. Lifecycle cleanup / duplicate subscription prevention
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConvocationRealtimeUnitTest {

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

    private fun createRepository(
        apiService: NivyaApiService,
        realtimeManager: NivyaRealtimeManager? = null
    ): ConvocationRepository {
        return ConvocationRepository(
            apiService = apiService,
            realtimeManager = realtimeManager,
            ioDispatcher = Dispatchers.Unconfined,
            coroutineScope = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    private fun waitUntil(timeoutMs: Long = 3000, condition: () -> Boolean) {
        val start = System.currentTimeMillis()
        while (!condition() && (System.currentTimeMillis() - start) < timeoutMs) {
            Thread.sleep(20)
        }
    }

    // =========================================================================
    // Test 1: Realtime Manager Connection & Duplicate Connection Prevention
    // =========================================================================
    @Test
    fun testRealtimeManagerConnectionAndDuplicatePrevention() {
        val okHttpClient = OkHttpClient()
        val manager = NivyaRealtimeManager(
            context = null,
            okHttpClient = okHttpClient,
            tokenProvider = { "test_token" }
        )

        assertEquals(NivyaRealtimeManager.ConnectionState.DISCONNECTED, manager.connectionState.value)

        // Simulate STOMP CONNECTED frame
        val connectedFrame = "CONNECTED\nversion:1.2\nheart-beat:10000,10000\n\n\u0000"
        manager.handleIncomingStompFrame(connectedFrame)
        assertEquals(NivyaRealtimeManager.ConnectionState.CONNECTED, manager.connectionState.value)

        // Calling start() while CONNECTED must not create a second connection
        manager.start()
        assertEquals(NivyaRealtimeManager.ConnectionState.CONNECTED, manager.connectionState.value)

        manager.stop()
        assertEquals(NivyaRealtimeManager.ConnectionState.DISCONNECTED, manager.connectionState.value)
    }

    // =========================================================================
    // Test 2: Realtime Manager Subscribe & Routing
    // =========================================================================
    @Test
    fun testRealtimeManagerSubscribeAndRouting() {
        val okHttpClient = OkHttpClient()
        val manager = NivyaRealtimeManager(
            context = null,
            okHttpClient = okHttpClient,
            tokenProvider = { "test_token" }
        )

        val receivedMessages = mutableListOf<String>()
        val unsub = manager.subscribe("/topic/convocation/messages") { body ->
            receivedMessages.add(body)
        }

        // Simulate incoming MESSAGE frame
        val frame = "MESSAGE\ndestination:/topic/convocation/messages\nmessage-id:msg-1\n\n{\"id\":1,\"message\":\"Hello\"}\u0000"
        manager.handleIncomingStompFrame(frame)

        assertEquals(1, receivedMessages.size)
        assertTrue(receivedMessages[0].contains("\"Hello\""))

        // Unsubscribe
        unsub()
        manager.handleIncomingStompFrame(frame)
        assertEquals(1, receivedMessages.size)
    }

    // =========================================================================
    // Test 3: Incoming Message Mapping DTO
    // =========================================================================
    @Test
    fun testIncomingMessageMappingDto() {
        val json = """
            {
                "id": 42,
                "senderUserId": 1,
                "senderName": "Alex",
                "receiverUserId": 2,
                "message": "Science project completed!",
                "childOriginated": true,
                "createdAt": "2026-09-20T10:00:00Z",
                "seen": true,
                "seenAt": "2026-09-20T10:05:00Z",
                "pinned": true,
                "reaction": "❤️",
                "status": "DELIVERED"
            }
        """.trimIndent()

        val dto = gson.fromJson(json, ParentConvocationMessageDto::class.java)

        assertEquals(42L, dto.id)
        assertEquals(1L, dto.senderUserId)
        assertEquals("Alex", dto.senderName)
        assertEquals(2L, dto.receiverUserId)
        assertEquals("Science project completed!", dto.message)
        assertTrue(dto.childOriginated)
        assertEquals("2026-09-20T10:00:00Z", dto.createdAt)
        assertTrue(dto.seen)
        assertEquals("2026-09-20T10:05:00Z", dto.seenAt)
        assertTrue(dto.pinned)
        assertEquals("❤️", dto.reaction)
        assertEquals("DELIVERED", dto.status)
    }

    // =========================================================================
    // Test 4: Parent Receives Child Note in Realtime
    // =========================================================================
    @Test
    fun testParentReceivesChildMessageInRealtime() = runBlocking {
        val historyJson = """{"success":true,"data":[]}"""
        val apiService = createApiService { req ->
            if (req.url.encodedPath.endsWith("/parent/history")) 200 to historyJson
            else 500 to """{"success":false}"""
        }

        val repository = createRepository(apiService)
        val viewModel = ParentConvocationViewModel(repository)
        waitUntil { !viewModel.uiState.value.isLoading }

        assertTrue(viewModel.uiState.value.messages.isEmpty())

        // Child sends note -> backend broadcasts on /topic/convocation/messages
        val childMsg = ParentConvocationMessageDto(
            id = 101L,
            senderUserId = 2L,
            senderName = "Alex",
            receiverUserId = 1L,
            message = "Leaving school now!",
            childOriginated = true,
            createdAt = "2026-09-20T14:30:00Z"
        )
        repository.emitIncomingMessageForTest(childMsg)

        assertEquals(1, viewModel.uiState.value.messages.size)
        val received = viewModel.uiState.value.messages[0]
        assertEquals(101L, received.id)
        assertEquals("Leaving school now!", received.message)
        assertTrue(received.childOriginated)
        assertEquals("Alex", received.senderName)
    }

    // =========================================================================
    // Test 5: Child Receives Parent Message While Viewing Is Active
    // =========================================================================
    @Test
    fun testChildReceivesParentMessageWhileViewingActive() = runBlocking {
        val sessionJson = """
            {
                "success": true,
                "data": {
                    "sessionUuid": "sess-1",
                    "viewStartedAt": "2026-09-20T10:00:00Z",
                    "visibilityExpiresAt": "2026-09-20T10:02:00Z",
                    "remainingSeconds": 120,
                    "messages": [{"id": 1, "message": "Initial guidance", "createdAt": "2026-09-20T10:00:00Z"}]
                }
            }
        """.trimIndent()

        val apiService = createApiService { req ->
            if (req.url.encodedPath.endsWith("/child/view/start")) 200 to sessionJson
            else 500 to """{"success":false}"""
        }

        val repository = createRepository(apiService)
        val viewModel = ChildConvocationViewModel(repository)

        // Turn ON viewing session
        viewModel.toggleConvocationMode(true)
        waitUntil { viewModel.uiState.value.isViewingActive }

        assertTrue(viewModel.uiState.value.isViewingActive)
        assertEquals(1, viewModel.uiState.value.activeMessages.size)

        // Incoming Parent message arrives in realtime
        val parentMsg = ParentConvocationMessageDto(
            id = 2L,
            senderUserId = 1L,
            senderName = "Parent",
            receiverUserId = 2L,
            message = "New priority guidance message",
            childOriginated = false,
            createdAt = "2026-09-20T10:01:00Z"
        )
        repository.emitIncomingMessageForTest(parentMsg)

        val visible = viewModel.uiState.value.activeMessages
        assertEquals(1, visible.size)
        assertEquals(2L, visible[0].id)
        assertEquals("New priority guidance message", visible[0].message)
    }

    // =========================================================================
    // Test 6: Child Does NOT Display Parent Message When Viewing Is OFF
    // =========================================================================
    @Test
    fun testChildDoesNotDisplayParentMessageWhenViewingOff() = runBlocking {
        val apiService = createApiService { 500 to """{"success":false}""" }
        val repository = createRepository(apiService)
        val viewModel = ChildConvocationViewModel(repository)

        // Default state: viewing is OFF
        assertFalse(viewModel.uiState.value.isViewingActive)
        assertFalse(viewModel.uiState.value.isConvocationEnabled)
        assertTrue(viewModel.uiState.value.activeMessages.isEmpty())

        // Incoming Parent message arrives in realtime
        val parentMsg = ParentConvocationMessageDto(
            id = 3L,
            senderUserId = 1L,
            senderName = "Parent",
            receiverUserId = 2L,
            message = "Secret note",
            childOriginated = false,
            createdAt = "2026-09-20T10:00:00Z"
        )
        repository.emitIncomingMessageForTest(parentMsg)

        // Must strictly remain empty when viewing state is OFF
        assertTrue("Child view must remain empty when viewing is OFF", viewModel.uiState.value.activeMessages.isEmpty())
    }

    // =========================================================================
    // Test 7: CRACK Message Realtime Delivery to Parent
    // =========================================================================
    @Test
    fun testCrackMessageRealtimeDeliveryToParent() = runBlocking {
        val historyJson = """{"success":true,"data":[]}"""
        val apiService = createApiService { req ->
            if (req.url.encodedPath.endsWith("/parent/history")) 200 to historyJson
            else 500 to """{"success":false}"""
        }

        val repository = createRepository(apiService)
        val viewModel = ParentConvocationViewModel(repository)
        waitUntil { !viewModel.uiState.value.isLoading }

        // CRACK sends exact message "Mom,here"
        val crackMsg = ParentConvocationMessageDto(
            id = 201L,
            senderUserId = 2L,
            senderName = "Alex",
            receiverUserId = 1L,
            message = "Mom,here",
            childOriginated = true,
            createdAt = "2026-09-20T15:00:00Z",
            status = "CHILD_SENT"
        )
        repository.emitIncomingMessageForTest(crackMsg)

        assertEquals(1, viewModel.uiState.value.messages.size)
        val msg = viewModel.uiState.value.messages[0]
        assertEquals("Mom,here", msg.message)
        assertTrue(msg.childOriginated)
        assertEquals(201L, msg.id)
    }

    // =========================================================================
    // Test 8: FREAK Message Realtime Delivery to Parent
    // =========================================================================
    @Test
    fun testFreakMessageRealtimeDeliveryToParent() = runBlocking {
        val historyJson = """{"success":true,"data":[]}"""
        val apiService = createApiService { req ->
            if (req.url.encodedPath.endsWith("/parent/history")) 200 to historyJson
            else 500 to """{"success":false}"""
        }

        val repository = createRepository(apiService)
        val viewModel = ParentConvocationViewModel(repository)
        waitUntil { !viewModel.uiState.value.isLoading }

        // FREAK sends exact message "Someone's,here"
        val freakMsg = ParentConvocationMessageDto(
            id = 202L,
            senderUserId = 2L,
            senderName = "Alex",
            receiverUserId = 1L,
            message = "Someone's,here",
            childOriginated = true,
            createdAt = "2026-09-20T15:01:00Z",
            status = "CHILD_SENT"
        )
        repository.emitIncomingMessageForTest(freakMsg)

        assertEquals(1, viewModel.uiState.value.messages.size)
        val msg = viewModel.uiState.value.messages[0]
        assertEquals("Someone's,here", msg.message)
        assertTrue(msg.childOriginated)
        assertEquals(202L, msg.id)
    }

    // =========================================================================
    // Test 9: Authoritative Message ID Deduplication
    // =========================================================================
    @Test
    fun testAuthoritativeMessageIdDeduplication() = runBlocking {
        val historyJson = """{"success":true,"data":[]}"""
        val apiService = createApiService { req ->
            if (req.url.encodedPath.endsWith("/parent/history")) 200 to historyJson
            else 500 to """{"success":false}"""
        }

        val repository = createRepository(apiService)
        val viewModel = ParentConvocationViewModel(repository)
        waitUntil { !viewModel.uiState.value.isLoading }

        val msg = ParentConvocationMessageDto(
            id = 50L,
            senderUserId = 1L,
            senderName = "Parent",
            receiverUserId = 2L,
            message = "Wrap up homework",
            childOriginated = false,
            createdAt = "2026-09-20T16:00:00Z"
        )

        // Emit duplicate realtime events for the same message ID
        repository.emitIncomingMessageForTest(msg)
        repository.emitIncomingMessageForTest(msg)
        repository.emitIncomingMessageForTest(msg)

        assertEquals("Message must not be duplicated by repeated incoming events", 1, viewModel.uiState.value.messages.size)
    }

    // =========================================================================
    // Test 10: REST + WebSocket Duplicate Reconciliation
    // =========================================================================
    @Test
    fun testRestAndWebSocketDuplicateReconciliation() = runBlocking {
        val sendResponseJson = """
            {
                "success": true,
                "data": {
                    "id": 99,
                    "senderUserId": 1,
                    "senderName": "Parent",
                    "receiverUserId": 2,
                    "message": "Dinner ready",
                    "childOriginated": false,
                    "createdAt": "2026-09-20T18:00:00Z",
                    "seen": false
                }
            }
        """.trimIndent()

        val apiService = createApiService { req ->
            when {
                req.url.encodedPath.endsWith("/parent/history") -> 200 to """{"success":true,"data":[]}"""
                req.url.encodedPath.endsWith("/parent/send") && req.method == "POST" -> 200 to sendResponseJson
                else -> 500 to """{"success":false}"""
            }
        }

        val repository = createRepository(apiService)
        val viewModel = ParentConvocationViewModel(repository)
        waitUntil { !viewModel.uiState.value.isLoading }

        // 1. Parent sends message via REST
        viewModel.updateInput("Dinner ready")
        viewModel.sendMessage()
        waitUntil { !viewModel.uiState.value.isSending }

        assertEquals(1, viewModel.uiState.value.messages.size)
        assertEquals(99L, viewModel.uiState.value.messages[0].id)

        // 2. The same message also arrives via WebSocket broadcast
        val wsMsg = ParentConvocationMessageDto(
            id = 99L,
            senderUserId = 1L,
            senderName = "Parent",
            receiverUserId = 2L,
            message = "Dinner ready",
            childOriginated = false,
            createdAt = "2026-09-20T18:00:00Z",
            seen = false
        )
        repository.emitIncomingMessageForTest(wsMsg)

        assertEquals("REST send + WS delivery must reconcile without duplicate", 1, viewModel.uiState.value.messages.size)
    }

    // =========================================================================
    // Test 11: Reconnect Subscription Restoration Without Duplicates
    // =========================================================================
    @Test
    fun testReconnectSubscriptionRestorationWithoutDuplicates() {
        val okHttpClient = OkHttpClient()
        val manager = NivyaRealtimeManager(
            context = null,
            okHttpClient = okHttpClient,
            tokenProvider = { "token_123" }
        )

        var callbackCount = 0
        manager.subscribe("/topic/convocation/messages") {
            callbackCount++
        }

        // Initial connect
        manager.handleIncomingStompFrame("CONNECTED\nversion:1.2\n\n\u0000")
        assertEquals(NivyaRealtimeManager.ConnectionState.CONNECTED, manager.connectionState.value)

        // Simulate network drop / disconnect
        manager.setConnectionStateForTest(NivyaRealtimeManager.ConnectionState.RECONNECTING)

        // Simulate reconnect
        manager.handleIncomingStompFrame("CONNECTED\nversion:1.2\n\n\u0000")
        assertEquals(NivyaRealtimeManager.ConnectionState.CONNECTED, manager.connectionState.value)

        // Dispatch a message after reconnect
        val frame = "MESSAGE\ndestination:/topic/convocation/messages\n\n{\"id\":1}\u0000"
        manager.handleIncomingStompFrame(frame)

        assertEquals(1, callbackCount)
    }

    // =========================================================================
    // Test 12: Backfill History Merge on Reconnect
    // =========================================================================
    @Test
    fun testBackfillHistoryMergeOnReconnect() = runBlocking {
        val callCount = AtomicInteger(0)
        val apiService = createApiService { req ->
            if (req.url.encodedPath.endsWith("/parent/history")) {
                val count = callCount.incrementAndGet()
                if (count == 1) {
                    200 to """{"success":true,"data":[{"id":1,"message":"First","createdAt":"2026-09-20T10:00:00Z"}]}"""
                } else {
                    200 to """{"success":true,"data":[{"id":1,"message":"First","createdAt":"2026-09-20T10:00:00Z"},{"id":2,"message":"Missed Second","createdAt":"2026-09-20T10:05:00Z"}]}"""
                }
            } else {
                500 to """{"success":false}"""
            }
        }

        val repository = createRepository(apiService)
        val viewModel = ParentConvocationViewModel(repository)
        waitUntil { viewModel.uiState.value.messages.isNotEmpty() }

        assertEquals(1, viewModel.uiState.value.messages.size)

        // Trigger reconnect event
        repository.emitReconnectForTest()
        waitUntil { viewModel.uiState.value.messages.size == 2 }

        assertEquals(2, viewModel.uiState.value.messages.size)
        assertEquals(1L, viewModel.uiState.value.messages[0].id)
        assertEquals(2L, viewModel.uiState.value.messages[1].id)
    }

    // =========================================================================
    // Test 13: Seen/Read Event Handling Updates Message State
    // =========================================================================
    @Test
    fun testSeenEventHandlingUpdatesMessageState() = runBlocking {
        val historyJson = """
            {
                "success": true,
                "data": [
                    {
                        "id": 10,
                        "senderUserId": 1,
                        "senderName": "Parent",
                        "receiverUserId": 2,
                        "message": "Check battery",
                        "childOriginated": false,
                        "createdAt": "2026-09-20T12:00:00Z",
                        "seen": false,
                        "seenAt": null
                    }
                ]
            }
        """.trimIndent()

        val apiService = createApiService { req ->
            if (req.url.encodedPath.endsWith("/parent/history")) 200 to historyJson
            else 500 to """{"success":false}"""
        }

        val repository = createRepository(apiService)
        val viewModel = ParentConvocationViewModel(repository)
        waitUntil { viewModel.uiState.value.messages.isNotEmpty() }

        val initialMsg = viewModel.uiState.value.messages[0]
        assertFalse(initialMsg.seen)
        assertNull(initialMsg.seenAt)

        // Seen event arrives via /topic/convocation/seen
        val seenEvent = ConvocationSeenEventDto(
            messageId = 10L,
            seenAt = "2026-09-20T12:02:00Z"
        )
        repository.emitSeenEventForTest(seenEvent)

        val updatedMsg = viewModel.uiState.value.messages[0]
        assertTrue("Message must be marked seen", updatedMsg.seen)
        assertEquals("2026-09-20T12:02:00Z", updatedMsg.seenAt)
        assertEquals("Check battery", updatedMsg.message)
        assertEquals(1, viewModel.uiState.value.messages.size)
    }

    // =========================================================================
    // Test 14: Failed Child Send Does NOT Fake Success
    // =========================================================================
    @Test
    fun testFailedChildSendDoesNotFakeSuccess() = runBlocking {
        val apiService = createApiService { 500 to """{"success":false,"message":"Error"}""" }

        val repository = createRepository(apiService)
        val viewModel = ChildConvocationViewModel(repository)

        viewModel.updateNoteText("Important note to parent")
        viewModel.sendChildNote()
        waitUntil { !viewModel.uiState.value.isSendingNote }

        assertFalse("noteSentFeedback must NOT be true on send failure", viewModel.uiState.value.noteSentFeedback)
        assertEquals("Note text must NOT be cleared when send fails", "Important note to parent", viewModel.uiState.value.noteText)
        assertNotNull("Error message must be populated on failure", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSendingNote)
    }

    // =========================================================================
    // Test 15: Failed Parent Send Does NOT Create Fake Message
    // =========================================================================
    @Test
    fun testFailedParentSendDoesNotCreateFakeMessage() = runBlocking {
        val historyJson = """{"success":true,"data":[]}"""
        val apiService = createApiService { req ->
            when {
                req.url.encodedPath.endsWith("/parent/history") -> 200 to historyJson
                req.url.encodedPath.endsWith("/parent/send") -> 503 to """{"success":false,"message":"Service Unavailable"}"""
                else -> 500 to """{"success":false}"""
            }
        }

        val repository = createRepository(apiService)
        val viewModel = ParentConvocationViewModel(repository)
        waitUntil { !viewModel.uiState.value.isLoading }

        viewModel.updateInput("Wrap up screen time")
        viewModel.sendMessage()
        waitUntil { !viewModel.uiState.value.isSending }

        assertTrue("Messages list must remain empty on send failure; no fake message fabricated", viewModel.uiState.value.messages.isEmpty())
        assertEquals("Input text must be retained on send failure", "Wrap up screen time", viewModel.uiState.value.inputMessage)
        assertNotNull("Error message must be set on failure", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSending)
    }

    // =========================================================================
    // Test 16: No Demo Fallback Data When History Fails
    // =========================================================================
    @Test
    fun testNoDemoFallbackDataWhenHistoryFails() = runBlocking {
        val apiService = createApiService { 500 to """{"success":false,"message":"Server error"}""" }

        val repository = createRepository(apiService)
        val viewModel = ParentConvocationViewModel(repository)
        waitUntil { !viewModel.uiState.value.isLoading }

        assertTrue("Messages list must be empty, not populated with demo history", viewModel.uiState.value.messages.isEmpty())
        assertNotNull("Error message must be set", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // =========================================================================
    // Test 17: No Demo Fallback Data When Child Viewing Fails
    // =========================================================================
    @Test
    fun testNoDemoFallbackDataWhenChildViewingFails() = runBlocking {
        val apiService = createApiService { 500 to """{"success":false,"message":"Server error"}""" }

        val repository = createRepository(apiService)
        val viewModel = ChildConvocationViewModel(repository)

        viewModel.toggleConvocationMode(true)
        waitUntil { !viewModel.uiState.value.isLoading }

        assertFalse("Viewing must NOT be active on session failure", viewModel.uiState.value.isViewingActive)
        assertTrue("Active messages must be empty, no demo message", viewModel.uiState.value.activeMessages.isEmpty())
        assertEquals(0L, viewModel.uiState.value.remainingSeconds)
        assertNotNull("Error message must be populated", viewModel.uiState.value.errorMessage)
    }

    // =========================================================================
    // Test 18: Lifecycle Cleanup & Duplicate Subscription Prevention
    // =========================================================================
    @Test
    fun testLifecycleCleanupAndDuplicateSubscriptionPrevention() {
        val okHttpClient = OkHttpClient()
        val manager = NivyaRealtimeManager(
            context = null,
            okHttpClient = okHttpClient,
            tokenProvider = { "token" }
        )

        val apiService = createApiService { 500 to """{"success":false}""" }
        val repository = createRepository(apiService, manager)

        repository.startRealtime()
        val subCount1 = manager.getSubscriptionsForTest().size

        repository.startRealtime()
        val subCount2 = manager.getSubscriptionsForTest().size
        assertEquals(subCount1, subCount2)

        repository.stopRealtime()
        val remainingSubscriptions = manager.getSubscriptionsForTest().values.any { it.isNotEmpty() }
        assertFalse("Active callbacks must be unsubscribed on stopRealtime", remainingSubscriptions)
    }
}
