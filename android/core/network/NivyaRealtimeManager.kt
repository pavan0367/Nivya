package com.nivya.core.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Real-time WebSocket / STOMP manager for Nivya Android.
 * Handles authenticated connections, automatic exponential backoff,
 * safe reconnect on network loss via NetworkMonitor, and destination subscription routing.
 */
class NivyaRealtimeManager(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val tokenProvider: () -> String?,
    private val wsUrlProvider: () -> String = { "ws://10.0.2.2:8080/ws/websocket" }
) {

    companion object {
        private const val TAG = "NivyaRealtime"
        private const val BASE_BACKOFF_MS = 1000L
        private const val MAX_BACKOFF_MS = 30000L
    }

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        RECONNECTING
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val networkMonitor = NetworkMonitor(context)

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var webSocket: WebSocket? = null
    private var isManuallyStopped = false
    private var reconnectAttempt = 0
    private var reconnectJob: Job? = null

    private val subscriptions = ConcurrentHashMap<String, MutableSet<(String) -> Unit>>()
    private val subIdCounter = AtomicInteger(1)
    private val activeSubIds = ConcurrentHashMap<String, String>() // destination -> subId

    init {
        // Observe network changes: if network returns while disconnected/reconnecting, reconnect immediately
        scope.launch {
            networkMonitor.isOnline.collect { online ->
                if (online && !isManuallyStopped && _connectionState.value != ConnectionState.CONNECTED) {
                    Log.i(TAG, "Network restored. Triggering immediate safe reconnection...")
                    reconnectJob?.cancel()
                    reconnectAttempt = 0
                    connectInternal()
                } else if (!online && _connectionState.value == ConnectionState.CONNECTED) {
                    Log.w(TAG, "Network lost. Marking as RECONNECTING...")
                    _connectionState.value = ConnectionState.RECONNECTING
                    webSocket?.cancel()
                    webSocket = null
                }
            }
        }
    }

    @Synchronized
    fun start() {
        isManuallyStopped = false
        reconnectAttempt = 0
        connectInternal()
    }

    @Synchronized
    fun stop() {
        isManuallyStopped = true
        reconnectJob?.cancel()
        reconnectJob = null
        reconnectAttempt = 0
        try {
            webSocket?.close(1000, "Normal closure")
        } catch (_: Exception) {}
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    private fun connectInternal() {
        if (isManuallyStopped) return

        _connectionState.value = if (reconnectAttempt > 0) ConnectionState.RECONNECTING else ConnectionState.CONNECTING

        val wsUrl = wsUrlProvider()
        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Transport WebSocket connected. Sending STOMP CONNECT frame...")
                sendStompConnect(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingStompFrame(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code / $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code / $reason")
                handleDisconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "WebSocket transport failure: ${t.message}")
                handleDisconnect()
            }
        })
    }

    private fun sendStompConnect(ws: WebSocket) {
        val token = tokenProvider()
        val connectFrame = buildString {
            append("CONNECT\n")
            append("accept-version:1.2,1.1,1.0\n")
            append("heart-beat:10000,10000\n")
            if (!token.isNullOrBlank()) {
                append("Authorization:Bearer $token\n")
            }
            append("\n\u0000")
        }
        ws.send(connectFrame)
    }

    private fun handleIncomingStompFrame(rawFrame: String) {
        val lines = rawFrame.trimEnd('\u0000').lines()
        if (lines.isEmpty()) return

        val command = lines.first().trim()
        when (command) {
            "CONNECTED" -> {
                Log.i(TAG, "STOMP protocol handshake connected.")
                _connectionState.value = ConnectionState.CONNECTED
                reconnectAttempt = 0
                // Resubscribe active subscriptions
                resubscribeAll()
            }
            "MESSAGE" -> {
                var destination = ""
                var bodyStartIndex = -1
                for (i in 1 until lines.size) {
                    val line = lines[i]
                    if (line.startsWith("destination:")) {
                        destination = line.substringAfter("destination:").trim()
                    } else if (line.isEmpty() && bodyStartIndex == -1) {
                        bodyStartIndex = i + 1
                        break
                    }
                }
                val body = if (bodyStartIndex != -1 && bodyStartIndex < lines.size) {
                    lines.subList(bodyStartIndex, lines.size).joinToString("\n")
                } else {
                    ""
                }
                if (destination.isNotBlank()) {
                    dispatchMessage(destination, body)
                }
            }
            "ERROR" -> {
                Log.e(TAG, "STOMP protocol error received: $rawFrame")
                handleDisconnect()
            }
        }
    }

    private fun dispatchMessage(destination: String, body: String) {
        subscriptions[destination]?.forEach { callback ->
            try {
                callback(body)
            } catch (e: Exception) {
                Log.e(TAG, "Error invoking subscription callback for $destination", e)
            }
        }
    }

    private fun handleDisconnect() {
        webSocket = null
        if (isManuallyStopped) {
            _connectionState.value = ConnectionState.DISCONNECTED
            return
        }

        _connectionState.value = ConnectionState.RECONNECTING
        scheduleReconnect()
    }

    private fun scheduleReconnect() {
        if (isManuallyStopped) return

        reconnectJob?.cancel()
        val delayMs = calculateBackoffDelay(reconnectAttempt)
        reconnectAttempt++

        Log.i(TAG, "Scheduling STOMP reconnect in ${delayMs}ms (attempt $reconnectAttempt)...")
        reconnectJob = scope.launch {
            delay(delayMs)
            if (!isManuallyStopped && networkMonitor.isOnline.value) {
                connectInternal()
            }
        }
    }

    internal fun calculateBackoffDelay(attempt: Int): Long {
        val factor = 1.5.pow(min(attempt, 8).toDouble())
        val computed = (BASE_BACKOFF_MS * factor).toLong()
        val jitter = Random.nextLong(0, 500)
        return min(MAX_BACKOFF_MS, computed + jitter)
    }

    /**
     * Subscribes to a real-time destination (e.g. /topic/battery/{deviceId}).
     * Returns an unsubscription callback.
     */
    fun subscribe(destination: String, callback: (String) -> Unit): () -> Unit {
        val callbacks = subscriptions.computeIfAbsent(destination) { ConcurrentHashMap.newKeySet() }
        callbacks.add(callback)

        if (_connectionState.value == ConnectionState.CONNECTED && !activeSubIds.containsKey(destination)) {
            sendSubscribeFrame(destination)
        }

        return {
            callbacks.remove(callback)
            if (callbacks.isEmpty()) {
                subscriptions.remove(destination)
                sendUnsubscribeFrame(destination)
            }
        }
    }

    private fun resubscribeAll() {
        activeSubIds.clear()
        for (destination in subscriptions.keys) {
            sendSubscribeFrame(destination)
        }
    }

    private fun sendSubscribeFrame(destination: String) {
        val ws = webSocket ?: return
        val subId = "sub-" + subIdCounter.getAndIncrement()
        activeSubIds[destination] = subId

        val frame = buildString {
            append("SUBSCRIBE\n")
            append("id:$subId\n")
            append("destination:$destination\n")
            append("ack:auto\n\n\u0000")
        }
        ws.send(frame)
    }

    private fun sendUnsubscribeFrame(destination: String) {
        val ws = webSocket ?: return
        val subId = activeSubIds.remove(destination) ?: return
        val frame = "UNSUBSCRIBE\nid:$subId\n\n\u0000"
        ws.send(frame)
    }
}
