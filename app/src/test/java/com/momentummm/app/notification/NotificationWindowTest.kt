package com.momentummm.app.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifica la ventana de entrega de los mensajes motivacionales.
 *
 * El caso que fallaba en producción: sin permiso de alarmas exactas —lo normal
 * en Android 14+, donde el permiso NO se concede por defecto— el sistema puede
 * retrasar una alarma hasta una hora. Un mensaje programado dentro de la
 * ventana llegaba fuera de ella y se descartaba en silencio.
 */
class NotificationWindowTest {

    private fun minutes(hour: Int, minute: Int = 0) = hour * 60 + minute

    @Test
    fun `dentro de la ventana se entrega`() {
        assertTrue(
            NotificationWindow.isWithin(
                nowMinutes = minutes(12),
                startMinutes = minutes(8),
                endMinutes = minutes(22)
            )
        )
    }

    @Test
    fun `antes de la ventana no se entrega`() {
        assertFalse(
            NotificationWindow.isWithin(
                nowMinutes = minutes(7, 59),
                startMinutes = minutes(8),
                endMinutes = minutes(22)
            )
        )
    }

    @Test
    fun `los limites de la ventana son inclusivos`() {
        assertTrue(
            "La hora de inicio debe contar",
            NotificationWindow.isWithin(minutes(8), minutes(8), minutes(22))
        )
        assertTrue(
            "La hora de fin debe contar",
            NotificationWindow.isWithin(minutes(22), minutes(8), minutes(22))
        )
    }

    @Test
    fun `sin margen una alarma retrasada se descarta`() {
        // Este es exactamente el mensaje que se perdía: programado a las 21:40,
        // entregado por el sistema a las 22:10.
        assertFalse(
            NotificationWindow.isWithin(
                nowMinutes = minutes(22, 10),
                startMinutes = minutes(8),
                endMinutes = minutes(22),
                graceMinutes = 0
            )
        )
    }

    @Test
    fun `con margen la alarma retrasada si se entrega`() {
        assertTrue(
            NotificationWindow.isWithin(
                nowMinutes = minutes(22, 10),
                startMinutes = minutes(8),
                endMinutes = minutes(22),
                graceMinutes = 90
            )
        )
    }

    @Test
    fun `el margen no abre la ventana indefinidamente`() {
        // 90 minutos de margen sobre las 22:00 llegan hasta las 23:30: a las
        // 2 de la mañana no se debe despertar a nadie.
        assertFalse(
            NotificationWindow.isWithin(
                nowMinutes = minutes(2),
                startMinutes = minutes(8),
                endMinutes = minutes(22),
                graceMinutes = 90
            )
        )
    }

    @Test
    fun `un margen negativo se ignora en lugar de encoger la ventana`() {
        assertTrue(
            NotificationWindow.isWithin(
                nowMinutes = minutes(22),
                startMinutes = minutes(8),
                endMinutes = minutes(22),
                graceMinutes = -600
            )
        )
    }

    @Test
    fun `una ventana que cruza medianoche funciona a ambos lados`() {
        val start = minutes(22)
        val end = minutes(6)

        assertTrue("23:00 está dentro", NotificationWindow.isWithin(minutes(23), start, end))
        assertTrue("03:00 está dentro", NotificationWindow.isWithin(minutes(3), start, end))
        assertFalse("12:00 está fuera", NotificationWindow.isWithin(minutes(12), start, end))
    }
}
