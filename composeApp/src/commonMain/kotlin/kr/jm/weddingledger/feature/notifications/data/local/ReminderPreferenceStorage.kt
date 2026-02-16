package kr.jm.weddingledger.feature.notifications.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private const val SCHEDULED_REMINDER_IDS_KEY: String = "scheduled_reminder_ids"

private object ReminderStorage {
    val preferencesDataStore: DataStore<Preferences> by lazy { createReminderDataStore() }
}

object ReminderPreferenceStorage {
    private val scheduledReminderIdsKey = stringSetPreferencesKey(SCHEDULED_REMINDER_IDS_KEY)

    val scheduledReminderIds: Flow<Set<String>> = ReminderStorage.preferencesDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[scheduledReminderIdsKey] ?: emptySet() }

    suspend fun addScheduledReminderId(id: String) {
        val normalized = id.trim()
        if (normalized.isBlank()) return
        ReminderStorage.preferencesDataStore.edit { prefs ->
            val next = (prefs[scheduledReminderIdsKey] ?: emptySet()) + normalized
            prefs[scheduledReminderIdsKey] = next
        }
    }

    suspend fun removeScheduledReminderId(id: String) {
        val normalized = id.trim()
        if (normalized.isBlank()) return
        ReminderStorage.preferencesDataStore.edit { prefs ->
            val next = (prefs[scheduledReminderIdsKey] ?: emptySet()) - normalized
            prefs[scheduledReminderIdsKey] = next
        }
    }
}

expect fun createReminderDataStore(): DataStore<Preferences>
