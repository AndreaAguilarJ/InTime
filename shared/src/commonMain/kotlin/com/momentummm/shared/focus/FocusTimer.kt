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
    /**
     * Instante en que termina el intervalo actual, en segundos desde la época.
     *
     * Es la fuente de verdad de cuánto queda, y existe por una razón concreta:
     * iOS **suspende** la app al pasar a segundo plano, así que un contador que
     * resta de uno en uno se congela. Al volver, [FocusTimer.syncToClock] compara
     * con el reloj real y la cuenta atrás aparece donde debe estar, no donde se
     * quedó.
     */
    val endsAtEpochSeconds: Long? = null,
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
 * persistente y el reparto de XP, iOS aporta los avisos locales, pero las
 * transiciones son estas y solo estas.
 *
 * Todas las funciones que dependen del tiempo reciben el instante actual como
 * parámetro en vez de leer el reloj por dentro: así son comprobables y no hay dos
 * llamadas seguidas que discrepen.
 */
object FocusTimer {

    /** Comienza una sesión. Una duración no positiva se rechaza dejando IDLE. */
    fun start(
        sessionName: String?,
        durationMinutes: Int,
        breakMinutes: Int = 0,
        nowEpochSeconds: Long = 0L,
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
            endsAtEpochSeconds = nowEpochSeconds + total,
        )
    }

    /**
     * Pone la cuenta atrás donde dice el reloj real.
     *
     * Es lo que hay que llamar al volver del segundo plano. Si el intervalo ya
     * venció mientras la app estaba suspendida, aplica la transición que
     * correspondía, de modo que la persona encuentra la sesión completada y no un
     * reloj parado a mitad.
     */
    fun syncToClock(state: FocusTimerState, nowEpochSeconds: Long): FocusTimerState {
        val endsAt = state.endsAtEpochSeconds ?: return state
        if (!state.isTicking) return state

        val restante = (endsAt - nowEpochSeconds).coerceAtMost(state.totalSeconds.toLong())
        if (restante > 0) return state.copy(remainingSeconds = restante.toInt())
        return finishInterval(state)
    }

    /**
     * Avanza un segundo, para que la cuenta atrás se mueva con suavidad mientras
     * la pantalla está a la vista. [syncToClock] sigue siendo la autoridad.
     */
    fun tick(state: FocusTimerState): FocusTimerState {
        if (!state.isTicking) return state
        val restante = state.remainingSeconds - 1
        if (restante > 0) return state.copy(remainingSeconds = restante)
        return finishInterval(state)
    }

    /**
     * Cierra el intervalo agotado.
     *
     * El enfoque pasa a COMPLETED --nunca directo al descanso-- porque el aviso de
     * sesión cumplida debe verse antes, y saltar el descanso es una decisión de la
     * persona, no del reloj.
     */
    private fun finishInterval(state: FocusTimerState): FocusTimerState = when (state.phase) {
        FocusPhase.RUNNING -> state.copy(
            phase = FocusPhase.COMPLETED,
            remainingSeconds = 0,
            creditedMinutes = state.totalSeconds / 60,
            endsAtEpochSeconds = null,
        )
        // Terminado el descanso se vuelve a reposo: la sesión acabó del todo.
        FocusPhase.BREAK -> FocusTimerState()
        else -> state
    }

    /**
     * Pausa conservando la fase de la que viene, enfoque o descanso.
     *
     * Al pausar se suelta el ancla temporal: lo que queda pasa a medirse en
     * segundos guardados, porque el reloj de pared ya no representa nada mientras
     * la sesión está detenida.
     */
    fun pause(state: FocusTimerState): FocusTimerState =
        if (state.isTicking) state.copy(phase = FocusPhase.PAUSED, endsAtEpochSeconds = null) else state

    /** Reanuda devolviendo la sesión a la fase correcta y reanclando el reloj. */
    fun resume(state: FocusTimerState, nowEpochSeconds: Long = 0L): FocusTimerState =
        if (state.phase == FocusPhase.PAUSED) {
            state.copy(
                phase = if (state.onBreak) FocusPhase.BREAK else FocusPhase.RUNNING,
                endsAtEpochSeconds = nowEpochSeconds + state.remainingSeconds,
            )
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
    fun startBreak(state: FocusTimerState, nowEpochSeconds: Long = 0L): FocusTimerState {
        if (state.phase != FocusPhase.COMPLETED || !state.hasBreak) return state
        val total = state.breakMinutes * 60
        return state.copy(
            phase = FocusPhase.BREAK,
            totalSeconds = total,
            remainingSeconds = total,
            onBreak = true,
            endsAtEpochSeconds = nowEpochSeconds + total,
        )
    }

    /**
     * Salta el descanso y vuelve a reposo.
     *
     * El estado nuevo deja [FocusTimerState.creditedMinutes] a cero porque el
     * premio ya se otorgó al completar el enfoque: volver a exponerlo aquí es lo
     * que provocaba registros duplicados.
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
