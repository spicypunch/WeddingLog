package kr.jm.weddingledger.core.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kr.jm.weddingledger.core.designsystem.WeddingLogThemeStyle

private const val THEME_STYLE_KEY = "theme_style"

private fun String.toWeddingLogThemeStyleOrDefault(): WeddingLogThemeStyle {
    return runCatching { WeddingLogThemeStyle.valueOf(this) }
        .getOrDefault(WeddingLogThemeStyle.Minimal)
}

private object ThemeStorage {
    val preferencesDataStore: DataStore<Preferences> by lazy { createThemeStyleDataStore() }
}

object ThemeStylePreferenceStorage {
    private val themeStyleKey = stringPreferencesKey(THEME_STYLE_KEY)

    val themeStyle: Flow<WeddingLogThemeStyle> = ThemeStorage.preferencesDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            prefs[themeStyleKey]
                ?.toWeddingLogThemeStyleOrDefault()
                ?: WeddingLogThemeStyle.Minimal
        }

    suspend fun saveThemeStyle(themeStyle: WeddingLogThemeStyle) {
        ThemeStorage.preferencesDataStore.edit { prefs ->
            prefs[themeStyleKey] = themeStyle.name
        }
    }
}

expect fun createThemeStyleDataStore(): DataStore<Preferences>
