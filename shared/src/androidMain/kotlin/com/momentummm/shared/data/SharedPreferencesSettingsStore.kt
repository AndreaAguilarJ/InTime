package com.momentummm.shared.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Ajustes en Android respaldados por `SharedPreferences`.
 *
 * Se usa un fichero propio (`momentum_shared`) en vez del de la app para que el
 * núcleo compartido no pise las preferencias que ya gestionan los managers
 * existentes.
 */
class SharedPreferencesSettingsStore(
    context: Context,
    fileName: String = "momentum_shared",
) : SettingsStore {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(fileName, Context.MODE_PRIVATE)

    override fun getStringOrNull(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun getLongOrNull(key: String): Long? =
        if (prefs.contains(key)) prefs.getLong(key, 0L) else null

    override fun putLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    override fun getBoolean(key: String, default: Boolean): Boolean =
        prefs.getBoolean(key, default)

    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }
}
