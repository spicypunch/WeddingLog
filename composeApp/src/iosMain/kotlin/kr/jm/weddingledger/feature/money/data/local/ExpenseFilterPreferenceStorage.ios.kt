package kr.jm.weddingledger.feature.money.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import platform.Foundation.NSHomeDirectory

private const val MONEY_FILTER_PREFERENCES_FILE: String = "weddinglog_money_filter.preferences_pb"

actual fun createMoneyFilterDataStore(): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath(
        scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default),
        produceFile = {
            "${NSHomeDirectory()}/Documents/$MONEY_FILTER_PREFERENCES_FILE".toPath()
        },
    )
}
