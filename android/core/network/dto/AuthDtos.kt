package com.nivya.core.network.dto

import com.google.gson.annotations.SerializedName

/**
 * Authentication DTO models matching the Spring Boot backend API.
 */
data class RegisterRequestDto(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("role") val role: String
)

data class LoginRequestDto(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("deviceFingerprint") val deviceFingerprint: String? = null,
    @SerializedName("fcmToken") val fcmToken: String? = null
)

data class RefreshTokenRequestDto(
    @SerializedName("refreshToken") val refreshToken: String
)

data class AuthResponseDataDto(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("tokenType") val tokenType: String = "Bearer",
    @SerializedName("expiresIn") val expiresIn: Long,
    @SerializedName("user") val user: UserDto
)

data class UserDto(
    @SerializedName("id") val id: Long,
    @SerializedName("uuid") val uuid: String?,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: String,
    @SerializedName("status") val status: String?
)

/**
 * Email verification request DTOs matching com.nivya.email.dto contracts.
 */
data class VerificationCodeRequestDto(
    @SerializedName("email") val email: String,
    @SerializedName("purpose") val purpose: String = "EMAIL_VERIFICATION"
)

data class VerificationConfirmRequestDto(
    @SerializedName("email") val email: String,
    @SerializedName("code") val code: String,
    @SerializedName("purpose") val purpose: String = "EMAIL_VERIFICATION"
)

