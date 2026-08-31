package com.momentummm.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.momentummm.app.data.manager.ThemeManager

/**
 * Capa Android del tema Momentum.
 *
 * Todo lo portable (esquemas, colores semánticos, tipografía) vive en el módulo
 * compartido, en `MomentumThemeCore`. Aquí queda solo lo que existe únicamente en
 * Android: leer las preferencias con `ThemeManager`, los colores dinámicos de
 * Material You y dejar transparentes las barras del sistema.
 */
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

    val accent = customPrimaryColor?.let { raw ->
        runCatching { Color(android.graphics.Color.parseColor(raw)) }.getOrNull()
    }

    // Material You gana sobre el acento elegido a mano: si la persona activó los
    // colores del fondo de pantalla, el sistema manda.
    val dynamicScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        else -> null
    }
    val semanticAccent = if (useDynamicColor) null else accent

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

    MomentumThemeCore(
        darkTheme = darkTheme,
        accent = semanticAccent,
        colorScheme = dynamicScheme,
        content = content,
    )
}
