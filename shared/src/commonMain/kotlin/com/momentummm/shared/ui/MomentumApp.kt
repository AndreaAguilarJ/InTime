package com.momentummm.shared.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.momentummm.app.ui.theme.MomentumThemeCore
import com.momentummm.app.ui.theme.momentum
import com.momentummm.shared.data.SettingsStore
import com.momentummm.shared.ui.screen.LifeWeeksSharedScreen
import androidx.compose.material3.MaterialTheme

/**
 * Raíz de la interfaz compartida entre Android e iOS.
 *
 * Aplica el tema Momentum --la misma paleta y tipografía que la app Android-- y
 * monta la pantalla. De aquí irán colgando el resto a medida que se compartan.
 */
@Composable
fun MomentumApp(store: SettingsStore) {
    MomentumThemeCore(darkTheme = isSystemInDarkTheme()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.momentum.canvas,
        ) {
            LifeWeeksSharedScreen(store = store)
        }
    }
}
