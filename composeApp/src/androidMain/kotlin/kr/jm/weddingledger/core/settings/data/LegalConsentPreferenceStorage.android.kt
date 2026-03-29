package kr.jm.weddingledger.core.settings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import java.io.File
import okio.Path.Companion.toPath
import org.koin.mp.KoinPlatformTools

private const val LEGAL_CONSENT_PREFERENCES_FILE: String =
    "weddinglog_legal_consent.preferences_pb"

private val dataStoreContext: Context
    get() = KoinPlatformTools.defaultContext().get().get()

actual fun createLegalConsentDataStore(): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath(
        scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default),
        produceFile = {
            File(
                dataStoreContext.filesDir,
                LEGAL_CONSENT_PREFERENCES_FILE,
            ).absolutePath.toPath()
        },
    )
}
