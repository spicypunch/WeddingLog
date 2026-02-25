package kr.jm.weddingledger.core.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val HOME_ENTRY_PROMPTED_KEY: String = "notification_prompted_home_entry"

private object NotificationPermissionPromptStore {
    val preferencesDataStore: DataStore<Preferences> by lazy { createNotificationPermissionPromptDataStore() }
}

object NotificationPermissionPromptStorage {
    private val homeEntryPromptedKey = booleanPreferencesKey(HOME_ENTRY_PROMPTED_KEY)

    suspend fun shouldPromptOnHomeEntry(): Boolean {
        return NotificationPermissionPromptStore.preferencesDataStore.data
            .catch { emit(emptyPreferences()) }
            .map { preferences -> !(preferences[homeEntryPromptedKey] ?: false) }
            .first()
    }

    suspend fun markHomeEntryPrompted() {
        NotificationPermissionPromptStore.preferencesDataStore.edit { preferences ->
            preferences[homeEntryPromptedKey] = true
        }
    }
}

expect fun createNotificationPermissionPromptDataStore(): DataStore<Preferences>
