package com.momentummm.shared.data

/**
 * Almacén de ajustes sencillos (clave -> valor), la pieza que cada sistema
 * resuelve a su manera: `SharedPreferences` en Android y `NSUserDefaults` en iOS.
 *
 * Se modela como interfaz y no con `expect class` a propósito: así cada plataforma
 * construye su implementación con lo que necesite --Android requiere un
 * `Context`-- y las pruebas pueden inyectar [InMemorySettingsStore] sin simuladores
 * ni emuladores.
 */
interface SettingsStore {
    fun getStringOrNull(key: String): String?
    fun putString(key: String, value: String)
    fun getLongOrNull(key: String): Long?
    fun putLong(key: String, value: Long)
    fun getBoolean(key: String, default: Boolean = false): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun remove(key: String)
}

/** Implementación en memoria, para pruebas y previsualizaciones. */
class InMemorySettingsStore(
    initial: Map<String, Any> = emptyMap(),
) : SettingsStore {

    private val values = initial.toMutableMap()

    override fun getStringOrNull(key: String): String? = values[key] as? String

    override fun putString(key: String, value: String) {
        values[key] = value
    }

    override fun getLongOrNull(key: String): Long? = values[key] as? Long

    override fun putLong(key: String, value: Long) {
        values[key] = value
    }

    override fun getBoolean(key: String, default: Boolean): Boolean =
        values[key] as? Boolean ?: default

    override fun putBoolean(key: String, value: Boolean) {
        values[key] = value
    }

    override fun remove(key: String) {
        values.remove(key)
    }
}

/** Claves compartidas, para que las dos plataformas lean y escriban lo mismo. */
object SettingsKeys {
    /** Fecha de nacimiento en formato ISO (`AAAA-MM-DD`). */
    const val BIRTH_DATE = "birth_date"

    /** Años que se representan en la cuadrícula de vida. */
    const val LIFE_EXPECTANCY = "life_expectancy"
}
