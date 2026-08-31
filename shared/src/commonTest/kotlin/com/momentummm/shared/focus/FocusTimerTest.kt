package com.momentummm.shared.focus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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

    // ── Anclaje al reloj: lo que hace que el cronometro sobreviva a que iOS
    //    suspenda la app en segundo plano ────────────────────────────────────

    @Test
    fun `al volver del segundo plano la cuenta atras salta a donde dice el reloj`() {
        val inicio = FocusTimer.start("Estudio", 25, breakMinutes = 5, nowEpochSeconds = 1_000)
        assertEquals(1_000 + 25 * 60, inicio.endsAtEpochSeconds)

        // La app estuvo suspendida diez minutos: sin esto seguiria en 24:59.
        val alVolver = FocusTimer.syncToClock(inicio, nowEpochSeconds = 1_000 + 600)
        assertEquals(FocusPhase.RUNNING, alVolver.phase)
        assertEquals(15 * 60, alVolver.remainingSeconds)
    }

    @Test
    fun `si el intervalo vencio estando suspendida, aparece completada`() {
        val inicio = FocusTimer.start("Estudio", 25, breakMinutes = 5, nowEpochSeconds = 1_000)
        val alVolver = FocusTimer.syncToClock(inicio, nowEpochSeconds = 1_000 + 30 * 60)
        assertEquals(FocusPhase.COMPLETED, alVolver.phase)
        assertEquals(0, alVolver.remainingSeconds)
        assertEquals(25, alVolver.creditedMinutes)
    }

    @Test
    fun `pausar suelta el ancla y reanudar la vuelve a poner`() {
        val inicio = FocusTimer.start("Estudio", 25, nowEpochSeconds = 1_000)
        val enMarcha = FocusTimer.syncToClock(inicio, nowEpochSeconds = 1_300)
        assertEquals(20 * 60, enMarcha.remainingSeconds)

        val pausada = FocusTimer.pause(enMarcha)
        assertNull(pausada.endsAtEpochSeconds)

        // Una hora pausada no consume nada del intervalo.
        val sincronizada = FocusTimer.syncToClock(pausada, nowEpochSeconds = 1_300 + 3_600)
        assertEquals(20 * 60, sincronizada.remainingSeconds)

        val reanudada = FocusTimer.resume(sincronizada, nowEpochSeconds = 5_000)
        assertEquals(5_000 + 20 * 60, reanudada.endsAtEpochSeconds)
    }

    @Test
    fun `un reloj que retrocede no alarga el intervalo mas alla de su duracion`() {
        val inicio = FocusTimer.start("Estudio", 10, nowEpochSeconds = 1_000)
        // Cambio de hora o ajuste manual hacia atras.
        val sincronizada = FocusTimer.syncToClock(inicio, nowEpochSeconds = 500)
        assertEquals(10 * 60, sincronizada.remainingSeconds)
    }

    @Test
    fun `el descanso tambien queda anclado al reloj`() {
        val completada = tickTimes(
            FocusTimer.start("Estudio", 1, breakMinutes = 5, nowEpochSeconds = 1_000), 60
        )
        val descanso = FocusTimer.startBreak(completada, nowEpochSeconds = 2_000)
        assertEquals(2_000 + 5 * 60, descanso.endsAtEpochSeconds)

        val fin = FocusTimer.syncToClock(descanso, nowEpochSeconds = 2_000 + 5 * 60)
        assertEquals(FocusPhase.IDLE, fin.phase)
    }
}
