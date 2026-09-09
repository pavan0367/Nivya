package com.nivya.convocation

import com.nivya.core.network.dto.*
import com.nivya.services.notification.ConvocationNotificationManager
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class ConvocationUnitTest {

    @Test
    fun testParentConvocationMessageDtoProperties() {
        val now = Instant.now().toString()
        val dto = ParentConvocationMessageDto(
            id = 10L,
            senderUserId = 1L,
            senderName = "Parent",
            receiverUserId = 2L,
            message = "Please wrap up gaming and join for dinner.",
            childOriginated = false,
            createdAt = now,
            seen = true,
            seenAt = now
        )

        assertEquals(10L, dto.id)
        assertEquals(1L, dto.senderUserId)
        assertEquals("Parent", dto.senderName)
        assertEquals(2L, dto.receiverUserId)
        assertEquals("Please wrap up gaming and join for dinner.", dto.message)
        assertFalse(dto.childOriginated)
        assertTrue(dto.seen)
        assertNotNull(dto.seenAt)
    }

    @Test
    fun testChildConvocationMessageDtoOmitsSeenMetadata() {
        val now = Instant.now().toString()
        val childDto = ChildConvocationMessageDto(
            id = 20L,
            message = "Priority message from family",
            createdAt = now
        )

        assertEquals(20L, childDto.id)
        assertEquals("Priority message from family", childDto.message)
        assertEquals(now, childDto.createdAt)

        // Reflection check: verify ChildConvocationMessageDto strictly DOES NOT have 'seen' or 'seenAt' fields
        val fieldNames = childDto.javaClass.declaredFields.map { it.name }
        assertFalse("Child DTO must not expose 'seen'", fieldNames.contains("seen"))
        assertFalse("Child DTO must not expose 'seenAt'", fieldNames.contains("seenAt"))
        assertFalse("Child DTO must not expose 'status'", fieldNames.contains("status"))
    }

    @Test
    fun testChildViewingSessionResponseProperties() {
        val now = Instant.now().toString()
        val session = ChildViewingSessionResponseDto(
            sessionUuid = "sess-12345",
            viewStartedAt = now,
            visibilityExpiresAt = now,
            remainingSeconds = 120L,
            messages = listOf(
                ChildConvocationMessageDto(1L, "Msg 1", now),
                ChildConvocationMessageDto(2L, "Msg 2", now)
            )
        )

        assertEquals("sess-12345", session.sessionUuid)
        assertEquals(120L, session.remainingSeconds)
        assertEquals(2, session.messages.size)
    }

    @Test
    fun testDecoyNotificationConstants() {
        // Must strictly display decoy text without revealing actual Parent message
        assertEquals("Check your battery status", ConvocationNotificationManager.DECOY_MESSAGE)
        assertEquals("Device Update", ConvocationNotificationManager.DECOY_TITLE)
        assertEquals("nivya_device_status_sync", ConvocationNotificationManager.CHANNEL_ID)
    }

    @Test
    fun testChildVisibilityStateDtoProperties() {
        val state = ChildVisibilityStateResponseDto(
            viewingActive = true,
            remainingSeconds = 85L,
            unreadCount = 2
        )

        assertTrue(state.viewingActive)
        assertEquals(85L, state.remainingSeconds)
        assertEquals(2, state.unreadCount)
    }
}
