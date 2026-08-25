package com.momentummm.app.ui.system

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.momentummm.app.ui.theme.momentum

enum class ButtonStyle {
    Primary,
    Secondary,
    Outline,
    Text
}

enum class ButtonSize {
    Small,
    Medium,
    Large
}

/**
 * Tarjeta base de Momentum.
 *
 * Superficie plana con borde de un pixel en lugar de sombra: en modo oscuro las
 * sombras no aportan separación y ensucian los bordes, así que la jerarquía se
 * construye con color de superficie + borde.
 */
@Composable
fun MomentumCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = MomentumDesign.Shapes.card,
    containerColor: Color = MaterialTheme.momentum.surface,
    contentColor: Color = MaterialTheme.momentum.textPrimary,
    elevation: Dp = MomentumDesign.Elevation.none,
    border: BorderStroke? = null,
    isPressed: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed || (onClick != null && pressed)) 0.975f else 1f,
        animationSpec = MomentumDesign.Motion.springSnappy(),
        label = "card_scale"
    )

    val resolvedBorder = border ?: BorderStroke(1.dp, MaterialTheme.momentum.border)

    Card(
        modifier = modifier
            .scale(scale)
            .then(
                if (onClick != null) {
                    Modifier
                        .semantics { role = Role.Button }
                        .clickable(
                            interactionSource = interactionSource,
                            indication = LocalIndication.current,
                            role = Role.Button,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onClick()
                            }
                        )
                } else Modifier
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = resolvedBorder,
        content = content
    )
}

/**
 * Botón de Momentum. Forma de pill, feedback de escala al pulsar y alturas
 * alineadas con el objetivo mínimo táctil de 48dp en el tamaño medio.
 */
@Composable
fun MomentumButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: ButtonStyle = ButtonStyle.Primary,
    size: ButtonSize = ButtonSize.Medium,
    icon: ImageVector? = null,
    content: @Composable RowScope.() -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = MomentumDesign.Motion.springSnappy(),
        label = "button_scale"
    )

    val height = when (size) {
        ButtonSize.Small -> 48.dp
        ButtonSize.Medium -> 48.dp
        ButtonSize.Large -> 56.dp
    }
    val horizontalPadding = when (size) {
        ButtonSize.Small -> 16.dp
        ButtonSize.Medium -> 22.dp
        ButtonSize.Large -> 28.dp
    }
    val iconSize = when (size) {
        ButtonSize.Small -> 16.dp
        ButtonSize.Medium -> 18.dp
        ButtonSize.Large -> 20.dp
    }
    val labelStyle = when (size) {
        ButtonSize.Small -> MaterialTheme.typography.labelMedium
        ButtonSize.Medium -> MaterialTheme.typography.labelLarge
        ButtonSize.Large -> MaterialTheme.typography.titleMedium
    }

    val baseModifier = modifier
        .height(height)
        .scale(scale)
    val shape = MomentumDesign.Shapes.pill
    val contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 0.dp)

    val body: @Composable RowScope.() -> Unit = {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(iconSize)
            )
            Spacer(modifier = Modifier.width(MomentumDesign.Spacing.small))
        }
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalTextStyle provides labelStyle
        ) {
            content()
        }
    }

    val tap = {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onClick()
    }

    when (style) {
        ButtonStyle.Primary -> Button(
            onClick = tap,
            modifier = baseModifier,
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.momentum.surfaceSunken,
                disabledContentColor = MaterialTheme.momentum.textTertiary,
            ),
            elevation = null,
            interactionSource = interactionSource,
            contentPadding = contentPadding,
            content = body
        )

        ButtonStyle.Secondary -> Button(
            onClick = tap,
            modifier = baseModifier,
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = MomentumDesign.Alpha.soft),
                contentColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.momentum.surfaceSunken,
                disabledContentColor = MaterialTheme.momentum.textTertiary,
            ),
            elevation = null,
            interactionSource = interactionSource,
            contentPadding = contentPadding,
            content = body
        )

        ButtonStyle.Outline -> OutlinedButton(
            onClick = tap,
            modifier = baseModifier,
            enabled = enabled,
            shape = shape,
            border = BorderStroke(1.5.dp, MaterialTheme.momentum.borderStrong),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.momentum.textPrimary,
                disabledContentColor = MaterialTheme.momentum.textTertiary,
            ),
            interactionSource = interactionSource,
            contentPadding = contentPadding,
            content = body
        )

        ButtonStyle.Text -> TextButton(
            onClick = tap,
            modifier = baseModifier,
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
                disabledContentColor = MaterialTheme.momentum.textTertiary,
            ),
            interactionSource = interactionSource,
            contentPadding = contentPadding,
            content = body
        )
    }
}

