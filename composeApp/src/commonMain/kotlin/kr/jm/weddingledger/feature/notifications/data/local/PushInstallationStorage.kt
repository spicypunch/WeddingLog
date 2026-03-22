package kr.jm.weddingledger.feature.notifications.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlin.random.Random
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

private const val PUSH_INSTALLATION_ID_KEY: String = "push_installation_id"

private object PushInstallationStore {
    val preferencesDataStore: DataStore<Preferences> by lazy { createPushInstallationDataStore() }
}

object PushInstallationStorage {
    private val installationIdKey = stringPreferencesKey(PUSH_INSTALLATION_ID_KEY)

    suspend fun getOrCreateInstallationId(): String {
        val existing = PushInstallationStore.preferencesDataStore.data
            .catch { emit(emptyPreferences()) }
            .map { it[installationIdKey] }
            .first()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        if (existing != null) return existing

        val created = buildInstallationId()
        PushInstallationStore.preferencesDataStore.edit { prefs ->
            prefs[installationIdKey] = created
        }
        return created
    }
}

private fun buildInstallationId(): String {
    val timestamp = Clock.System.now().toEpochMilliseconds().toString(16)
    val randomPart = buildString {
        repeat(16) {
            append(Random.nextInt(16).toString(16))
        }
    }
    return "wl-$timestamp-$randomPart"
}

expect fun createPushInstallationDataStore(): DataStore<Preferences>
