package com.momentummm.app.widget

import androidx.compose.ui.unit.dp
import androidx.work.ExistingWorkPolicy
import com.momentummm.app.data.entity.MessageCategory
import com.momentummm.app.data.entity.MessageTone
import com.momentummm.app.notification.MessageRelevance
import com.momentummm.app.worker.WidgetUpdateWorker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class YearProgressTest {

    @Test
    fun `el 1 de enero no ha pasado ningun dia y queda el año entero`() {
        val p = YearProgress.of(LocalDate.of(2026, 1, 1))
        assertEquals(0, p.daysPassed)
        assertEquals(365, p.daysLeft)
        assertEquals(0, p.wholePercent)
    }

    @Test
    fun `el 31 de diciembre queda un dia y no se anuncia el 100 por cien`() {
        val p = YearProgress.of(LocalDate.of(2026, 12, 31))
        assertEquals(364, p.daysPassed)
        assertEquals(1, p.daysLeft)
        assertEquals(99, p.wholePercent)
        assertTrue(p.fraction < 1f)
    }

    @Test
    fun `los dias pasados y restantes siempre suman el año, tambien en bisiesto`() {
        var date = LocalDate.of(2028, 1, 1)
        while (date.year == 2028) {
            val p = YearProgress.of(date)
            assertEquals("$date", 366, p.daysPassed + p.daysLeft)
            date = date.plusDays(1)
        }
    }

    @Test
    fun `el porcentaje se trunca, no se redondea`() {
        // 27 oct 2026: día 300, 299 pasados de 365 = 81,9 %.
        val p = YearProgress.of(LocalDate.of(2026, 10, 27))
        assertEquals(300, p.dayOfYear)
        assertEquals(81, p.wholePercent)
    }
}

class DailyPickTest {

    @Test
    fun `el mismo dia da siempre el mismo elemento`() {
        val day = LocalDate.of(2026, 10, 27).toEpochDay()
        assertEquals(DailyPick.index(day, 12), DailyPick.index(day, 12))
    }

    @Test
    fun `dias seguidos dan elementos distintos`() {
        val day = LocalDate.of(2026, 10, 27).toEpochDay()
        assertNotEquals(DailyPick.index(day, 12), DailyPick.index(day + 1, 12))
    }

    @Test
    fun `recorre todos los elementos antes de repetir`() {
        val start = LocalDate.of(2026, 1, 1).toEpochDay()
        val seen = (0 until 12).map { DailyPick.index(start + it, 12) }.toSet()
        assertEquals(12, seen.size)
    }

    @Test
    fun `el desplazamiento separa dos widgets`() {
        val day = 20_000L
        assertNotEquals(DailyPick.index(day, 12, salt = 0), DailyPick.index(day, 12, salt = 5))
    }

    @Test
    fun `dias anteriores a 1970 no dan indices negativos`() {
        assertTrue(DailyPick.index(-3, 12) in 0 until 12)
    }
}

class WidgetQuotesTest {

    @Test
    fun `separa texto y autor`() {
        assertEquals(WidgetQuote("Somos lo que hacemos.", "Will Durant"), WidgetQuotes.parse("Somos lo que hacemos.|Will Durant"))
    }

    @Test
    fun `sin autor no inventa uno`() {
        assertEquals(WidgetQuote("Solo texto", null), WidgetQuotes.parse("Solo texto"))
        assertEquals(WidgetQuote("Solo texto", null), WidgetQuotes.parse("Solo texto| "))
    }

    @Test
    fun `una entrada vacia se descarta`() {
        assertNull(WidgetQuotes.parse(" |Autor"))
    }
}

class WidgetMessagesTest {

    @Test
    fun `el widget prefiere mensajes que se leen de un vistazo`() {
        // Hay mensajes guardados de hasta 353 caracteres: en un widget de 3 × 2
        // entran unos 100 y el resto acabaría cortado.
        assertTrue(WidgetMessages.fitsWidget("Un rato sin el móvil también es avanzar."))
        assertFalse(WidgetMessages.fitsWidget("a".repeat(WidgetMessages.MAX_WIDGET_CHARS + 1)))
    }

    @Test
    fun `un mensaje con marcadores sin rellenar no se muestra`() {
        assertFalse(WidgetMessages.isShowable("{saludo}, {nombre}: sigue así"))
        assertFalse(WidgetMessages.isShowable("   "))
        assertTrue(WidgetMessages.isShowable("Un rato sin el móvil también es avanzar."))
    }

