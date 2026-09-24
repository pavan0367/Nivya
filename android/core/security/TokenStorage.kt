package com.nivya.core.security

/**
 * Interface contract for persisting and retrieving authentication session tokens,
 * device identification, and assigned user role.
 */
interface TokenStorage {
    fun saveTokens(accessToken: String, refreshToken: String)
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun saveUserRole(role: String)
    fun getUserRole(): String?
    fun saveDeviceUuid(uuid: String)
    fun getDeviceUuid(): String
    fun clearAll()
    fun hasAccessToken(): Boolean
}
