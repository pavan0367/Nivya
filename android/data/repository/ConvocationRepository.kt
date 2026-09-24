package com.nivya.data.repository

import android.util.Log
import com.google.gson.Gson
import com.nivya.core.network.NivyaApiService
import com.nivya.core.network.NivyaRealtimeManager
import com.nivya.core.network.dto.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Repository coordinating Parent and Child Convocation messaging.
 * Completely isolated from other telemetry and hardware repositories.
 * Incorporates real-time WebSocket STOMP messaging, read/seen status updates,
 * action events, and reconnect reconciliation.
 */
class ConvocationRepository(
    private val apiService: NivyaApiService,
    private val realtimeManager: NivyaRealtimeManager? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val coroutineScope: CoroutineScope = CoroutineScope(ioDispatcher + SupervisorJob())
) {
    private val gson = Gson()

    private val _incomingMessages = MutableSharedFlow<ParentConvocationMessageDto>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val incomingMessages: SharedFlow<ParentConvocationMessageDto> = _incomingMessages.asSharedFlow()

    private val _seenEvents = MutableSharedFlow<ConvocationSeenEventDto>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val seenEvents: SharedFlow<ConvocationSeenEventDto> = _seenEvents.asSharedFlow()

    private val _convocationActions = MutableSharedFlow<ConvocationActionEventDto>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val convocationActions: SharedFlow<ConvocationActionEventDto> = _convocationActions.asSharedFlow()

    private val _reconnectEvents = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val reconnectEvents: SharedFlow<Unit> = _reconnectEvents.asSharedFlow()

    val connectionState: StateFlow<NivyaRealtimeManager.ConnectionState>? = realtimeManager?.connectionState

    private var isSubscribed = false
    private val unsubs = mutableListOf<() -> Unit>()
    private var wasConnected = false

    @Synchronized
    fun startRealtime() {
        val manager = realtimeManager ?: return
        manager.start()

        if (!isSubscribed) {
            isSubscribed = true
            unsubs += manager.subscribe("/topic/convocation/messages") { body ->
                try {
                    val msg = gson.fromJson(body, ParentConvocationMessageDto::class.java)
                    if (msg != null) {
                        _incomingMessages.tryEmit(msg)
                    }
                } catch (e: Exception) {
                    Log.e("ConvocationRepo", "Error parsing incoming message", e)
                }
            }

            unsubs += manager.subscribe("/topic/convocation/seen") { body ->
                try {
                    val seen = gson.fromJson(body, ConvocationSeenEventDto::class.java)
                    if (seen != null) {
                        _seenEvents.tryEmit(seen)
                    }
                } catch (e: Exception) {
                    Log.e("ConvocationRepo", "Error parsing seen event", e)
                }
            }

            unsubs += manager.subscribe("/topic/convocation/actions") { body ->
                try {
                    val action = gson.fromJson(body, ConvocationActionEventDto::class.java)
                    if (action != null) {
                        _convocationActions.tryEmit(action)
                    }
                } catch (e: Exception) {
                    Log.e("ConvocationRepo", "Error parsing action event", e)
                }
            }

            coroutineScope.launch {
                manager.connectionState.collect { state ->
                    if (state == NivyaRealtimeManager.ConnectionState.CONNECTED) {
                        if (wasConnected) {
                            _reconnectEvents.emit(Unit)
                        }
                        wasConnected = true
                    }
                }
            }
        }
    }

    @Synchronized
    fun stopRealtime() {
        unsubs.forEach { it() }
        unsubs.clear()
        isSubscribed = false
        wasConnected = false
    }

    internal fun emitIncomingMessageForTest(msg: ParentConvocationMessageDto) {
        _incomingMessages.tryEmit(msg)
    }

    internal fun emitSeenEventForTest(event: ConvocationSeenEventDto) {
        _seenEvents.tryEmit(event)
    }

    internal fun emitActionEventForTest(action: ConvocationActionEventDto) {
        _convocationActions.tryEmit(action)
    }

    internal fun emitReconnectForTest() {
        _reconnectEvents.tryEmit(Unit)
    }

    // =========================================================================
    // PARENT OPERATIONS
    // =========================================================================

    suspend fun parentSendMessage(
        message: String,
        receiverUserId: Long? = null
    ): Result<ParentConvocationMessageDto> = withContext(ioDispatcher) {
        try {
            val response = apiService.parentSendConvocationMessage(
                ParentSendMessageRequestDto(receiverUserId = receiverUserId, message = message)
            )
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Failed to send convocation message: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun parentGetHistory(): Result<List<ParentConvocationMessageDto>> = withContext(ioDispatcher) {
        try {
            val response = apiService.parentGetConvocationHistory()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Failed to fetch convocation history: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun parentGetSeenState(): Result<Map<String, Boolean>> = withContext(ioDispatcher) {
        try {
            val response = apiService.parentGetConvocationSeenState()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Failed to fetch seen state: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun parentUnsendMessage(messageId: Long): Result<Unit> = withContext(ioDispatcher) {
        try {
            val response = apiService.parentUnsendConvocationMessage(messageId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to unsend message: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun parentTogglePinMessage(messageId: Long): Result<ParentConvocationMessageDto> = withContext(ioDispatcher) {
        try {
            val response = apiService.parentTogglePinMessage(messageId)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Failed to toggle pin: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================================================================
    // CHILD OPERATIONS
    // =========================================================================

    suspend fun childGetUnread(): Result<List<ChildConvocationMessageDto>> = withContext(ioDispatcher) {
        try {
            val response = apiService.childGetConvocationUnread()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Failed to fetch unread messages: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun childStartViewing(): Result<ChildViewingSessionResponseDto> = withContext(ioDispatcher) {
        try {
            val response = apiService.childStartConvocationViewing()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Failed to activate viewing session: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun childSendMessage(message: String): Result<Boolean> = withContext(ioDispatcher) {
        try {
            val response = apiService.childSendConvocationMessage(
                ChildSendMessageRequestDto(message = message)
            )
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to send note: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun childGetVisibility(): Result<ChildVisibilityStateResponseDto> = withContext(ioDispatcher) {
        try {
            val response = apiService.childGetConvocationVisibility()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Failed to fetch visibility state: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
