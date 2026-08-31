package com.momentummm.shared.focus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FocusTimerTest {

    private fun tickTimes(state: FocusTimerState, times: Int): FocusTimerState {
        var actual = state
        repeat(times) { actual = FocusTimer.tick(actual) }
        return actual
    }

    @Test
    fun `una duracion no positiva no arranca sesion`() {
        assertEquals(FocusPhase.IDLE, FocusTimer.start("Estudio", 0).phase)
        assertEquals(FocusPhase.IDLE, FocusTimer.start("Estudio", -5).phase)
    }

    @Test
    fun `arrancar deja la sesion en marcha con el tiempo completo`() {
        val estado = FocusTimer.start("Estudio", durationMinutes = 25, breakMinutes = 5)
        assertEquals(FocusPhase.RUNNING, estado.phase)
        assertEquals(25 * 60, estado.totalSeconds)
        assertEquals(25 * 60, estado.remainingSeconds)
        assertEquals(0f, estado.progress)
        assertFalse(estado.onBreak)
    }

    @Test
    fun `el reloj no corre estando pausada`() {
        val pausada = FocusTimer.pause(FocusTimer.start("Estudio", 1))
        val despues = tickTimes(pausada, 30)
        assertEquals(pausada.remainingSeconds, despues.remainingSeconds)
    }

    @Test
    fun `al agotarse el enfoque pasa a completado y acredita los minutos`() {
        val terminada = tickTimes(FocusTimer.start("Estudio", 2, breakMinutes = 5), 120)
        assertEquals(FocusPhase.COMPLETED, terminada.phase)
        assertEquals(0, terminada.remainingSeconds)
        assertEquals(2, terminada.creditedMinutes)
        assertEquals(1f, terminada.progress)
    }

    @Test
    fun `el enfoque terminado no salta solo al descanso`() {
        val terminada = tickTimes(FocusTimer.start("Estudio", 1, breakMinutes = 5), 60)
        assertEquals(FocusPhase.COMPLETED, terminada.phase)
        assertFalse(terminada.onBreak)
    }

    @Test
    fun `el descanso solo arranca desde completado y con descanso configurado`() {
        val enMarcha = FocusTimer.start("Estudio", 25, breakMinutes = 5)
        assertEquals(enMarcha, FocusTimer.startBreak(enMarcha))

        val sinDescanso = tickTimes(FocusTimer.start("Estudio", 1, breakMinutes = 0), 60)
        assertEquals(sinDescanso, FocusTimer.startBreak(sinDescanso))

        val conDescanso = tickTimes(FocusTimer.start("Estudio", 1, breakMinutes = 5), 60)
        val descanso = FocusTimer.startBreak(conDescanso)
        assertEquals(FocusPhase.BREAK, descanso.phase)
        assertEquals(5 * 60, descanso.remainingSeconds)
        assertTrue(descanso.onBreak)
    }

    @Test
    fun `pausar el descanso y reanudar vuelve al descanso, no al enfoque`() {
        val descanso = FocusTimer.startBreak(
            tickTimes(FocusTimer.start("Estudio", 1, breakMinutes = 5), 60)
        )
        val pausado = FocusTimer.pause(descanso)
        assertEquals(FocusPhase.PAUSED, pausado.phase)
        // La bandera sobrevive a la pausa: es lo que impide que la pantalla
        // vuelva a mostrar las metricas del enfoque en medio de un descanso.
        assertTrue(pausado.onBreak)

        val reanudado = FocusTimer.resume(pausado)
        assertEquals(FocusPhase.BREAK, reanudado.phase)
        assertTrue(reanudado.onBreak)
    }

    @Test
    fun `pausar el enfoque y reanudar vuelve al enfoque`() {
        val reanudado = FocusTimer.resume(FocusTimer.pause(FocusTimer.start("Estudio", 25)))
        assertEquals(FocusPhase.RUNNING, reanudado.phase)
        assertFalse(reanudado.onBreak)
    }

    @Test
    fun `al agotarse el descanso la sesion vuelve a reposo`() {
        val descanso = FocusTimer.startBreak(
            tickTimes(FocusTimer.start("Estudio", 1, breakMinutes = 1), 60)
        )
        val fin = tickTimes(descanso, 60)
        assertEquals(FocusPhase.IDLE, fin.phase)
        assertFalse(fin.onBreak)
    }

    @Test
    fun `saltar el descanso no vuelve a acreditar minutos`() {
        val completada = tickTimes(FocusTimer.start("Estudio", 3, breakMinutes = 5), 180)
        assertEquals(3, completada.creditedMinutes)

        val saltado = FocusTimer.skipBreak(completada)
        assertEquals(FocusPhase.IDLE, saltado.phase)
        assertEquals(0, saltado.creditedMinutes)
    }

    @Test
    fun `reanudar una sesion que no esta pausada no la altera`() {
        val enMarcha = FocusTimer.start("Estudio", 25)
        assertEquals(enMarcha, FocusTimer.resume(enMarcha))
        val reposo = FocusTimerState()
        assertEquals(reposo, FocusTimer.resume(reposo))
    }

    @Test
    fun `la cuenta atras se formatea con minutos y segundos, y con horas si hace falta`() {
        assertEquals("25:00", FocusTimer.formatCountdown(25 * 60))
        assertEquals("04:09", FocusTimer.formatCountdown(249))
        assertEquals("00:00", FocusTimer.formatCountdown(0))
        assertEquals("00:00", FocusTimer.formatCountdown(-30))
        assertEquals("1:30:05", FocusTimer.formatCountdown(5405))
    }

    @Test
    fun `el progreso queda acotado entre cero y uno`() {
        assertEquals(0f, FocusTimerState().progress)
        val mitad = tickTimes(FocusTimer.start("Estudio", 10), 5 * 60)
        assertEquals(0.5f, mitad.progress)
    }
}
