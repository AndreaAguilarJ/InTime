package com.momentummm.shared.data

/**
 * Apariencia elegida por la persona.
 *
 * `SYSTEM` es el valor por defecto a propósito: respetar el ajuste del teléfono es
 * lo que espera quien ya lo tiene configurado, y solo quien quiera lo contrario
 * necesita tocar nada.
 */
enum class ThemeChoice {
    SYSTEM,
    LIGHT,
    DARK,
}

/** Clave donde vive la apariencia elegida. */
private const val KEY_THEME = "theme_choice"

fun SettingsStore.readThemeChoice(): ThemeChoice =
    when (getStringOrNull(KEY_THEME)) {
        "LIGHT" -> ThemeChoice.LIGHT
        "DARK" -> ThemeChoice.DARK
        // Un valor desconocido --de una versión futura o de un dato corrupto-- cae
        // en el del sistema en vez de romper la pantalla.
        else -> ThemeChoice.SYSTEM
    }

fun SettingsStore.writeThemeChoice(choice: ThemeChoice) {
    putString(KEY_THEME, choice.name)
}
