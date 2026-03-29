package kr.jm.weddingledger.core.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private const val LEGAL_CONSENT_VERSION_KEY: String = "legal_consent_version"
private const val CURRENT_LEGAL_CONSENT_VERSION: String = "2026-03-29"

private object LegalConsentStore {
    val preferencesDataStore: DataStore<Preferences> by lazy { createLegalConsentDataStore() }
}

object LegalConsentPreferenceStorage {
    private val consentVersionKey = stringPreferencesKey(LEGAL_CONSENT_VERSION_KEY)

    val hasAcceptedCurrentVersion: Flow<Boolean> = LegalConsentStore.preferencesDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            preferences[consentVersionKey] == CURRENT_LEGAL_CONSENT_VERSION
        }

    suspend fun markAcceptedCurrentVersion() {
        LegalConsentStore.preferencesDataStore.edit { preferences ->
            preferences[consentVersionKey] = CURRENT_LEGAL_CONSENT_VERSION
        }
    }
}

expect fun createLegalConsentDataStore(): DataStore<Preferences>
