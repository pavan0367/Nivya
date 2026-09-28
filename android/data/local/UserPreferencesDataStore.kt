package com.nivya.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "nivya_user_prefs")

/**
 * DataStore managing non-sensitive application preferences.
 * Sensitive tokens are strictly stored in SecureTokenStorage.
 */
open class UserPreferencesDataStore(private val context: Context? = null) {

    open val selectedRoleFlow: Flow<String?> = (context?.userDataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_SELECTED_ROLE]
        }

    open suspend fun saveSelectedRole(role: String) {
        context?.userDataStore?.edit { preferences ->
            preferences[KEY_SELECTED_ROLE] = role
        }
    }

    open val fcmTokenFlow: Flow<String?> = (context?.userDataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_FCM_TOKEN]
        }

    open suspend fun getFcmToken(): String? {
        return fcmTokenFlow.first()
    }

    open suspend fun saveFcmToken(token: String) {
        context?.userDataStore?.edit { preferences ->
            preferences[KEY_FCM_TOKEN] = token
        }
    }

    open val lastSyncedFcmTokenFlow: Flow<String?> = (context?.userDataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_LAST_SYNCED_FCM_TOKEN]
        }

    open suspend fun getLastSyncedFcmToken(): String? {
        return lastSyncedFcmTokenFlow.first()
    }

    open suspend fun saveLastSyncedFcmToken(token: String?) {
        context?.userDataStore?.edit { preferences ->
            if (token != null) {
                preferences[KEY_LAST_SYNCED_FCM_TOKEN] = token
            } else {
                preferences.remove(KEY_LAST_SYNCED_FCM_TOKEN)
            }
        }
    }

    open suspend fun clear() {
        context?.userDataStore?.edit { preferences ->
            preferences.clear()
        }
    }

    companion object {
        val KEY_SELECTED_ROLE = stringPreferencesKey("selected_role")
        val KEY_LAST_SYNC_TIME = longPreferencesKey("last_sync_timestamp")
        val KEY_FCM_TOKEN = stringPreferencesKey("fcm_device_token")
        val KEY_LAST_SYNCED_FCM_TOKEN = stringPreferencesKey("last_synced_fcm_token")
    }
}
