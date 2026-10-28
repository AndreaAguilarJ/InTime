package com.momentummm.app.util

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Geometría del fondo de pantalla «Mi vida en semanas», sin dependencias de
 * Android para probarla con tests JVM.
 *
 * El bloque (título, cuadrícula y leyenda) se centra entre el reloj de la
 * pantalla de bloqueo, arriba, y los accesos directos y la barra de gestos,
 * abajo. Cada fila es un año de 52 semanas, como en la app.
 */
object LifeWallpaperLayout {

    const val COLUMNS = LifeWeeksMath.WEEKS_PER_ROW

    /** Margen a cada lado, en fracción del ancho. */
    const val SIDE_MARGIN = 0.08f

    /** Alto reservado arriba (reloj y fecha de la pantalla de bloqueo). */
    const val TOP_RESERVED = 0.22f

    /** Alto reservado abajo (accesos directos y barra de gestos). */
    const val BOTTOM_RESERVED = 0.12f

    private const val TITLE_SIZE = 0.05f
    private const val LEGEND_SIZE = 0.032f
    private const val TITLE_GAP = 0.9f
    private const val LEGEND_GAP = 1.6f
    private const val LEGEND_LINE_GAP = 0.6f

    /** Blanco y negro puros: uno de los dos supera siempre 4,5:1 sobre cualquier fondo. */
    const val LIGHT_TEXT: Int = -0x1 // 0xFFFFFFFF
    const val DARK_TEXT: Int = -0x1000000 // 0xFF000000

    data class Layout(
        val width: Int,
        val height: Int,
        val rows: Int,
        /** Lado de cada celda (punto + hueco), en píxeles enteros. */
        val cellPx: Float,
        val gridLeft: Float,
        val gridTop: Float,
        val titleSize: Float,
        val titleBaseline: Float,
        val legendSize: Float,
        val legendFirstBaseline: Float,
        val legendSecondBaseline: Float,
    ) {
        val gridWidth: Float get() = COLUMNS * cellPx
        val gridHeight: Float get() = rows * cellPx

        /** Hueco entre puntos: un 20 % de la celda y nunca menos de 1 px. */
        val gapPx: Float get() = max(1f, cellPx * 0.2f)
    }

    /**
     * @param screenWidth y [screenHeight] en píxeles; si llegan en apaisado se
     *   intercambian, porque el fondo se usa en vertical.
     */
    fun compute(screenWidth: Int, screenHeight: Int, lifeYears: Int): Layout {
        val width = min(screenWidth, screenHeight)
        val height = max(screenWidth, screenHeight)
        val rows = lifeYears.coerceAtLeast(1)

        val titleSize = width * TITLE_SIZE
        val legendSize = width * LEGEND_SIZE
        val fixed = titleSize * (1 + TITLE_GAP) + legendSize * (LEGEND_GAP + 1 + LEGEND_LINE_GAP + 1)

        val areaTop = height * TOP_RESERVED
        val areaHeight = height * (1 - BOTTOM_RESERVED) - areaTop
        val cell = floor(min(width * (1 - 2 * SIDE_MARGIN) / COLUMNS, (areaHeight - fixed) / rows))
            .coerceAtLeast(1f)
        val gridHeight = rows * cell

        val blockTop = areaTop + (areaHeight - fixed - gridHeight) / 2
        val titleBaseline = blockTop + titleSize
        val gridTop = titleBaseline + titleSize * TITLE_GAP
        val legendFirst = gridTop + gridHeight + legendSize * (LEGEND_GAP + 1)
        return Layout(
            width = width,
            height = height,
            rows = rows,
            cellPx = cell,
            gridLeft = (width - COLUMNS * cell) / 2,
            gridTop = gridTop,
            titleSize = titleSize,
            titleBaseline = titleBaseline,
            legendSize = legendSize,
            legendFirstBaseline = legendFirst,
            legendSecondBaseline = legendFirst + legendSize * (LEGEND_LINE_GAP + 1),
        )
    }

    /** Color de texto legible sobre [background] (ARGB). */
    fun textColorOn(background: Int): Int =
        if (contrast(LIGHT_TEXT, background) >= contrast(DARK_TEXT, background)) LIGHT_TEXT else DARK_TEXT

    /** Relación de contraste WCAG entre dos colores opacos. */
    fun contrast(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun luminance(color: Int): Double {
        fun channel(shift: Int): Double {
            val c = ((color shr shift) and 0xFF) / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }
}
