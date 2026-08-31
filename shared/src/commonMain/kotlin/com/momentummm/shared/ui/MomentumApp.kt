package com.momentummm.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.momentummm.shared.Plataforma

/**
 * Raíz de la interfaz compartida entre Android e iOS.
 *
 * De momento solo enseña la plataforma anfitriona, lo que sirve como prueba de
 * que un mismo composable se dibuja en las dos tiendas. Las pantallas reales de
 * Momentum se irán colgando de aquí.
 */
@Composable
fun MomentumApp() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Momentum",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = Plataforma.nombre,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
