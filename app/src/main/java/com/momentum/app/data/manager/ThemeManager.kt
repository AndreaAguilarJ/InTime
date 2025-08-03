package com.momentum.app.data.manager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

class ThemeManager(private val context: Context) {
    
    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val CUSTOM_PRIMARY_COLOR = stringPreferencesKey("custom_primary_color")
    }
    
    enum class ThemeMode {
        SYSTEM, LIGHT, DARK
    }
    
    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        when (preferences[PreferencesKeys.THEME_MODE]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }
    
    val useDynamicColor: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USE_DYNAMIC_COLOR] ?: true
    }
    
    val customPrimaryColor: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CUSTOM_PRIMARY_COLOR]
    }
    
    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }
    
    suspend fun setUseDynamicColor(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_DYNAMIC_COLOR] = enabled
        }
    }
    
    suspend fun setCustomPrimaryColor(colorHex: String?) {
        context.dataStore.edit { preferences ->
            if (colorHex != null) {
                preferences[PreferencesKeys.CUSTOM_PRIMARY_COLOR] = colorHex
            } else {
                preferences.remove(PreferencesKeys.CUSTOM_PRIMARY_COLOR)
            }
        }
    }
}