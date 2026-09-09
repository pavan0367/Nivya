package com.nivya.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "nivya_user_prefs")

/**
 * DataStore managing non-sensitive application preferences.
 * Sensitive tokens are strictly stored in SecureTokenStorage.
 */
class UserPreferencesDataStore(private val context: Context) {

    val selectedRoleFlow: Flow<String?> = context.userDataStore.data
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

    suspend fun saveSelectedRole(role: String) {
        context.userDataStore.edit { preferences ->
            preferences[KEY_SELECTED_ROLE] = role
        }
    }

    val fcmTokenFlow: Flow<String?> = context.userDataStore.data
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

    suspend fun saveFcmToken(token: String) {
        context.userDataStore.edit { preferences ->
            preferences[KEY_FCM_TOKEN] = token
        }
    }

    suspend fun clear() {
        context.userDataStore.edit { preferences ->
            preferences.clear()
        }
    }

    companion object {
        val KEY_SELECTED_ROLE = stringPreferencesKey("selected_role")
        val KEY_LAST_SYNC_TIME = longPreferencesKey("last_sync_timestamp")
        val KEY_FCM_TOKEN = stringPreferencesKey("fcm_device_token")
    }
}
