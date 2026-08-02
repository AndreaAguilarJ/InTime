package com.momentummm.app.util

import java.util.*
import java.util.concurrent.TimeUnit

object LifeWeeksCalculator {
    
    private const val TOTAL_WEEKS_IN_80_YEARS = 4160 // 80 years * 52 weeks
    private const val WEEKS_PER_ROW = 52 // 52 weeks per year
    private const val TOTAL_ROWS = 80 // 80 years
    
    data class LifeWeeksData(
        val totalWeeks: Int = TOTAL_WEEKS_IN_80_YEARS,
        val weeksLived: Int,
        val weeksRemaining: Int,
        val currentAge: Int,
        val progressPercentage: Float
    )
    
    fun calculateLifeWeeks(birthDate: Date, lifeExpectancy: Int = 80): LifeWeeksData {
        val totalWeeksInLife = lifeExpectancy * 52
        val currentDate = Date()
        val ageInMillis = currentDate.time - birthDate.time
        val ageInDays = TimeUnit.MILLISECONDS.toDays(ageInMillis)
        val weeksLived = (ageInDays / 7).toInt()
        val weeksRemaining = maxOf(0, totalWeeksInLife - weeksLived)

        // BUG CORREGIDO: la edad se calculaba como `ageInDays / 365`, que ignora
        // los años bisiestos. El error acumulado (un día cada cuatro años) hace
        // que la división cruce el umbral antes del cumpleaños: alguien a diez
        // días de cumplir 40 ya aparecía como de 40. El desfase crece con la
        // edad. Con java.time la cuenta es exacta.
        val currentAge = calculateAgeInYears(birthDate)

        val progressPercentage = (weeksLived.toFloat() / totalWeeksInLife) * 100f
        
        return LifeWeeksData(
            totalWeeks = totalWeeksInLife,
            weeksLived = weeksLived,
            weeksRemaining = weeksRemaining,
            currentAge = currentAge,
            progressPercentage = progressPercentage
        )
    }

    /** Años completos cumplidos, con años bisiestos correctos. */
    fun calculateAgeInYears(birthDate: Date): Int {
        return try {
            val birth = birthDate.toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
            java.time.temporal.ChronoUnit.YEARS.between(birth, java.time.LocalDate.now())
                .toInt()
                .coerceAtLeast(0)
        } catch (e: Exception) {
            // Respaldo con 365.25 días por año: sigue siendo mejor que 365.
            val days = TimeUnit.MILLISECONDS.toDays(Date().time - birthDate.time)
            (days / 365.25).toInt().coerceAtLeast(0)
        }
    }
    
    fun getWeekPosition(weekIndex: Int): Pair<Int, Int> {
        val row = weekIndex / WEEKS_PER_ROW
        val col = weekIndex % WEEKS_PER_ROW
        return Pair(row, col)
    }
    
    fun getWeekIndexFromPosition(row: Int, col: Int): Int {
        return row * WEEKS_PER_ROW + col
    }
    
    fun formatTimeFromMillis(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "< 1m"
        }
    }
}