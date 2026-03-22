package kr.jm.weddingledger.feature.notifications.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import platform.Foundation.NSHomeDirectory

private const val PUSH_INSTALLATION_PREFERENCES_FILE: String = "weddinglog_push_installation.preferences_pb"

actual fun createPushInstallationDataStore(): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath(
        scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default),
        produceFile = {
            "${NSHomeDirectory()}/Documents/$PUSH_INSTALLATION_PREFERENCES_FILE".toPath()
        },
    )
}
