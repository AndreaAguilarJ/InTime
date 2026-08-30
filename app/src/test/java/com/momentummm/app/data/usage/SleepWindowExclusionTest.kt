package com.momentummm.app.data.usage

import com.momentummm.app.data.entity.SmartBlockingConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Pruebas de la Ventana de sueño.
 *
 * La función prometía «no contar el uso durante las horas de sueño» y en
 * realidad sólo abandonaba una comprobación del monitor: el uso nocturno se
 * seguía sumando y reaparecía en el total al amanecer. Estas pruebas fijan la
 * aritmética que lo corrige, incluido el caso que más importa —una ventana que
 * cruza medianoche— porque es el horario de sueño normal de cualquier persona.
 */
class SleepWindowExclusionTest {

    private val minute = 60_000L

    /** Instante local del día de hoy a la hora indicada. */
    private fun today(hour: Int, min: Int = 0): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, min)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun window(startHour: Int, startMin: Int, endHour: Int, endMin: Int) =
        DailyUsageCalculator.ExcludedWindow(
            startMinuteOfDay = startHour * 60 + startMin,
            endMinuteOfDay = endHour * 60 + endMin
        )

    // ── Ventana dentro del mismo día ──────────────────────────────────────

    @Test
    fun `uso completamente dentro de la ventana se excluye entero`() {
        val overlap = DailyUsageCalculator.excludedOverlapMillis(
            from = today(13, 10),
            to = today(13, 40),
            window = window(13, 0, 14, 0)
        )
        assertEquals(30 * minute, overlap)
    }

    @Test
    fun `uso completamente fuera de la ventana no se excluye`() {
        val overlap = DailyUsageCalculator.excludedOverlapMillis(
            from = today(16, 0),
            to = today(16, 30),
            window = window(13, 0, 14, 0)
        )
        assertEquals(0L, overlap)
    }

    @Test
    fun `uso que entra a mitad de la ventana solo excluye la parte solapada`() {
        // 13:45 → 14:15 con ventana 13:00-14:00: sólo los 15 min iniciales.
        val overlap = DailyUsageCalculator.excludedOverlapMillis(
            from = today(13, 45),
            to = today(14, 15),
            window = window(13, 0, 14, 0)
        )
        assertEquals(15 * minute, overlap)
    }

    // ── Ventana que cruza medianoche (el caso real) ───────────────────────

    @Test
    fun `ventana nocturna excluye el uso antes de medianoche`() {
        val overlap = DailyUsageCalculator.excludedOverlapMillis(
            from = today(23, 30),
            to = today(23, 50),
            window = window(23, 0, 7, 0)
        )
        assertEquals(20 * minute, overlap)
    }

    @Test
    fun `ventana nocturna excluye el uso de madrugada`() {
        val overlap = DailyUsageCalculator.excludedOverlapMillis(
            from = today(2, 0),
            to = today(2, 45),
            window = window(23, 0, 7, 0)
        )
        assertEquals(45 * minute, overlap)
    }

    @Test
    fun `ventana nocturna no excluye el uso de la manana`() {
        // A las 07:00 la ventana ya terminó: el fin es exclusivo.
        val overlap = DailyUsageCalculator.excludedOverlapMillis(
            from = today(8, 0),
            to = today(8, 30),
            window = window(23, 0, 7, 0)
        )
        assertEquals(0L, overlap)
    }

    @Test
    fun `uso a caballo del final de la ventana nocturna se recorta`() {
        // 06:40 → 07:20 con ventana 23:00-07:00: sólo los 20 min anteriores a
        // las 07:00 quedan excluidos; los 20 posteriores SÍ cuentan.
        val overlap = DailyUsageCalculator.excludedOverlapMillis(
            from = today(6, 40),
            to = today(7, 20),
            window = window(23, 0, 7, 0)
        )
        assertEquals(20 * minute, overlap)
    }

    // ── Casos límite ─────────────────────────────────────────────────────

    @Test
    fun `inicio igual a fin describe una ventana vacia y no excluye nada`() {
        val overlap = DailyUsageCalculator.excludedOverlapMillis(
            from = today(10, 0),
            to = today(23, 0),
            window = window(22, 0, 22, 0)
        )
        assertEquals(0L, overlap)
    }

    @Test
    fun `un intervalo invertido no excluye nada`() {
        val overlap = DailyUsageCalculator.excludedOverlapMillis(
            from = today(14, 0),
            to = today(13, 0),
            window = window(13, 0, 15, 0)
        )
        assertEquals(0L, overlap)
    }

    // ── setExcludedWindow ────────────────────────────────────────────────

    @Test
    fun `la ventana excluida se publica y se retira`() {
        try {
            DailyUsageCalculator.setExcludedWindow(window(23, 0, 7, 0))
            assertEquals(
                DailyUsageCalculator.ExcludedWindow(23 * 60, 7 * 60),
                DailyUsageCalculator.excludedWindow()
            )

            DailyUsageCalculator.setExcludedWindow(null)
            assertEquals(null, DailyUsageCalculator.excludedWindow())
        } finally {
            // El calculador es un objeto compartido: no dejar estado colgando
            // para las demás pruebas.
            DailyUsageCalculator.setExcludedWindow(null)
        }
    }

    // ── isInSleepHours ───────────────────────────────────────────────────

    private fun configWithSleep(startHour: Int, endHour: Int) = SmartBlockingConfig(
        sleepModeEnabled = true,
        sleepStartHour = startHour,
        sleepStartMinute = 0,
        sleepEndHour = endHour,
        sleepEndMinute = 0
    )

    @Test
    fun `una ventana de sueno con inicio igual al fin nunca esta activa`() {
        // Antes esto caía en `x in start until start`, un rango vacío, pero sin
        // decirlo. Ahora es una decisión explícita y coincide con la exclusión
        // de uso, para que las dos mitades de la función no se contradigan.
        assertFalse(configWithSleep(startHour = 22, endHour = 22).isInSleepHours())
    }

    @Test
    fun `la ventana de sueno esta apagada si la funcion esta desactivada`() {
        val config = configWithSleep(startHour = 0, endHour = 23).copy(sleepModeEnabled = false)
        assertFalse(config.isInSleepHours())
    }

    @Test
    fun `una ventana que cubre casi todo el dia esta activa ahora`() {
        // 00:00–23:59 cubre cualquier instante salvo el último minuto; se
        // comprueba que la evaluación del mismo día funciona de verdad y no
        // devuelve siempre false.
        val config = SmartBlockingConfig(
            sleepModeEnabled = true,
            sleepStartHour = 0,
            sleepStartMinute = 0,
            sleepEndHour = 23,
            sleepEndMinute = 59
        )
        val nowIsLastMinuteOfDay = Calendar.getInstance().let {
            it.get(Calendar.HOUR_OF_DAY) == 23 && it.get(Calendar.MINUTE) == 59
        }
        if (!nowIsLastMinuteOfDay) {
            assertTrue(config.isInSleepHours())
        }
    }
}
