package com.nivya.core.network.dto

import com.google.gson.annotations.SerializedName

/**
 * Standard API envelope returned by backend endpoints.
 */
data class ApiResponseDto<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: T?,
    @SerializedName("timestamp") val timestamp: String?,
    @SerializedName("traceId") val traceId: String?
)
