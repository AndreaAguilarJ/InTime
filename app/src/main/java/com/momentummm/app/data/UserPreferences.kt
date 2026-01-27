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
    
    // Temporary unlocks for Shame or Pay feature
    val TEMPORARY_UNLOCKS: Preferences.Key<Set<String>> = stringSetPreferencesKey("temporary_unlocks")
    
    // Whitelist temporal para apps de compartir durante el flujo de shame share
    val SHARE_WHITELIST_EXPIRATION: Preferences.Key<Long> = longPreferencesKey("share_whitelist_expiration")
    val SHARE_PENDING_PACKAGE: Preferences.Key<String> = stringPreferencesKey("share_pending_package")
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

    /**
     * Flow para observar cambios en el color de semanas vividas
     */
    fun getLivedWeeksColorFlow(context: Context): Flow<String> {
        return context.userPreferencesDataStore.data.map { prefs ->
            prefs[UserPreferencesKeys.LIVED_COLOR] ?: "#4CAF50"
        }
    }

    /**
     * Flow para observar cambios en el color de semanas futuras
     */
    fun getFutureWeeksColorFlow(context: Context): Flow<String> {
        return context.userPreferencesDataStore.data.map { prefs ->
            prefs[UserPreferencesKeys.FUTURE_COLOR] ?: "#E0E0E0"
        }
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

    // === SHAME OR PAY: Temporary Unlocks ===
    
    /**
     * Añade un desbloqueo temporal para una app (formato: "packageName:expirationTime")
     */
    suspend fun addTemporaryUnlock(context: Context, packageName: String, expirationTime: Long) {
        context.userPreferencesDataStore.edit { prefs ->
            val currentUnlocks = prefs[UserPreferencesKeys.TEMPORARY_UNLOCKS]?.toMutableSet() ?: mutableSetOf()
            // Limpiar desbloqueos expirados
            val now = System.currentTimeMillis()
            val validUnlocks = currentUnlocks.filter { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) {
                    parts[1].toLongOrNull()?.let { it > now } ?: false
                } else false
            }.toMutableSet()
            
            // Añadir nuevo desbloqueo
            validUnlocks.add("$packageName:$expirationTime")
            prefs[UserPreferencesKeys.TEMPORARY_UNLOCKS] = validUnlocks
        }
    }

    /**
     * Verifica si una app tiene un desbloqueo temporal activo
     */
    suspend fun isAppTemporarilyUnlocked(context: Context, packageName: String): Boolean {
        val prefs = context.userPreferencesDataStore.data.first()
        val unlocks = prefs[UserPreferencesKeys.TEMPORARY_UNLOCKS] ?: return false
        val now = System.currentTimeMillis()
        
        return unlocks.any { entry ->
            val parts = entry.split(":")
            if (parts.size == 2 && parts[0] == packageName) {
                parts[1].toLongOrNull()?.let { it > now } ?: false
            } else false
        }
    }

    /**
     * Obtiene el tiempo de expiración del desbloqueo temporal
     */
    suspend fun getTemporaryUnlockExpiration(context: Context, packageName: String): Long? {
        val prefs = context.userPreferencesDataStore.data.first()
        val unlocks = prefs[UserPreferencesKeys.TEMPORARY_UNLOCKS] ?: return null
        
        return unlocks.firstNotNullOfOrNull { entry ->
            val parts = entry.split(":")
            if (parts.size == 2 && parts[0] == packageName) {
                parts[1].toLongOrNull()
            } else null
        }
    }

    /**
     * Limpia todos los desbloqueos temporales expirados
     */
    suspend fun cleanExpiredUnlocks(context: Context) {
        context.userPreferencesDataStore.edit { prefs ->
            val currentUnlocks = prefs[UserPreferencesKeys.TEMPORARY_UNLOCKS] ?: return@edit
            val now = System.currentTimeMillis()
            val validUnlocks = currentUnlocks.filter { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) {
                    parts[1].toLongOrNull()?.let { it > now } ?: false
                } else false
            }.toSet()
            prefs[UserPreferencesKeys.TEMPORARY_UNLOCKS] = validUnlocks
        }
    }

    // === SHARE WHITELIST: Apps temporalmente permitidas durante el flujo de share ===
    
    /**
     * Apps de compartir que estarán en whitelist temporal durante el flujo de shame share.
     * Estas apps no serán bloqueadas mientras el usuario está compartiendo.
     */
    val SHARE_APPS = setOf(
        "com.instagram.android",           // Instagram
        "com.whatsapp",                    // WhatsApp
        "com.twitter.android",             // Twitter
        "com.x.android",                   // X (Twitter nuevo)
        "com.facebook.katana",             // Facebook
        "com.facebook.orca",               // Messenger
        "org.telegram.messenger",          // Telegram
        "com.snapchat.android",            // Snapchat
        "com.google.android.apps.messaging", // Google Messages
        "com.android.mms",                 // SMS
        "com.samsung.android.messaging"    // Samsung Messages
    )
    
    /**
     * Duración de la whitelist temporal para compartir (2 minutos)
     */
    const val SHARE_WHITELIST_DURATION_MS = 2 * 60 * 1000L
    
    /**
     * Activa la whitelist temporal para las apps de compartir.
     * Se guarda el paquete de la app que queremos desbloquear después del share.
     */
    suspend fun enableShareWhitelist(context: Context, pendingPackageToUnlock: String) {
        val expirationTime = System.currentTimeMillis() + SHARE_WHITELIST_DURATION_MS
        context.userPreferencesDataStore.edit { prefs ->
            prefs[UserPreferencesKeys.SHARE_WHITELIST_EXPIRATION] = expirationTime
            prefs[UserPreferencesKeys.SHARE_PENDING_PACKAGE] = pendingPackageToUnlock
        }
    }
    
    /**
     * Desactiva la whitelist temporal de compartir.
     */
    suspend fun disableShareWhitelist(context: Context) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs.remove(UserPreferencesKeys.SHARE_WHITELIST_EXPIRATION)
            prefs.remove(UserPreferencesKeys.SHARE_PENDING_PACKAGE)
        }
    }
    
    /**
     * Verifica si una app está en la whitelist temporal de compartir.
     * Retorna true si la app es una de las apps de share Y la whitelist está activa.
     */
    suspend fun isAppInShareWhitelist(context: Context, packageName: String): Boolean {
        if (!SHARE_APPS.contains(packageName)) return false
        
        val prefs = context.userPreferencesDataStore.data.first()
        val expiration = prefs[UserPreferencesKeys.SHARE_WHITELIST_EXPIRATION] ?: return false
        
        return System.currentTimeMillis() < expiration
    }
    
    /**
     * Obtiene el paquete pendiente que se desbloqueará después de compartir.
     * Retorna null si no hay share pendiente o si expiró.
     */
    suspend fun getPendingSharePackage(context: Context): String? {
        val prefs = context.userPreferencesDataStore.data.first()
        val expiration = prefs[UserPreferencesKeys.SHARE_WHITELIST_EXPIRATION] ?: return null
        
        if (System.currentTimeMillis() >= expiration) {
            // Expiró, limpiar
            disableShareWhitelist(context)
            return null
        }
        
        return prefs[UserPreferencesKeys.SHARE_PENDING_PACKAGE]
    }
    
    /**
     * Confirma que el share fue exitoso y otorga el desbloqueo temporal.
     * Retorna true si se otorgó el desbloqueo.
     */
    suspend fun confirmShareAndUnlock(context: Context): Boolean {
        val pendingPackage = getPendingSharePackage(context) ?: return false
        
        // Desactivar whitelist de share
        disableShareWhitelist(context)
        
        // Otorgar desbloqueo temporal de 5 minutos para la app bloqueada
        val unlockExpiration = System.currentTimeMillis() + (5 * 60 * 1000L)
        addTemporaryUnlock(context, pendingPackage, unlockExpiration)
        
        return true
    }
}
