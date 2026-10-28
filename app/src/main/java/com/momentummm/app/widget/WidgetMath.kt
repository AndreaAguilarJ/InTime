package com.momentummm.app.widget

import java.time.LocalDate
import kotlin.math.floor
import kotlin.math.min

/**
 * Lógica de los widgets que no depende de Android, para probarla con tests JVM.
 */

/**
 * Progreso del año en curso.
 *
 * Cuenta como «pasados» los días TERMINADOS: hoy todavía se está viviendo. Así el
 * 1 de enero marca 0 % con todo el año por delante y el 31 de diciembre queda en
 * 99 % con 1 día restante, en vez del 100 % con «0 restantes» que mostraba la
 * versión anterior mientras aún quedaba un día entero.
 */
data class YearProgress(
    val year: Int,
    /** Día del año de hoy, empezando en 1. */
    val dayOfYear: Int,
    val daysInYear: Int,
) {
    val daysPassed: Int get() = dayOfYear - 1

    /** Incluye hoy. [daysPassed] + [daysLeft] = [daysInYear]. */
    val daysLeft: Int get() = daysInYear - daysPassed

    /** Fracción para la barra, entre 0 y 1. */
    val fraction: Float get() = daysPassed.toFloat() / daysInYear

    /**
     * Porcentaje entero que se pinta. Se TRUNCA: redondear anunciaría el 100 % el
     * 30 de diciembre, con dos días por delante.
     */
    val wholePercent: Int get() = daysPassed * 100 / daysInYear

    companion object {
        fun of(date: LocalDate): YearProgress =
            YearProgress(date.year, date.dayOfYear, date.lengthOfYear())
    }
}

/**
 * Elección estable de un elemento por día.
 *
 * La Frase del día salía de `ORDER BY RANDOM()` en cada refresco, así que cambiaba
 * cada hora y no era «del día». Con el día del calendario como semilla el elemento
 * es el mismo desde que se despierta el usuario hasta medianoche, y al día
 * siguiente siempre es otro (si hay más de uno).
 */
object DailyPick {

    /** @param salt desplaza la secuencia para que dos widgets no avancen a la par. */
    fun index(epochDay: Long, size: Int, salt: Int = 0): Int {
        require(size > 0) { "No hay elementos entre los que elegir" }
        return Math.floorMod(epochDay + salt, size.toLong()).toInt()
    }
}

/** Una frase con su autor; el autor puede faltar. */
data class WidgetQuote(val text: String, val author: String?)

object WidgetMessages {

    /**
     * Un mensaje puede llevar marcadores ({nombre}, {racha}, {saludo}…) que las
     * notificaciones rellenan al enviarse. En un widget que pasa el día entero en
     * la pantalla no tienen sentido (un saludo de mañana a medianoche, el tiempo
     * de pantalla de hace horas) y sin rellenar se verían las llaves: se descartan.
     */
    fun isShowable(content: String): Boolean = content.isNotBlank() && '{' !in content

    /**
     * Longitud que se lee de un vistazo en un widget de 3 × 2. Hay mensajes
     * guardados de hasta 353 caracteres; se prefieren los que caben y los largos
     * solo se usan si no queda otro.
     */
    const val MAX_WIDGET_CHARS = 120

    fun fitsWidget(content: String): Boolean = content.trim().length <= MAX_WIDGET_CHARS
}

object WidgetQuotes {

    private const val SEPARATOR = '|'

    /** Lee una entrada de `R.array.widget_quotes` («texto|autor»). */
    fun parse(entry: String): WidgetQuote? {
        val text = entry.substringBefore(SEPARATOR).trim()
        if (text.isEmpty()) return null
        val author = entry.substringAfter(SEPARATOR, missingDelimiterValue = "")
            .trim()
            .takeIf { it.isNotEmpty() }
        return WidgetQuote(text, author)
    }
}

