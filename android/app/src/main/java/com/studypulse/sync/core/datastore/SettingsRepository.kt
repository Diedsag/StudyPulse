package com.studypulse.sync.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.UUID

object SettingsKeys {
    val SERVER_URL = stringPreferencesKey("server_url")
    val DEVICE_UID = stringPreferencesKey("device_uid")
    val NICKNAME = stringPreferencesKey("nickname")
    val RATING_OPT_IN = booleanPreferencesKey("rating_opt_in")
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
}

data class AppSettings(
    val serverUrl: String,
    val deviceUid: String,
    val nickname: String,
    val ratingOptIn: Boolean,
    val notificationsEnabled: Boolean,
    val lastSyncAt: Long?
)

class SettingsRepository(
    private val dataStore: DataStore<Preferences>
) {

    val settings: Flow<AppSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppSettings(
                serverUrl = preferences[SettingsKeys.SERVER_URL] ?: DEFAULT_SERVER_URL,
                deviceUid = preferences[SettingsKeys.DEVICE_UID] ?: "",
                nickname = preferences[SettingsKeys.NICKNAME] ?: DEFAULT_NICKNAME,
                ratingOptIn = preferences[SettingsKeys.RATING_OPT_IN] ?: true,
                notificationsEnabled = preferences[SettingsKeys.NOTIFICATIONS_ENABLED] ?: true,
                lastSyncAt = preferences[SettingsKeys.LAST_SYNC_AT]
            )
        }

    suspend fun currentSettings(): AppSettings = settings.first()

    suspend fun ensureDeviceUid(): String {
        val current = currentSettings()

        if (current.deviceUid.isNotBlank()) {
            return current.deviceUid
        }

        val newDeviceUid = "android-" + UUID.randomUUID().toString()

        dataStore.edit { preferences ->
            preferences[SettingsKeys.DEVICE_UID] = newDeviceUid
        }

        return newDeviceUid
    }

    suspend fun setServerUrl(rawUrl: String) {
        val normalized = normalizeBaseUrl(rawUrl)

        dataStore.edit { preferences ->
            preferences[SettingsKeys.SERVER_URL] = normalized
        }
    }

    suspend fun setNickname(nickname: String) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.NICKNAME] = nickname.ifBlank { DEFAULT_NICKNAME }
        }
    }

    suspend fun setRatingOptIn(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.RATING_OPT_IN] = value
        }
    }

    suspend fun setNotificationsEnabled(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.NOTIFICATIONS_ENABLED] = value
        }
    }

    suspend fun setLastSyncAt(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.LAST_SYNC_AT] = timestamp
        }
    }

    companion object {
        const val DEFAULT_SERVER_URL = "http://10.0.2.2:5000/"
        const val DEFAULT_NICKNAME = "Student Demo"

        fun normalizeBaseUrl(rawUrl: String): String {
            val trimmed = rawUrl.trim()

            if (trimmed.isBlank()) {
                return DEFAULT_SERVER_URL
            }

            return if (trimmed.endsWith("/")) {
                trimmed
            } else {
                "$trimmed/"
            }
        }
    }
}