    @Test
    fun `el widget nunca elige categorias atadas a una hora, un evento o una racha`() {
        val monday = LocalDate.of(2026, 10, 26)
        val saturday = LocalDate.of(2026, 10, 31)
        val forbidden = MessageRelevance.EVENT_ONLY + setOf(
            MessageCategory.MORNING,
            MessageCategory.EVENING,
            MessageCategory.MONDAY,
            MessageCategory.STREAK,
        )
        for (date in listOf(monday, saturday)) {
            val categories = MotivationalMessageWidget.categoriesFor(date)
            assertTrue("$date: ${categories.intersect(forbidden)}", categories.none { it in forbidden })
        }
        assertFalse(MessageCategory.WEEKEND in MotivationalMessageWidget.categoriesFor(monday))
        assertTrue(MessageCategory.WEEKEND in MotivationalMessageWidget.categoriesFor(saturday))
    }

    private val monday = LocalDate.of(2026, 10, 26)

    @Test
    fun `el widget solo usa las categorias y tonos que el usuario dejo activados`() {
        val selection = MotivationalMessageWidget.selectionFor(
            date = monday,
            enabledCategories = listOf(MessageCategory.MINDFULNESS, MessageCategory.MORNING),
            enabledTones = listOf(MessageTone.CALM),
        )
        assertNotNull(selection)
        // MORNING está activada pero no vale todo el día: no entra.
        assertEquals(listOf(MessageCategory.MINDFULNESS), selection!!.categories)
        assertEquals(listOf(MessageTone.CALM), selection.tones)
    }

    @Test
    fun `si ninguna categoria activada vale todo el dia no hay seleccion`() {
        // Entonces el widget muestra un mensaje neutro traducido en vez de saltarse
        // lo que el usuario apagó.
        assertNull(
            MotivationalMessageWidget.selectionFor(
                date = monday,
                enabledCategories = listOf(MessageCategory.MORNING, MessageCategory.STREAK),
                enabledTones = MessageTone.entries,
            ),
        )
    }

    @Test
    fun `preferencias vacias o ilegibles cuentan como todo activado`() {
        val selection = MotivationalMessageWidget.selectionFor(monday, emptyList(), emptyList())
        assertEquals(MotivationalMessageWidget.categoriesFor(monday), selection!!.categories)
        assertEquals(MessageTone.entries, selection.tones)
    }

    @Test
    fun `el mensaje del dia guardado se descarta si ya no encaja con lo elegido`() {
        val selection = MotivationalMessageWidget.selectionFor(
            monday, listOf(MessageCategory.GROWTH), listOf(MessageTone.WISE),
        )!!
        assertTrue(selection.accepts(MessageCategory.GROWTH, MessageTone.WISE))
        assertFalse(selection.accepts(MessageCategory.GROWTH, MessageTone.HUMOROUS))
        assertFalse(selection.accepts(MessageCategory.FOCUS, MessageTone.WISE))
    }
}

class LifeWeeksGridLayoutTest {

    @Test
    fun `un widget apaisado pone varios años por fila con una celda por semana`() {
        // 3 × 2 celdas en un teléfono de densidad 2,75: ~250 × 110 dp de hueco.
        val layout = LifeWeeksGridLayout.choose(lifeYears = 80, widthPx = 687f, heightPx = 302f)
        assertNotNull(layout)
        layout!!
        assertEquals(LifeWeeksGridLayout.Unit.WEEK, layout.unit)
        // 2 años por fila: 104 × 40 celdas de 6 px.
        assertEquals(104, layout.columns)
        assertEquals(6f, layout.cellPx, 0f)
        assertTrue(layout.columns * layout.rows >= 80 * 52)
        assertTrue(layout.widthPx <= 687f && layout.heightPx <= 302f)
        assertTrue(layout.cellPx >= LifeWeeksGridLayout.MIN_WEEK_CELL_PX)
    }

    @Test
    fun `cada fila es un numero entero de medios años`() {
        val sizes = listOf(687f to 302f, 300f to 600f, 1000f to 200f, 495f to 495f)
        for ((w, h) in sizes) {
            val layout = LifeWeeksGridLayout.choose(80, w, h) ?: continue
            if (layout.unit == LifeWeeksGridLayout.Unit.WEEK) {
                assertEquals("${w}x$h", 0, layout.columns % 26)
            }
        }
    }

    @Test
    fun `si una semana no se distingue pasa a un punto por año`() {
        val layout = LifeWeeksGridLayout.choose(lifeYears = 80, widthPx = 220f, heightPx = 120f)
        assertNotNull(layout)
        assertEquals(LifeWeeksGridLayout.Unit.YEAR, layout!!.unit)
        assertTrue(layout.columns * layout.rows >= 80)
        assertTrue(layout.cellPx >= LifeWeeksGridLayout.MIN_YEAR_CELL_PX)
    }

    @Test
    fun `sin hueco no hay cuadricula`() {
        assertNull(LifeWeeksGridLayout.choose(80, 0f, 100f))
        assertNull(LifeWeeksGridLayout.choose(80, 20f, 10f))
        assertNull(LifeWeeksGridLayout.choose(0, 500f, 500f))
    }

