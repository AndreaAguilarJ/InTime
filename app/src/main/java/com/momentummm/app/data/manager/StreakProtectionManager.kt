package com.momentummm.app.data.manager

import android.util.Log
import com.momentummm.app.data.dao.SmartBlockingConfigDao
import com.momentummm.app.data.entity.SmartBlockingConfig
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * PROTECCIÓN DE RACHAS — consumo real de días de gracia
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * POR QUÉ EXISTE ESTA CLASE
 *
 * La pantalla de Bloqueo inteligente ofrecía «días de gracia para que no
 * pierdas tu racha», guardaba cuántos querías y mostraba cuántos habías
 * gastado. Nada de eso tenía efecto: `useGraceDay()` y
 * `getGraceDaysRemaining()` vivían en [SmartBlockingManager] y NINGÚN archivo
 * del proyecto los llamaba. El contador de días usados no subía nunca, así que
 * los días parecían no gastarse porque literalmente no se gastaban.
 *
 * El problema de fondo era de arquitectura: la racha la gestiona
 * [GamificationManager] contra `user_settings`, y la protección vivía en el
 * subsistema de bloqueo. Eran dos mitades sin puente.
 *
 * Esta clase es ese puente, y depende sólo del DAO de configuración para que
 * [GamificationManager] pueda usarla sin crear un ciclo de inyección con
 * [SmartBlockingManager].
 */
@Singleton
class StreakProtectionManager @Inject constructor(
    private val configDao: SmartBlockingConfigDao
) {

    private val tag = "StreakProtection"

    /** Resultado de intentar rescatar una racha. */
    sealed interface Outcome {
        /** Se consumió un día de gracia; la racha se conserva. */
        data class GraceUsed(val remainingAfter: Int) : Outcome

        /** El usuario no activó la protección. */
        data object Disabled : Outcome

        /** Protección activa pero sin días disponibles esta semana. */
        data object NoGraceLeft : Outcome
    }

    /**
     * Intenta consumir un día de gracia para salvar la racha.
     *
     * Antes de decidir aplica el reinicio semanal pendiente, de modo que la
     * primera comprobación de una semana nueva ya vea el cupo restaurado.
     */
    suspend fun tryConsumeGraceDay(): Outcome {
        resetWeeklyGraceDaysIfNeeded()

        val config = configDao.getConfigSync() ?: return Outcome.Disabled
        if (!config.streakProtectionEnabled) return Outcome.Disabled

        val used = config.graceDaysUsedThisWeek
        val allowed = config.graceDaysPerWeek
        if (used >= allowed) return Outcome.NoGraceLeft

        val newUsed = used + 1
        configDao.updateConfig(
            config.copy(
                graceDaysUsedThisWeek = newUsed,
                updatedAt = System.currentTimeMillis()
            )
        )
        val remaining = (allowed - newUsed).coerceAtLeast(0)
        Log.d(tag, "Día de gracia consumido; quedan $remaining esta semana")
        return Outcome.GraceUsed(remaining)
    }

    /** Días de gracia que quedan esta semana. */
    suspend fun graceDaysRemaining(): Int {
        val config = configDao.getConfigSync() ?: return 0
        if (!config.streakProtectionEnabled) return 0
        return (config.graceDaysPerWeek - config.graceDaysUsedThisWeek).coerceAtLeast(0)
    }

    /**
     * Reinicia el cupo si empezó una semana nueva.
     *
     * BUG CORREGIDO: la comprobación anterior comparaba `WEEK_OF_YEAR` y, por
     * separado, `Calendar.YEAR`. Una misma semana puede abarcar diciembre y
     * enero, así que al cambiar el año calendario el cupo se reiniciaba por
     * segunda vez dentro de la misma semana y regalaba días extra.
     *
     * Ahora se compara el instante normalizado de INICIO de semana, que es
     * único por semana y no depende de la numeración ni del año.
     */
    suspend fun resetWeeklyGraceDaysIfNeeded() {
        val config = configDao.getConfigSync() ?: return
        val lastReset = config.lastGraceDayResetDate
        val currentWeekStart = startOfWeek(Date())

        val needsReset = lastReset == null || startOfWeek(lastReset) != currentWeekStart
        if (!needsReset) return

        configDao.updateConfig(
            config.copy(
                graceDaysUsedThisWeek = 0,
                lastGraceDayResetDate = Date(currentWeekStart),
                updatedAt = System.currentTimeMillis()
            )
        )
        Log.d(tag, "Cupo semanal de días de gracia reiniciado")
    }

    /**
     * Instante de medianoche del primer día de la semana que contiene [date].
     *
     * Se retrocede día a día hasta el primer día de la semana del locale en vez
     * de asignar `DAY_OF_WEEK` directamente: asignarlo puede mover la fecha
     * hacia DELANTE, lo que colocaría el inicio de semana en el futuro.
     */
    internal fun startOfWeek(date: Date): Long = Companion.startOfWeek(date)

    /**
     * ¿Hay que avisar al usuario de que está a punto de romper su racha?
     *
     * BUG CORREGIDO: el predicado anterior no consultaba
     * `streakProtectionEnabled`, así que el aviso llegaba incluso con la
     * Protección de rachas apagada.
     */
    fun shouldWarn(
        config: SmartBlockingConfig,
        currentUsageMinutes: Int,
        limitMinutes: Int
    ): Boolean = Companion.shouldWarn(config, currentUsageMinutes, limitMinutes)

    /**
     * Las dos funciones puras viven en el companion para poder probarlas sin
     * construir la clase, que necesitaría un doble del DAO de Room.
     */
    companion object {

        internal fun startOfWeek(date: Date): Long {
            val calendar = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            while (calendar.get(Calendar.DAY_OF_WEEK) != calendar.firstDayOfWeek) {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }
            return calendar.timeInMillis
        }

        internal fun shouldWarn(
            config: SmartBlockingConfig,
            currentUsageMinutes: Int,
            limitMinutes: Int
        ): Boolean {
            if (!config.streakProtectionEnabled) return false
            if (!config.warningBeforeStreakBreak) return false
            val remaining = limitMinutes - currentUsageMinutes
            return remaining in 1..config.warningMinutesBeforeLimit
        }
    }
}
