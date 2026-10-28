package com.momentummm.app.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.layout.Spacer
import androidx.glance.layout.height
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlin.math.floor

/**
 * Tarjeta de texto de los widgets «Frase del día» y «Mensaje motivacional».
 *
 * Glance no pone puntos suspensivos (su `glance_text.xml` no lleva ellipsize y
 * RemoteViews no puede activarlo): un texto con más líneas de las que caben se
 * cortaba a media frase sin aviso. Con el tamaño inicial de la Frase del día
 * (110 dp de alto) entraba UNA línea, así que casi todas las frases salían
 * mutiladas. Ahora, por orden: se reduce la letra, se quita la etiqueta y, solo
 * si aun así no cabe, se corta en una palabra con «…». Las cuentas incluyen el
 * tamaño de letra del sistema: con letra grande el texto se salía por abajo.
 */
@Composable
fun WidgetTextCard(label: String, body: String, footer: String?, onClick: Action) {
    val size = LocalSize.current
    val fontScale = LocalContext.current.resources.configuration.fontScale
    val compact = size.height.value < WidgetTextFit.COMPACT_BELOW_DP
    val padding = if (compact) WidgetDimens.compactPadding else WidgetDimens.padding
    val card = WidgetTextFit.card(
        text = body,
        widthDp = size.width.value,
        heightDp = size.height.value,
        paddingDp = padding.value,
        labelAllowed = size.width.value >= WidgetTextFit.LABEL_MIN_WIDTH_DP,
        hasFooter = footer != null,
        fontScale = fontScale,
    )

    WidgetSurface(onClick = onClick, padding = padding) {
        if (card.showLabel) {
            WidgetLabel(text = label, fontSize = WidgetTextFit.LABEL_SP.sp)
            Spacer(modifier = GlanceModifier.height(WidgetTextFit.GAP_DP.dp))
        }
        Text(
            text = card.body.text,
            style = TextStyle(
                color = WidgetColors.onSurface,
                fontSize = card.body.fontSp.sp,
                fontWeight = if (compact) FontWeight.Normal else FontWeight.Medium,
            ),
            maxLines = card.body.maxLines,
        )
        if (card.showFooter && footer != null) {
            Spacer(modifier = GlanceModifier.height(WidgetTextFit.GAP_DP.dp))
            Text(
                text = footer,
                style = TextStyle(color = WidgetColors.onSurfaceVariant, fontSize = WidgetTextFit.FOOTER_SP.sp),
                maxLines = 1,
            )
        }
    }
}

/** Cómo cabe un texto en el hueco de un widget. Puro para probarlo. */
internal object WidgetTextFit {

    /** Ancho medio de un carácter, en fracción de la letra (Roboto ≈ 0,5; con margen). */
    private const val AVG_CHAR_EM = 0.55f

    /** Alto de línea de un TextView, en fracción de la letra. */
    private const val LINE_HEIGHT_EM = 1.34f

    private const val MAX_LINES = 8
    const val ELLIPSIS = "…"

    const val COMPACT_BELOW_DP = 96f
    /** La etiqueta más larga («Nachricht des Tages») mide ~115 dp. */
    const val LABEL_MIN_WIDTH_DP = 160f
    const val LABEL_SP = 11
    const val FOOTER_SP = 12
    const val GAP_DP = 6f

    private val REGULAR_SIZES = listOf(15, 14, 13)
    private val COMPACT_SIZES = listOf(13, 12)

    data class Fit(val text: String, val fontSp: Int, val maxLines: Int)

    data class Card(val showLabel: Boolean, val showFooter: Boolean, val body: Fit)

    fun lineHeightDp(fontSp: Int, fontScale: Float): Float = fontSp * fontScale * LINE_HEIGHT_EM

    fun charsPerLine(widthDp: Float, fontSp: Int, fontScale: Float): Int =
        floor(widthDp / (fontSp * fontScale * AVG_CHAR_EM)).toInt().coerceAtLeast(1)

