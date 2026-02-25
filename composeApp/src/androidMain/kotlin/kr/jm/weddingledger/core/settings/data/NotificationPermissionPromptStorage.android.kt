package kr.jm.weddingledger.core.settings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import java.io.File
import okio.Path.Companion.toPath
import org.koin.mp.KoinPlatformTools

private const val NOTIFICATION_PERMISSION_PROMPT_PREFERENCES_FILE: String =
    "weddinglog_notification_prompt.preferences_pb"

private val dataStoreContext: Context
    get() = KoinPlatformTools.defaultContext().get().get()

actual fun createNotificationPermissionPromptDataStore(): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath(
        scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default),
        produceFile = {
            File(
                dataStoreContext.filesDir,
                NOTIFICATION_PERMISSION_PROMPT_PREFERENCES_FILE,
            ).absolutePath.toPath()
        },
    )
}
