package com.momentummm.app.ui.system

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.momentummm.app.ui.theme.momentum

/** Un destino de la barra de navegación. */
data class MomentumNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String,
)

/**
 * Barra de navegación flotante de Momentum.
 *
 * Sustituye a la `NavigationBar` de Material, que con seis destinos partía las
 * etiquetas en dos líneas ("Commun / ity"). Aquí la etiqueta sólo aparece en el
 * destino activo, dentro de una pastilla que se expande: los iconos inactivos
 * recuperan el espacio y todo cabe en una línea sin truncar.
 */
@Composable
fun MomentumNavBar(
    items: List<MomentumNavItem>,
    selectedRoute: String?,
    onSelect: (MomentumNavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                horizontal = MomentumDesign.Spacing.compact,
                vertical = MomentumDesign.Spacing.compact
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(MomentumDesign.Size.navBarHeight)
                .clip(MomentumDesign.Shapes.pill)
                .background(MaterialTheme.momentum.surfaceElevated)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.momentum.border,
                    shape = MomentumDesign.Shapes.pill
                )
                .padding(horizontal = MomentumDesign.Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items.forEach { item ->
                NavBarItem(
                    item = item,
                    selected = item.route == selectedRoute,
                    onClick = { onSelect(item) },
                    modifier = Modifier.weight(if (item.route == selectedRoute) 1.9f else 1f)
                )
            }
        }
    }
}

@Composable
private fun NavBarItem(
    item: MomentumNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = MomentumDesign.Motion.springSnappy(),
        label = "nav_item_scale"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.momentum.textSecondary,
        animationSpec = MomentumDesign.Motion.tweenStandard(),
        label = "nav_item_color"
    )
    val pillHeight by animateDpAsState(
        targetValue = if (selected) 44.dp else 40.dp,
        animationSpec = MomentumDesign.Motion.springSnappy(),
        label = "nav_item_height"
    )

    Box(
        modifier = modifier
            .height(pillHeight)
            .scale(scale)
            .clip(MomentumDesign.Shapes.pill)
            .then(
                if (selected) {
                    Modifier.background(MaterialTheme.momentum.brandGradient)
                } else {
                    Modifier
                }
            )
            .selectable(
                selected = selected,
                onClick = {
                    if (!selected) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null,
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = MomentumDesign.Spacing.small)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = contentColor,
                modifier = Modifier.size(21.dp)
            )
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(MomentumDesign.Motion.tweenStandard()) +
                    expandHorizontally(MomentumDesign.Motion.tweenStandard()),
                exit = fadeOut(MomentumDesign.Motion.tweenStandard(MomentumDesign.Motion.durationFast)) +
                    shrinkHorizontally(MomentumDesign.Motion.tweenStandard(MomentumDesign.Motion.durationFast)),
            ) {
                Row {
                    Spacer(Modifier.width(MomentumDesign.Spacing.extraSmall + 2.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
        }
    }
}
