package com.momentummm.app.notification

import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.number
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Decisiones de contenido de los avisos basados en datos del usuario.
 *
 * Aquí vive la parte que decide QUÉ decir y CUÁNDO tiene sentido decirlo; el
 * gestor de notificaciones solo traduce el resultado a texto localizado. Al
 * no depender de Android se puede comprobar con tests JVM, que es donde se
 * caen los casos de borde: la primera semana sin historial, el usuario sin
 * permiso de uso, el cumpleaños del 29 de febrero.
 */
object NotificationInsights {

    /** Por debajo de esto no hay datos reales que resumir: no se envía. */
    const val MIN_WEEK_MINUTES_TO_SUMMARIZE = 30

    /** Una semana anterior con menos de una hora no es una base comparable. */
    const val MIN_PREVIOUS_WEEK_MINUTES = 60

    /** Cambios menores que esto se cuentan como «igual que la semana pasada». */
    const val SAME_WEEK_TOLERANCE_PERCENT = 5

    /** Techo del «día equilibrado»: tres horas de pantalla. */
    const val BALANCED_DAY_MAX_MINUTES = 180

    /** Mínimo de uso para que un «día equilibrado» no sea un día sin datos. */
    const val BALANCED_DAY_MIN_MINUTES = 30

    /** Hora desde la que se puede valorar el día como equilibrado. */
    const val BALANCED_DAY_FROM_HOUR = 20

    enum class Trend { DOWN, UP, SAME, NONE }

    data class WeekComparison(
        val trend: Trend,
        /** Cambio en minutos, siempre positivo. */
        val deltaMinutes: Int = 0,
        /** Cambio relativo en %, siempre positivo y redondeado. */
        val percent: Int = 0
    )

    /** ¿Hay una semana real que resumir? */
    fun shouldSendWeeklySummary(hasUsagePermission: Boolean, totalMinutes: Int): Boolean =
        hasUsagePermission && totalMinutes >= MIN_WEEK_MINUTES_TO_SUMMARIZE

    /**
     * Compara esta semana con la anterior REGISTRADA por la app.
     *
     * [previousMinutes] es null la primera vez: Android solo guarda unos siete
     * días de estadísticas diarias, así que la semana anterior no se puede
     * pedir al sistema; la app guarda su propio total al enviar cada resumen.
     */
    fun compareWeeks(currentMinutes: Int, previousMinutes: Int?): WeekComparison {
        if (previousMinutes == null || previousMinutes < MIN_PREVIOUS_WEEK_MINUTES) {
            return WeekComparison(Trend.NONE)
        }
        val delta = currentMinutes - previousMinutes
        val percent = (abs(delta) * 100.0 / previousMinutes).roundToInt()
        val trend = when {
            percent < SAME_WEEK_TOLERANCE_PERCENT -> Trend.SAME
            delta < 0 -> Trend.DOWN
            else -> Trend.UP
        }
        return WeekComparison(trend, abs(delta), percent)
    }

    /**
     * ¿Merece el día una felicitación por uso equilibrado?
     *
     * Antes bastaba con «menos de 3 h», y sin permiso de uso las estadísticas
     * valen 0, así que se felicitaba cada noche a quien la app no podía medir.
     * Ahora hace falta permiso, un mínimo de uso real y no superar la propia
     * media del usuario cuando se conoce.
     */
    fun isBalancedDay(
        hasUsagePermission: Boolean,
        hourOfDay: Int,
        todayMinutes: Int,
        averageMinutes: Int
    ): Boolean {
        if (!hasUsagePermission) return false
        if (hourOfDay < BALANCED_DAY_FROM_HOUR) return false
        if (todayMinutes !in BALANCED_DAY_MIN_MINUTES until BALANCED_DAY_MAX_MINUTES) return false
        return averageMinutes <= 0 || todayMinutes <= averageMinutes
    }

    /**
     * ¿Es hoy su cumpleaños? Quien nació un 29 de febrero lo celebra el 28 en
     * los años no bisiestos, en vez de pasar tres años sin felicitación.
     */
    fun isBirthday(birthDate: LocalDate, today: LocalDate): Boolean {
        if (birthDate >= today) return false
        val bornOnLeapDay = birthDate.month == Month.FEBRUARY && birthDate.dayOfMonth == 29
        if (bornOnLeapDay && !isLeapYear(today.year)) {
            return today.month == Month.FEBRUARY && today.dayOfMonth == 28
        }
        return today.month.number == birthDate.month.number &&
            today.dayOfMonth == birthDate.dayOfMonth
    }

    private fun isLeapYear(year: Int): Boolean =
        (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
}
