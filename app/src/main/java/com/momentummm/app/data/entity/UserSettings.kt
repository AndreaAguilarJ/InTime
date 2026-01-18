package com.momentummm.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val id: Int = 1,
    val birthDate: Date?,
    val isOnboardingCompleted: Boolean = false,
    val hasSeenTutorial: Boolean = false,
    val livedWeeksColor: String = "#6366F1",
    val futureWeeksColor: String = "#E5E7EB",
    val backgroundColor: String = "#FFFFFF",
    
    // === GAMIFICATION FIELDS ===
    // Sistema de niveles y XP para retención
    val userLevel: Int = 1,
    val currentXp: Int = 0,
    val totalXp: Int = 0,
    val timeCoins: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActiveDate: Date? = null,
    val totalFocusMinutes: Int = 0,
    val totalSessionsCompleted: Int = 0,
    val perfectDaysCount: Int = 0,  // Días sin romper ningún límite
    
    // === GAMIFICATION SETTINGS ===
    val gamificationEnabled: Boolean = true,
    val showXpNotifications: Boolean = true,
    val showStreakReminders: Boolean = true
) {
    companion object {
        // XP necesario para cada nivel (fórmula exponencial)
        fun getXpForLevel(level: Int): Int {
            return (100 * level * level * 0.8).toInt()
        }

        // XP ganado por minuto de foco
        const val XP_PER_FOCUS_MINUTE = 2
        
        // TimeCoins por minuto de foco
        const val COINS_PER_FOCUS_MINUTE = 1
        
        // Bonus por completar sesión completa
        const val XP_SESSION_BONUS = 50
        const val COINS_SESSION_BONUS = 25
        
        // Bonus por día perfecto (sin romper límites)
        const val XP_PERFECT_DAY_BONUS = 100
        const val COINS_PERFECT_DAY_BONUS = 50
        
        // Penalización por romper racha (Loss Aversion)
        const val XP_STREAK_BREAK_PENALTY = 75
        
        // Multiplicadores de racha
        fun getStreakMultiplier(streakDays: Int): Float {
            return when {
                streakDays >= 30 -> 2.0f
                streakDays >= 14 -> 1.75f
                streakDays >= 7 -> 1.5f
                streakDays >= 3 -> 1.25f
                else -> 1.0f
            }
        }
    }

    /**
     * Calcula el progreso hacia el siguiente nivel (0.0 - 1.0)
     */
    fun getLevelProgress(): Float {
        val xpForCurrentLevel = getXpForLevel(userLevel - 1)
        val xpForNextLevel = getXpForLevel(userLevel)
        val xpInCurrentLevel = currentXp - xpForCurrentLevel
        val xpNeededForNext = xpForNextLevel - xpForCurrentLevel
        return (xpInCurrentLevel.toFloat() / xpNeededForNext.toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Retorna el XP necesario para el siguiente nivel
     */
    fun getXpToNextLevel(): Int {
        return getXpForLevel(userLevel) - currentXp
    }

    /**
     * Título del nivel actual
     */
    fun getLevelTitle(): String {
        return when (userLevel) {
            1 -> "Novato"
            2 -> "Aprendiz"
            3 -> "Enfocado"
            4 -> "Disciplinado"
            5 -> "Guerrero del Tiempo"
            6 -> "Maestro del Foco"
            7 -> "Leyenda Productiva"
            8 -> "Titán del Tiempo"
            9 -> "Guardián Supremo"
            else -> "Gran Maestro Lv.$userLevel"
        }
    }

    /**
     * Emoji del nivel actual
     */
    fun getLevelEmoji(): String {
        return when (userLevel) {
            1 -> "🌱"
            2 -> "🌿"
            3 -> "🎯"
            4 -> "⚔️"
            5 -> "🛡️"
            6 -> "👑"
            7 -> "🏆"
            8 -> "⚡"
            9 -> "🔥"
            else -> "💎"
        }
    }
}