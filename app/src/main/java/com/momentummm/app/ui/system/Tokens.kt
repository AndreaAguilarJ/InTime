package com.momentummm.app.ui.system

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Tokens del sistema de diseño de Momentum.
 *
 * Regla: ninguna pantalla debería escribir un `dp` mágico. Si un valor se repite
 * dos veces, vive aquí.
 */
object MomentumDesign {

    object Spacing {
        /** 2dp — separaciones ópticas dentro de un mismo bloque de texto. */
        val hairline = 2.dp
        val extraSmall = 4.dp
        val small = 8.dp
        /** 12dp — el gap por defecto entre elementos de una lista densa. */
        val compact = 12.dp
        val medium = 16.dp
        val cozy = 20.dp
        val large = 24.dp
        val extraLarge = 32.dp
        val huge = 48.dp

        /** Padding horizontal estándar de pantalla. */
        val screenHorizontal = 20.dp
        /** Espacio reservado al final de las listas para que la nav no tape contenido. */
        val bottomBarClearance = 108.dp
    }

    object CornerRadius {
        val extraSmall = 6.dp
        val small = 8.dp
        val medium = 12.dp
        val large = 16.dp
        val extraLarge = 24.dp
        /** 28dp — tarjetas héroe y bottom sheets. */
        val hero = 28.dp
        /** Radio efectivamente circular para pills. */
        val pill = 999.dp
    }

    object Shapes {
        val card: Shape = RoundedCornerShape(CornerRadius.extraLarge)
        val cardCompact: Shape = RoundedCornerShape(CornerRadius.large)
        val hero: Shape = RoundedCornerShape(CornerRadius.hero)
        val sheet: Shape = RoundedCornerShape(topStart = CornerRadius.hero, topEnd = CornerRadius.hero)
        val pill: Shape = RoundedCornerShape(CornerRadius.pill)
        val field: Shape = RoundedCornerShape(CornerRadius.medium)
        val icon: Shape = RoundedCornerShape(CornerRadius.medium)
    }

    object Elevation {
        val none = 0.dp
        val small = 1.dp
        val medium = 3.dp
        val large = 8.dp
        val extraLarge = 16.dp
    }

    object Size {
        /** Icono dentro de un contenedor cuadrado (app icons, tiles de ajustes). */
        val iconTile = 44.dp
        val iconTileSmall = 36.dp
        val iconTileLarge = 56.dp
        val icon = 20.dp
        val iconSmall = 16.dp
        val iconLarge = 24.dp
        val avatar = 40.dp
        /** Diámetro del anillo de progreso principal del dashboard. */
        val ring = 208.dp
        /** Diámetro del anillo del timer de enfoque. */
        val ringHero = 268.dp
        val ringStroke = 14.dp
        val barTrack = 6.dp
        val navBarHeight = 68.dp
        val touchTarget = 48.dp
    }

    object Alpha {
        const val disabled = 0.38f
        const val subtle = 0.08f
        const val soft = 0.14f
        const val medium = 0.24f
        const val strong = 0.55f
        const val scrim = 0.72f
    }

    /**
     * Curvas y specs de animación. La app usa muelles para todo lo que responde a un
     * gesto (escala al pulsar, entradas) y tweens para transiciones de datos, donde un
     * rebote confundiría la lectura de una cifra.
     */
    object Motion {
        val emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
        val standard: Easing = CubicBezierEasing(0.2f, 0f, 0.2f, 1f)
        val decelerate: Easing = CubicBezierEasing(0f, 0f, 0.2f, 1f)

        const val durationFast = 150
        const val durationMedium = 280
        const val durationSlow = 450
        /** Barrido del anillo/gráficas al entrar. */
        const val durationReveal = 900

        fun <T> springSnappy(): FiniteAnimationSpec<T> = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow,
        )

        fun <T> springBouncy(): FiniteAnimationSpec<T> = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        )

        fun <T> tweenStandard(durationMillis: Int = durationMedium): FiniteAnimationSpec<T> =
            tween(durationMillis = durationMillis, easing = standard)

        fun <T> tweenReveal(): FiniteAnimationSpec<T> =
            tween(durationMillis = durationReveal, easing = decelerate)
    }
}