/** Tarjeta con relleno de gradiente, para héroes y promos. */
@Composable
fun MomentumGradientCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    gradient: Brush = MaterialTheme.momentum.brandGradient,
    shape: Shape = MomentumDesign.Shapes.hero,
    contentColor: Color = Color.White,
    content: @Composable ColumnScope.() -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (onClick != null && pressed) 0.975f else 1f,
        animationSpec = MomentumDesign.Motion.springSnappy(),
        label = "gradient_card_scale"
    )

    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(gradient)
            .then(
                if (onClick != null) {
                    Modifier
                        .semantics { role = Role.Button }
                        .clickable(
                            interactionSource = interactionSource,
                            indication = LocalIndication.current,
                            role = Role.Button,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onClick()
                            }
                        )
                } else Modifier
            )
            .padding(MomentumDesign.Spacing.cozy),
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides contentColor
        ) {
            content()
        }
    }
}

/**
 * Anillo de progreso circular con porcentaje opcional.
 * Delegado en [ProgressRing] para tener extremos redondeados y gradiente.
 */
@Composable
fun MomentumProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    showPercentage: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.momentum.surfaceSunken,
    strokeWidth: Dp = MomentumDesign.Size.ringStroke
) {
    ProgressRing(
        progress = progress,
        modifier = modifier,
        diameter = 132.dp,
        strokeWidth = strokeWidth,
        trackColor = trackColor,
        brush = Brush.sweepGradient(listOf(color, color.copy(alpha = 0.55f), color)),
    ) {
        if (showPercentage) {
            Text(
                text = "${(progress.coerceIn(0f, 1f) * 100).toInt()}%",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.momentum.textPrimary
            )
        }
    }
}

@Composable
fun MomentumDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
    color: Color = MaterialTheme.momentum.border
) {
    Divider(modifier = modifier, thickness = thickness, color = color)
}

/** Chip seleccionable. Usado para filtros de rango y categorías. */
@Composable
fun MomentumChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val haptics = LocalHapticFeedback.current
    val background by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.momentum.surfaceSunken,
        animationSpec = MomentumDesign.Motion.tweenStandard(),
        label = "chip_background"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.momentum.textSecondary,
        animationSpec = MomentumDesign.Motion.tweenStandard(),
        label = "chip_content"
    )

    Surface(
        modifier = modifier
            .semantics {
                role = Role.Checkbox
                selected = isSelected
            }
            .minimumInteractiveComponentSize()
            .clip(MomentumDesign.Shapes.pill)
            .clickable(role = Role.Checkbox) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        color = background,
        shape = MomentumDesign.Shapes.pill
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = MomentumDesign.Spacing.medium,
                vertical = 9.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MomentumDesign.Spacing.small)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(MomentumDesign.Size.iconSmall)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor
            )
        }
    }
}

/** Contenedor cuadrado con fondo teñido para iconos. */
@Composable
fun IconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    background: Color = tint.copy(alpha = MomentumDesign.Alpha.soft),
    size: Dp = MomentumDesign.Size.iconTile,
    shape: Shape = RoundedCornerShape(size / 3),
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}
