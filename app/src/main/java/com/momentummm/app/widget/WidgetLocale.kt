package com.momentummm.app.widget

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Idioma en el que se escriben los widgets.
 *
 * El usuario puede elegir en Ajustes un idioma distinto al del sistema
 * (`LanguageManager`, que lo aplica con `AppCompatDelegate`). Desde Android 13 el
 * sistema lo extiende a todo el proceso, pero antes solo lo ven las actividades:
 * un widget, que se pinta desde un receptor, salía en el idioma del teléfono
 * mientras la app hablaba otro. Aquí se aplica a mano el idioma guardado.
 */
object WidgetLocale {

    // Mismo almacén y clave que LanguageManager.
    private const val PREFS = "language_prefs"
    private const val KEY_LANGUAGE = "selected_language"
    private const val SYSTEM = "system"

    /** Contexto cuyos recursos están en el idioma elegido en la app. */
    fun localized(base: Context): Context {
        val code = base.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, SYSTEM)
            ?.takeIf { it.isNotBlank() && it != SYSTEM }
            ?: return base
        val wanted = Locale.forLanguageTag(code)
        if (locale(base).language == wanted.language) return base
        val config = Configuration(base.resources.configuration).apply { setLocale(wanted) }
        return base.createConfigurationContext(config)
    }

    fun locale(context: Context): Locale = context.resources.configuration.locales[0]

    /** true si el contenido guardado en la base de datos (en español) sirve tal cual. */
    fun usesStoredSpanishContent(context: Context): Boolean =
        locale(context).language == SPANISH

    private const val SPANISH = "es"
}
