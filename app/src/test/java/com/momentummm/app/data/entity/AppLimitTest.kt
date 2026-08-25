package com.momentummm.app.data.entity

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLimitTest {

    @Test
    fun `sin horario habilitado nunca bloquea`() {
        assertFalse(limit().isWithinScheduleBlock())
    }

    @Test
    fun `un horario sin dias activos nunca bloquea`() {
        assertFalse(
            limit(
                hasScheduleLimit = true,
                scheduleDaysOfWeek = ""
            ).isWithinScheduleBlock()
        )
    }

    @Test
    fun `la copia con horario conserva el original inmutable`() {
        val original = limit()
        val updated = original.withScheduleLimit(
            enabled = true,
            startHour = 22,
            startMinute = 15,
            endHour = 6,
            endMinute = 45,
            daysOfWeek = "1,3,5"
        )

        assertFalse(original.hasScheduleLimit)
        assertEquals(9, original.scheduleStartHour)
        assertTrue(updated.hasScheduleLimit)
        assertEquals(22, updated.scheduleStartHour)
        assertEquals(15, updated.scheduleStartMinute)
        assertEquals(6, updated.scheduleEndHour)
        assertEquals(45, updated.scheduleEndMinute)
        assertEquals("1,3,5", updated.scheduleDaysOfWeek)
    }

    @Test
    fun `el horario se formatea con ceros`() {
        val value = limit(
            scheduleStartHour = 7,
            scheduleStartMinute = 5,
            scheduleEndHour = 9,
            scheduleEndMinute = 0
        )

        assertEquals("07:05 - 09:00", value.getScheduleFormatted())
    }

    @Test
    fun `los dias invalidos se ignoran al mostrar el horario`() {
        val value = limit(scheduleDaysOfWeek = "1, 3, basura, 8, 7")

        assertEquals("Lun, Mié, Dom", value.getScheduleDaysAsText())
    }

    @Test
    fun `una excedencia de hoy bloquea la edicion`() {
        val middayToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        assertTrue(limit(lastExceededAt = middayToday).isEditBlocked())
    }

    @Test
    fun `sin excedencia o con una antigua se permite editar`() {
        assertFalse(limit().isEditBlocked())
        assertNull(limit().lastExceededAt)
        assertFalse(limit(lastExceededAt = 0L).isEditBlocked())
    }

    @Test
    fun `marcar excedencia devuelve una copia y no muta el original`() {
        val before = System.currentTimeMillis()
        val original = limit()
        val exceeded = original.withExceeded()
        val after = System.currentTimeMillis()

        assertNull(original.lastExceededAt)
        assertTrue(exceeded.lastExceededAt in before..after)
        assertTrue(exceeded.updatedAt in before..after)
    }

    private fun limit(
        hasScheduleLimit: Boolean = false,
        scheduleStartHour: Int = 9,
        scheduleStartMinute: Int = 0,
        scheduleEndHour: Int = 17,
        scheduleEndMinute: Int = 0,
        scheduleDaysOfWeek: String = "1,2,3,4,5",
        lastExceededAt: Long? = null
    ) = AppLimit(
        packageName = "com.example.app",
        appName = "Example",
        dailyLimitMinutes = 30,
        hasScheduleLimit = hasScheduleLimit,
        scheduleStartHour = scheduleStartHour,
        scheduleStartMinute = scheduleStartMinute,
        scheduleEndHour = scheduleEndHour,
        scheduleEndMinute = scheduleEndMinute,
        scheduleDaysOfWeek = scheduleDaysOfWeek,
        lastExceededAt = lastExceededAt
    )
}
