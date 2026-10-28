package com.momentummm.app.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.graphics.ColorUtils
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * Dibuja la cuadrícula de «Mi vida en semanas» para el widget.
 *
 * Es un mapa de bits porque un widget no puede tener miles de vistas: Glance
 * recorta cada fila a 10 elementos y RemoteViews no aguantaría 4.160 cajas. El
 * fondo es transparente, así que la superficie del widget (clara u oscura) se ve
 * entre los puntos y el mapa sirve en los dos modos.
 */
object LifeWeeksGridBitmap {

    /**
     * Tope de píxeles por mapa (1,6 MB en ARGB). Android limita la memoria de
     * mapas de bits de un widget a 1,5 veces la pantalla y Glance puede enviar
     * uno por cada tamaño (vertical y apaisado).
     */
    const val MAX_PIXELS = 400_000

    /** Alfa del año en curso cuando cada punto es un año: ni vivido ni por vivir. */
    private const val CURRENT_YEAR_ALPHA = 115

    /**
     * Reduce el hueco disponible si dibujarlo a resolución completa superaría
     * [MAX_PIXELS]. Devuelve el factor aplicado (1 = sin reducir).
     */
    fun downscaleFor(widthPx: Float, heightPx: Float): Float {
        val pixels = widthPx * heightPx
        return if (pixels <= MAX_PIXELS) 1f else sqrt(MAX_PIXELS / pixels)
    }

    fun render(
        layout: LifeWeeksGridLayout.Layout,
        lifeYears: Int,
        weeksLived: Int,
        yearsLived: Int,
        livedColor: Int,
        futureColor: Int,
    ): Bitmap {
        val width = ceil(layout.widthPx).toInt().coerceAtLeast(1)
        val height = ceil(layout.heightPx).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        // Sin antialias: los puntos son cuadrados alineados a píxel y así salen nítidos.
        val paint = Paint().apply { style = Paint.Style.FILL }
        val currentColor = ColorUtils.setAlphaComponent(livedColor, CURRENT_YEAR_ALPHA)

        val cells = when (layout.unit) {
            LifeWeeksGridLayout.Unit.WEEK -> lifeYears * LifeWeeksGridLayout.WEEKS_PER_YEAR
            LifeWeeksGridLayout.Unit.YEAR -> lifeYears
        }
        val dot = layout.cellPx - layout.gapPx
        for (index in 0 until cells) {
            val x = (index % layout.columns) * layout.cellPx
            val y = (index / layout.columns) * layout.cellPx
            paint.color = when (LifeWeeksGridLayout.fill(layout, index, weeksLived, yearsLived)) {
                LifeWeeksGridLayout.Fill.LIVED -> livedColor
                LifeWeeksGridLayout.Fill.CURRENT -> currentColor
                LifeWeeksGridLayout.Fill.FUTURE -> futureColor
            }
            canvas.drawRect(x, y, x + dot, y + dot, paint)
        }
        return bitmap
    }
}
