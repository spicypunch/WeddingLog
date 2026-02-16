package kr.jm.weddingledger.core.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import platform.Foundation.NSHomeDirectory

private const val THEME_STYLE_PREFERENCES_FILE = "weddinglog_theme.preferences_pb"

actual fun createThemeStyleDataStore(): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath(
        scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default),
        produceFile = {
            "${NSHomeDirectory()}/Documents/$THEME_STYLE_PREFERENCES_FILE".toPath()
        },
    )
}
