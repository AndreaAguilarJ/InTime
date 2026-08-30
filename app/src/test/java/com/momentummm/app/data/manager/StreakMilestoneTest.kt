package com.momentummm.app.data.manager

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueba del predicado de hito de racha.
 *
 * Decide en qué días de racha se emite la notificación real (con el número
 * verdadero de días). El usuario pidió que llegar a p. ej. 3 días produzca una
 * notificación real; este predicado es el que la habilita ese día sin spamear
 * cada día.
 */
class StreakMilestoneTest {

    @Test
    fun `dia 3 es hito (el ejemplo del usuario)`() {
        assertTrue(GamificationManager.isStreakMilestone(3))
    }

    @Test
    fun `dia 1 es hito (racha iniciada)`() {
        assertTrue(GamificationManager.isStreakMilestone(1))
    }

    @Test
    fun `cada semana cumplida es hito`() {
        assertTrue(GamificationManager.isStreakMilestone(7))
        assertTrue(GamificationManager.isStreakMilestone(14))
        assertTrue(GamificationManager.isStreakMilestone(28))
        assertTrue(GamificationManager.isStreakMilestone(70))
    }

    @Test
    fun `dias intermedios no son hito (no spamear)`() {
        assertFalse(GamificationManager.isStreakMilestone(2))
        assertFalse(GamificationManager.isStreakMilestone(4))
        assertFalse(GamificationManager.isStreakMilestone(5))
        assertFalse(GamificationManager.isStreakMilestone(6))
        assertFalse(GamificationManager.isStreakMilestone(8))
        assertFalse(GamificationManager.isStreakMilestone(13))
    }

    @Test
    fun `cero y negativos no son hito`() {
        assertFalse(GamificationManager.isStreakMilestone(0))
        assertFalse(GamificationManager.isStreakMilestone(-3))
    }
}
