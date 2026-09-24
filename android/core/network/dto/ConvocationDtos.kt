package com.nivya.core.network.dto

import com.google.gson.annotations.SerializedName

data class ParentSendMessageRequestDto(
    @SerializedName("receiverUserId") val receiverUserId: Long? = null,
    @SerializedName("message") val message: String
)

data class ChildSendMessageRequestDto(
    @SerializedName("message") val message: String
)

data class ParentConvocationMessageDto(
    @SerializedName("id") val id: Long,
    @SerializedName("senderUserId") val senderUserId: Long = 0L,
    @SerializedName("senderName") val senderName: String? = "",
    @SerializedName("receiverUserId") val receiverUserId: Long = 0L,
    @SerializedName("message") val message: String = "",
    @SerializedName("childOriginated") val childOriginated: Boolean = false,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("seen") val seen: Boolean = false,
    @SerializedName("seenAt") val seenAt: String? = null,
    @SerializedName("pinned") val pinned: Boolean = false,
    @SerializedName("reaction") val reaction: String? = null,
    @SerializedName("replyToId") val replyToId: Long? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("familyId") val familyId: Long? = null
)

data class ConvocationSeenEventDto(
    @SerializedName("messageId") val messageId: Long,
    @SerializedName("seenAt") val seenAt: String? = null
)

data class ConvocationActionEventDto(
    @SerializedName("action") val action: String,
    @SerializedName("messageId") val messageId: Long,
    @SerializedName("isPinned") val isPinned: Boolean? = null,
    @SerializedName("reaction") val reaction: String? = null
)

/**
 * Child Convocation Message DTO strictly omitting Seen status and metadata.
 */
data class ChildConvocationMessageDto(
    @SerializedName("id") val id: Long,
    @SerializedName("message") val message: String,
    @SerializedName("createdAt") val createdAt: String
)

data class ChildViewingSessionResponseDto(
    @SerializedName("sessionUuid") val sessionUuid: String,
    @SerializedName("viewStartedAt") val viewStartedAt: String,
    @SerializedName("visibilityExpiresAt") val visibilityExpiresAt: String,
    @SerializedName("remainingSeconds") val remainingSeconds: Long,
    @SerializedName("messages") val messages: List<ChildConvocationMessageDto> = emptyList()
)

data class ChildVisibilityStateResponseDto(
    @SerializedName("viewingActive") val viewingActive: Boolean,
    @SerializedName("remainingSeconds") val remainingSeconds: Long,
    @SerializedName("unreadCount") val unreadCount: Int
)
