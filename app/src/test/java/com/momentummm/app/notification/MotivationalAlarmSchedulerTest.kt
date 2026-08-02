package com.momentummm.app.notification

import com.momentummm.app.data.entity.MotivationalPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.util.Calendar
import org.junit.Test

/**
 * Verifica que las preferencias del usuario se traducen realmente en horarios
 * de notificación.
 *
 * Este es el punto exacto donde estaba el bug: el programador anterior fijaba
 * un trabajo periódico cada 2 horas y las horas 8:00/21:00 a fuego, así que
 * `dailyFrequency`, `startHour` y `endHour` no tenían ningún efecto por mucho
 * que el usuario los cambiara en los ajustes.
 */
class MotivationalAlarmSchedulerTest {

    private fun weekday(): Calendar = Calendar.getInstance().apply {
        // 2026-08-03 es lunes: evita que la lógica de fin de semana interfiera.
        set(2026, Calendar.AUGUST, 3, 12, 0, 0)
    }

    private fun saturday(): Calendar = Calendar.getInstance().apply {
        set(2026, Calendar.AUGUST, 1, 12, 0, 0)
    }

    @Test
    fun `la frecuencia diaria determina el numero de mensajes`() {
        val threePerDay = MotivationalPreferences(dailyFrequency = 3)
        val eightPerDay = MotivationalPreferences(dailyFrequency = 8)

        assertEquals(
            3,
            MotivationalAlarmScheduler.periodicSlotsInMinutes(threePerDay, weekday()).size
        )
        assertEquals(
            8,
            MotivationalAlarmScheduler.periodicSlotsInMinutes(eightPerDay, weekday()).size
        )
    }

    @Test
    fun `todas las franjas caen dentro del rango horario configurado`() {
        val preferences = MotivationalPreferences(
            dailyFrequency = 6,
            startHour = 10,
            startMinute = 0,
            endHour = 20,
            endMinute = 0
        )

        val slots = MotivationalAlarmScheduler.periodicSlotsInMinutes(preferences, weekday())

        assertEquals(6, slots.size)
        slots.forEach { minuteOfDay ->
            assertTrue(
                "La franja $minuteOfDay queda fuera de 10:00-20:00",
                minuteOfDay in (10 * 60)..(20 * 60)
            )
        }
    }

    @Test
    fun `cambiar el rango horario cambia las franjas`() {
        val morningWindow = MotivationalPreferences(dailyFrequency = 4, startHour = 6, endHour = 12)
        val eveningWindow = MotivationalPreferences(dailyFrequency = 4, startHour = 16, endHour = 22)

        val morningSlots = MotivationalAlarmScheduler.periodicSlotsInMinutes(morningWindow, weekday())
        val eveningSlots = MotivationalAlarmScheduler.periodicSlotsInMinutes(eveningWindow, weekday())

        assertTrue("Las franjas de mañana deben acabar antes del mediodía", morningSlots.max() <= 12 * 60)
        assertTrue("Las franjas de tarde deben empezar después de las 16:00", eveningSlots.min() >= 16 * 60)
    }

    @Test
    fun `las franjas estan ordenadas y no se repiten`() {
        val preferences = MotivationalPreferences(dailyFrequency = 8, startHour = 8, endHour = 22)

        val slots = MotivationalAlarmScheduler.periodicSlotsInMinutes(preferences, weekday())

        assertEquals("No debe haber franjas duplicadas", slots.distinct().size, slots.size)
        assertEquals("Las franjas deben venir ordenadas", slots.sorted(), slots)
    }

    @Test
    fun `el horario de fin de semana se aplica solo si esta activado`() {
        val withWeekendSchedule = MotivationalPreferences(
            dailyFrequency = 8,
            differentWeekendSchedule = true,
            weekendFrequency = 2,
            weekendStartHour = 11,
            weekendEndHour = 19
        )

        val saturdaySlots = MotivationalAlarmScheduler.periodicSlotsInMinutes(withWeekendSchedule, saturday())
        val mondaySlots = MotivationalAlarmScheduler.periodicSlotsInMinutes(withWeekendSchedule, weekday())

        assertEquals("El sábado usa la frecuencia de fin de semana", 2, saturdaySlots.size)
        assertEquals("El lunes usa la frecuencia normal", 8, mondaySlots.size)
        saturdaySlots.forEach { minuteOfDay ->
            assertTrue(
                "La franja de sábado $minuteOfDay queda fuera de 11:00-19:00",
                minuteOfDay in (11 * 60)..(19 * 60)
            )
        }
    }

    @Test
    fun `el horario de fin de semana se ignora cuando esta desactivado`() {
        val sharedSchedule = MotivationalPreferences(
            dailyFrequency = 5,
            differentWeekendSchedule = false,
            weekendFrequency = 1
        )

        val saturdaySlots = MotivationalAlarmScheduler.periodicSlotsInMinutes(sharedSchedule, saturday())

        assertEquals(5, saturdaySlots.size)
    }

    @Test
    fun `una frecuencia fuera de rango se acota en lugar de romper`() {
        val absurd = MotivationalPreferences(dailyFrequency = 500)

        val slots = MotivationalAlarmScheduler.periodicSlotsInMinutes(absurd, weekday())

        assertTrue("La frecuencia debe acotarse", slots.size <= 12)
        assertTrue("Debe producir al menos una franja", slots.isNotEmpty())
    }

    @Test
    fun `un rango que cruza medianoche sigue produciendo franjas`() {
        // endHour < startHour: antes esto podía dejar al usuario sin mensajes.
        val overnight = MotivationalPreferences(dailyFrequency = 3, startHour = 22, endHour = 6)

        val slots = MotivationalAlarmScheduler.periodicSlotsInMinutes(overnight, weekday())

        assertTrue("Debe programar algo aunque el rango cruce medianoche", slots.isNotEmpty())
        slots.forEach { minuteOfDay ->
            assertTrue("La franja $minuteOfDay debe ser un minuto válido del día", minuteOfDay in 0 until 24 * 60)
        }
    }
}
