package com.nivya.data.repository

import com.nivya.core.database.NivyaDatabase
import com.nivya.core.network.NetworkResult
import com.nivya.core.network.NivyaApiService
import com.nivya.core.network.dto.*
import com.nivya.core.security.TokenStorage
import com.nivya.data.local.UserPreferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository handling user registration, authentication, token rotation, and local session purge.
 */
open class AuthRepository(
    protected val apiService: NivyaApiService? = null,
    protected val tokenStorage: TokenStorage? = null,
    protected val preferencesDataStore: UserPreferencesDataStore? = null,
    protected val database: NivyaDatabase? = null
) {

    open suspend fun login(email: String, password: String): NetworkResult<AuthResponseDataDto> {
        val api = apiService ?: return NetworkResult.Error(500, "API service unavailable")
        val storage = tokenStorage ?: return NetworkResult.Error(500, "Token storage unavailable")
        val prefs = preferencesDataStore ?: return NetworkResult.Error(500, "Preferences unavailable")
        return withContext(Dispatchers.IO) {
            try {
                val response = api.login(
                    LoginRequestDto(
                        email = email.trim(),
                        password = password
                    )
                )
                if (response.isSuccessful && response.body()?.data != null) {
                    val authData = response.body()!!.data!!
                    storage.saveTokens(authData.accessToken, authData.refreshToken)
                    storage.saveUserRole(authData.user.role)
                    prefs.saveSelectedRole(authData.user.role)
                    try {
                        syncStoredPushToken(force = true)
                    } catch (_: Exception) {}
                    NetworkResult.Success(authData)
                } else {
                    val errorMsg = response.body()?.message
                        ?: response.errorBody()?.string()
                        ?: "Authentication failed (${response.code()})"
                    NetworkResult.Error(code = response.code(), message = errorMsg)
                }
            } catch (e: Exception) {
                NetworkResult.Exception(e)
            }
        }
    }

    open suspend fun register(
        name: String,
        email: String,
        password: String,
        role: String
    ): NetworkResult<AuthResponseDataDto> {
        val api = apiService ?: return NetworkResult.Error(500, "API service unavailable")
        return withContext(Dispatchers.IO) {
            try {
                val response = api.register(
                    RegisterRequestDto(
                        name = name.trim(),
                        email = email.trim(),
                        password = password,
                        role = role.trim().uppercase()
                    )
                )
                if (response.isSuccessful && response.body()?.data != null) {
                    val authData = response.body()!!.data!!
                    val isPending = authData.user.status.equals("PENDING", ignoreCase = true)
                    if (!isPending) {
                        tokenStorage?.saveTokens(authData.accessToken, authData.refreshToken)
                        tokenStorage?.saveUserRole(authData.user.role)
                        preferencesDataStore?.saveSelectedRole(authData.user.role)
                    } else {
                        // Unverified account: ensure no active session is saved until email is confirmed
                        tokenStorage?.clearAll()
                    }
                    NetworkResult.Success(authData)
                } else {
                    val errorMsg = response.body()?.message
                        ?: response.errorBody()?.string()
                        ?: "Registration failed (${response.code()})"
                    NetworkResult.Error(code = response.code(), message = errorMsg)
                }
            } catch (e: Exception) {
                NetworkResult.Exception(e)
            }
        }
    }

    open suspend fun sendVerificationCode(email: String): NetworkResult<String> {
        val api = apiService ?: return NetworkResult.Error(500, "API service unavailable")
        return withContext(Dispatchers.IO) {
            try {
                val response = api.sendVerificationCode(
                    VerificationCodeRequestDto(email = email.trim())
                )
                if (response.isSuccessful) {
                    val message = response.body()?.message
                        ?: response.body()?.data?.get("message")
                        ?: "Verification code dispatched"
                    NetworkResult.Success(message)
                } else {
                    val errorMsg = response.body()?.message
                        ?: response.errorBody()?.string()
                        ?: "Failed to send verification code (${response.code()})"
                    NetworkResult.Error(code = response.code(), message = errorMsg)
                }
            } catch (e: Exception) {
                NetworkResult.Exception(e)
            }
        }
    }

    open suspend fun confirmVerificationCode(email: String, code: String): NetworkResult<Boolean> {
        val api = apiService ?: return NetworkResult.Error(500, "API service unavailable")
        return withContext(Dispatchers.IO) {
            try {
                val response = api.confirmVerificationCode(
                    VerificationConfirmRequestDto(
                        email = email.trim(),
                        code = code.trim()
                    )
                )
                if (response.isSuccessful) {
                    val isVerified = response.body()?.data?.get("verified") ?: true
                    NetworkResult.Success(isVerified)
                } else {
                    val errorMsg = response.body()?.message
                        ?: response.errorBody()?.string()
                        ?: "Invalid or expired verification code (${response.code()})"
                    NetworkResult.Error(code = response.code(), message = errorMsg)
                }
            } catch (e: Exception) {
                NetworkResult.Exception(e)
            }
        }
    }

    open suspend fun refreshToken(): NetworkResult<AuthResponseDataDto> {
        val storage = tokenStorage ?: return NetworkResult.Error(500, "Token storage unavailable")
        val api = apiService ?: return NetworkResult.Error(500, "API service unavailable")
        return withContext(Dispatchers.IO) {
            val currentRefreshToken = storage.getRefreshToken()
            if (currentRefreshToken.isNullOrBlank()) {
                return@withContext NetworkResult.Error(code = 401, message = "No refresh token available")
            }
            try {
                val response = api.refreshToken(RefreshTokenRequestDto(currentRefreshToken))
                if (response.isSuccessful && response.body()?.data != null) {
                    val authData = response.body()!!.data!!
                    val newRefreshToken = if (authData.refreshToken.isNotBlank()) authData.refreshToken else currentRefreshToken
                    storage.saveTokens(authData.accessToken, newRefreshToken)
                    if (authData.user.role.isNotBlank()) {
                        storage.saveUserRole(authData.user.role)
                        preferencesDataStore?.saveSelectedRole(authData.user.role)
                    }
                    NetworkResult.Success(authData)
                } else {
                    val errorMsg = response.body()?.message
                        ?: response.errorBody()?.string()
                        ?: "Token refresh failed (${response.code()})"
                    NetworkResult.Error(code = response.code(), message = errorMsg)
                }
            } catch (e: Exception) {
                NetworkResult.Exception(e)
            }
        }
    }

    open suspend fun logout(): NetworkResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Unregister FCM Push Token on backend before session purge
                val deviceUuid = tokenStorage?.getDeviceUuid()
                if (deviceUuid != null) {
                    apiService?.unregisterPushToken(UnregisterPushTokenRequestDto(deviceUuid))
                }
            } catch (_: Exception) {
                // Best effort unregister
            }
            try {
                val refreshToken = tokenStorage?.getRefreshToken()
                if (!refreshToken.isNullOrBlank()) {
                    apiService?.logout(RefreshTokenRequestDto(refreshToken))
                }
            } catch (_: Exception) {
                // Ignore remote network error on logout to guarantee local wipe
            } finally {
                tokenStorage?.clearAll()
                preferencesDataStore?.clear()
                database?.familyDao()?.clearFamily()
                database?.deviceStatusDao()?.clearDevices()
            }
            NetworkResult.Success(Unit)
        }
    }

    open suspend fun syncPushToken(token: String): NetworkResult<PushTokenResponseDto> {
        val api = apiService ?: return NetworkResult.Error(500, "API service unavailable")
        val storage = tokenStorage ?: return NetworkResult.Error(500, "Token storage unavailable")
        return withContext(Dispatchers.IO) {
            try {
                val deviceUuid = storage.getDeviceUuid()
                val response = api.registerPushToken(
                    RegisterPushTokenRequestDto(
                        deviceUuid = deviceUuid,
                        pushToken = token,
                        platform = "ANDROID",
                        deviceName = android.os.Build.MODEL
                    )
                )
                android.util.Log.i("AuthRepo", "Register push token response: code=${response.code()} success=${response.body()?.success}")
                if (response.isSuccessful && response.body()?.data != null) {
                    preferencesDataStore?.saveLastSyncedFcmToken(token)
                    NetworkResult.Success(response.body()!!.data!!)
                } else {
                    NetworkResult.Error(code = response.code(), message = response.message())
                }
            } catch (e: Exception) {
                android.util.Log.w("AuthRepo", "Register push token failed: ${e.message}")
                NetworkResult.Exception(e)
            }
        }
    }

    open suspend fun syncStoredPushToken(force: Boolean = false): NetworkResult<PushTokenResponseDto>? {
        if (!isLoggedIn()) {
            android.util.Log.i("AuthRepo", "syncStoredPushToken: not logged in")
            return null
        }
        val token = preferencesDataStore?.getFcmToken()
        if (token.isNullOrBlank()) {
            android.util.Log.i("AuthRepo", "syncStoredPushToken: stored token is null or blank")
            return null
        }
        if (!force) {
            val lastSynced = preferencesDataStore?.getLastSyncedFcmToken()
            if (token == lastSynced) {
                android.util.Log.i("AuthRepo", "syncStoredPushToken: token already synced (${token.take(8)}...)")
                return null
            }
        }
        android.util.Log.i("AuthRepo", "syncStoredPushToken: synchronizing token (${token.take(8)}...)")
        return syncPushToken(token)
    }

    open fun isLoggedIn(): Boolean {
        return tokenStorage?.hasAccessToken() == true
    }

    open fun getSavedUserRole(): String? {
        return tokenStorage?.getUserRole()
    }
}
