package com.momentummm.app.data.entity

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas del Ayuno digital y del Modo nuclear.
 *
 * Los dos defectos que más importan aquí son de aritmética de calendario y no
 * se ven a ojo: una franja de ayuno que cruza medianoche NUNCA se activaba, y
 * el conteo de días del modo nuclear mostraba siempre un día menos del real.
 */
class FastingAndNuclearTest {

    /** Índice propio de la app para el día de hoy: 1 = lunes … 7 = domingo. */
    private fun todayIndex(): Int {
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        return if (dayOfWeek == 1) 7 else dayOfWeek - 1
    }

    private fun yesterdayIndex(): Int {
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val dayOfWeek = yesterday.get(Calendar.DAY_OF_WEEK)
        return if (dayOfWeek == 1) 7 else dayOfWeek - 1
    }

    private fun nowMinuteOfDay(): Int = Calendar.getInstance().let {
        it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE)
    }

    private fun fasting(
        startHour: Int,
        endHour: Int,
        days: String,
        limit: Int = 30
    ) = SmartBlockingConfig(
        digitalFastingEnabled = true,
        fastingStartHour = startHour,
        fastingStartMinute = 0,
        fastingEndHour = endHour,
        fastingEndMinute = 0,
        fastingDailyLimitMinutes = limit,
        fastingDaysOfWeek = days
    )

    // ── Ayuno: franja del mismo día ───────────────────────────────────────

    @Test
    fun `el ayuno esta activo dentro de una franja del mismo dia`() {
        // Una franja de 00:00 a 23:59 hoy cubre cualquier momento salvo el
        // último minuto del día.
        val config = SmartBlockingConfig(
            digitalFastingEnabled = true,
            fastingStartHour = 0,
            fastingStartMinute = 0,
            fastingEndHour = 23,
            fastingEndMinute = 59,
            fastingDaysOfWeek = todayIndex().toString()
        )
        if (nowMinuteOfDay() < 23 * 60 + 59) {
            assertTrue(config.isInFastingHours())
        }
    }

    @Test
    fun `el ayuno no se activa en un dia no seleccionado`() {
        // Se elige explícitamente un día distinto de hoy.
        val otherDay = if (todayIndex() == 1) 2 else 1
        val config = fasting(startHour = 0, endHour = 23, days = otherDay.toString())
        assertFalse(config.isInFastingHours())
    }

    @Test
    fun `el ayuno desactivado nunca esta en franja`() {
        val config = fasting(startHour = 0, endHour = 23, days = todayIndex().toString())
            .copy(digitalFastingEnabled = false)
        assertFalse(config.isInFastingHours())
    }

    // ── Ayuno: franja que cruza medianoche (el bug principal) ─────────────

    @Test
    fun `una franja que cruza medianoche si puede estar activa`() {
        // BUG CORREGIDO: `currentTime in start until end` con start > end es un
        // rango vacío, así que 23:00–07:00 nunca se activaba.
        //
        // Se construye una franja que con seguridad contiene el instante
        // actual y que además cruza medianoche: desde un minuto antes de ahora
        // hasta un minuto después, desplazada para forzar start > end.
        val now = nowMinuteOfDay()
        // Franja "casi todo el día" que cruza medianoche: empieza 2 min después
        // de ahora y termina 1 min después de ahora, cubriendo todo excepto ese
        // minuto. Si ahora está cubierto, la rama de medianoche funciona.
        val startMinuteOfDay = (now + 2) % (24 * 60)
        val endMinuteOfDay = (now + 1) % (24 * 60)

        val config = SmartBlockingConfig(
            digitalFastingEnabled = true,
            fastingStartHour = startMinuteOfDay / 60,
            fastingStartMinute = startMinuteOfDay % 60,
            fastingEndHour = endMinuteOfDay / 60,
            fastingEndMinute = endMinuteOfDay % 60,
            // La franja vigente pudo empezar ayer, así que ambos días valen.
            fastingDaysOfWeek = "${todayIndex()},${yesterdayIndex()}"
        )

        // Sólo tiene sentido si realmente quedó start > end.
        if (startMinuteOfDay > endMinuteOfDay) {
            assertTrue(
                "Una franja que cruza medianoche debe poder estar activa",
                config.isInFastingHours()
            )
        }
    }

    @Test
    fun `inicio igual a fin describe una franja de ayuno vacia`() {
        val config = fasting(startHour = 9, endHour = 9, days = "1,2,3,4,5,6,7")
        assertFalse(config.isInFastingHours())
    }

    // ── Ayuno: inicio de la franja vigente ───────────────────────────────

    @Test
    fun `sin franja activa no hay instante de inicio`() {
        val otherDay = if (todayIndex() == 1) 2 else 1
        val config = fasting(startHour = 0, endHour = 23, days = otherDay.toString())
        assertNull(config.fastingWindowStartMillis())
    }

    @Test
    fun `con franja activa el inicio es pasado y del mismo dia`() {
        val config = SmartBlockingConfig(
            digitalFastingEnabled = true,
            fastingStartHour = 0,
            fastingStartMinute = 0,
            fastingEndHour = 23,
            fastingEndMinute = 59,
            fastingDaysOfWeek = todayIndex().toString()
        )
        if (nowMinuteOfDay() < 23 * 60 + 59) {
            val start = config.fastingWindowStartMillis()
            assertNotNull(start)
            assertTrue(
                "El inicio de la franja no puede estar en el futuro",
                start!! <= System.currentTimeMillis()
            )

            val startCal = Calendar.getInstance().apply { timeInMillis = start }
            assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))
            assertEquals(0, startCal.get(Calendar.MINUTE))
        }
    }

    // ── Modo nuclear: ventana temporal ───────────────────────────────────

    private fun nuclear(startOffsetDays: Int, endOffsetDays: Int) = SmartBlockingConfig(
        nuclearModeEnabled = true,
        nuclearModeStartDate = Calendar.getInstance()
            .apply { add(Calendar.DAY_OF_YEAR, startOffsetDays) }.time,
        nuclearModeEndDate = Calendar.getInstance()
            .apply { add(Calendar.DAY_OF_YEAR, endOffsetDays) }.time
    )

    @Test
    fun `el modo nuclear esta activo dentro de su ventana`() {
        assertTrue(nuclear(startOffsetDays = -1, endOffsetDays = 30).isNuclearModeActive())
    }

    @Test
    fun `el modo nuclear expira al pasar su fecha final`() {
        assertFalse(nuclear(startOffsetDays = -30, endOffsetDays = -1).isNuclearModeActive())
    }

    @Test
    fun `el modo nuclear apagado no esta activo aunque queden fechas`() {
        val config = nuclear(startOffsetDays = -1, endOffsetDays = 30)
            .copy(nuclearModeEnabled = false)
        assertFalse(config.isNuclearModeActive())
    }

    @Test
    fun `sin fechas el modo nuclear no esta activo`() {
        val config = SmartBlockingConfig(nuclearModeEnabled = true)
        assertFalse(config.isNuclearModeActive())
    }

    // ── Modo nuclear: apps objetivo ──────────────────────────────────────

    @Test
    fun `una lista de apps vacia no significa todas las apps`() {
        // Es la causa de que el modo no bloqueara nada: el diálogo confirmaba
        // con emptyList() y `contains` siempre devolvía false. La semántica se
        // fija aquí explícitamente para que nadie la reinterprete.
        val config = nuclear(startOffsetDays = -1, endOffsetDays = 30)
            .copy(nuclearModeApps = "")
        assertTrue(config.getNuclearModeAppsList().isEmpty())
    }

    @Test
    fun `las apps elegidas se recuperan de la cadena guardada`() {
        val config = nuclear(startOffsetDays = -1, endOffsetDays = 30)
            .copy(nuclearModeApps = "com.instagram.android,com.zhiliaoapp.musically")
        val apps = config.getNuclearModeAppsList()
        assertEquals(2, apps.size)
        assertTrue(apps.contains("com.instagram.android"))
        assertTrue(apps.contains("com.zhiliaoapp.musically"))
    }

    // ── Modo nuclear: solicitud de desbloqueo ────────────────────────────

    @Test
    fun `la solicitud de desbloqueo esta apagada por defecto`() {
        // Si naciera en true, el primer arranque permitiría desbloquear sin
        // haber esperado nada.
        assertFalse(SmartBlockingConfig.DEFAULT.nuclearModeUnlockRequested)
        assertEquals(0, SmartBlockingConfig.DEFAULT.nuclearModeCurrentWaitSeconds)
    }

    // ── Ventana de sueño: bloqueo desacoplado del recuento ───────────────

    @Test
    fun `el bloqueo nocturno esta apagado por defecto`() {
        // No contar el uso y bloquear apps eran la misma decisión invertida.
        // Por defecto se cuenta menos, pero NO se bloquea nada.
        assertTrue(SmartBlockingConfig.DEFAULT.sleepModeIgnoreTracking)
        assertFalse(SmartBlockingConfig.DEFAULT.sleepModeBlockApps)
    }
}
