package com.momentummm.app.data.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserSettingsGamificationTest {

    @Test
    fun `el xp requerido sigue la formula de niveles`() {
        assertEquals(80, UserSettings.getXpForLevel(1))
        assertEquals(320, UserSettings.getXpForLevel(2))
        assertEquals(720, UserSettings.getXpForLevel(3))
    }

    @Test
    fun `el xp requerido crece con cada nivel`() {
        val requirements = (1..20).map(UserSettings::getXpForLevel)

        assertTrue(requirements.zipWithNext().all { (current, next) -> next > current })
    }

    @Test
    fun `los umbrales de racha aplican el multiplicador correcto`() {
        assertEquals(1.0f, UserSettings.getStreakMultiplier(0), 0f)
        assertEquals(1.25f, UserSettings.getStreakMultiplier(3), 0f)
        assertEquals(1.5f, UserSettings.getStreakMultiplier(7), 0f)
        assertEquals(1.75f, UserSettings.getStreakMultiplier(14), 0f)
        assertEquals(2.0f, UserSettings.getStreakMultiplier(30), 0f)
    }

    @Test
    fun `el progreso empieza en cero al alcanzar un nivel`() {
        val settings = settings(
            userLevel = 2,
            currentXp = UserSettings.getXpForLevel(1)
        )

        assertEquals(0f, settings.getLevelProgress(), 0.0001f)
    }

    @Test
    fun `el progreso intermedio se calcula dentro del tramo actual`() {
        val currentLevelXp = UserSettings.getXpForLevel(1)
        val nextLevelXp = UserSettings.getXpForLevel(2)
        val midpoint = currentLevelXp + (nextLevelXp - currentLevelXp) / 2

        assertEquals(
            0.5f,
            settings(userLevel = 2, currentXp = midpoint).getLevelProgress(),
            0.0001f
        )
    }

    @Test
    fun `el progreso se acota entre cero y uno`() {
        assertEquals(0f, settings(userLevel = 2, currentXp = -100).getLevelProgress(), 0f)
        assertEquals(1f, settings(userLevel = 2, currentXp = 10_000).getLevelProgress(), 0f)
    }

    @Test
    fun `el xp restante usa el objetivo del nivel actual`() {
        val settings = settings(userLevel = 3, currentXp = 500)

        assertEquals(220, settings.getXpToNextLevel())
    }

    private fun settings(userLevel: Int, currentXp: Int) = UserSettings(
        birthDate = null,
        userLevel = userLevel,
        currentXp = currentXp
    )
}
