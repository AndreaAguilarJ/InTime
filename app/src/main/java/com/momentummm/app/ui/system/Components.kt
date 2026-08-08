package com.momentummm.app.ui.system

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.momentummm.app.ui.theme.MomentumTextStyles
import com.momentummm.app.ui.theme.momentum

/**
 * Anillo de progreso con extremos redondeados y relleno de gradiente.
 *
 * Dibujado a mano en Canvas en lugar de usar `CircularProgressIndicator` porque
 * necesitamos: trazo grueso con caps redondeados, gradiente a lo largo del arco y
 * contenido libre en el centro. Empieza a las 12 en punto y avanza en horario.
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = MomentumDesign.Size.ring,
    strokeWidth: Dp = MomentumDesign.Size.ringStroke,
    trackColor: Color = MaterialTheme.momentum.surfaceSunken,
    brush: Brush = MaterialTheme.momentum.brandGradient,
    animate: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = if (animate) MomentumDesign.Motion.tweenReveal() else MomentumDesign.Motion.tweenStandard(0),
        label = "ring_progress"
    )

    Box(
        modifier = modifier.size(diameter),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(diameter)) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            if (animated > 0f) {
                drawArc(
                    brush = brush,
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        content()
    }
}

/**
 * Anillo dividido en segmentos proporcionales, uno por categoría.
 * Se usa para desglosar el tiempo de pantalla por tipo de app.
 */
@Composable
fun SegmentedRing(
    segments: List<RingSegment>,
    modifier: Modifier = Modifier,
    diameter: Dp = MomentumDesign.Size.ring,
    strokeWidth: Dp = MomentumDesign.Size.ringStroke,
    trackColor: Color = MaterialTheme.momentum.surfaceSunken,
    gapDegrees: Float = 3f,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val total = segments.sumOf { it.value.toDouble() }.toFloat()
    val reveal by animateFloatAsState(
        targetValue = 1f,
        animationSpec = MomentumDesign.Motion.tweenReveal(),
        label = "segmented_ring_reveal"
    )

    Box(
        modifier = modifier.size(diameter),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(diameter)) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            if (total <= 0f) return@Canvas

            var cursor = -90f
            segments.forEach { segment ->
                val share = segment.value / total
                val sweep = (360f * share * reveal) - gapDegrees
                if (sweep > 0f) {
                    drawArc(
                        color = segment.color,
                        startAngle = cursor,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                cursor += 360f * share * reveal
            }
        }
        content()
    }
}

data class RingSegment(
    val label: String,
    val value: Float,
    val color: Color,
)

/** Encabezado de sección: título, subtítulo opcional y acción a la derecha. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    overline: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (overline != null) {
                Text(
                    text = overline.uppercase(),
                    style = MomentumTextStyles.overline,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(MomentumDesign.Spacing.extraSmall))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.momentum.textPrimary
            )
            if (subtitle != null) {
                Spacer(Modifier.height(MomentumDesign.Spacing.hairline))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.momentum.textSecondary
                )
            }
        }
        if (action != null) {
            Spacer(Modifier.width(MomentumDesign.Spacing.small))
            action()
        }
    }
}

/**
 * Tarjeta de métrica para rejillas tipo bento: icono, valor grande, etiqueta y
 * delta opcional. Pensada para ocupar la mitad del ancho en un [Row].
 */
@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    delta: MetricDelta? = null,
    caption: String? = null,
    onClick: (() -> Unit)? = null,
) {
    MomentumCard(
        modifier = modifier,
        onClick = onClick,
        shape = MomentumDesign.Shapes.cardCompact,
    ) {
        Column(modifier = Modifier.padding(MomentumDesign.Spacing.medium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (icon != null) {
                    IconTile(
                        icon = icon,
                        tint = accent,
                        size = MomentumDesign.Size.iconTileSmall,
                    )
                } else {
                    Spacer(Modifier.size(0.dp))
                }
                if (delta != null) {
                    DeltaBadge(delta)
                }
            }
            Spacer(Modifier.height(MomentumDesign.Spacing.compact))
            Text(
                text = value,
                style = MomentumTextStyles.metric,
                color = MaterialTheme.momentum.textPrimary
            )
            Spacer(Modifier.height(MomentumDesign.Spacing.hairline))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.momentum.textSecondary
            )
            if (caption != null) {
                Spacer(Modifier.height(MomentumDesign.Spacing.small))
                Text(
                    text = caption,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.momentum.textTertiary
                )
            }
        }
    }
}