    @Test
    fun `las celdas son de pixel entero para que todos los puntos midan igual`() {
        val layout = LifeWeeksGridLayout.choose(80, 687f, 302f)!!
        assertEquals(layout.cellPx, kotlin.math.floor(layout.cellPx), 0f)
    }

    @Test
    fun `cuenta exactamente las semanas vividas`() {
        val layout = LifeWeeksGridLayout.choose(80, 687f, 302f)!!
        val lived = (0 until 80 * 52).count {
            LifeWeeksGridLayout.fill(layout, it, weeksLived = 1560, yearsLived = 30) == LifeWeeksGridLayout.Fill.LIVED
        }
        assertEquals(1560, lived)
    }

    @Test
    fun `por años marca aparte el año en curso`() {
        val layout = LifeWeeksGridLayout.Layout(LifeWeeksGridLayout.Unit.YEAR, columns = 10, rows = 8, cellPx = 10f)
        assertEquals(LifeWeeksGridLayout.Fill.LIVED, LifeWeeksGridLayout.fill(layout, 29, weeksLived = 1580, yearsLived = 30))
        assertEquals(LifeWeeksGridLayout.Fill.CURRENT, LifeWeeksGridLayout.fill(layout, 30, weeksLived = 1580, yearsLived = 30))
        assertEquals(LifeWeeksGridLayout.Fill.FUTURE, LifeWeeksGridLayout.fill(layout, 31, weeksLived = 1580, yearsLived = 30))
    }
}

class WidgetTextFitTest {

    private val longQuote = "No es que tengamos poco tiempo, sino que perdemos mucho. La vida es larga si sabes emplearla."

    @Test
    fun `el ajuste por palabras cuenta las lineas como un TextView`() {
        assertEquals(1, WidgetTextFit.wrappedLines("uno dos tres", 12))
        assertEquals(2, WidgetTextFit.wrappedLines("uno dos tres", 11))
        // Una palabra más larga que la línea se parte en varias.
        assertEquals(3, WidgetTextFit.wrappedLines("supercalifragilístico", 8))
    }

    @Test
    fun `un texto que cabe conserva la letra grande y no se recorta`() {
        val fit = WidgetTextFit.fit("Hoy cuenta.", widthDp = 260f, heightDp = 100f, sizes = listOf(15, 14, 13), fontScale = 1f)
        assertEquals("Hoy cuenta.", fit.text)
        assertEquals(15, fit.fontSp)
    }

    @Test
    fun `si no cabe se reduce la letra antes de recortar`() {
        // 260 dp × 3 líneas: a 15 sp no entra entera; a 13 sp sí.
        val fit = WidgetTextFit.fit(longQuote, widthDp = 260f, heightDp = 60f, sizes = listOf(15, 14, 13), fontScale = 1f)
        assertEquals(longQuote, fit.text)
        assertTrue(fit.fontSp < 15)
    }

    @Test
    fun `si ni asi cabe se corta en una palabra y con puntos suspensivos`() {
        val fit = WidgetTextFit.fit(longQuote, widthDp = 200f, heightDp = 40f, sizes = listOf(15, 13), fontScale = 1f)
        assertTrue(fit.text, fit.text.endsWith("…"))
        val kept = fit.text.removeSuffix("…")
        val rest = longQuote.removePrefix(kept)
        assertTrue("es el principio de la frase: «$kept»", longQuote.startsWith(kept))
        assertTrue("corta entre palabras: «$kept»", rest.first() == ' ' || (rest.first() in ",;:." && rest[1] == ' '))
        assertFalse("sin puntuación colgando", kept.last() in ",;:.")
        val cpl = WidgetTextFit.charsPerLine(200f, fit.fontSp, 1f)
        assertTrue(WidgetTextFit.wrappedLines(fit.text, cpl) <= fit.maxLines)
    }

    @Test
    fun `con la letra del sistema agrandada caben menos lineas`() {
        // Antes las líneas salían del alto suponiendo letra normal: con letra
        // grande el texto se salía por abajo.
        val normal = WidgetTextFit.linesThatFit(120f, 15, fontScale = 1f)
        val large = WidgetTextFit.linesThatFit(120f, 15, fontScale = 1.5f)
        assertEquals(5, normal)
        assertEquals(3, large)
    }

