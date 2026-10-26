package com.momentummm.app.notification

import com.momentummm.app.data.entity.MessageCategory
import java.util.Calendar

/**
 * Decide qué mensaje motivacional tiene sentido AHORA y si conviene enviarlo.
 *
 * ─── EL FALLO QUE CORRIGE ────────────────────────────────────────────────
 * Los mensajes periódicos se sorteaban entre TODAS las categorías activas, y
 * el título se construye a partir de la categoría. Así llegaban, a cualquier
 * hora y día, avisos como «🌅 Buenos días» a las 18:00, «💼 ¡Nuevo lunes!» un
 * viernes, «🔥 Tu racha» sin racha, «🔄 ¡De vuelta!» a quien no se había ido
 * o «🏆 ¡Logro!» sin ningún logro. Cada uno contradecía lo que el usuario
 * estaba viviendo, que es justo lo que hace que una notificación se ignore.
 *
 * Además, «Horario inteligente» era un interruptor sin efecto: nada lo leía.
 * Ahora significa algo comprobable: no interrumpir una sesión de enfoque ni un
 * «No molestar» del sistema.
 *
 * Todo es puro (sin Android) para verificarlo con tests JVM.
 */
object MessageRelevance {

    /**
     * Categorías que solo tienen sentido como respuesta a algo que acaba de
     * pasar (un logro, un hito, una vuelta tras días fuera). Nunca se sortean
     * en un mensaje periódico; las piden los avisos de ese evento.
     */
    val EVENT_ONLY: Set<MessageCategory> = setOf(
        MessageCategory.ACHIEVEMENT,
        MessageCategory.CELEBRATION,
        MessageCategory.MILESTONE,
        MessageCategory.COMEBACK
    )

    /** Válidas a cualquier hora: el respaldo cuando nada de lo elegido encaja. */
    val UNIVERSAL: List<MessageCategory> = listOf(
        MessageCategory.MOTIVATION,
        MessageCategory.MINDFULNESS,
        MessageCategory.GROWTH,
        MessageCategory.SELF_CARE,
        MessageCategory.FOCUS,
        MessageCategory.PRODUCTIVITY,
        MessageCategory.GRATITUDE,
        MessageCategory.RESILIENCE,
        MessageCategory.CHALLENGE
    )

    /** Racha mínima para que un mensaje de racha no suene a burla. */
    const val MIN_STREAK_FOR_STREAK_MESSAGES = MessageTemplates.STREAK_WORTH_MENTIONING

    /**
     * Momento del usuario.
     *
     * @param dayOfWeek constante de [Calendar] (`Calendar.MONDAY`…).
     */
    data class Moment(
        val hourOfDay: Int,
        val dayOfWeek: Int,
        val streakDays: Int = 0
    )

    /** true si un mensaje de [category] encaja con [moment]. */
    fun fitsMoment(category: MessageCategory, moment: Moment): Boolean = when (category) {
        MessageCategory.MORNING -> moment.hourOfDay in 5..11
        MessageCategory.EVENING -> moment.hourOfDay >= 19 || moment.hourOfDay < 2
        MessageCategory.MONDAY -> moment.dayOfWeek == Calendar.MONDAY && moment.hourOfDay < 14
        MessageCategory.WEEKEND ->
            moment.dayOfWeek == Calendar.SATURDAY || moment.dayOfWeek == Calendar.SUNDAY
        MessageCategory.STREAK -> moment.streakDays >= MIN_STREAK_FOR_STREAK_MESSAGES
        in EVENT_ONLY -> false
        else -> true
    }

    /**
     * Categorías entre las que sortear un mensaje periódico.
     *
     * Respeta lo que el usuario eligió y quita lo que contradice el momento.
     * Si eso lo deja vacío (por ejemplo, solo activó «Buenos días» y son las
     * 17:00) se usan las universales: es mejor un mensaje general que uno que
     * diga algo falso o que ninguno, porque el usuario sí pidió mensajes.
     */
    fun periodicCategories(
        enabled: Collection<MessageCategory>,
        moment: Moment
    ): List<MessageCategory> {
        val fitting = enabled.filter { fitsMoment(it, moment) }.distinct()
        if (fitting.isNotEmpty()) return fitting
        return UNIVERSAL.filter { fitsMoment(it, moment) }
    }

    /** Motivo por el que se aplaza un mensaje, o null si se puede enviar. */
    enum class Deferral { FOCUS_SESSION, DO_NOT_DISTURB }

    /**
     * Decide si un mensaje PERIÓDICO debe esperar.
     *
     * Solo aplica con «Horario inteligente» activado. Los avisos que el usuario
     * pide a mano (botón «Probar», «Otro mensaje») no pasan por aquí.
     */
    fun deferral(
        smartTimingEnabled: Boolean,
        respectFocusMode: Boolean,
        respectDnd: Boolean,
        focusSessionActive: Boolean,
        doNotDisturbActive: Boolean
    ): Deferral? {
        if (!smartTimingEnabled) return null
        if (respectFocusMode && focusSessionActive) return Deferral.FOCUS_SESSION
        if (respectDnd && doNotDisturbActive) return Deferral.DO_NOT_DISTURB
        return null
    }
}