    fun linesThatFit(heightDp: Float, fontSp: Int, fontScale: Float): Int =
        floor(heightDp / lineHeightDp(fontSp, fontScale)).toInt().coerceIn(1, MAX_LINES)

    /** Líneas que ocupa [text] partiendo por palabras, como un TextView. */
    fun wrappedLines(text: String, charsPerLine: Int): Int {
        var lines = 0
        var current = 0
        for (word in text.split(' ', '\n').filter { it.isNotEmpty() }) {
            if (word.length > charsPerLine) {
                // Una palabra más larga que la línea se parte en varias.
                if (current > 0) lines++
                lines += word.length / charsPerLine
                current = word.length % charsPerLine
                continue
            }
            current = when {
                current == 0 -> word.length
                current + 1 + word.length <= charsPerLine -> current + 1 + word.length
                else -> { lines++; word.length }
            }
        }
        return lines + if (current > 0) 1 else 0
    }

    /**
     * El tamaño más grande de [sizes] (de mayor a menor) en el que [text] cabe
     * entero; si no cabe en ninguno, el más pequeño con el texto cortado.
     */
    fun fit(text: String, widthDp: Float, heightDp: Float, sizes: List<Int>, fontScale: Float): Fit {
        fitWhole(text, widthDp, heightDp, sizes, fontScale)?.let { return it }
        val sp = sizes.last()
        val lines = linesThatFit(heightDp, sp, fontScale)
        return Fit(truncate(text, charsPerLine(widthDp, sp, fontScale), lines), sp, lines)
    }

    private fun fitWhole(text: String, widthDp: Float, heightDp: Float, sizes: List<Int>, fontScale: Float): Fit? =
        sizes.firstNotNullOfOrNull { sp ->
            val lines = linesThatFit(heightDp, sp, fontScale)
            Fit(text, sp, lines).takeIf { wrappedLines(text, charsPerLine(widthDp, sp, fontScale)) <= lines }
        }

    /** Corta [text] en una palabra para que, con «…», ocupe como mucho [lines]. */
    fun truncate(text: String, charsPerLine: Int, lines: Int): String {
        if (wrappedLines(text, charsPerLine) <= lines) return text
        val words = text.split(' ').filter { it.isNotEmpty() }
        var best: String? = null
        for (count in 1..words.size) {
            val candidate = words.take(count).joinToString(" ").trimEnd(',', ';', ':', '.', '—', '-')
            if (wrappedLines(candidate + ELLIPSIS, charsPerLine) > lines) break
            best = candidate
        }
        // Sin una sola palabra que quepa: corte a pelo.
        return (best ?: text.take((charsPerLine * lines - 1).coerceAtLeast(1)).trimEnd()) + ELLIPSIS
    }

    /**
     * Reparto de la tarjeta. Antes de cortar el texto se prueba sin la etiqueta:
     * el widget ya dice qué es, la frase no se entiende a medias.
     */
    fun card(
        text: String,
        widthDp: Float,
        heightDp: Float,
        paddingDp: Float,
        labelAllowed: Boolean,
        hasFooter: Boolean,
        fontScale: Float,
    ): Card {
        val innerWidth = widthDp - paddingDp * 2
        val innerHeight = heightDp - paddingDp * 2
        if (heightDp < COMPACT_BELOW_DP) {
            return Card(false, false, fit(text, innerWidth, innerHeight, COMPACT_SIZES, fontScale))
        }
        val footerDp = if (hasFooter) lineHeightDp(FOOTER_SP, fontScale) + GAP_DP else 0f
        val labelDp = lineHeightDp(LABEL_SP, fontScale) + GAP_DP
        if (labelAllowed) {
            fitWhole(text, innerWidth, innerHeight - labelDp - footerDp, REGULAR_SIZES, fontScale)
                ?.let { return Card(true, hasFooter, it) }
        }
        return Card(false, hasFooter, fit(text, innerWidth, innerHeight - footerDp, REGULAR_SIZES, fontScale))
    }
}
