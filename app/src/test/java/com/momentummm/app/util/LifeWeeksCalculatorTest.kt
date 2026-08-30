package com.momentummm.app.util

import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LifeWeeksCalculatorTest {

    @Test
    fun `menos de un minuto se muestra sin redondear hacia arriba`() {
        assertEquals("< 1m", LifeWeeksCalculator.formatTimeFromMillis(0))
        assertEquals("< 1m", LifeWeeksCalculator.formatTimeFromMillis(59_999))
    }

    @Test
    fun `los minutos exactos se muestran sin horas`() {
        assertEquals("1m", LifeWeeksCalculator.formatTimeFromMillis(60_000))
        assertEquals("59m", LifeWeeksCalculator.formatTimeFromMillis(3_599_999))
    }

    @Test
    fun `horas y minutos se muestran juntos`() {
        assertEquals("1h 0m", LifeWeeksCalculator.formatTimeFromMillis(3_600_000))
        assertEquals("2h 5m", LifeWeeksCalculator.formatTimeFromMillis(7_500_000))
    }

    @Test
    fun `indice y posicion de semana son operaciones inversas`() {
        listOf(0, 51, 52, 53, 4_159).forEach { index ->
            val (row, column) = LifeWeeksCalculator.getWeekPosition(index)
            assertEquals(index, LifeWeeksCalculator.getWeekIndexFromPosition(row, column))
        }
    }

    @Test
    fun `una fecha futura nunca produce edad negativa`() {
        val futureDate = Date.from(
            LocalDate.now()
                .plusYears(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
        )

        assertEquals(0, LifeWeeksCalculator.calculateAgeInYears(futureDate))
    }

    @Test
    fun `las semanas restantes nunca son negativas`() {
        val veryOldDate = Date.from(
            LocalDate.of(1900, 1, 1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
        )

        assertTrue(LifeWeeksCalculator.calculateLifeWeeks(veryOldDate).weeksRemaining >= 0)
    }
}
