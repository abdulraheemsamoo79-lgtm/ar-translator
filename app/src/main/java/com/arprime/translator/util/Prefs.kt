package com.arprime.translator.util

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "ar_translator_prefs")

class Prefs(private val context: Context) {

    private object Keys {
        val SOURCE_LANG = stringPreferencesKey("source_lang")
        val TARGET_LANG = stringPreferencesKey("target_lang")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val DISMISSED_VERSION_CODE = intPreferencesKey("dismissed_version_code")
    }

    val sourceLang: Flow<String> = context.dataStore.data.map { it[Keys.SOURCE_LANG] ?: "en" }
    val targetLang: Flow<String> = context.dataStore.data.map { it[Keys.TARGET_LANG] ?: "ur" }
    val darkTheme: Flow<Boolean> = context.dataStore.data.map { it[Keys.DARK_THEME] ?: false }

    suspend fun setLanguages(source: String, target: String) {
        context.dataStore.edit {
            it[Keys.SOURCE_LANG] = source
            it[Keys.TARGET_LANG] = target
        }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_THEME] = enabled }
    }

    suspend fun dismissedVersionCode(): Int =
        context.dataStore.data.map { it[Keys.DISMISSED_VERSION_CODE] ?: 0 }.first()

    suspend fun dismissUpdate(versionCode: Int) {
        context.dataStore.edit { it[Keys.DISMISSED_VERSION_CODE] = versionCode }
    }
}
