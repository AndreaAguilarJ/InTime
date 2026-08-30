package com.momentummm.app.data.manager

import com.momentummm.app.data.entity.SmartBlockingConfig
import java.util.Calendar
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas de la Protección de rachas.
 *
 * Esta función era la más rota de la pantalla: sus días de gracia no se
 * consumían nunca porque `useGraceDay()` no tenía un solo llamador, y su
 * reinicio semanal comparaba número de semana y año por separado, lo que
 * regalaba días extra en la semana que cruza el fin de año.
 *
 * Aquí se prueba la aritmética pura, que es la que no se puede ver a ojo. El
 * consumo real contra Room requiere una prueba instrumentada y queda para la
 * verificación en dispositivo.
 */
class StreakProtectionTest {

    // `startOfWeek` y `shouldWarn` viven en el companion object precisamente
    // para poder probarlas sin instanciar la clase, que necesitaría un doble
    // del DAO de Room.
    private val manager = StreakProtectionManager

    private fun date(year: Int, month: Int, day: Int): Date =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, 12, 0, 0)
        }.time

    // ── Inicio de semana normalizado ─────────────────────────────────────

    @Test
    fun `dos dias de la misma semana comparten inicio de semana`() {
        // 2026-08-24 es lunes y 2026-08-26 miércoles: misma semana.
        val monday = manager.startOfWeek(date(2026, Calendar.AUGUST, 24))
        val wednesday = manager.startOfWeek(date(2026, Calendar.AUGUST, 26))
        assertEquals(monday, wednesday)
    }

    @Test
    fun `dos semanas distintas tienen inicios distintos`() {
        val thisWeek = manager.startOfWeek(date(2026, Calendar.AUGUST, 26))
        val nextWeek = manager.startOfWeek(date(2026, Calendar.SEPTEMBER, 2))
        assertNotEquals(thisWeek, nextWeek)
    }

    @Test
    fun `la semana que cruza el fin de ano no se parte en dos`() {
        // ESTE es el bug: `WEEK_OF_YEAR` vuelve a 1 y `YEAR` cambia, así que la
        // comprobación anterior veía dos semanas distintas donde hay una sola y
        // reiniciaba el cupo por segunda vez, regalando días de gracia.
        //
        // 2026-12-31 es jueves y 2027-01-01 viernes: la MISMA semana.
        val december = manager.startOfWeek(date(2026, Calendar.DECEMBER, 31))
        val january = manager.startOfWeek(date(2027, Calendar.JANUARY, 1))
        assertEquals(
            "31 de diciembre y 1 de enero de esa semana deben compartir inicio de semana",
            december,
            january
        )
    }

    @Test
    fun `el inicio de semana nunca queda en el futuro`() {
        // Asignar DAY_OF_WEEK directamente puede mover la fecha hacia delante;
        // por eso la implementación retrocede día a día.
        val now = Date()
        assertTrue(manager.startOfWeek(now) <= now.time)
    }

    @Test
    fun `el inicio de semana es medianoche`() {
        val start = manager.startOfWeek(date(2026, Calendar.AUGUST, 26))
        val calendar = Calendar.getInstance().apply { timeInMillis = start }
        assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calendar.get(Calendar.MINUTE))
        assertEquals(0, calendar.get(Calendar.SECOND))
        assertEquals(0, calendar.get(Calendar.MILLISECOND))
    }

    @Test
    fun `el inicio de semana cae en el primer dia de la semana del locale`() {
        val start = manager.startOfWeek(date(2026, Calendar.AUGUST, 26))
        val calendar = Calendar.getInstance().apply { timeInMillis = start }
        assertEquals(calendar.firstDayOfWeek, calendar.get(Calendar.DAY_OF_WEEK))
    }

    // ── Aviso previo a romper la racha ───────────────────────────────────

    private fun config(
        protectionEnabled: Boolean = true,
        warnEnabled: Boolean = true,
        warnMinutes: Int = 5
    ) = SmartBlockingConfig(
        streakProtectionEnabled = protectionEnabled,
        warningBeforeStreakBreak = warnEnabled,
        warningMinutesBeforeLimit = warnMinutes
    )

    @Test
    fun `avisa cuando quedan menos minutos que el umbral`() {
        // Límite 30, usados 27 -> quedan 3, dentro del umbral de 5.
        assertTrue(manager.shouldWarn(config(), currentUsageMinutes = 27, limitMinutes = 30))
    }

    @Test
    fun `no avisa cuando aun queda mucho`() {
        assertFalse(manager.shouldWarn(config(), currentUsageMinutes = 10, limitMinutes = 30))
    }

    @Test
    fun `no avisa cuando el limite ya se alcanzo`() {
        // Con 0 o menos restantes ya no es un aviso previo, es un bloqueo.
        assertFalse(manager.shouldWarn(config(), currentUsageMinutes = 30, limitMinutes = 30))
        assertFalse(manager.shouldWarn(config(), currentUsageMinutes = 35, limitMinutes = 30))
    }

    @Test
    fun `no avisa si la proteccion de rachas esta apagada`() {
        // BUG CORREGIDO: el predicado anterior no consultaba
        // streakProtectionEnabled, así que avisaba con la función apagada.
        assertFalse(
            manager.shouldWarn(
                config(protectionEnabled = false),
                currentUsageMinutes = 27,
                limitMinutes = 30
            )
        )
    }

    @Test
    fun `no avisa si el aviso previo esta apagado`() {
        assertFalse(
            manager.shouldWarn(
                config(warnEnabled = false),
                currentUsageMinutes = 27,
                limitMinutes = 30
            )
        )
    }

    @Test
    fun `el umbral elegido por el usuario se respeta`() {
        // Con 15 minutos de umbral, quedar 12 debe avisar; con 5, no.
        assertTrue(
            manager.shouldWarn(
                config(warnMinutes = 15),
                currentUsageMinutes = 18,
                limitMinutes = 30
            )
        )
        assertFalse(
            manager.shouldWarn(
                config(warnMinutes = 5),
                currentUsageMinutes = 18,
                limitMinutes = 30
            )
        )
    }
}
