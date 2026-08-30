package com.momentummm.app.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationWindowBoundaryTest {

    @Test
    fun `el ultimo minuto del margen sigue dentro`() {
        assertTrue(
            NotificationWindow.isWithin(
                nowMinutes = minutes(22, 30),
                startMinutes = minutes(8),
                endMinutes = minutes(22),
                graceMinutes = 30
            )
        )
    }

    @Test
    fun `un minuto despues del margen queda fuera`() {
        assertFalse(
            NotificationWindow.isWithin(
                nowMinutes = minutes(22, 31),
                startMinutes = minutes(8),
                endMinutes = minutes(22),
                graceMinutes = 30
            )
        )
    }

    @Test
    fun `el margen funciona en una ventana que cruza medianoche`() {
        val start = minutes(22)
        val end = minutes(6)

        assertTrue(NotificationWindow.isWithin(minutes(6, 30), start, end, graceMinutes = 30))
        assertFalse(NotificationWindow.isWithin(minutes(6, 31), start, end, graceMinutes = 30))
    }

    private fun minutes(hour: Int, minute: Int = 0): Int = hour * 60 + minute
}
