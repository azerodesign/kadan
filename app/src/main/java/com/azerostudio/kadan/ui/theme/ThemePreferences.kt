package com.azerostudio.kadan.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/*
 * Theme mode persistence for Kadan v1.2 (DataStore, no new dependency:
 * androidx.datastore.preferences was already in the version catalog).
 *
 * SYSTEM is the default: follow the phone. LIGHT / DARK are manual
 * overrides chosen from the Tema chips on the Home screen. An unknown
 * stored value falls back to SYSTEM instead of crashing.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

private val Context.kadanDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "kadan_settings"
)

object ThemePreferences {
    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")

    fun themeModeFlow(context: Context): Flow<ThemeMode> =
        context.kadanDataStore.data.map { preferences ->
            when (preferences[THEME_MODE_KEY]) {
                ThemeMode.LIGHT.name -> ThemeMode.LIGHT
                ThemeMode.DARK.name -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
        }

    suspend fun setThemeMode(context: Context, mode: ThemeMode) {
        context.kadanDataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.name
        }
    }
}
