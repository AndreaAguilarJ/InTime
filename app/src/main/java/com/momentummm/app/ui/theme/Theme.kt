package com.momentummm.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.momentummm.app.data.manager.ThemeManager

private val DarkColorScheme = darkColorScheme(
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

private val LightColorScheme = lightColorScheme(
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
    fun series(index: Int): Color = dataSeries[((index % dataSeries.size) + dataSeries.size) % dataSeries.size]
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

@Composable
fun MomentumTheme(
    themeManager: ThemeManager? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val manager = themeManager ?: ThemeManager(context)

    val themeMode by manager.themeMode.collectAsStateWithLifecycle(initialValue = ThemeManager.ThemeMode.SYSTEM)
    val useDynamicColor by manager.useDynamicColor.collectAsStateWithLifecycle(initialValue = false)
    val customPrimaryColor by manager.customPrimaryColor.collectAsStateWithLifecycle(initialValue = null)

    val systemInDarkTheme = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeManager.ThemeMode.DARK -> true
        ThemeManager.ThemeMode.LIGHT -> false
        ThemeManager.ThemeMode.SYSTEM -> systemInDarkTheme
    }

    val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val accent = customPrimaryColor?.let { raw ->
        runCatching { Color(android.graphics.Color.parseColor(raw)) }.getOrNull()
    }

    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        accent != null -> baseScheme.copy(primary = accent, surfaceTint = accent)
        else -> baseScheme
    }

    val momentumColors = (if (darkTheme) DarkMomentumColors else LightMomentumColors).let { base ->
        if (accent != null && !useDynamicColor) {
            base.copy(gradientStart = accent, gradientEnd = base.gradientEnd)
        } else {
            base
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            // El contexto no siempre es una Activity: este tema también se usa en
            // overlays dibujados desde un Service (AppBlockOverlayService), donde el
            // cast directo lanzaba ClassCastException al mostrar la pantalla de bloqueo.
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            // Barras transparentes: el contenido define el color, no el sistema.
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val insets = WindowCompat.getInsetsController(window, view)
            insets.isAppearanceLightStatusBars = !darkTheme
            insets.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalMomentumColors provides momentumColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
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
