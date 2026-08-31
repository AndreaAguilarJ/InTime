package com.momentummm.shared.gamification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GamificationTest {

    @Test
    fun `el primer nivel empieza en uno y a los 80 puntos se sube al segundo`() {
        assertEquals(1, Gamification.levelFor(0))
        assertEquals(1, Gamification.levelFor(79))
        assertEquals(2, Gamification.levelFor(80))
        assertEquals(2, Gamification.levelFor(319))
        assertEquals(3, Gamification.levelFor(320))
    }

    @Test
    fun `nivel y umbral son inversos entre si`() {
        for (nivel in 1..20) {
            val umbral = Gamification.xpForLevel(nivel - 1)
            assertEquals(nivel, Gamification.levelFor(umbral), "umbral de nivel $nivel")
            if (umbral > 0) {
                assertEquals(nivel - 1, Gamification.levelFor(umbral - 1))
            }
        }
    }

    @Test
    fun `una experiencia negativa no rompe el nivel`() {
        assertEquals(1, Gamification.levelFor(-500))
        assertEquals(0f, Gamification.levelProgress(-500))
    }

    @Test
    fun `lo que falta para el siguiente nivel cuadra con la barra`() {
        // Cifra tomada de la app real: con 50 puntos faltan 30 para el nivel 2.
        assertEquals(30, Gamification.xpToNextLevel(50))
        assertEquals(1, Gamification.levelFor(50))
        assertEquals(50f / 80f, Gamification.levelProgress(50))
    }

    @Test
    fun `justo al subir de nivel la barra vuelve a empezar, no se queda llena`() {
        assertEquals(0f, Gamification.levelProgress(80))
        assertEquals(2, Gamification.levelFor(80))
        assertEquals(240, Gamification.xpToNextLevel(80))
    }

    @Test
    fun `el multiplicador de racha sube por tramos`() {
        assertEquals(1.0f, Gamification.streakMultiplier(0))
        assertEquals(1.0f, Gamification.streakMultiplier(2))
        assertEquals(1.25f, Gamification.streakMultiplier(3))
        assertEquals(1.5f, Gamification.streakMultiplier(7))
        assertEquals(1.75f, Gamification.streakMultiplier(14))
        assertEquals(2.0f, Gamification.streakMultiplier(30))
        assertEquals(2.0f, Gamification.streakMultiplier(365))
    }

    @Test
    fun `una sesion abandonada al instante no premia`() {
        assertEquals(Gamification.Reward(0, 0), Gamification.rewardForFocusMinutes(0, 10))
        assertEquals(Gamification.Reward(0, 0), Gamification.rewardForFocusMinutes(-5, 10))
    }

    @Test
    fun `un pomodoro sin racha da 125 de experiencia y 25 monedas`() {
        val enfoque = Gamification.rewardForFocusMinutes(25, streakDays = 0)
        assertEquals(125, enfoque.xp)
        assertEquals(25, enfoque.coins)
    }

    @Test
    fun `la racha multiplica el premio del mismo pomodoro`() {
        val sinRacha = Gamification.rewardForFocusMinutes(25, streakDays = 0)
        val conRacha = Gamification.rewardForFocusMinutes(25, streakDays = 30)
        assertEquals(sinRacha.xp * 2, conRacha.xp)
        assertEquals(sinRacha.coins * 2, conRacha.coins)
    }

    @Test
    fun `los premios se pueden sumar`() {
        val total = Gamification.rewardForFocusMinutes(25, 0) +
            Gamification.rewardForSessionCompleted(0)
        assertEquals(125 + 150, total.xp)
        assertEquals(25 + 30, total.coins)
    }

    @Test
    fun `romper la racha no deja la experiencia en negativo`() {
        assertEquals(0, Gamification.xpAfterStreakBreak(0))
        assertEquals(0, Gamification.xpAfterStreakBreak(40))
        assertEquals(25, Gamification.xpAfterStreakBreak(100))
    }

    @Test
    fun `el dia perfecto es el premio mas alto sin racha`() {
        val perfecto = Gamification.rewardForPerfectDay()
        assertTrue(perfecto.xp > Gamification.rewardForSessionCompleted(0).xp)
        assertTrue(perfecto.xp > Gamification.rewardForFocusMinutes(60, 0).xp)
    }

    @Test
    fun `la barra de progreso se mantiene entre cero y uno en todo el rango`() {
        var xp = 0
        while (xp < 200_000) {
            val progreso = Gamification.levelProgress(xp)
            assertTrue(progreso in 0f..1f, "progreso fuera de rango con xp=$xp")
            xp += 37
        }
    }
}
