package com.momentummm.app.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.yearsUntil

/**
 * Cálculo de «Mi vida en semanas», compartido entre Android e iOS.
 *
 * Es aritmética de calendario pura, sin nada del sistema operativo, así que vive
 * en el módulo común y las dos plataformas obtienen exactamente el mismo
 * resultado. La fecha de «hoy» entra como parámetro en vez de leerse del reloj:
 * eso hace el cálculo comprobable y evita que dos llamadas dentro del mismo
 * dibujo den números distintos si se cruza la medianoche.
 */
object LifeWeeksMath {

    /** Una fila de la cuadrícula es un año de vida. */
    const val WEEKS_PER_ROW = 52

    /** Expectativa por defecto, la de la charla de Tim Urban. */
    const val DEFAULT_LIFE_EXPECTANCY = 80

    data class LifeWeeksData(
        val totalWeeks: Int,
        val weeksLived: Int,
        val weeksRemaining: Int,
        val currentAge: Int,
        val progressPercentage: Float,
    )

    /**
     * @param birthDate fecha de nacimiento.
     * @param today día de referencia; normalmente el de hoy en la zona local.
     * @param lifeExpectancy años que se representan en la cuadrícula.
     */
    fun calculate(
        birthDate: LocalDate,
        today: LocalDate,
        lifeExpectancy: Int = DEFAULT_LIFE_EXPECTANCY,
    ): LifeWeeksData {
        val totalWeeks = lifeExpectancy * WEEKS_PER_ROW
        // Una fecha de nacimiento en el futuro (dedo torpe en el selector) daría
        // días negativos y una cuadrícula sin sentido: se trata como cero.
        val daysLived = birthDate.daysUntil(today).coerceAtLeast(0)
        val weeksLived = (daysLived / 7).coerceAtMost(totalWeeks)
        return LifeWeeksData(
            totalWeeks = totalWeeks,
            weeksLived = weeksLived,
            weeksRemaining = (totalWeeks - weeksLived).coerceAtLeast(0),
            currentAge = ageInYears(birthDate, today),
            progressPercentage = weeksLived.toFloat() / totalWeeks * 100f,
        )
    }

    /**
     * Años cumplidos, con años bisiestos exactos.
     *
     * Dividir los días por 365 --como hacía la versión original-- adelanta el
     * cumpleaños un día cada cuatro años, así que alguien a diez días de cumplir
     * 40 ya aparecía con 40. `yearsUntil` cuenta años de calendario de verdad.
     */
    fun ageInYears(birthDate: LocalDate, today: LocalDate): Int =
        birthDate.yearsUntil(today).coerceAtLeast(0)

    /** Fila y columna de una semana dentro de la cuadrícula. */
    fun weekPosition(weekIndex: Int): Pair<Int, Int> =
        weekIndex / WEEKS_PER_ROW to weekIndex % WEEKS_PER_ROW

    /** Índice lineal de una semana a partir de su fila y columna. */
    fun weekIndex(row: Int, column: Int): Int = row * WEEKS_PER_ROW + column
}
