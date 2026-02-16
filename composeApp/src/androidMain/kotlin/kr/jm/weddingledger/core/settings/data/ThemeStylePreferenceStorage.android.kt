package kr.jm.weddingledger.core.settings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import okio.Path.Companion.toPath
import org.koin.mp.KoinPlatformTools
import java.io.File

private const val THEME_STYLE_PREFERENCES_FILE = "weddinglog_theme.preferences_pb"

private val dataStoreContext: Context
    get() = KoinPlatformTools.defaultContext().get().get()

actual fun createThemeStyleDataStore(): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath(
        scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default),
        produceFile = {
            File(dataStoreContext.filesDir, THEME_STYLE_PREFERENCES_FILE).absolutePath.toPath()
        },
    )
}
