package kr.jm.weddingledger.feature.money.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val EXPENSE_FILTER_STATE_KEY: String = "money_expense_filter_state_v1"

private object MoneyFilterStorage {
    val preferencesDataStore: DataStore<Preferences> by lazy { createMoneyFilterDataStore() }
}

object ExpenseFilterPreferenceStorage {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    private val stateKey = stringPreferencesKey(EXPENSE_FILTER_STATE_KEY)

    val state: Flow<ExpenseFilterPreferenceLocalState?> =
        MoneyFilterStorage.preferencesDataStore.data
            .catch { emit(emptyPreferences()) }
            .map { prefs ->
                val raw = prefs[stateKey] ?: return@map null
                runCatching {
                    json.decodeFromString(ExpenseFilterPreferenceLocalState.serializer(), raw)
                }.getOrNull()
            }

    suspend fun save(state: ExpenseFilterPreferenceLocalState) {
        val encoded = json.encodeToString(ExpenseFilterPreferenceLocalState.serializer(), state)
        MoneyFilterStorage.preferencesDataStore.edit { prefs ->
            prefs[stateKey] = encoded
        }
    }
}

@Serializable
data class ExpenseFilterPreferenceLocalState(
    val selectedCategoryId: String? = null,
    val selectedPeriodFilterName: String = "All",
    val selectedStatusFilterName: String = "All",
    val selectedPresetId: String? = null,
    val presetSeed: Int = 1,
    val overBudgetOnly: Boolean = false,
    val presets: List<ExpenseFilterPresetLocalState> = emptyList(),
)

@Serializable
data class ExpenseFilterPresetLocalState(
    val id: String,
    val label: String,
    val categoryId: String? = null,
    val periodFilterName: String,
    val statusFilterName: String,
)

expect fun createMoneyFilterDataStore(): DataStore<Preferences>
