package com.momentummm.shared.data

import platform.Foundation.NSUserDefaults

/**
 * Ajustes en iOS respaldados por `NSUserDefaults`.
 *
 * `NSUserDefaults` no distingue «no existe» de «vale cero», así que los getters
 * numéricos comprueban primero con `objectForKey` antes de leer el valor: sin eso,
 * una expectativa de vida sin configurar se leería como 0 y la cuadrícula saldría
 * vacía en vez de usar el valor por defecto.
 */
class UserDefaultsSettingsStore(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : SettingsStore {

    override fun getStringOrNull(key: String): String? = defaults.stringForKey(key)

    override fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }

    override fun getLongOrNull(key: String): Long? =
        if (defaults.objectForKey(key) == null) null else defaults.integerForKey(key)

    override fun putLong(key: String, value: Long) {
        defaults.setInteger(value, forKey = key)
    }

    override fun getBoolean(key: String, default: Boolean): Boolean =
        if (defaults.objectForKey(key) == null) default else defaults.boolForKey(key)

    override fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, forKey = key)
    }

    override fun remove(key: String) {
        defaults.removeObjectForKey(key)
    }
}
