package com.momentummm.shared.focus

/** Fases por las que pasa una sesión de enfoque. */
enum class FocusPhase {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED,
    BREAK,
}

/**
 * Estado completo de una sesión de enfoque.
 *
 * Es inmutable: cada transición devuelve una copia nueva. Así el estado nunca
 * cambia a espaldas de quien lo está leyendo, que en una pantalla con cuenta atrás
 * es la diferencia entre un número estable y uno que parpadea.
 */
data class FocusTimerState(
    val phase: FocusPhase = FocusPhase.IDLE,
    val sessionName: String? = null,
    val totalSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val breakMinutes: Int = 0,
    /**
     * `true` mientras la sesión está en su fase de DESCANSO, incluso si está
     * pausada. La pantalla lo necesita porque [FocusPhase.PAUSED] por sí solo no
     * dice de qué fase viene, y las métricas del enfoque no deben reaparecer en
     * medio de un descanso.
     */
    val onBreak: Boolean = false,
    /** Minutos de enfoque ya acreditados, para no repartir premio dos veces. */
    val creditedMinutes: Int = 0,
) {
    val elapsedSeconds: Int get() = (totalSeconds - remainingSeconds).coerceAtLeast(0)

    /** Fracción recorrida del intervalo actual, entre 0 y 1. */
    val progress: Float
        get() = if (totalSeconds <= 0) 0f else (elapsedSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f)

    /** `true` cuando la cuenta atrás debe avanzar. */
    val isTicking: Boolean get() = phase == FocusPhase.RUNNING || phase == FocusPhase.BREAK

    val hasBreak: Boolean get() = breakMinutes > 0
}

/**
 * Reglas del cronómetro de enfoque, sin nada del sistema operativo.
 *
 * Vive aquí para que Android e iOS se comporten igual y para que las reglas se
 * puedan comprobar sin teléfono: el servicio de Android aporta la notificación
 * persistente y el reparto de XP, iOS aportará las notificaciones locales, pero
 * las transiciones son estas y solo estas.
 */
object FocusTimer {

    /** Comienza una sesión. Una duración no positiva se rechaza dejando IDLE. */
    fun start(
        sessionName: String?,
        durationMinutes: Int,
        breakMinutes: Int = 0,
    ): FocusTimerState {
        if (durationMinutes <= 0) return FocusTimerState()
        val total = durationMinutes * 60
        return FocusTimerState(
            phase = FocusPhase.RUNNING,
            sessionName = sessionName,
            totalSeconds = total,
            remainingSeconds = total,
            breakMinutes = breakMinutes.coerceAtLeast(0),
            onBreak = false,
        )
    }

    /**
     * Avanza un segundo. Solo tiene efecto en marcha o en descanso: pausada o
     * terminada, el reloj no corre.
     *
     * Al llegar a cero el enfoque pasa a COMPLETED --nunca directo al descanso--
     * porque el aviso de sesión cumplida debe verse antes, y saltar el descanso
     * es una decisión de la persona, no del reloj.
     */
    fun tick(state: FocusTimerState): FocusTimerState {
        if (!state.isTicking) return state
        val restante = state.remainingSeconds - 1
        if (restante > 0) return state.copy(remainingSeconds = restante)

        return when (state.phase) {
            FocusPhase.RUNNING -> state.copy(
                phase = FocusPhase.COMPLETED,
                remainingSeconds = 0,
                creditedMinutes = state.totalSeconds / 60,
            )
            // Terminado el descanso se vuelve a reposo: la sesión acabó del todo.
            FocusPhase.BREAK -> FocusTimerState()
            else -> state
        }
    }

    /** Pausa conservando la fase de la que viene, enfoque o descanso. */
    fun pause(state: FocusTimerState): FocusTimerState =
        if (state.isTicking) state.copy(phase = FocusPhase.PAUSED) else state

    /** Reanuda devolviendo la sesión a la fase correcta según [FocusTimerState.onBreak]. */
    fun resume(state: FocusTimerState): FocusTimerState =
        if (state.phase == FocusPhase.PAUSED) {
            state.copy(phase = if (state.onBreak) FocusPhase.BREAK else FocusPhase.RUNNING)
        } else {
            state
        }

    /** Abandona la sesión y vuelve a reposo. */
    fun stop(): FocusTimerState = FocusTimerState()

    /**
     * Arranca el descanso tras completar el enfoque.
     *
     * Solo es válido desde COMPLETED y con descanso configurado; cualquier otra
     * combinación devuelve el estado intacto en vez de inventar un intervalo.
     */
    fun startBreak(state: FocusTimerState): FocusTimerState {
        if (state.phase != FocusPhase.COMPLETED || !state.hasBreak) return state
        val total = state.breakMinutes * 60
        return state.copy(
            phase = FocusPhase.BREAK,
            totalSeconds = total,
            remainingSeconds = total,
            onBreak = true,
        )
    }

    /**
     * Salta el descanso y vuelve a reposo.
     *
     * Conserva [FocusTimerState.creditedMinutes] a cero en el estado nuevo porque
     * el premio ya se otorgó al completar el enfoque: volver a exponerlo aquí es
     * lo que provocaba registros duplicados.
     */
    fun skipBreak(state: FocusTimerState): FocusTimerState =
        if (state.phase == FocusPhase.BREAK || state.phase == FocusPhase.COMPLETED) {
            FocusTimerState()
        } else {
            state
        }

    /** Formatea un intervalo como `mm:ss`, o `h:mm:ss` si pasa de una hora. */
    fun formatCountdown(seconds: Int): String {
        val total = seconds.coerceAtLeast(0)
        val horas = total / 3600
        val minutos = (total % 3600) / 60
        val segundos = total % 60
        return if (horas > 0) {
            "$horas:${minutos.pad()}:${segundos.pad()}"
        } else {
            "${minutos.pad()}:${segundos.pad()}"
        }
    }

    private fun Int.pad(): String = if (this < 10) "0$this" else toString()
}