/**
 * Cómo repartir «Mi vida en semanas» dentro del hueco que deja el widget.
 *
 * ─── EL FALLO QUE CORRIGE ───────────────────────────────────────────────
 * La cuadrícula anterior pintaba filas de 26 cajas de Glance, pero Glance 1.0
 * recorta cualquier fila o columna a 10 elementos («Truncated container from 26
 * to 10 elements»): se veía una rejilla de 10 × 10 donde cada punto valía 16
 * semanas y las filas saltaban de 26 en 26, así que no correspondía ni a semanas
 * ni a años. Ahora se dibuja un mapa de bits con TODAS las semanas, como en la
 * pantalla de la app.
 *
 * Cada fila es un número entero de años (52 semanas) para que se lea igual que la
 * cuadrícula de la app, y se elige cuántos años caben por fila según la forma del
 * widget. Si ni así se distingue una semana, se pasa a un punto por año.
 */
object LifeWeeksGridLayout {

    const val WEEKS_PER_YEAR = 52

    /**
     * Por debajo de 3 px una semana deja de verse como punto: con 1 px de hueco
     * quedarían 2 px de color, que en pantalla es una mancha continua.
     */
    const val MIN_WEEK_CELL_PX = 3f

    /** Un año por punto también necesita verse como punto. */
    const val MIN_YEAR_CELL_PX = 4f

    /** Columnas por fila: media vida, 1, 2, 3 o 4 años por fila. */
    private val WEEK_COLUMNS = intArrayOf(26, 52, 104, 156, 208)

    /** Columnas cuando cada punto es un año (80 años: 10 × 8, 20 × 4…). */
    private val YEAR_COLUMNS = intArrayOf(5, 8, 10, 16, 20, 40)

    enum class Unit { WEEK, YEAR }

    data class Layout(
        val unit: Unit,
        val columns: Int,
        val rows: Int,
        /** Lado de cada celda (punto + hueco), en px. */
        val cellPx: Float,
    ) {
        val widthPx: Float get() = columns * cellPx
        val heightPx: Float get() = rows * cellPx

        /** Hueco entre puntos: un 20 % de la celda y nunca menos de 1 px. */
        val gapPx: Float get() = maxOf(1f, cellPx * 0.2f)
    }

    /**
     * @return la disposición con las celdas más grandes que quepa en
     *   [widthPx] × [heightPx], o null si el hueco es demasiado pequeño incluso
     *   para un punto por año.
     */
    fun choose(lifeYears: Int, widthPx: Float, heightPx: Float): Layout? {
        if (lifeYears <= 0 || widthPx <= 0f || heightPx <= 0f) return null
        val totalWeeks = lifeYears * WEEKS_PER_YEAR

        val byWeek = best(Unit.WEEK, totalWeeks, WEEK_COLUMNS, widthPx, heightPx)
        if (byWeek != null && byWeek.cellPx >= MIN_WEEK_CELL_PX) return byWeek

        val byYear = best(Unit.YEAR, lifeYears, YEAR_COLUMNS, widthPx, heightPx)
        return byYear?.takeIf { it.cellPx >= MIN_YEAR_CELL_PX }
    }

    private fun best(
        unit: Unit,
        cells: Int,
        options: IntArray,
        widthPx: Float,
        heightPx: Float,
    ): Layout? = options
        .filter { it <= cells }
        .map { columns ->
            val rows = (cells + columns - 1) / columns
            // Se redondea hacia abajo a píxel entero: con celdas fraccionarias los
            // puntos salen de anchos distintos (unos de 3 px y otros de 4).
            val cell = floor(min(widthPx / columns, heightPx / rows))
            Layout(unit, columns, rows, cell)
        }
        .maxByOrNull { it.cellPx }

    /** Estado de cada punto, para dibujarlo y para probarlo. */
    enum class Fill { LIVED, CURRENT, FUTURE }

    /**
     * @param index posición del punto en orden de lectura.
     * @param weeksLived semanas completas vividas.
     * @param yearsLived años cumplidos.
     */
    fun fill(layout: Layout, index: Int, weeksLived: Int, yearsLived: Int): Fill = when (layout.unit) {
        Unit.WEEK -> if (index < weeksLived) Fill.LIVED else Fill.FUTURE
        // El año en curso no está vivido ni por vivir: se marca aparte.
        Unit.YEAR -> when {
            index < yearsLived -> Fill.LIVED
            index == yearsLived -> Fill.CURRENT
            else -> Fill.FUTURE
        }
    }
}
