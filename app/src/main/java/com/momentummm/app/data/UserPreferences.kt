package com.momentummm.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val PREFERENCES_NAME = "momentum_user_prefs"

val Context.userPreferencesDataStore by preferencesDataStore(name = PREFERENCES_NAME)

object UserPreferencesKeys {
    val DOB_ISO: Preferences.Key<String> = stringPreferencesKey("dob_iso")
    val SUPPRESS_TUTORIAL_ONCE: Preferences.Key<Boolean> = booleanPreferencesKey("suppress_tutorial_once")
    val LIVED_COLOR: Preferences.Key<String> = stringPreferencesKey("lived_color_hex")
    val FUTURE_COLOR: Preferences.Key<String> = stringPreferencesKey("future_color_hex")
    val ONBOARDING_COMPLETED: Preferences.Key<Boolean> = booleanPreferencesKey("onboarding_completed")

    // Nuevas configuraciones para persistencia completa
    val THEME_MODE: Preferences.Key<String> = stringPreferencesKey("theme_mode")
    val USE_DYNAMIC_COLOR: Preferences.Key<Boolean> = booleanPreferencesKey("use_dynamic_color")
    val NOTIFICATIONS_ENABLED: Preferences.Key<Boolean> = booleanPreferencesKey("notifications_enabled")
    val DAILY_GOAL_MINUTES: Preferences.Key<Int> = intPreferencesKey("daily_goal_minutes")
    val LAST_SYNC_TIMESTAMP: Preferences.Key<Long> = longPreferencesKey("last_sync_timestamp")
    val AUTO_SYNC_ENABLED: Preferences.Key<Boolean> = booleanPreferencesKey("auto_sync_enabled")
    val FOCUS_MODE_ENABLED: Preferences.Key<Boolean> = booleanPreferencesKey("focus_mode_enabled")
    val FOCUS_MODE_BLOCKED_APPS: Preferences.Key<Set<String>> = stringSetPreferencesKey("focus_mode_blocked_apps")
}

object UserPreferencesRepository {
    fun isOnboardingCompletedFlow(context: Context): Flow<Boolean> {
        return context.userPreferencesDataStore.data.map { prefs ->
            prefs[UserPreferencesKeys.ONBOARDING_COMPLETED] ?: false
        }
    }

    suspend fun setOnboardingCompleted(context: Context, completed: Boolean) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[UserPreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun getDobIso(context: Context): String? {
        val prefs = context.userPreferencesDataStore.data.first()
        return prefs[UserPreferencesKeys.DOB_ISO]
    }

    suspend fun setDobIso(context: Context, isoDate: String) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[UserPreferencesKeys.DOB_ISO] = isoDate
        }
    }

    suspend fun setSuppressTutorialOnce(context: Context, value: Boolean) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[UserPreferencesKeys.SUPPRESS_TUTORIAL_ONCE] = value
        }
    }

    suspend fun consumeSuppressTutorialOnce(context: Context): Boolean {
        val current = context.userPreferencesDataStore.data.first()[UserPreferencesKeys.SUPPRESS_TUTORIAL_ONCE] ?: false
        if (current) {
            context.userPreferencesDataStore.edit { prefs ->
                prefs[UserPreferencesKeys.SUPPRESS_TUTORIAL_ONCE] = false
            }
        }
        return current
    }

    suspend fun setWidgetColors(context: Context, livedHex: String, futureHex: String) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[UserPreferencesKeys.LIVED_COLOR] = livedHex
            prefs[UserPreferencesKeys.FUTURE_COLOR] = futureHex
        }
    }

    suspend fun getWidgetColors(context: Context): Pair<String?, String?> {
        val prefs = context.userPreferencesDataStore.data.first()
        return prefs[UserPreferencesKeys.LIVED_COLOR] to prefs[UserPreferencesKeys.FUTURE_COLOR]
    }

    // Nuevos métodos para configuraciones adicionales
    suspend fun setThemeMode(context: Context, mode: String) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[UserPreferencesKeys.THEME_MODE] = mode
        }
    }

    fun getThemeMode(context: Context): Flow<String> {
        return context.userPreferencesDataStore.data.map { prefs ->
            prefs[UserPreferencesKeys.THEME_MODE] ?: "SYSTEM"
        }
    }

    suspend fun setUseDynamicColor(context: Context, enabled: Boolean) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[UserPreferencesKeys.USE_DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[UserPreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun getNotificationsEnabled(context: Context): Boolean {
        val prefs = context.userPreferencesDataStore.data.first()
        return prefs[UserPreferencesKeys.NOTIFICATIONS_ENABLED] ?: true
    }

    suspend fun setDailyGoalMinutes(context: Context, minutes: Int) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[UserPreferencesKeys.DAILY_GOAL_MINUTES] = minutes
        }
    }

    suspend fun getDailyGoalMinutes(context: Context): Int {
        val prefs = context.userPreferencesDataStore.data.first()
        return prefs[UserPreferencesKeys.DAILY_GOAL_MINUTES] ?: 120 // 2 horas por defecto
    }

    suspend fun setLastSyncTimestamp(context: Context, timestamp: Long) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[UserPreferencesKeys.LAST_SYNC_TIMESTAMP] = timestamp
        }
    }

    suspend fun getLastSyncTimestamp(context: Context): Long {
        val prefs = context.userPreferencesDataStore.data.first()
        return prefs[UserPreferencesKeys.LAST_SYNC_TIMESTAMP] ?: 0L
    }

    suspend fun setAutoSyncEnabled(context: Context, enabled: Boolean) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[UserPreferencesKeys.AUTO_SYNC_ENABLED] = enabled
        }
    }

    suspend fun getAutoSyncEnabled(context: Context): Boolean {
        val prefs = context.userPreferencesDataStore.data.first()
        return prefs[UserPreferencesKeys.AUTO_SYNC_ENABLED] ?: true
    }

    suspend fun setFocusModeEnabled(context: Context, enabled: Boolean) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[UserPreferencesKeys.FOCUS_MODE_ENABLED] = enabled
        }
    }

    suspend fun getFocusModeEnabled(context: Context): Boolean {
        val prefs = context.userPreferencesDataStore.data.first()
        return prefs[UserPreferencesKeys.FOCUS_MODE_ENABLED] ?: false
    }

    suspend fun setFocusModeBlockedApps(context: Context, apps: List<String>) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[UserPreferencesKeys.FOCUS_MODE_BLOCKED_APPS] = apps.toSet()
        }
    }

    suspend fun getFocusModeBlockedApps(context: Context): List<String> {
        val prefs = context.userPreferencesDataStore.data.first()
        return prefs[UserPreferencesKeys.FOCUS_MODE_BLOCKED_APPS]?.toList() ?: emptyList()
    }
}
