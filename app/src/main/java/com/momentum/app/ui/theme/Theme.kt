package com.momentum.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.momentum.app.data.manager.ThemeManager

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    secondary = SecondaryDark,
    tertiary = Pink80,
    background = BackgroundDark,
    surface = SurfaceDark,
    error = ErrorDark,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onError = Color.White,
    primaryContainer = Color(0xFF3730A3),
    secondaryContainer = Color(0xFF047857),
    onPrimaryContainer = Color.White,
    onSecondaryContainer = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    secondary = Secondary,
    tertiary = Pink40,
    background = BackgroundWhite,
    surface = SurfaceLight,
    error = Error,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onError = Color.White,
    primaryContainer = Color(0xFFEEF2FF),
    secondaryContainer = Color(0xFFECFDF5),
    onPrimaryContainer = Color(0xFF312E81),
    onSecondaryContainer = Color(0xFF065F46)
)

@Composable
fun MomentumTheme(
    themeManager: ThemeManager? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val manager = themeManager ?: ThemeManager(context)
    
    val themeMode by manager.themeMode.collectAsState(initial = ThemeManager.ThemeMode.SYSTEM)
    val useDynamicColor by manager.useDynamicColor.collectAsState(initial = true)
    val customPrimaryColor by manager.customPrimaryColor.collectAsState(initial = null)
    
    val systemInDarkTheme = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeManager.ThemeMode.DARK -> true
        ThemeManager.ThemeMode.LIGHT -> false
        ThemeManager.ThemeMode.SYSTEM -> systemInDarkTheme
    }
    
    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> {
            if (customPrimaryColor != null) {
                try {
                    val customColor = Color(android.graphics.Color.parseColor(customPrimaryColor))
                    DarkColorScheme.copy(primary = customColor)
                } catch (e: Exception) {
                    DarkColorScheme
                }
            } else {
                DarkColorScheme
            }
        }
        else -> {
            if (customPrimaryColor != null) {
                try {
                    val customColor = Color(android.graphics.Color.parseColor(customPrimaryColor))
                    LightColorScheme.copy(primary = customColor)
                } catch (e: Exception) {
                    LightColorScheme
                }
            } else {
                LightColorScheme
            }
        }
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Extension properties for easy access to custom colors
val MaterialTheme.customColors: CustomColors
    @Composable
    get() = if (isSystemInDarkTheme()) {
        darkCustomColors
    } else {
        lightCustomColors
    }

data class CustomColors(
    val success: Color,
    val warning: Color,
    val weeksLived: Color,
    val weeksFuture: Color,
    val backgroundGray: Color,
    val textSecondary: Color,
    val gradientStart: Color,
    val gradientEnd: Color
)

private val lightCustomColors = CustomColors(
    success = Success,
    warning = Warning,
    weeksLived = WeeksLived,
    weeksFuture = WeeksFuture,
    backgroundGray = BackgroundGray,
    textSecondary = TextSecondary,
    gradientStart = GradientStart,
    gradientEnd = GradientEnd
)

private val darkCustomColors = CustomColors(
    success = SuccessDark,
    warning = WarningDark,
    weeksLived = WeeksLivedDark,
    weeksFuture = WeeksFutureDark,
    backgroundGray = BackgroundGrayDark,
    textSecondary = TextSecondaryDark,
    gradientStart = GradientStartDark,
    gradientEnd = GradientEndDark
)