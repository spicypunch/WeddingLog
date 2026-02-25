package kr.jm.weddingledger.core.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import platform.Foundation.NSHomeDirectory

private const val NOTIFICATION_PERMISSION_PROMPT_PREFERENCES_FILE: String =
    "weddinglog_notification_prompt.preferences_pb"

actual fun createNotificationPermissionPromptDataStore(): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath(
        scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default),
        produceFile = {
            "${NSHomeDirectory()}/Documents/$NOTIFICATION_PERMISSION_PROMPT_PREFERENCES_FILE".toPath()
        },
    )
}
