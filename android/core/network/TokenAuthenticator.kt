package com.nivya.core.network

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nivya.core.network.dto.ApiResponseDto
import com.nivya.core.network.dto.AuthResponseDataDto
import com.nivya.core.network.dto.RefreshTokenRequestDto
import com.nivya.core.security.TokenStorage
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Production-safe OkHttp Authenticator handling HTTP 401 challenges with token rotation.
 *
 * Guarantees:
 * - Thread-safe synchronization preventing concurrent refresh storms.
 * - Single-retry limit per failed request preventing infinite loops.
 * - Excludes auth endpoints (refresh, login, register) from recursive interception.
 * - Zero plaintext secret logging.
 * - Secure local session purge upon refresh rejection/failure.
 */
class TokenAuthenticator(
    private val tokenStorage: TokenStorage,
    private val baseUrl: String,
    private val unauthenticatedClient: OkHttpClient,
    private val onSessionExpired: (() -> Unit)? = null
) : Authenticator {

    private val lock = Any()
    private val gson = Gson()

    override fun authenticate(route: Route?, response: Response): Request? {
        // 1. Prevent infinite retry loops: stop retrying if we already retried this request
        if (responseCount(response) >= 2) {
            return null
        }

        // 2. Never attempt token refresh for authentication endpoints
        val path = response.request.url.encodedPath
        if (path.contains("/auth/refresh") || path.contains("/auth/login") || path.contains("/auth/register")) {
            return null
        }

        // 3. Extract the token that was used in the failed request
        val requestAuthHeader = response.request.header("Authorization")
        val failedToken = requestAuthHeader?.removePrefix("Bearer ")?.trim()

        // 4. Synchronize concurrent 401 responses around a single atomic refresh operation
        synchronized(lock) {
            val currentAccessToken = tokenStorage.getAccessToken()

            // If another thread already completed a refresh while this request was in flight,
            // immediately retry with the newly refreshed token without calling the backend again
            if (!currentAccessToken.isNullOrBlank() && currentAccessToken != failedToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentAccessToken")
                    .build()
            }

            // 5. Read the currently stored refresh token
            val currentRefreshToken = tokenStorage.getRefreshToken()
            if (currentRefreshToken.isNullOrBlank()) {
                handleSessionExpired()
                return null
            }

            // 6. Execute token refresh against the existing backend endpoint
            val refreshSuccess = executeTokenRefresh(currentRefreshToken)
            if (!refreshSuccess) {
                handleSessionExpired()
                return null
            }

            // 7. Token refreshed: return retried request with updated access token
            val newAccessToken = tokenStorage.getAccessToken()
            return if (!newAccessToken.isNullOrBlank()) {
                response.request.newBuilder()
                    .header("Authorization", "Bearer $newAccessToken")
                    .build()
            } else {
                handleSessionExpired()
                null
            }
        }
    }

    private fun executeTokenRefresh(refreshToken: String): Boolean {
        return try {
            val refreshUrl = NetworkConfig.getRefreshUrl(baseUrl)
            val requestBodyJson = gson.toJson(RefreshTokenRequestDto(refreshToken = refreshToken))
            val mediaType = "application/json; charset=utf-8".toMediaType()

            val refreshRequest = Request.Builder()
                .url(refreshUrl)
                .post(requestBodyJson.toRequestBody(mediaType))
                .header("Content-Type", "application/json")
                .build()

            unauthenticatedClient.newCall(refreshRequest).execute().use { res ->
                if (!res.isSuccessful) {
                    return false
                }

                val bodyString = res.body?.string() ?: return false
                val type = object : TypeToken<ApiResponseDto<AuthResponseDataDto>>() {}.type
                val apiResponse: ApiResponseDto<AuthResponseDataDto> = gson.fromJson(bodyString, type)

                val authData = apiResponse.data
                if (apiResponse.success && authData != null && authData.accessToken.isNotBlank()) {
                    val newRefreshToken = if (authData.refreshToken.isNotBlank()) {
                        authData.refreshToken
                    } else {
                        refreshToken
                    }
                    tokenStorage.saveTokens(authData.accessToken, newRefreshToken)
                    if (authData.user.role.isNotBlank()) {
                        tokenStorage.saveUserRole(authData.user.role)
                    }
                    true
                } else {
                    false
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun handleSessionExpired() {
        tokenStorage.clearAll()
        try {
            onSessionExpired?.invoke()
        } catch (_: Exception) {}
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
