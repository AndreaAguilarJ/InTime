package com.momentummm.shared.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarViewMonth
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.momentummm.app.ui.theme.MomentumThemeCore
import com.momentummm.app.ui.theme.momentum
import com.momentummm.shared.data.SettingsStore
import com.momentummm.shared.data.ThemeChoice
import com.momentummm.shared.data.readThemeChoice
import com.momentummm.shared.notification.MomentumNotifier
import com.momentummm.shared.resources.Res
import com.momentummm.shared.resources.tab_focus
import com.momentummm.shared.resources.tab_life
import com.momentummm.shared.resources.tab_settings
import com.momentummm.shared.ui.screen.FocusSharedScreen
import com.momentummm.shared.ui.screen.LifeWeeksSharedScreen
import com.momentummm.shared.ui.screen.SettingsSharedScreen
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Secciones de la app compartida. */
private enum class MomentumTab(val label: StringResource, val icon: ImageVector) {
    FOCUS(Res.string.tab_focus, Icons.Outlined.Timer),
    LIFE(Res.string.tab_life, Icons.Outlined.CalendarViewMonth),
    SETTINGS(Res.string.tab_settings, Icons.Outlined.Settings),
}

/**
 * Raíz de la interfaz compartida entre Android e iOS.
 *
 * Aplica el tema Momentum --la misma paleta y tipografía que la app Android-- y
 * monta la navegación. Las secciones que ya funcionan en iOS sin permisos
 * especiales de Apple son el enfoque, la vida en semanas y los ajustes; el tiempo
 * de pantalla y el bloqueo de apps se añadirán cuando Apple conceda el permiso
 * Family Controls.
 *
 * @param appVersion la aporta la plataforma, porque la versión vive en el paquete
 *   de la app y el módulo compartido no puede leerla.
 */
@Composable
fun MomentumApp(
    store: SettingsStore,
    notifier: MomentumNotifier,
    appVersion: String,
) {
    var tab by remember { mutableStateOf(MomentumTab.FOCUS) }
    // La apariencia se lee una vez y se conserva en estado, así que cambiarla en
    // los ajustes se ve al instante en toda la app y no solo al reabrirla.
    var theme by remember { mutableStateOf(store.readThemeChoice()) }

    val darkTheme = when (theme) {
        ThemeChoice.SYSTEM -> isSystemInDarkTheme()
        ThemeChoice.LIGHT -> false
        ThemeChoice.DARK -> true
    }

    MomentumThemeCore(darkTheme = darkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.momentum.canvas,
        ) {
            Scaffold(
                containerColor = MaterialTheme.momentum.canvas,
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.momentum.surface,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        MomentumTab.entries.forEach { entrada ->
                            NavigationBarItem(
                                selected = tab == entrada,
                                onClick = { tab = entrada },
                                label = { Text(stringResource(entrada.label)) },
                                icon = {
                                    Icon(
                                        imageVector = entrada.icon,
                                        contentDescription = stringResource(entrada.label),
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.momentum.textSecondary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedTextColor = MaterialTheme.momentum.textSecondary,
                                ),
                            )
                        }
                    }
                },
            ) { insets ->
                // El contenido se acota y se centra en vez de estirarse. En iPad la
                // app se veía como un móvil ensanchado: la cabecera cruzaba los 834
                // puntos de ancho y quedaba medio panel vacío debajo. Un ancho de
                // lectura fijo hace que la pantalla grande se vea intencionada, y de
                // paso mejora los iPhone más anchos.
                Box(
                    modifier = Modifier.fillMaxSize().padding(insets),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Column(modifier = Modifier.widthIn(max = 560.dp).fillMaxSize()) {
                    when (tab) {
                        MomentumTab.FOCUS -> FocusSharedScreen(
                            notifier = notifier,
                            store = store,
                        )
                        MomentumTab.LIFE -> LifeWeeksSharedScreen(store = store)
                        MomentumTab.SETTINGS -> SettingsSharedScreen(
                            store = store,
                            appVersion = appVersion,
                            onThemeChange = { elegido -> theme = elegido },
                        )
                    }
                    }
                }
            }
        }
    }
}
