package com.momentummm.app.util

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LifeWeeksMathTest {

    @Test
    fun `la cuadricula por defecto son 4160 semanas`() {
        val datos = LifeWeeksMath.calculate(
            birthDate = LocalDate(1990, 1, 1),
            today = LocalDate(2026, 1, 1),
        )
        assertEquals(4160, datos.totalWeeks)
        assertEquals(datos.totalWeeks, datos.weeksLived + datos.weeksRemaining)
    }

    @Test
    fun `no cumple anios hasta el dia del cumpleanios aunque haya bisiestos`() {
        val nacimiento = LocalDate(1986, 3, 1)
        // Diez días antes de cumplir 40: la division por 365 daba 40 aqui.
        assertEquals(39, LifeWeeksMath.ageInYears(nacimiento, LocalDate(2026, 2, 19)))
        assertEquals(39, LifeWeeksMath.ageInYears(nacimiento, LocalDate(2026, 2, 28)))
        assertEquals(40, LifeWeeksMath.ageInYears(nacimiento, LocalDate(2026, 3, 1)))
    }

    @Test
    fun `una fecha de nacimiento futura no produce semanas negativas`() {
        val datos = LifeWeeksMath.calculate(
            birthDate = LocalDate(2030, 6, 15),
            today = LocalDate(2026, 8, 31),
        )
        assertEquals(0, datos.weeksLived)
        assertEquals(0, datos.currentAge)
        assertEquals(4160, datos.weeksRemaining)
        assertEquals(0f, datos.progressPercentage)
    }

    @Test
    fun `pasada la expectativa la cuadricula se queda llena, no se desborda`() {
        val datos = LifeWeeksMath.calculate(
            birthDate = LocalDate(1900, 1, 1),
            today = LocalDate(2026, 8, 31),
        )
        assertEquals(4160, datos.weeksLived)
        assertEquals(0, datos.weeksRemaining)
        assertEquals(100f, datos.progressPercentage)
    }

    @Test
    fun `una semana recien nacida cuenta cero y a los siete dias cuenta una`() {
        val nacimiento = LocalDate(2026, 8, 1)
        assertEquals(0, LifeWeeksMath.calculate(nacimiento, LocalDate(2026, 8, 7)).weeksLived)
        assertEquals(1, LifeWeeksMath.calculate(nacimiento, LocalDate(2026, 8, 8)).weeksLived)
    }

    @Test
    fun `posicion e indice de semana son operaciones inversas`() {
        for (indice in listOf(0, 1, 51, 52, 53, 2599, 4159)) {
            val (fila, columna) = LifeWeeksMath.weekPosition(indice)
            assertEquals(indice, LifeWeeksMath.weekIndex(fila, columna))
            assertTrue(columna in 0 until LifeWeeksMath.WEEKS_PER_ROW)
        }
    }

    @Test
    fun `una expectativa distinta cambia el tamanio de la cuadricula`() {
        val datos = LifeWeeksMath.calculate(
            birthDate = LocalDate(2000, 1, 1),
            today = LocalDate(2026, 1, 1),
            lifeExpectancy = 90,
        )
        assertEquals(90 * 52, datos.totalWeeks)
    }
}
