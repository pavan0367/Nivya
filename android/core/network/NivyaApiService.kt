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

    // --- Network Telemetry ---
    @POST("api/v1/network/telemetry")
    suspend fun sendNetworkTelemetry(
        @Body request: NetworkTelemetryRequestDto
    ): Response<ApiResponseDto<NetworkStatusResponseDto>>

    @GET("api/v1/network/current/{deviceId}")
    suspend fun getCurrentNetwork(
        @retrofit2.http.Path("deviceId") deviceId: Long
    ): Response<ApiResponseDto<NetworkStatusResponseDto>>

    @GET("api/v1/network/history/{deviceId}")
    suspend fun getNetworkHistory(
        @retrofit2.http.Path("deviceId") deviceId: Long
    ): Response<ApiResponseDto<NetworkHistoryResponseDto>>

    // --- Screen Time & App Usage ---
    @POST("api/v1/usage/telemetry")
    suspend fun sendUsageTelemetry(
        @Body request: UsageTelemetryRequestDto
    ): Response<ApiResponseDto<UsageSummaryResponseDto>>

    @GET("api/v1/usage/summary/{deviceId}")
    suspend fun getDailyUsageSummary(
        @retrofit2.http.Path("deviceId") deviceId: Long,
        @retrofit2.http.Query("date") date: String? = null
    ): Response<ApiResponseDto<UsageSummaryResponseDto>>

    @GET("api/v1/usage/apps/{deviceId}")
    suspend fun getAppUsage(
        @retrofit2.http.Path("deviceId") deviceId: Long,
        @retrofit2.http.Query("date") date: String? = null
    ): Response<ApiResponseDto<AppUsageResponseDto>>

    @GET("api/v1/usage/trends/{deviceId}")
    suspend fun getUsageTrends(
        @retrofit2.http.Path("deviceId") deviceId: Long
    ): Response<ApiResponseDto<UsageTrendResponseDto>>

    // --- Location Telemetry ---
    @POST("api/v1/location/telemetry")
    suspend fun sendLocationTelemetry(
        @Body request: LocationTelemetryRequestDto
    ): Response<ApiResponseDto<LocationStatusResponseDto>>

    @GET("api/v1/location/current/{deviceId}")
    suspend fun getCurrentLocation(
        @retrofit2.http.Path("deviceId") deviceId: Long
    ): Response<ApiResponseDto<LocationStatusResponseDto>>

    @GET("api/v1/location/history/{deviceId}")
    suspend fun getLocationHistory(
        @retrofit2.http.Path("deviceId") deviceId: Long,
        @retrofit2.http.Query("startTime") startTime: String? = null,
        @retrofit2.http.Query("endTime") endTime: String? = null
    ): Response<ApiResponseDto<LocationHistoryResponseDto>>

    // --- Device Health Telemetry ---
    @POST("api/v1/device/health/telemetry")
    suspend fun sendDeviceHealthTelemetry(
        @Body request: DeviceHealthTelemetryRequestDto
    ): Response<ApiResponseDto<DeviceHealthResponseDto>>

    @GET("api/v1/device/health/{deviceId}")
    suspend fun getDeviceHealth(
        @retrofit2.http.Path("deviceId") deviceId: Long
    ): Response<ApiResponseDto<DeviceHealthResponseDto>>

    @GET("api/v1/device/health/my")
    suspend fun getMyDeviceHealth(): Response<ApiResponseDto<DeviceHealthResponseDto>>

    // --- Alerts & Notifications ---
    @GET("api/v1/alerts/family/{familyId}")
    suspend fun getFamilyAlerts(
        @retrofit2.http.Path("familyId") familyId: Long,
        @retrofit2.http.Query("unreadOnly") unreadOnly: Boolean? = null,
        @retrofit2.http.Query("severity") severity: String? = null
    ): Response<ApiResponseDto<List<AlertDto>>>

    @GET("api/v1/alerts/my")
    suspend fun getChildAlerts(): Response<ApiResponseDto<List<AlertDto>>>

    @POST("api/v1/alerts/{alertId}/read")
    suspend fun markAlertAsRead(
        @retrofit2.http.Path("alertId") alertId: Long
    ): Response<ApiResponseDto<AlertDto>>

    @POST("api/v1/alerts/{alertId}/resolve")
    suspend fun resolveAlert(
        @retrofit2.http.Path("alertId") alertId: Long
    ): Response<ApiResponseDto<AlertDto>>

    @GET("api/v1/alerts/unread-count")
    suspend fun getUnreadAlertsCount(): Response<ApiResponseDto<UnreadCountDto>>

    @GET("api/v1/alerts/rules/{familyId}")
    suspend fun getAlertRules(
        @retrofit2.http.Path("familyId") familyId: Long
    ): Response<ApiResponseDto<List<AlertRuleDto>>>

    @retrofit2.http.PUT("api/v1/alerts/rules/{ruleId}")
    suspend fun updateAlertRule(
        @retrofit2.http.Path("ruleId") ruleId: Long,
        @Body request: UpdateAlertRuleRequestDto
    ): Response<ApiResponseDto<AlertRuleDto>>
}


