package com.nivya.data.repository

import com.nivya.core.database.NivyaDatabase
import com.nivya.core.network.NetworkResult
import com.nivya.core.network.NivyaApiService
import com.nivya.core.network.dto.*
import com.nivya.core.security.SecureTokenStorage
import com.nivya.data.local.UserPreferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository handling user registration, authentication, token rotation, and local session purge.
 */
class AuthRepository(
    private val apiService: NivyaApiService,
    private val tokenStorage: SecureTokenStorage,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val database: NivyaDatabase
) {

    suspend fun login(email: String, password: String): NetworkResult<AuthResponseDataDto> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.login(
                    LoginRequestDto(
                        email = email.trim(),
                        password = password
                    )
                )
                if (response.isSuccessful && response.body()?.data != null) {
                    val authData = response.body()!!.data!!
                    tokenStorage.saveTokens(authData.accessToken, authData.refreshToken)
                    tokenStorage.saveUserRole(authData.user.role)
                    preferencesDataStore.saveSelectedRole(authData.user.role)
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

    suspend fun register(
        name: String,
        email: String,
        password: String,
        role: String
    ): NetworkResult<AuthResponseDataDto> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.register(
                    RegisterRequestDto(
                        name = name.trim(),
                        email = email.trim(),
                        password = password,
                        role = role.trim().uppercase()
                    )
                )
                if (response.isSuccessful && response.body()?.data != null) {
                    val authData = response.body()!!.data!!
                    tokenStorage.saveTokens(authData.accessToken, authData.refreshToken)
                    tokenStorage.saveUserRole(authData.user.role)
                    preferencesDataStore.saveSelectedRole(authData.user.role)
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

    suspend fun logout(): NetworkResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val refreshToken = tokenStorage.getRefreshToken()
                if (!refreshToken.isNullOrBlank()) {
                    apiService.logout(RefreshTokenRequestDto(refreshToken))
                }
            } catch (_: Exception) {
                // Ignore remote network error on logout to guarantee local wipe
            } finally {
                tokenStorage.clearAll()
                preferencesDataStore.clear()
                database.familyDao().clearFamily()
                database.deviceStatusDao().clearDevices()
            }
            NetworkResult.Success(Unit)
        }
    }

    fun isLoggedIn(): Boolean {
        return tokenStorage.hasAccessToken()
    }

    fun getSavedUserRole(): String? {
        return tokenStorage.getUserRole()
    }
}
