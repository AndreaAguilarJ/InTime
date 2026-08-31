package com.momentummm.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Núcleo del tema Momentum, compartido entre Android e iOS.
 *
 * Aquí vive todo lo que no depende del sistema: los dos esquemas de Material 3,
 * los colores semánticos propios y el composable que los publica. Cada plataforma
 * añade encima lo que solo ella tiene -- en Android, los colores dinámicos de
 * Material You y el tintado de las barras del sistema.
 */

private val DarkColorSchemeBase = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = Neutral950,
    primaryContainer = Indigo800,
    onPrimaryContainer = Indigo100,
    inversePrimary = Indigo600,

    secondary = SecondaryDark,
    onSecondary = Neutral950,
    secondaryContainer = Color(0xFF0C3D2E),
    onSecondaryContainer = Mint300,

    tertiary = Violet400,
    onTertiary = Neutral950,
    tertiaryContainer = Violet900,
    onTertiaryContainer = Violet200,

    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceElevatedDark,
    onSurfaceVariant = TextSecondaryDark,
    surfaceTint = PrimaryDark,
    inverseSurface = Neutral100,
    inverseOnSurface = Neutral900,

    error = ErrorDark,
    onError = Neutral950,
    errorContainer = Color(0xFF4C1220),
    onErrorContainer = Rose300,

    outline = OutlineDark,
    outlineVariant = Neutral800,
    scrim = Color(0xFF000000),
)

private val LightColorSchemeBase = lightColorScheme(
    primary = Primary,
    onPrimary = Neutral0,
    primaryContainer = Indigo50,
    onPrimaryContainer = Indigo800,
    inversePrimary = Indigo300,

    secondary = Secondary,
    onSecondary = Neutral0,
    secondaryContainer = Color(0xFFECFDF5),
    onSecondaryContainer = Color(0xFF065F46),

    tertiary = Violet600,
    onTertiary = Neutral0,
    tertiaryContainer = Color(0xFFF3EEFF),
    onTertiaryContainer = Violet900,

    background = BackgroundWhite,
    onBackground = TextPrimary,
    surface = SurfaceLight,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceSunkenLight,
    onSurfaceVariant = TextSecondary,
    surfaceTint = Primary,
    inverseSurface = Neutral900,
    inverseOnSurface = Neutral50,

    error = Error,
    onError = Neutral0,
    errorContainer = Color(0xFFFFE4E8),
    onErrorContainer = Color(0xFF8C1030),

    outline = OutlineLight,
    outlineVariant = Neutral200,
    scrim = Color(0xFF000000),
)

/**
 * Colores semánticos que Material 3 no modela pero la app necesita en varias
 * pantallas: jerarquía de superficies, estados de progreso y gradientes de marca.
 */
@Immutable
data class MomentumColors(
    val isDark: Boolean,
    /** Fondo del lienzo. Más profundo que [surface]. */
    val canvas: Color,
    /** Tarjeta estándar. */
    val surface: Color,
    /** Tarjeta sobre tarjeta (sheets, tarjetas destacadas). */
    val surfaceElevated: Color,
    /** Hueco: campos, tracks de progreso, chips no seleccionados. */
    val surfaceSunken: Color,
    val border: Color,
    val borderStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val info: Color,
    val weeksLived: Color,
    val weeksFuture: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
    val categorySocial: Color,
    val categoryEntertainment: Color,
    val categoryProductivity: Color,
    val categoryGames: Color,
    val categoryCommunication: Color,
    val categoryOther: Color,
    val dataSeries: List<Color>,
) {
    /** Gradiente de marca en diagonal, para héroes y CTAs. */
    val brandGradient: Brush
        get() = Brush.linearGradient(listOf(gradientStart, gradientEnd))

    /** Gradiente vertical suave para fondos de sección. */
    fun veil(alpha: Float = 0.14f): Brush = Brush.verticalGradient(
        listOf(gradientStart.copy(alpha = alpha), Color.Transparent)
    )

    /** Color de acento de una serie de datos por índice, con wrap-around. */
    fun series(index: Int): Color =
        dataSeries[((index % dataSeries.size) + dataSeries.size) % dataSeries.size]
}

