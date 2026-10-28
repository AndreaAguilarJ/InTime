package com.momentummm.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fondo de pantalla «Mi vida en semanas». La versión anterior:
 * - pintaba título y leyenda en negro sobre el fondo por defecto #1F2937 (gris
 *   casi negro): contraste 1,4:1, ilegible;
 * - dibujaba siempre 80 años y 1080 × 1920, aunque el teléfono fuera más alto;
 * - separaba las columnas más que las filas, así que los «cuadritos» no formaban
 *   una cuadrícula regular.
 */
class LifeWallpaperLayoutTest {

    private val screens = listOf(720 to 1280, 1080 to 1920, 1080 to 2400, 1344 to 2992, 1440 to 3120)

    @Test
    fun `la cuadricula cabe entera, centrada y lejos del reloj y de la barra inferior`() {
        for ((w, h) in screens) {
            for (years in listOf(LifeExpectancy.MIN_YEARS, 80, 95, LifeExpectancy.MAX_YEARS)) {
                val l = LifeWallpaperLayout.compute(w, h, years)
                val tag = "${w}x$h $years años"
                assertEquals(tag, years, l.rows)
                assertTrue(tag, l.cellPx >= 3f)
                assertEquals("$tag: celdas de píxel entero", l.cellPx, kotlin.math.floor(l.cellPx), 0f)
                assertTrue("$tag: borde izquierdo", l.gridLeft >= w * LifeWallpaperLayout.SIDE_MARGIN - 0.5f)
                assertTrue("$tag: borde derecho", l.gridLeft + l.gridWidth <= w * (1 - LifeWallpaperLayout.SIDE_MARGIN) + 0.5f)
                assertEquals("$tag: centrada", (w - l.gridWidth) / 2f, l.gridLeft, 0.5f)
                assertTrue("$tag: título bajo el reloj", l.titleBaseline - l.titleSize >= h * LifeWallpaperLayout.TOP_RESERVED - 0.5f)
                assertTrue("$tag: título sobre la cuadrícula", l.titleBaseline < l.gridTop)
                assertTrue("$tag: leyenda bajo la cuadrícula", l.legendFirstBaseline - l.legendSize > l.gridTop + l.gridHeight)
                assertTrue("$tag: sobre la barra inferior", l.legendSecondBaseline <= h * (1 - LifeWallpaperLayout.BOTTOM_RESERVED) + 0.5f)
            }
        }
    }

    @Test
    fun `columnas y filas tienen la misma separacion`() {
        val l = LifeWallpaperLayout.compute(1080, 2400, 80)
        assertEquals(LifeWallpaperLayout.COLUMNS * l.cellPx, l.gridWidth, 0.01f)
        assertEquals(80 * l.cellPx, l.gridHeight, 0.01f)
    }

    @Test
    fun `una pantalla apaisada se dibuja en vertical`() {
        assertEquals(LifeWallpaperLayout.compute(1080, 2400, 80), LifeWallpaperLayout.compute(2400, 1080, 80))
    }

    @Test
    fun `el texto es claro sobre fondo oscuro y oscuro sobre fondo claro`() {
        val dark = 0xFF1F2937.toInt()
        val white = 0xFFFFFFFF.toInt()
        val indigo = 0xFF6366F1.toInt()
        assertEquals(LifeWallpaperLayout.LIGHT_TEXT, LifeWallpaperLayout.textColorOn(dark))
        assertEquals(LifeWallpaperLayout.DARK_TEXT, LifeWallpaperLayout.textColorOn(white))
        for (bg in listOf(dark, white, indigo, 0xFF000000.toInt(), 0xFFFFEB3B.toInt(), 0xFF808080.toInt())) {
            val ratio = LifeWallpaperLayout.contrast(LifeWallpaperLayout.textColorOn(bg), bg)
            assertTrue("contraste ${"%.2f".format(ratio)} sobre ${Integer.toHexString(bg)}", ratio >= 4.5)
        }
    }

    @Test
    fun `el generador no lleva textos fijos ni color de texto fijo`() {
        val main = listOf(java.io.File("src/main"), java.io.File("app/src/main"))
            .first { java.io.File(it, "AndroidManifest.xml").isFile }
        val code = java.io.File(main, "java/com/momentummm/app/util/WallpaperGenerator.kt").readText()
            .replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("""//[^\n]*"""), "")
        assertTrue("texto en español fijo", Regex(""""[^"]*(Semanas|Mi Vida|vividas)""").find(code) == null)
        assertTrue("color de texto fijo", !code.contains("Color.BLACK") && !code.contains("Color.WHITE"))
        assertTrue("usa el color legible", code.contains("LifeWallpaperLayout.textColorOn("))
    }
}
