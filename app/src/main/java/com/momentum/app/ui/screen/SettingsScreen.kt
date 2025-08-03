package com.momentum.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.momentum.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.nav_settings),
                style = MaterialTheme.typography.headlineMedium
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { /* TODO: Navigate to app limits */ }
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.app_limits)) },
                    supportingContent = { Text("Configura límites de tiempo para aplicaciones") }
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { /* TODO: Navigate to notifications */ }
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.notifications)) },
                    supportingContent = { Text("Configurar notificaciones y recordatorios") }
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { /* TODO: Navigate to widget settings */ }
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.widget_settings)) },
                    supportingContent = { Text("Personalizar widgets de pantalla de inicio") }
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { /* TODO: Navigate to about */ }
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.about)) },
                    supportingContent = { Text("Información sobre la aplicación") }
                )
            }
        }
    }
}