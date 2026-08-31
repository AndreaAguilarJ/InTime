package com.momentummm.shared.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarViewMonth
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.momentummm.app.ui.theme.MomentumThemeCore
import com.momentummm.app.ui.theme.momentum
import com.momentummm.shared.data.SettingsStore
import com.momentummm.shared.notification.MomentumNotifier
import com.momentummm.shared.ui.screen.FocusSharedScreen
import com.momentummm.shared.ui.screen.LifeWeeksSharedScreen

/** Secciones de la app compartida. */
private enum class MomentumTab(val label: String, val icon: ImageVector) {
    FOCUS("Enfoque", Icons.Outlined.Timer),
    LIFE("Mi vida", Icons.Outlined.CalendarViewMonth),
}

/**
 * Raíz de la interfaz compartida entre Android e iOS.
 *
 * Aplica el tema Momentum --la misma paleta y tipografía que la app Android-- y
 * monta la navegación. Las dos secciones que ya funcionan en iOS sin permisos
 * especiales de Apple son el enfoque y la vida en semanas; el tiempo de pantalla y
 * el bloqueo de apps se añadirán cuando Apple conceda el permiso Family Controls.
 */
@Composable
fun MomentumApp(store: SettingsStore, notifier: MomentumNotifier) {
    MomentumThemeCore(darkTheme = isSystemInDarkTheme()) {
        var tab by remember { mutableStateOf(MomentumTab.FOCUS) }

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
                                label = { Text(entrada.label) },
                                icon = {
                                    Icon(
                                        imageVector = entrada.icon,
                                        contentDescription = entrada.label,
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
                Column(modifier = Modifier.fillMaxSize().padding(insets)) {
                    when (tab) {
                        MomentumTab.FOCUS -> FocusSharedScreen(notifier = notifier, store = store)
                        MomentumTab.LIFE -> LifeWeeksSharedScreen(store = store)
                    }
                }
            }
        }
    }
}
