package com.momentummm.shared.gamification

import com.momentummm.shared.data.InMemorySettingsStore
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProgressStoreTest {

    private fun store() = ProgressStore(InMemorySettingsStore())

    private val lunes = LocalDate(2026, 8, 31)
    private val martes = LocalDate(2026, 9, 1)
    private val viernes = LocalDate(2026, 9, 4)

    @Test
    fun `sin historial el progreso empieza a cero y en nivel uno`() {
        val progreso = store().read()
        assertEquals(0, progreso.totalXp)
        assertEquals(0, progreso.coins)
        assertEquals(0, progreso.streakDays)
        assertNull(progreso.lastActiveDate)
        assertEquals(1, progreso.level)
    }

    @Test
    fun `la primera sesion arranca la racha en uno`() {
        val resultado = store().recordFocusSession(25, completedWholeSession = true, today = lunes)
        assertEquals(1, resultado.progress.streakDays)
        assertEquals(lunes, resultado.progress.lastActiveDate)
        // 25 min * 5 + bonus 150, sin multiplicador todavia.
        assertEquals(125 + 150, resultado.earned.xp)
    }

    @Test
    fun `el progreso se guarda y se recupera`() {
        val ajustes = InMemorySettingsStore()
        ProgressStore(ajustes).recordFocusSession(25, true, lunes)
        val recuperado = ProgressStore(ajustes).read()
        assertEquals(275, recuperado.totalXp)
        assertEquals(55, recuperado.coins)
        assertEquals(1, recuperado.streakDays)
        assertEquals(lunes, recuperado.lastActiveDate)
    }

    @Test
    fun `dos sesiones el mismo dia no inflan la racha`() {
        val ajustes = InMemorySettingsStore()
        val progreso = ProgressStore(ajustes)
        progreso.recordFocusSession(25, true, lunes)
        val segunda = progreso.recordFocusSession(25, true, lunes)
        assertEquals(1, segunda.progress.streakDays)
        assertFalse(segunda.streakGrew)
        // El premio sí se acumula: la segunda sesión del día también cuenta.
        assertEquals(275 * 2, segunda.progress.totalXp)
    }

    @Test
    fun `una sesion al dia siguiente suma un dia de racha y su premio`() {
        val ajustes = InMemorySettingsStore()
        val progreso = ProgressStore(ajustes)
        progreso.recordFocusSession(25, true, lunes)
        val segunda = progreso.recordFocusSession(25, true, martes)
        assertEquals(2, segunda.progress.streakDays)
        assertTrue(segunda.streakGrew)
        // Ahora el premio incluye los 40 puntos del día de racha.
        assertEquals(125 + 150 + 40, segunda.earned.xp)
    }

    @Test
    fun `saltarse dias rompe la racha y aplica la penalizacion una sola vez`() {
        val ajustes = InMemorySettingsStore()
        val progreso = ProgressStore(ajustes)
        progreso.recordFocusSession(25, true, lunes)
        progreso.recordFocusSession(25, true, martes)
        val xpAntes = progreso.read().totalXp

        val tras = progreso.recordFocusSession(25, true, viernes)
        assertTrue(tras.streakBroken)
        assertEquals(1, tras.progress.streakDays)
        // Se resta la penalización de 75 y se suma el premio de la sesión.
        assertEquals(xpAntes - 75 + tras.earned.xp, tras.progress.totalXp)
    }

    @Test
    fun `al llegar al septimo dia se aplica ya el multiplicador del septimo`() {
        val ajustes = InMemorySettingsStore()
        val progreso = ProgressStore(ajustes)
        var dia = lunes
        repeat(6) {
            progreso.recordFocusSession(10, completedWholeSession = false, today = dia)
            dia = LocalDate.fromEpochDays(dia.toEpochDays() + 1)
        }
        assertEquals(6, progreso.read().streakDays)

        val septimo = progreso.recordFocusSession(10, completedWholeSession = false, today = dia)
        assertEquals(7, septimo.progress.streakDays)
        // 10 min * 5 * 1.5 = 75, mas el dia de racha 40 * 1.5 = 60.
        assertEquals(75 + 60, septimo.earned.xp)
        assertEquals(1.5f, septimo.progress.streakMultiplier)
    }

    @Test
    fun `una sesion abandonada sin minutos no premia pero mantiene la racha del dia`() {
        val ajustes = InMemorySettingsStore()
        val progreso = ProgressStore(ajustes)
        progreso.recordFocusSession(25, true, lunes)
        val vacia = progreso.recordFocusSession(0, completedWholeSession = false, today = lunes)
        assertEquals(0, vacia.earned.xp)
        assertEquals(0, vacia.earned.coins)
        assertEquals(1, vacia.progress.streakDays)
    }

    @Test
    fun `subir de nivel se anuncia una sola vez`() {
        val ajustes = InMemorySettingsStore()
        val progreso = ProgressStore(ajustes)
        // La primera sesión ya cruza los 80 puntos del nivel 2.
        val primera = progreso.recordFocusSession(25, true, lunes)
        assertTrue(primera.leveledUp)
        assertEquals(2, primera.progress.level)

        // Una sesión que no cruza otro umbral no vuelve a anunciarlo.
        val segunda = progreso.recordFocusSession(1, completedWholeSession = false, today = lunes)
        assertFalse(segunda.leveledUp)
    }
}
