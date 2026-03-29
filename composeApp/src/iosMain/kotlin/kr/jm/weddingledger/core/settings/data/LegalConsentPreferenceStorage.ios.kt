package kr.jm.weddingledger.core.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import platform.Foundation.NSHomeDirectory

private const val LEGAL_CONSENT_PREFERENCES_FILE: String =
    "weddinglog_legal_consent.preferences_pb"

actual fun createLegalConsentDataStore(): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath(
        scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default),
        produceFile = {
            "${NSHomeDirectory()}/Documents/$LEGAL_CONSENT_PREFERENCES_FILE".toPath()
        },
    )
}
