package com.momentummm.app.notification

import com.momentummm.app.data.entity.MessageCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Un mensaje periódico no puede contradecir lo que vive el usuario.
 *
 * El fallo de partida: se sorteaba entre todas las categorías activas y el
 * título sale de la categoría, así que llegaban «Buenos días» a las 18:00,
 * «¡Nuevo lunes!» un viernes o «Tu racha» con la racha a cero.
 */
class MessageRelevanceTest {

    private val fridayAfternoon = MessageRelevance.Moment(
        hourOfDay = 18, dayOfWeek = Calendar.FRIDAY, streakDays = 0
    )
    private val mondayMorning = MessageRelevance.Moment(
        hourOfDay = 8, dayOfWeek = Calendar.MONDAY, streakDays = 5
    )

    @Test
    fun `por la tarde de un viernes no salen buenos dias, lunes, fin de semana ni racha`() {
        val categories = MessageRelevance.periodicCategories(MessageCategory.entries, fridayAfternoon)

        assertFalse(MessageCategory.MORNING in categories)
        assertFalse(MessageCategory.MONDAY in categories)
        assertFalse(MessageCategory.WEEKEND in categories)
        assertFalse("Sin racha no se habla de racha", MessageCategory.STREAK in categories)
        assertTrue(MessageCategory.MOTIVATION in categories)
    }

    @Test
    fun `un lunes por la manana si encajan buenos dias, lunes y la racha que existe`() {
        val categories = MessageRelevance.periodicCategories(MessageCategory.entries, mondayMorning)

        assertTrue(MessageCategory.MORNING in categories)
        assertTrue(MessageCategory.MONDAY in categories)
        assertTrue(MessageCategory.STREAK in categories)
        assertFalse(MessageCategory.EVENING in categories)
    }

    @Test
    fun `los mensajes de evento nunca se sortean como periodicos`() {
        val categories = MessageRelevance.periodicCategories(MessageCategory.entries, mondayMorning)

        MessageRelevance.EVENT_ONLY.forEach {
            assertFalse("$it solo debe llegar cuando ocurre el evento", it in categories)
        }
    }

    @Test
    fun `si nada de lo elegido encaja se usan las universales en vez de mentir`() {
        val onlyMorning = listOf(MessageCategory.MORNING)

        val categories = MessageRelevance.periodicCategories(onlyMorning, fridayAfternoon)

        assertFalse(MessageCategory.MORNING in categories)
        assertTrue(categories.isNotEmpty())
        assertTrue(categories.all { it in MessageRelevance.UNIVERSAL })
    }

    @Test
    fun `se respeta lo que el usuario eligio cuando encaja`() {
        val chosen = listOf(MessageCategory.MINDFULNESS, MessageCategory.MORNING)

        val categories = MessageRelevance.periodicCategories(chosen, fridayAfternoon)

        assertEquals(listOf(MessageCategory.MINDFULNESS), categories)
    }

    @Test
    fun `la noche cubre de las 19 a la 1 y no la tarde`() {
        fun fits(hour: Int) = MessageRelevance.fitsMoment(
            MessageCategory.EVENING, MessageRelevance.Moment(hour, Calendar.TUESDAY)
        )
        assertFalse(fits(18))
        assertTrue(fits(19))
        assertTrue(fits(23))
        assertTrue(fits(1))
        assertFalse(fits(2))
    }

    // ─── Horario inteligente ─────────────────────────────────────────────

    @Test
    fun `con horario inteligente no se interrumpe una sesion de enfoque`() {
        val reason = MessageRelevance.deferral(
            smartTimingEnabled = true, respectFocusMode = true, respectDnd = true,
            focusSessionActive = true, doNotDisturbActive = false
        )
        assertEquals(MessageRelevance.Deferral.FOCUS_SESSION, reason)
    }

    @Test
    fun `con horario inteligente se respeta No molestar`() {
        val reason = MessageRelevance.deferral(
            smartTimingEnabled = true, respectFocusMode = true, respectDnd = true,
            focusSessionActive = false, doNotDisturbActive = true
        )
        assertEquals(MessageRelevance.Deferral.DO_NOT_DISTURB, reason)
    }

    @Test
    fun `sin horario inteligente el mensaje sale aunque haya enfoque`() {
        assertNull(
            MessageRelevance.deferral(
                smartTimingEnabled = false, respectFocusMode = true, respectDnd = true,
                focusSessionActive = true, doNotDisturbActive = true
            )
        )
    }

    @Test
    fun `sin enfoque ni No molestar el mensaje sale`() {
        assertNull(
            MessageRelevance.deferral(
                smartTimingEnabled = true, respectFocusMode = true, respectDnd = true,
                focusSessionActive = false, doNotDisturbActive = false
            )
        )
    }
}
