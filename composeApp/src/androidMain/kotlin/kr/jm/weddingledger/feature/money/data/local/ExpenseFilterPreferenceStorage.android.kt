package kr.jm.weddingledger.feature.money.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import java.io.File
import okio.Path.Companion.toPath
import org.koin.mp.KoinPlatformTools

private const val MONEY_FILTER_PREFERENCES_FILE: String = "weddinglog_money_filter.preferences_pb"

private val dataStoreContext: Context
    get() = KoinPlatformTools.defaultContext().get().get()

actual fun createMoneyFilterDataStore(): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath(
        scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default),
        produceFile = {
            File(dataStoreContext.filesDir, MONEY_FILTER_PREFERENCES_FILE).absolutePath.toPath()
        },
    )
}
