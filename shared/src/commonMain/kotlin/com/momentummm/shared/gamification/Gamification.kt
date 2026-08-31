package com.momentummm.shared.gamification

import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Reglas de progreso de Momentum: experiencia, monedas, niveles y racha.
 *
 * Son las mismas cifras que usa la app Android, extraídas aquí para que iOS
 * premie exactamente igual y para que las reglas se puedan comprobar sin
 * teléfono ni base de datos.
 *
 * Una diferencia deliberada con la versión Android: allí el nivel se guarda en un
 * campo aparte de la experiencia, así que ambos pueden desincronizarse y el
 * progreso de la barra se calcula sobre el nivel guardado. Aquí el nivel se
 * DERIVA de la experiencia, que es el único dato que se acumula: dos cifras que
 * deben concordar no pueden discrepar si solo se almacena una.
 */
object Gamification {

    /** Experiencia por cada minuto de enfoque completado. */
    const val XP_PER_FOCUS_MINUTE = 5

    /** Monedas por cada minuto de enfoque completado. */
    const val COINS_PER_FOCUS_MINUTE = 1

    /** Premio extra por terminar la sesión entera, no solo parte. */
    const val XP_SESSION_BONUS = 150
    const val COINS_SESSION_BONUS = 30

    /** Premio por sumar un día más de racha. */
    const val XP_PER_STREAK_DAY = 40
    const val COINS_PER_STREAK_DAY = 8

    /** Premio por un día sin saltarse ningún límite. */
    const val XP_PERFECT_DAY_BONUS = 500
    const val COINS_PERFECT_DAY_BONUS = 100

    /** Coste de romper la racha. */
    const val XP_STREAK_BREAK_PENALTY = 75

    /**
     * Experiencia acumulada necesaria para haber alcanzado [level].
     *
     * Es cuadrática: cada nivel cuesta más que el anterior, así que el progreso
     * temprano se siente rápido y el tardío tiene valor.
     */
    fun xpForLevel(level: Int): Int {
        if (level <= 0) return 0
        return (100.0 * level * level * 0.8).toInt()
    }

    /** Nivel que corresponde a una experiencia acumulada. El primero es 1. */
    fun levelFor(totalXp: Int): Int {
        if (totalXp <= 0) return 1
        // Invierte xpForLevel: nivel = raíz(xp / 80) redondeado a la baja, +1.
        return floor(sqrt(totalXp / 80.0)).toInt() + 1
    }

    /** Fracción recorrida del nivel actual, entre 0 y 1. */
    fun levelProgress(totalXp: Int): Float {
        val nivel = levelFor(totalXp)
        val inicio = xpForLevel(nivel - 1)
        val fin = xpForLevel(nivel)
        val tramo = fin - inicio
        if (tramo <= 0) return 0f
        return ((totalXp - inicio).toFloat() / tramo).coerceIn(0f, 1f)
    }

    /** Experiencia que falta para el siguiente nivel. Nunca negativa. */
    fun xpToNextLevel(totalXp: Int): Int =
        (xpForLevel(levelFor(totalXp)) - totalXp).coerceAtLeast(0)

    /**
     * Multiplicador por racha. Premia la constancia por tramos y no de forma
     * continua, para que llegar a un tramo se note.
     */
    fun streakMultiplier(streakDays: Int): Float = when {
        streakDays >= 30 -> 2.0f
        streakDays >= 14 -> 1.75f
        streakDays >= 7 -> 1.5f
        streakDays >= 3 -> 1.25f
        else -> 1.0f
    }

    /** Lo ganado por una acción, ya con el multiplicador aplicado. */
    data class Reward(val xp: Int, val coins: Int) {
        operator fun plus(other: Reward) = Reward(xp + other.xp, coins + other.coins)
    }

    /**
     * Premio por los minutos de enfoque cumplidos.
     *
     * Los minutos negativos o cero no premian: un cronómetro abandonado al
     * instante no es una sesión.
     */
    fun rewardForFocusMinutes(minutes: Int, streakDays: Int): Reward {
        if (minutes <= 0) return Reward(0, 0)
        val multiplicador = streakMultiplier(streakDays)
        return Reward(
            xp = (minutes * XP_PER_FOCUS_MINUTE * multiplicador).toInt(),
            coins = (minutes * COINS_PER_FOCUS_MINUTE * multiplicador).toInt(),
        )
    }

    /** Premio extra por haber llegado al final de la sesión. */
    fun rewardForSessionCompleted(streakDays: Int): Reward {
        val multiplicador = streakMultiplier(streakDays)
        return Reward(
            xp = (XP_SESSION_BONUS * multiplicador).toInt(),
            coins = (COINS_SESSION_BONUS * multiplicador).toInt(),
        )
    }

    /** Premio por sumar un día de racha. */
    fun rewardForStreakDay(streakDays: Int): Reward {
        val multiplicador = streakMultiplier(streakDays)
        return Reward(
            xp = (XP_PER_STREAK_DAY * multiplicador).toInt(),
            coins = (COINS_PER_STREAK_DAY * multiplicador).toInt(),
        )
    }

    /** Premio por un día perfecto. No escala con la racha, ya es el mayor. */
    fun rewardForPerfectDay(): Reward = Reward(XP_PERFECT_DAY_BONUS, COINS_PERFECT_DAY_BONUS)

    /**
     * Experiencia tras romper la racha.
     *
     * La penalización nunca deja la experiencia en negativo: perder progreso ya
     * conseguido desmotiva más de lo que la penalización pretende enseñar.
     */
    fun xpAfterStreakBreak(totalXp: Int): Int =
        (totalXp - XP_STREAK_BREAK_PENALTY).coerceAtLeast(0)
}
