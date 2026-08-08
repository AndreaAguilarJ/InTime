package com.momentummm.app.ui.system

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.momentummm.app.ui.theme.momentum

/** Una barra del gráfico semanal. */
data class BarDatum(
    val label: String,
    val value: Float,
    val caption: String? = null,
)

/**
 * Gráfico de barras verticales.
 *
 * Material 3 1.1 no trae gráficas, así que se dibuja en Canvas. La barra con el
 * valor máximo se resalta con el color de acento y el resto queda atenuado: es la
 * forma más rápida de responder "¿qué día me pasé?" sin leer números.
 */
@Composable
fun BarChart(
    data: List<BarDatum>,
    modifier: Modifier = Modifier,
    barHeight: Dp = 132.dp,
    accent: Color = MaterialTheme.colorScheme.primary,
    valueFormatter: (Float) -> String = { it.toString() },
    highlightMax: Boolean = true,
) {
    if (data.isEmpty()) return

    val rawMax = data.maxOf { it.value }
    val maxValue = rawMax.coerceAtLeast(0.0001f)
    // rawMax ya es el máximo: reutilizarlo evita recalcular data.maxOf{} por cada
    // elemento dentro de indexOfFirst (era O(n²)) manteniendo el mismo resultado.
    val maxIndex = data.indexOfFirst { it.value == rawMax }
    val reveal by animateFloatAsState(
        targetValue = 1f,
        animationSpec = MomentumDesign.Motion.tweenReveal(),
        label = "bar_chart_reveal"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight),
            horizontalArrangement = Arrangement.spacedBy(MomentumDesign.Spacing.small),
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEachIndexed { index, datum ->
                val isMax = highlightMax && index == maxIndex
                val fraction = (datum.value / maxValue).coerceIn(0f, 1f) * reveal
                val barColor = if (isMax) accent else accent.copy(alpha = 0.28f)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = valueFormatter(datum.value),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMax) accent else MaterialTheme.momentum.textTertiary,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(MomentumDesign.Spacing.extraSmall))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction.coerceAtLeast(0.02f))
                                .clip(
                                    RoundedCornerShape(
                                        topStart = MomentumDesign.CornerRadius.small,
                                        topEnd = MomentumDesign.CornerRadius.small,
                                        bottomStart = 2.dp,
                                        bottomEnd = 2.dp,
                                    )
                                )
                                .background(barColor)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(MomentumDesign.Spacing.small))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MomentumDesign.Spacing.small)
        ) {
            data.forEachIndexed { index, datum ->
                Text(
                    text = datum.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (highlightMax && index == maxIndex) {
                        MaterialTheme.momentum.textPrimary
                    } else {
                        MaterialTheme.momentum.textTertiary
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Barra horizontal apilada: una sola línea que reparte el 100% entre categorías.
 * Alternativa compacta al donut cuando hay muchas categorías.
 */
@Composable
fun StackedBar(
    segments: List<RingSegment>,
    modifier: Modifier = Modifier,
    height: Dp = 12.dp,
    gap: Dp = 2.dp,
) {
    val total = segments.sumOf { it.value.toDouble() }.toFloat()
    if (total <= 0f) return

    val reveal by animateFloatAsState(
        targetValue = 1f,
        animationSpec = MomentumDesign.Motion.tweenReveal(),
        label = "stacked_bar_reveal"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        horizontalArrangement = Arrangement.spacedBy(gap)
    ) {
        segments.forEach { segment ->
            val weight = (segment.value / total).coerceAtLeast(0.01f)
            Box(
                modifier = Modifier
                    .weight(weight * reveal + 0.0001f)
                    .fillMaxHeight()
                    .clip(MomentumDesign.Shapes.pill)
                    .background(segment.color)
            )
        }
    }
}

/** Línea de tendencia sin ejes, para mostrar forma más que valores exactos. */
@Composable
fun Sparkline(
    values: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 48.dp,
    strokeWidth: Dp = 2.5.dp,
    showFill: Boolean = true,
) {
    if (values.size < 2) return

    val minValue = values.min()
    val maxValue = values.max()
    val range = (maxValue - minValue).takeIf { it > 0f } ?: 1f

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val stepX = size.width / (values.size - 1)
        val points = values.mapIndexed { index, value ->
            val normalized = (value - minValue) / range
            Offset(x = stepX * index, y = size.height - (normalized * size.height))
        }

        if (showFill) {
            val fill = Path().apply {
                moveTo(points.first().x, size.height)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, size.height)
                close()
            }
            drawPath(
                path = fill,
                brush = Brush.verticalGradient(
                    listOf(color.copy(alpha = 0.28f), Color.Transparent)
                )
            )
        }

        val line = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(
            path = line,
            color = color,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )
    }
}

/**
 * Selector segmentado tipo pill.
 *
 * Reemplaza a `SegmentedButton`, que no existe en Material 3 1.1. El indicador es
 * un fondo que se mueve por selección; el track hundido da la sensación de control
 * físico sin necesitar bordes.
 */
@Composable
fun SegmentedTabs(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .clip(MomentumDesign.Shapes.pill)
            .background(MaterialTheme.momentum.surfaceSunken)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(MomentumDesign.Shapes.pill)
                    .background(
                        if (selected) MaterialTheme.momentum.surface else Color.Transparent
                    )
                    .clickable(enabled = !selected) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelect(index)
                    }
                    .padding(vertical = MomentumDesign.Spacing.small),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) {
                        MaterialTheme.momentum.textPrimary
                    } else {
                        MaterialTheme.momentum.textSecondary
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Fila de leyenda: punto de color, etiqueta y valor alineado a la derecha. */
@Composable
fun LegendRow(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    share: Float? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(MomentumDesign.Shapes.pill)
                .background(color)
        )
        Spacer(Modifier.width(MomentumDesign.Spacing.compact))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.momentum.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (share != null) {
                Spacer(Modifier.height(MomentumDesign.Spacing.extraSmall))
                ProgressBar(progress = share, color = color, height = 4.dp)
            }
        }
        Spacer(Modifier.width(MomentumDesign.Spacing.compact))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.momentum.textSecondary
        )
    }
}
