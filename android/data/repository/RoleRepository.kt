package com.nivya.data.repository

import com.nivya.core.network.NetworkResult
import com.nivya.core.network.NivyaApiService
import com.nivya.core.network.dto.AuthResponseDataDto
import com.nivya.core.network.dto.RoleInfoResponseDto
import com.nivya.core.network.dto.SelectRoleRequestDto
import com.nivya.core.security.SecureTokenStorage
import com.nivya.data.local.UserPreferencesDataStore
import com.nivya.ui.role.RoleType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository coordinating role selection, authoritative server validation, and session rotation.
 */
class RoleRepository(
    private val apiService: NivyaApiService,
    private val tokenStorage: SecureTokenStorage,
    private val preferencesDataStore: UserPreferencesDataStore
) {

    val selectedRoleFlow: Flow<String?> = preferencesDataStore.selectedRoleFlow

    suspend fun selectRole(role: RoleType): NetworkResult<AuthResponseDataDto> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.selectRole(SelectRoleRequestDto(role = role.name))
                if (response.isSuccessful && response.body()?.data != null) {
                    val authData = response.body()!!.data!!
                    tokenStorage.saveTokens(authData.accessToken, authData.refreshToken)
                    tokenStorage.saveUserRole(role.name)
                    preferencesDataStore.saveSelectedRole(role.name)
                    NetworkResult.Success(authData)
                } else {
                    val errorMsg = response.body()?.message
                        ?: response.errorBody()?.string()
                        ?: "Role selection rejected by server (${response.code()})"
                    NetworkResult.Error(code = response.code(), message = errorMsg)
                }
            } catch (e: Exception) {
                NetworkResult.Exception(e)
            }
        }
    }

    suspend fun getCurrentRoleInfo(): NetworkResult<RoleInfoResponseDto> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getCurrentRole()
                if (response.isSuccessful && response.body()?.data != null) {
                    NetworkResult.Success(response.body()!!.data!!)
                } else {
                    val errorMsg = response.body()?.message
                        ?: response.errorBody()?.string()
                        ?: "Failed to fetch role info (${response.code()})"
                    NetworkResult.Error(code = response.code(), message = errorMsg)
                }
            } catch (e: Exception) {
                NetworkResult.Exception(e)
            }
        }
    }

    fun getSavedRole(): RoleType? {
        val saved = tokenStorage.getUserRole() ?: return null
        return try {
            RoleType.valueOf(saved.uppercase())
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
