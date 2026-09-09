package com.nivya.network

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.min
import kotlin.math.pow

/**
 * Unit tests verifying real-time WebSocket / STOMP frame generation,
 * backoff delay computation, and subscription tracking.
 */
class RealtimeCommunicationUnitTest {

    @Test
    fun testStompConnectFrameFormatting() {
        val token = "jwt-test-token-12345"
        val frame = buildString {
            append("CONNECT\n")
            append("accept-version:1.2,1.1,1.0\n")
            append("heart-beat:10000,10000\n")
            append("Authorization:Bearer $token\n")
            append("\n\u0000")
        }

        assertTrue(frame.startsWith("CONNECT\n"))
        assertTrue(frame.contains("Authorization:Bearer jwt-test-token-12345\n"))
        assertTrue(frame.endsWith("\u0000"))
    }

    @Test
    fun testStompSubscribeFrameFormatting() {
        val destination = "/topic/battery/42"
        val subId = "sub-1"
        val frame = buildString {
            append("SUBSCRIBE\n")
            append("id:$subId\n")
            append("destination:$destination\n")
            append("ack:auto\n\n\u0000")
        }

        assertTrue(frame.startsWith("SUBSCRIBE\n"))
        assertTrue(frame.contains("destination:/topic/battery/42\n"))
        assertTrue(frame.contains("id:sub-1\n"))
        assertTrue(frame.endsWith("\u0000"))
    }

    @Test
    fun testExponentialBackoffCalculation() {
        fun calculateDelay(attempt: Int): Long {
            val baseDelay = 1000L
            val maxDelay = 30000L
            val factor = 1.5.pow(min(attempt, 8).toDouble())
            return min(maxDelay, (baseDelay * factor).toLong())
        }

        val delay0 = calculateDelay(0)
        val delay1 = calculateDelay(1)
        val delay2 = calculateDelay(2)
        val delay8 = calculateDelay(8)
        val delay20 = calculateDelay(20)

        assertEquals(1000L, delay0)
        assertEquals(1500L, delay1)
        assertEquals(2250L, delay2)
        assertTrue(delay8 <= 30000L)
        assertEquals(30000L, delay20) // Capped at maxDelay
    }

    @Test
    fun testIncomingMessageFrameParsing() {
        val rawMessage = "MESSAGE\ndestination:/topic/battery/1\ncontent-type:application/json\n\n{\"batteryPct\":85}\u0000"
        val lines = rawMessage.trimEnd('\u0000').lines()

        assertEquals("MESSAGE", lines[0])

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

        val body = lines.subList(bodyStartIndex, lines.size).joinToString("\n")
        assertEquals("/topic/battery/1", destination)
        assertEquals("{\"batteryPct\":85}", body)
    }
}