/**
 * Variación de una métrica frente a un periodo anterior.
 *
 * [goodWhenDown] es clave en una app de bienestar digital: bajar el tiempo de
 * pantalla es bueno, así que el color no puede derivarse solo del signo.
 */
data class MetricDelta(
    val text: String,
    val isUp: Boolean,
    val goodWhenDown: Boolean = true,
)

@Composable
fun DeltaBadge(delta: MetricDelta, modifier: Modifier = Modifier) {
    val positive = if (delta.goodWhenDown) !delta.isUp else delta.isUp
    val color = if (positive) MaterialTheme.momentum.success else MaterialTheme.momentum.danger
    val arrow = if (delta.isUp) "\u2191" else "\u2193"

    Row(
        modifier = modifier
            .clip(MomentumDesign.Shapes.pill)
            .background(color.copy(alpha = MomentumDesign.Alpha.soft))
            .padding(horizontal = MomentumDesign.Spacing.small, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$arrow ${delta.text}",
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

/** Estado vacío consistente: icono, título, mensaje y acción opcional. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    action: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = MomentumDesign.Spacing.large,
                vertical = MomentumDesign.Spacing.extraLarge
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconTile(
            icon = icon,
            tint = accent,
            size = MomentumDesign.Size.iconTileLarge,
        )
        Spacer(Modifier.height(MomentumDesign.Spacing.medium))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.momentum.textPrimary,
            textAlign = TextAlign.Center
        )
        if (message != null) {
            Spacer(Modifier.height(MomentumDesign.Spacing.small))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.momentum.textSecondary,
                textAlign = TextAlign.Center
            )
        }
        if (action != null) {
            Spacer(Modifier.height(MomentumDesign.Spacing.cozy))
            action()
        }
    }
}

/** Barra de progreso horizontal fina con extremos redondeados. */
@Composable
fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = MomentumDesign.Size.barTrack,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.momentum.surfaceSunken,
    animate: Boolean = true,
) {
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = if (animate) MomentumDesign.Motion.tweenReveal() else MomentumDesign.Motion.tweenStandard(0),
        label = "bar_progress"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val radius = size.height / 2f
        drawRoundRect(
            color = trackColor,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
        )
        if (animated > 0f) {
            drawRoundRect(
                color = color,
                size = Size(width = (size.width * animated).coerceAtLeast(size.height), height = size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
            )
        }
    }
}

/**
 * Indicador de progreso por pasos.
 *
 * Una barra continua sólo dice "vas por la mitad"; los segmentos dicen además
 * "quedan tres". En un onboarding, saber cuánto falta es lo que evita el abandono,
 * así que se dibuja un tramo por paso.
 */
@Composable
fun StepProgress(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.momentum.surfaceSunken,
    height: Dp = 6.dp,
) {
    if (totalSteps <= 0) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        horizontalArrangement = Arrangement.spacedBy(MomentumDesign.Spacing.extraSmall)
    ) {
        repeat(totalSteps) { index ->
            val done = index <= currentStep
            val color by androidx.compose.animation.animateColorAsState(
                targetValue = if (done) activeColor else inactiveColor,
                animationSpec = MomentumDesign.Motion.tweenStandard(),
                label = "step_$index"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(MomentumDesign.Shapes.pill)
                    .background(color)
            )
        }
    }
}

/** Fila con etiqueta a la izquierda y valor a la derecha. Base de los ajustes. */
@Composable
fun LabelValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(MomentumDesign.Size.icon)
            )
            Spacer(Modifier.width(MomentumDesign.Spacing.compact))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.momentum.textSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.momentum.textPrimary
        )
    }
}
