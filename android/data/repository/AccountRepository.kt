package com.nivya.data.repository

import com.google.gson.JsonParser
import com.nivya.core.database.NivyaDatabase
import com.nivya.core.network.NetworkResult
import com.nivya.core.network.NivyaApiService
import com.nivya.core.network.dto.*
import com.nivya.core.security.TokenStorage
import com.nivya.data.local.UserPreferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository coordinating permanent account deletion, parent approval requests,
 * code verification, and session cleanup according to backend contracts.
 */
class AccountRepository(
    private val apiService: NivyaApiService,
    private val tokenStorage: TokenStorage,
    private val preferencesDataStore: UserPreferencesDataStore? = null,
    private val database: NivyaDatabase? = null,
    private val onSessionPurged: (() -> Unit)? = null
) {

    suspend fun getDeletionStatus(): NetworkResult<AccountDeletionStatusDto> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getDeletionStatus()
                if (response.isSuccessful && response.body()?.data != null) {
                    NetworkResult.Success(response.body()!!.data!!)
                } else {
                    val errorMsg = extractErrorMessage(
                        response.errorBody()?.string(),
                        response.body()?.message,
                        "Failed to retrieve deletion status (${response.code()})"
                    )
                    NetworkResult.Error(code = response.code(), message = errorMsg)
                }
            } catch (e: Exception) {
                NetworkResult.Exception(e)
            }
        }
    }

    suspend fun requestChildApproval(): NetworkResult<RequestChildApprovalResponseDto> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.requestChildDeletionApproval()
                if (response.isSuccessful && response.body()?.data != null) {
                    NetworkResult.Success(response.body()!!.data!!)
                } else {
                    val errorMsg = extractErrorMessage(
                        response.errorBody()?.string(),
                        response.body()?.message,
                        "Failed to request parent approval (${response.code()})"
                    )
                    NetworkResult.Error(code = response.code(), message = errorMsg)
                }
            } catch (e: Exception) {
                NetworkResult.Exception(e)
            }
        }
    }

    suspend fun verifyChildCode(code: String): NetworkResult<Boolean> {
        val trimmedCode = code.trim()
        if (trimmedCode.length != 6) {
            return NetworkResult.Error(code = 400, message = "Approval code must be exactly 6 digits.")
        }
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.verifyChildCode(VerifyChildCodeRequestDto(code = trimmedCode))
                if (response.isSuccessful && response.body()?.success == true) {
                    val approved = response.body()?.data?.get("approved") == true
                    if (approved) {
                        NetworkResult.Success(true)
                    } else {
                        NetworkResult.Error(code = 400, message = "Invalid or unapproved code.")
                    }
                } else {
                    val errorMsg = extractErrorMessage(
                        response.errorBody()?.string(),
                        response.body()?.message,
                        "Invalid, expired, or exhausted parent approval code."
                    )
                    NetworkResult.Error(code = response.code(), message = errorMsg)
                }
            } catch (e: Exception) {
                NetworkResult.Exception(e)
            }
        }
    }

    suspend fun deleteAccount(
        password: String? = null,
        approvalCode: String? = null
    ): NetworkResult<String> {
        return withContext(Dispatchers.IO) {
            try {
                val req = DeleteAccountRequestDto(
                    password = password?.trim(),
                    approvalCode = approvalCode?.trim()
                )
                val response = apiService.deleteAccount(req)
                if (response.isSuccessful && response.body()?.success == true) {
                    val successMsg = response.body()?.data?.get("message")
                        ?: response.body()?.message
                        ?: "Account permanently deleted"

                    // Backend confirmed permanent deletion: purge local session
                    purgeLocalSession()

                    NetworkResult.Success(successMsg)
                } else {
                    val errorMsg = extractErrorMessage(
                        response.errorBody()?.string(),
                        response.body()?.message,
                        "Account deletion failed (${response.code()})"
                    )
                    NetworkResult.Error(code = response.code(), message = errorMsg)
                }
            } catch (e: Exception) {
                NetworkResult.Exception(e)
            }
        }
    }

    suspend fun purgeLocalSession() {
        withContext(Dispatchers.IO) {
            try {
                val deviceUuid = tokenStorage.getDeviceUuid()
                apiService.unregisterPushToken(UnregisterPushTokenRequestDto(deviceUuid))
            } catch (_: Exception) {}

            tokenStorage.clearAll()
            try {
                preferencesDataStore?.clear()
            } catch (_: Exception) {}
            try {
                database?.clearAllTables()
            } catch (_: Exception) {
                try {
                    database?.familyDao()?.clearFamily()
                    database?.deviceStatusDao()?.clearDevices()
                } catch (_: Exception) {}
            }
            try {
                onSessionPurged?.invoke()
            } catch (_: Exception) {}
        }
    }

    private fun extractErrorMessage(errorBody: String?, bodyMessage: String?, defaultMsg: String): String {
        if (!bodyMessage.isNullOrBlank()) return bodyMessage
        if (!errorBody.isNullOrBlank()) {
            try {
                val jsonObject = JsonParser.parseString(errorBody).asJsonObject
                if (jsonObject.has("message") && !jsonObject.get("message").isJsonNull) {
                    val msg = jsonObject.get("message").asString
                    if (msg.isNotBlank()) return msg
                }
            } catch (_: Exception) {}
        }
        return defaultMsg
    }
}
