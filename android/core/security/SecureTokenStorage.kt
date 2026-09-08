package com.nivya.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure token storage using hardware-backed EncryptedSharedPreferences (AES-256 GCM).
 * Never persists plaintext credentials or secrets in source code or unencrypted storage.
 */
class SecureTokenStorage(context: Context) {

    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveTokens(accessToken: String, refreshToken: String) {
        sharedPreferences.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    fun getAccessToken(): String? {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }

    fun getRefreshToken(): String? {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    }

    fun saveUserRole(role: String) {
        sharedPreferences.edit()
            .putString(KEY_USER_ROLE, role)
            .apply()
    }

    fun getUserRole(): String? {
        return sharedPreferences.getString(KEY_USER_ROLE, null)
    }

    fun saveDeviceUuid(uuid: String) {
        sharedPreferences.edit()
            .putString(KEY_DEVICE_UUID, uuid)
            .apply()
    }

    fun getDeviceUuid(): String {
        var uuid = sharedPreferences.getString(KEY_DEVICE_UUID, null)
        if (uuid == null) {
            uuid = java.util.UUID.randomUUID().toString()
            saveDeviceUuid(uuid)
        }
        return uuid
    }

    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }

    fun hasAccessToken(): Boolean {
        val token = getAccessToken()
        return !token.isNullOrBlank()
    }

    companion object {
        private const val PREFS_NAME = "nivya_secure_prefs"
        private const val KEY_ACCESS_TOKEN = "jwt_access_token"
        private const val KEY_REFRESH_TOKEN = "jwt_refresh_token"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_DEVICE_UUID = "device_uuid"
    }
}
