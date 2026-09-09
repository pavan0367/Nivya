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
    @SerializedName("senderUserId") val senderUserId: Long,
    @SerializedName("senderName") val senderName: String,
    @SerializedName("receiverUserId") val receiverUserId: Long,
    @SerializedName("message") val message: String,
    @SerializedName("childOriginated") val childOriginated: Boolean,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("seen") val seen: Boolean,
    @SerializedName("seenAt") val seenAt: String? = null
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