    @Test
    fun `la etiqueta se quita antes que recortar la frase`() {
        // Frase del día con su tamaño inicial mínimo (110 dp de alto).
        val card = WidgetTextFit.card(
            text = longQuote, widthDp = 250f, heightDp = 110f, paddingDp = 14f,
            labelAllowed = true, hasFooter = true, fontScale = 1f,
        )
        assertFalse(card.showLabel)
        // Con sitio de sobra, la etiqueta se queda y la frase entra entera.
        val roomy = WidgetTextFit.card(
            text = longQuote, widthDp = 250f, heightDp = 220f, paddingDp = 14f,
            labelAllowed = true, hasFooter = true, fontScale = 1f,
        )
        assertTrue(roomy.showLabel)
        assertEquals(longQuote, roomy.body.text)
    }

    @Test
    fun `siempre cabe al menos una linea`() {
        val card = WidgetTextFit.card(
            text = longQuote, widthDp = 110f, heightDp = 40f, paddingDp = 10f,
            labelAllowed = false, hasFooter = false, fontScale = 1f,
        )
        assertEquals(1, card.body.maxLines)
        assertTrue(card.body.text.endsWith("…"))
    }
}

class MidnightRefreshTest {

    private val madrid = ZoneId.of("Europe/Madrid")

    private fun at(zone: ZoneId, y: Int, m: Int, d: Int, h: Int, min: Int = 0): ZonedDateTime =
        ZonedDateTime.of(LocalDateTime.of(y, m, d, h, min), zone)

    @Test
    fun `se programa un minuto despues de la proxima medianoche`() {
        val delay = WidgetUpdateWorker.delayUntilNextMidnight(at(madrid, 2026, 10, 27, 23, 30))
        assertEquals(Duration.ofMinutes(31), delay)
    }

    @Test
    fun `justo a medianoche espera al dia siguiente, no cero`() {
        val delay = WidgetUpdateWorker.delayUntilNextMidnight(at(madrid, 2026, 10, 27, 0))
        assertEquals(Duration.ofHours(24).plusMinutes(1), delay)
    }

    @Test
    fun `el dia que se atrasa la hora la espera dura una hora mas`() {
        // 25 oct 2026: a las 03:00 vuelven a ser las 02:00. Desde la 01:00 hasta la
        // medianoche pasan 24 h reales; contando en hora local salían 23 y el
        // refresco llegaba a las 23:01, todavía con el día anterior.
        val delay = WidgetUpdateWorker.delayUntilNextMidnight(at(madrid, 2026, 10, 25, 1))
        assertEquals(Duration.ofHours(24).plusMinutes(1), delay)
    }

    @Test
    fun `el dia que se adelanta la hora la espera dura una hora menos`() {
        // 29 mar 2026: a las 02:00 pasan a ser las 03:00. Desde la 01:00 son 22 h
        // reales; en hora local salían 23 y el refresco llegaba a la 01:01.
        val delay = WidgetUpdateWorker.delayUntilNextMidnight(at(madrid, 2026, 3, 29, 1))
        assertEquals(Duration.ofHours(22).plusMinutes(1), delay)
    }

    @Test
    fun `en cualquier zona el refresco cae al empezar el dia siguiente`() {
        // Incluye zonas donde un día del año no empieza a las 00:00 (Chile cambia de
        // hora a medianoche): el refresco debe caer al principio del día siguiente.
        val zones = listOf("America/Santiago", "America/Mexico_City", "Asia/Kolkata", "Pacific/Chatham", "UTC")
        var day = LocalDate.of(2026, 1, 1)
        while (day.year == 2026) {
            for (zoneId in zones) {
                val zone = ZoneId.of(zoneId)
                val now = ZonedDateTime.of(day.atTime(18, 0), zone)
                val fire = now.plus(WidgetUpdateWorker.delayUntilNextMidnight(now))
                val nextDay = day.plusDays(1)
                assertEquals("$zoneId $day", nextDay, fire.toLocalDate())
                assertEquals("$zoneId $day", nextDay.atStartOfDay(zone).plusMinutes(1), fire)
            }
            day = day.plusDays(1)
        }
    }

    @Test
    fun `al arrancar la app no se cancela el refresco de medianoche pendiente`() {
        // El propio refresco de medianoche arranca el proceso cuando está muerto, y
        // Application.onCreate lo reprogramaba con REPLACE: lo cancelaba justo antes
        // de ejecutarse y lo movía a la noche siguiente.
        assertEquals(
            ExistingWorkPolicy.KEEP,
            WidgetUpdateWorker.midnightPolicy(WidgetUpdateWorker.MidnightReason.APP_START),
        )
    }

    @Test
    fun `si cambia el reloj o ya se refresco, se recalcula`() {
        assertEquals(
            ExistingWorkPolicy.REPLACE,
            WidgetUpdateWorker.midnightPolicy(WidgetUpdateWorker.MidnightReason.CLOCK_CHANGED),
        )
        assertEquals(
            ExistingWorkPolicy.REPLACE,
            WidgetUpdateWorker.midnightPolicy(WidgetUpdateWorker.MidnightReason.AFTER_RUN),
        )
    }
}