private val LightMomentumColors = MomentumColors(
    isDark = false,
    canvas = BackgroundWhite,
    surface = SurfaceLight,
    surfaceElevated = SurfaceElevatedLight,
    surfaceSunken = SurfaceSunkenLight,
    border = OutlineLight,
    borderStrong = Neutral300,
    textPrimary = TextPrimary,
    textSecondary = TextSecondary,
    textTertiary = TextTertiary,
    success = Success,
    warning = Warning,
    danger = Error,
    info = Sky700,
    weeksLived = WeeksLived,
    weeksFuture = WeeksFuture,
    gradientStart = GradientStart,
    gradientEnd = GradientEnd,
    categorySocial = CategorySocial,
    categoryEntertainment = CategoryEntertainment,
    categoryProductivity = CategoryProductivity,
    categoryGames = CategoryGames,
    categoryCommunication = CategoryCommunication,
    categoryOther = CategoryOther,
    dataSeries = DataSeries,
)

private val DarkMomentumColors = MomentumColors(
    isDark = true,
    canvas = BackgroundDark,
    surface = SurfaceDark,
    surfaceElevated = SurfaceElevatedDark,
    surfaceSunken = Neutral850,
    border = OutlineDark,
    borderStrong = Neutral700,
    textPrimary = TextPrimaryDark,
    textSecondary = TextSecondaryDark,
    textTertiary = TextTertiaryDark,
    success = SuccessDark,
    warning = WarningDark,
    danger = ErrorDark,
    info = Sky400,
    weeksLived = WeeksLivedDark,
    weeksFuture = WeeksFutureDark,
    gradientStart = Indigo500,
    gradientEnd = Violet500,
    categorySocial = Violet400,
    categoryEntertainment = Rose400,
    categoryProductivity = Mint400,
    categoryGames = Amber400,
    categoryCommunication = Sky400,
    categoryOther = Neutral500,
    dataSeries = listOf(Indigo400, Violet400, Sky400, Mint400, Amber400, Rose400, Coral400),
)

val LocalMomentumColors = staticCompositionLocalOf { LightMomentumColors }

/**
 * Esquema Material 3 de Momentum. [accent] sustituye el color primario cuando la
 * persona ha elegido uno propio en los ajustes.
 */
fun momentumColorScheme(darkTheme: Boolean, accent: Color? = null): ColorScheme {
    val base = if (darkTheme) DarkColorSchemeBase else LightColorSchemeBase
    return if (accent != null) base.copy(primary = accent, surfaceTint = accent) else base
}

/** Colores semánticos propios, con el acento elegido aplicado al gradiente. */
fun momentumSemanticColors(darkTheme: Boolean, accent: Color? = null): MomentumColors {
    val base = if (darkTheme) DarkMomentumColors else LightMomentumColors
    return if (accent != null) base.copy(gradientStart = accent) else base
}

/**
 * Aplica el tema Momentum. Es el punto común: iOS lo usa directamente y Android
 * lo envuelve para añadir Material You y el tintado de las barras del sistema.
 *
 * @param colorScheme permite inyectar un esquema calculado por la plataforma
 *   (los colores dinámicos de Android); si es `null` se usa el de Momentum.
 */
@Composable
fun MomentumThemeCore(
    darkTheme: Boolean,
    accent: Color? = null,
    colorScheme: ColorScheme? = null,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalMomentumColors provides momentumSemanticColors(darkTheme, accent)
    ) {
        MaterialTheme(
            colorScheme = colorScheme ?: momentumColorScheme(darkTheme, accent),
            typography = Typography,
            content = content,
        )
    }
}

/** Acceso a los colores semánticos de Momentum: `MaterialTheme.momentum.success`. */
val MaterialTheme.momentum: MomentumColors
    @Composable
    get() = LocalMomentumColors.current

/**
 * Alias histórico. Se mantiene para no romper llamadas existentes; internamente ya
 * respeta el modo de tema elegido por el usuario (antes leía el del sistema).
 */
val MaterialTheme.customColors: MomentumColors
    @Composable
    get() = LocalMomentumColors.current
