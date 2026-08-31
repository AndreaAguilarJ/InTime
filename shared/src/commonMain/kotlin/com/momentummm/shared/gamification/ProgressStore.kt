package com.momentummm.shared.gamification

import com.momentummm.shared.data.SettingsStore
import kotlinx.datetime.LocalDate

/**
 * Progreso de la persona: experiencia, monedas y racha de días.
 *
 * Guarda solo lo que se acumula --experiencia, monedas, racha y el último día
 * activo-- y deriva de ahí el nivel y la barra. La fecha de referencia se recibe
 * como parámetro en vez de leerse del reloj, para que la racha se pueda comprobar
 * sin esperar días.
 */
class ProgressStore(private val settings: SettingsStore) {

    data class Progress(
        val totalXp: Int,
        val coins: Int,
        val streakDays: Int,
        val lastActiveDate: LocalDate?,
    ) {
        val level: Int get() = Gamification.levelFor(totalXp)
        val levelProgress: Float get() = Gamification.levelProgress(totalXp)
        val xpToNextLevel: Int get() = Gamification.xpToNextLevel(totalXp)
        val streakMultiplier: Float get() = Gamification.streakMultiplier(streakDays)
    }

    /** Lo que cambió al registrar una sesión, para poder contarlo en pantalla. */
    data class SessionOutcome(
        val progress: Progress,
        val earned: Gamification.Reward,
        val streakGrew: Boolean,
        val streakBroken: Boolean,
        val leveledUp: Boolean,
    )

    fun read(): Progress = Progress(
        totalXp = settings.getLongOrNull(KEY_XP)?.toInt() ?: 0,
        coins = settings.getLongOrNull(KEY_COINS)?.toInt() ?: 0,
        streakDays = settings.getLongOrNull(KEY_STREAK)?.toInt() ?: 0,
        lastActiveDate = settings.getStringOrNull(KEY_LAST_ACTIVE)?.let { texto ->
            runCatching { LocalDate.parse(texto) }.getOrNull()
        },
    )

    /**
     * Registra una sesión de enfoque terminada y devuelve lo que cambió.
     *
     * La racha se resuelve antes de repartir el premio, porque el multiplicador
     * de la racha NUEVA es el que debe aplicarse: quien completa el séptimo día
     * seguido merece ya el multiplicador del séptimo, no el del sexto.
     */
    fun recordFocusSession(
        minutes: Int,
        completedWholeSession: Boolean,
        today: LocalDate,
    ): SessionOutcome {
        val antes = read()
        val nivelAntes = antes.level

        val diasPrevios = antes.streakDays
        val ultimo = antes.lastActiveDate
        val rachaNueva = when {
            ultimo == null -> 1
            ultimo == today -> diasPrevios.coerceAtLeast(1)
            ultimo.plusDays(1) == today -> diasPrevios + 1
            else -> 1
        }
        val rachaCrecio = rachaNueva > diasPrevios
        // Solo se considera rota si había una racha viva de más de un día y el
        // último día activo no es ni hoy ni ayer.
        val rachaRota = diasPrevios > 1 && rachaNueva == 1

        var premio = Gamification.rewardForFocusMinutes(minutes, rachaNueva)
        if (completedWholeSession) {
            premio += Gamification.rewardForSessionCompleted(rachaNueva)
        }
        if (rachaCrecio && ultimo != null) {
            premio += Gamification.rewardForStreakDay(rachaNueva)
        }

        val baseXp = if (rachaRota) Gamification.xpAfterStreakBreak(antes.totalXp) else antes.totalXp
        val xpFinal = baseXp + premio.xp
        val monedasFinal = antes.coins + premio.coins

        settings.putLong(KEY_XP, xpFinal.toLong())
        settings.putLong(KEY_COINS, monedasFinal.toLong())
        settings.putLong(KEY_STREAK, rachaNueva.toLong())
        settings.putString(KEY_LAST_ACTIVE, today.toString())

        val despues = Progress(xpFinal, monedasFinal, rachaNueva, today)
        return SessionOutcome(
            progress = despues,
            earned = premio,
            streakGrew = rachaCrecio,
            streakBroken = rachaRota,
            leveledUp = despues.level > nivelAntes,
        )
    }

    private companion object {
        const val KEY_XP = "player_total_xp"
        const val KEY_COINS = "player_coins"
        const val KEY_STREAK = "player_streak_days"
        const val KEY_LAST_ACTIVE = "player_last_active_date"
    }
}

/** Suma días a una fecha sin depender de la zona horaria del sistema. */
private fun LocalDate.plusDays(days: Int): LocalDate =
    LocalDate.fromEpochDays(toEpochDays() + days)
