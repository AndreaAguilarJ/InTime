package com.momentummm.app.util

/**
 * Esperanza de vida con la que se dibuja «Mi vida en semanas».
 *
 * La elige el usuario en Ajustes > Mi vida en semanas y la usan la pantalla Mi
 * vida, el widget, la vista previa de Ajustes > Widgets y el modo minimalista,
 * para que todos cuenten las mismas semanas.
 */
object LifeExpectancy {
    const val MIN_YEARS = 60
    const val MAX_YEARS = 120
    const val DEFAULT_YEARS = LifeWeeksMath.DEFAULT_LIFE_EXPECTANCY

    /** Lo guardado, dentro del rango del control; 80 si no hay nada guardado. */
    fun sanitize(years: Int?): Int = years?.coerceIn(MIN_YEARS, MAX_YEARS) ?: DEFAULT_YEARS
}
