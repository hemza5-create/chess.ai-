package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.chess.AppLang
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "chess_vision_prefs")

data class UserPreferences(
    val lang: AppLang = AppLang.AR,
    val showVisionArrows: Boolean = true,
    val searchDepth: Int = 2
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val LANG = stringPreferencesKey("app_lang")
        val VISION_ARROWS = booleanPreferencesKey("show_vision_arrows")
        val SEARCH_DEPTH = intPreferencesKey("search_depth")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val lang = when (prefs[Keys.LANG]) {
            AppLang.EN.name -> AppLang.EN
            else -> AppLang.AR
        }
        UserPreferences(
            lang = lang,
            showVisionArrows = prefs[Keys.VISION_ARROWS] ?: true,
            searchDepth = (prefs[Keys.SEARCH_DEPTH] ?: 2).coerceIn(1, 3)
        )
    }

    suspend fun setLanguage(lang: AppLang) {
        context.dataStore.edit { it[Keys.LANG] = lang.name }
    }

    suspend fun setShowVisionArrows(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VISION_ARROWS] = enabled }
    }

    suspend fun setSearchDepth(depth: Int) {
        context.dataStore.edit { it[Keys.SEARCH_DEPTH] = depth.coerceIn(1, 3) }
    }
}
