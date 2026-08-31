package com.momentummm.app.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toKotlinInstant
import kotlinx.datetime.toLocalDateTime
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Adaptador Android de «Mi vida en semanas».
 *
 * El cálculo real vive en [LifeWeeksMath], en el módulo compartido con iOS. Este
 * objeto solo traduce `java.util.Date` --el tipo que ya usan las pantallas y
 * widgets que lo llaman-- a fechas de calendario, así que ninguna de esas llamadas
 * cambia y las dos plataformas cuentan igual.
 */
object LifeWeeksCalculator {

    fun calculateLifeWeeks(birthDate: Date, lifeExpectancy: Int = 80): LifeWeeksMath.LifeWeeksData =
        LifeWeeksMath.calculate(
            birthDate = birthDate.toLocalDate(),
            today = today(),
            lifeExpectancy = lifeExpectancy,
        )

    /** Años completos cumplidos, con años bisiestos correctos. */
    fun calculateAgeInYears(birthDate: Date): Int =
        LifeWeeksMath.ageInYears(birthDate.toLocalDate(), today())

    fun getWeekPosition(weekIndex: Int): Pair<Int, Int> = LifeWeeksMath.weekPosition(weekIndex)

    fun getWeekIndexFromPosition(row: Int, col: Int): Int = LifeWeeksMath.weekIndex(row, col)

    fun formatTimeFromMillis(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "< 1m"
        }
    }

    private fun today(): LocalDate = Date().toLocalDate()

    private fun Date.toLocalDate(): LocalDate =
        toInstant().toKotlinInstant().toLocalDateTime(TimeZone.currentSystemDefault()).date
}

/**
 * Alias del tipo de datos, para que el código existente que escribía
 * `LifeWeeksCalculator.LifeWeeksData` siga compilando ahora que el tipo vive en el
 * módulo compartido.
 */
typealias LifeWeeksData = LifeWeeksMath.LifeWeeksData
