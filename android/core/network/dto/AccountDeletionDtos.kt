package com.nivya.core.network.dto

import com.google.gson.annotations.SerializedName

/**
 * DTOs matching backend Account Deletion API contracts:
 * - GET  /api/v1/account/deletion/status
 * - POST /api/v1/account/deletion/request-child-approval
 * - POST /api/v1/account/deletion/verify-child-code
 * - POST /api/v1/account/delete
 */

data class AccountDeletionStatusDto(
    @SerializedName("role") val role: String? = null,
    @SerializedName(value = "child", alternate = ["isChild"]) val isChild: Boolean = false,
    @SerializedName(value = "hasConnectedParent", alternate = ["connectedParent", "isHasConnectedParent"]) val hasConnectedParent: Boolean = false,
    @SerializedName("parentEmailMasked") val parentEmailMasked: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName(value = "hasPendingApprovalCode", alternate = ["isHasPendingApprovalCode"]) val hasPendingApprovalCode: Boolean = false,
    @SerializedName("approvalCodeExpiresInSeconds") val approvalCodeExpiresInSeconds: Long? = null,
    @SerializedName("deliveryStatus") val deliveryStatus: String = "IDLE"
)

data class RequestChildApprovalResponseDto(
    @SerializedName("message") val message: String? = null,
    @SerializedName("parentEmailMasked") val parentEmailMasked: String? = null,
    @SerializedName("expiresInMinutes") val expiresInMinutes: Int = 15
)

data class VerifyChildCodeRequestDto(
    @SerializedName("code") val code: String
)

data class DeleteAccountRequestDto(
    @SerializedName("password") val password: String? = null,
    @SerializedName("approvalCode") val approvalCode: String? = null
)
