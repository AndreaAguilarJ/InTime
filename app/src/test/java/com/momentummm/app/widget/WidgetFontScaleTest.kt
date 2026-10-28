package com.momentummm.app.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Los umbrales de cada variante se midieron con la letra normal. Con la letra del
 * sistema agrandada (Ajustes > Pantalla > Tamaño de fuente) las etiquetas
 * ocupaban más que su columna y se cortaban a media palabra, y la cifra de 36 sp
 * se salía por abajo. Ahora los textos cuentan con la escala de la letra.
 */
class WidgetFontScaleTest {

    @Test
    fun `el hueco para texto se reduce con la letra grande`() {
        val room = WidgetTextRoom.of(DpSize(260.dp, 200.dp), fontScale = 1.3f)
        assertEquals(200f, room.width.value, 0.01f)
        assertEquals(153.85f, room.height.value, 0.01f)
        // Con letra pequeña hay más sitio, pero nunca menos de lo que mide el widget.
        assertEquals(DpSize(260.dp, 200.dp), WidgetTextRoom.of(DpSize(260.dp, 200.dp), fontScale = 1f))
    }

    @Test
    fun `mi vida en semanas oculta las cifras si con letra grande no caben`() {
        val size = DpSize(230.dp, 180.dp)
        assertTrue(LifeWeeksSizing.showStats(size, fontScale = 1f))
        assertFalse(LifeWeeksSizing.showStats(size, fontScale = 1.3f))
    }

    @Test
    fun `mi vida en semanas acorta la cabecera con letra grande`() {
        assertEquals(LifeWeeksSizing.Header.TITLE_AND_PERCENT, LifeWeeksSizing.header(210.dp, fontScale = 1f))
        assertEquals(LifeWeeksSizing.Header.PERCENT_OF_LIFE, LifeWeeksSizing.header(210.dp, fontScale = 1.3f))
        assertEquals(LifeWeeksSizing.Header.PERCENT, LifeWeeksSizing.header(160.dp, fontScale = 1.3f))
    }

    @Test
    fun `la cuadricula descuenta la cabecera y las cifras a su tamaño real`() {
        val normal = LifeWeeksSizing.reservedTextHeightDp(showStats = true, fontScale = 1f)
        val large = LifeWeeksSizing.reservedTextHeightDp(showStats = true, fontScale = 1.5f)
        assertTrue(large > normal)
    }

    @Test
    fun `progreso del año pasa a la variante compacta con letra grande`() {
        val size = DpSize(180.dp, 140.dp)
        val normal = YearProgressSizing.of(size, fontScale = 1f)
        val large = YearProgressSizing.of(size, fontScale = 1.4f)
        assertEquals(YearProgressSizing.Variant.ROOMY, normal.variant)
        assertTrue(normal.showDayOf)
        assertEquals(YearProgressSizing.Variant.COMPACT, large.variant)
        assertFalse(large.showDayOf)
        assertFalse(large.showStats)
    }

    @Test
    fun `una sola fila de alto sigue siendo la variante minima`() {
        assertEquals(YearProgressSizing.Variant.TINY, YearProgressSizing.of(DpSize(180.dp, 80.dp), 1f).variant)
        assertEquals(YearProgressSizing.Variant.TINY, YearProgressSizing.of(DpSize(180.dp, 110.dp), 1.3f).variant)
    }
}
