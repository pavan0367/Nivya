package com.nivya.core.network

import com.nivya.core.network.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit interface defining the Nivya REST API contracts.
 */
interface NivyaApiService {

    // --- Authentication ---
    @POST("api/v1/auth/register")
    suspend fun register(
        @Body request: RegisterRequestDto
    ): Response<ApiResponseDto<AuthResponseDataDto>>

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequestDto
    ): Response<ApiResponseDto<AuthResponseDataDto>>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequestDto
    ): Response<ApiResponseDto<AuthResponseDataDto>>

    @POST("api/v1/auth/logout")
    suspend fun logout(
        @Body request: RefreshTokenRequestDto? = null
    ): Response<ApiResponseDto<Unit>>

    @GET("api/v1/auth/me")
    suspend fun getCurrentUser(): Response<ApiResponseDto<UserDto>>

    // --- Role Selection ---
    @POST("api/v1/role/select")
    suspend fun selectRole(
        @Body request: SelectRoleRequestDto
    ): Response<ApiResponseDto<AuthResponseDataDto>>

    @GET("api/v1/role/current")
    suspend fun getCurrentRole(): Response<ApiResponseDto<RoleInfoResponseDto>>

    // --- Pairing & Connection ---
    @POST("api/v1/pairing/code")
    suspend fun generatePairingCode(
        @Body request: GenerateCodeRequestDto? = null
    ): Response<ApiResponseDto<PairingCodeResponseDto>>

    @POST("api/v1/pairing/connect")
    suspend fun connectDevices(
        @Body request: ConnectPairingRequestDto
    ): Response<ApiResponseDto<PairingStatusResponseDto>>

    @GET("api/v1/pairing/status")
    suspend fun getPairingStatus(): Response<ApiResponseDto<PairingStatusResponseDto>>

    @POST("api/v1/pairing/revoke")
    suspend fun revokePairing(
        @Body request: RevokePairingRequestDto
    ): Response<ApiResponseDto<Unit>>

    // --- Battery Telemetry ---
    @POST("api/v1/battery/telemetry")
    suspend fun sendBatteryTelemetry(
        @Body request: BatteryTelemetryRequestDto
    ): Response<ApiResponseDto<BatteryStatusResponseDto>>

    @GET("api/v1/battery/current/{deviceId}")
    suspend fun getCurrentBattery(
        @retrofit2.http.Path("deviceId") deviceId: Long
    ): Response<ApiResponseDto<BatteryStatusResponseDto>>

    @GET("api/v1/battery/history/{deviceId}")
    suspend fun getBatteryHistory(
        @retrofit2.http.Path("deviceId") deviceId: Long
    ): Response<ApiResponseDto<BatteryHistoryResponseDto>>

    @GET("api/v1/battery/trends/{deviceId}")
    suspend fun getBatteryTrends(
        @retrofit2.http.Path("deviceId") deviceId: Long
    ): Response<ApiResponseDto<BatteryTrendResponseDto>>
}
