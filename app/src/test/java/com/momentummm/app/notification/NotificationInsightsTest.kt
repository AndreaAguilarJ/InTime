package com.momentummm.app.notification

import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationInsightsTest {

    // ─── Resumen semanal ─────────────────────────────────────────────────

    @Test
    fun `sin permiso de uso no se envia un resumen de cero minutos`() {
        assertFalse(NotificationInsights.shouldSendWeeklySummary(hasUsagePermission = false, totalMinutes = 900))
        assertFalse(NotificationInsights.shouldSendWeeklySummary(hasUsagePermission = true, totalMinutes = 0))
        assertTrue(NotificationInsights.shouldSendWeeklySummary(hasUsagePermission = true, totalMinutes = 900))
    }

    @Test
    fun `la primera semana no se compara con nada`() {
        val result = NotificationInsights.compareWeeks(currentMinutes = 1200, previousMinutes = null)
        assertEquals(NotificationInsights.Trend.NONE, result.trend)
    }

    @Test
    fun `una semana anterior casi vacia no sirve de base`() {
        val result = NotificationInsights.compareWeeks(currentMinutes = 1200, previousMinutes = 20)
        assertEquals(NotificationInsights.Trend.NONE, result.trend)
    }

    @Test
    fun `bajar de 20 h a 15 h es un 25 por ciento menos`() {
        val result = NotificationInsights.compareWeeks(currentMinutes = 900, previousMinutes = 1200)

        assertEquals(NotificationInsights.Trend.DOWN, result.trend)
        assertEquals(300, result.deltaMinutes)
        assertEquals(25, result.percent)
    }

    @Test
    fun `subir tambien se dice, sin signo negativo en el porcentaje`() {
        val result = NotificationInsights.compareWeeks(currentMinutes = 1500, previousMinutes = 1200)

        assertEquals(NotificationInsights.Trend.UP, result.trend)
        assertEquals(25, result.percent)
    }

    @Test
    fun `un cambio menor del 5 por ciento cuenta como igual`() {
        val result = NotificationInsights.compareWeeks(currentMinutes = 1230, previousMinutes = 1200)
        assertEquals(NotificationInsights.Trend.SAME, result.trend)
    }

    // ─── Día equilibrado ─────────────────────────────────────────────────

    @Test
    fun `sin permiso de uso nunca se felicita por un dia equilibrado`() {
        assertFalse(NotificationInsights.isBalancedDay(false, 21, todayMinutes = 90, averageMinutes = 0))
    }

    @Test
    fun `un dia sin datos no es un dia equilibrado`() {
        assertFalse(NotificationInsights.isBalancedDay(true, 21, todayMinutes = 0, averageMinutes = 0))
    }

    @Test
    fun `antes de las 20 h el dia no ha terminado`() {
        assertFalse(NotificationInsights.isBalancedDay(true, 15, todayMinutes = 90, averageMinutes = 0))
    }

    @Test
    fun `por encima de su propia media no se felicita aunque sea menos de 3 h`() {
        assertFalse(NotificationInsights.isBalancedDay(true, 21, todayMinutes = 150, averageMinutes = 60))
        assertTrue(NotificationInsights.isBalancedDay(true, 21, todayMinutes = 50, averageMinutes = 60))
    }

    @Test
    fun `tres horas o mas no es equilibrado`() {
        assertFalse(NotificationInsights.isBalancedDay(true, 21, todayMinutes = 180, averageMinutes = 0))
    }

    // ─── Cumpleaños ──────────────────────────────────────────────────────

    @Test
    fun `el cumpleanos coincide en dia y mes`() {
        val birth = LocalDate(1990, 5, 14)
        assertTrue(NotificationInsights.isBirthday(birth, LocalDate(2026, 5, 14)))
        assertFalse(NotificationInsights.isBirthday(birth, LocalDate(2026, 5, 15)))
    }

    @Test
    fun `el dia de nacimiento no es un cumpleanos`() {
        val birth = LocalDate(2026, 5, 14)
        assertFalse(NotificationInsights.isBirthday(birth, LocalDate(2026, 5, 14)))
    }

    @Test
    fun `quien nacio un 29 de febrero lo celebra el 28 en anos no bisiestos`() {
        val leap = LocalDate(2000, 2, 29)
        assertTrue(NotificationInsights.isBirthday(leap, LocalDate(2027, 2, 28)))
        assertFalse(NotificationInsights.isBirthday(leap, LocalDate(2028, 2, 28)))
        assertTrue(NotificationInsights.isBirthday(leap, LocalDate(2028, 2, 29)))
    }
}
