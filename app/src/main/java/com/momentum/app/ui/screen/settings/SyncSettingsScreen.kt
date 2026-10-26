package com.momentummm.app.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.momentummm.app.data.manager.AutoSyncManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncSettingsScreen(
    autoSyncManager: AutoSyncManager,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    val syncStatus by autoSyncManager.syncStatus.collectAsState()
    val lastSyncTime by autoSyncManager.lastSyncTime.collectAsState()

    val isSyncing = syncStatus == AutoSyncManager.SyncStatus.Syncing

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sincronización") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Estado de sincronización
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Estado de Sincronización",
                            style = MaterialTheme.typography.titleMedium
                        )

                        when (syncStatus) {
                            AutoSyncManager.SyncStatus.Idle -> {
                                Icon(
                                    Icons.Default.CloudOff,
                                    contentDescription = "Inactivo",
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                            AutoSyncManager.SyncStatus.Syncing -> {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                            AutoSyncManager.SyncStatus.Success -> {
                                Icon(
                                    Icons.Default.CloudDone,
                                    contentDescription = "Sincronizado",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            AutoSyncManager.SyncStatus.Failed -> {
                                Icon(
                                    Icons.Default.CloudOff,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Divider()

                    // Mensaje de estado
                    Text(
                        text = when (syncStatus) {
                            AutoSyncManager.SyncStatus.Idle -> "No sincronizado"
                            AutoSyncManager.SyncStatus.Syncing -> "Sincronizando..."
                            AutoSyncManager.SyncStatus.Success -> "Sincronización exitosa"
                            AutoSyncManager.SyncStatus.Failed -> "Error al sincronizar. Tus datos están guardados localmente."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (syncStatus) {
                            AutoSyncManager.SyncStatus.Failed -> MaterialTheme.colorScheme.error
                            AutoSyncManager.SyncStatus.Success -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    Divider()

                    // Última sincronización
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Última sincronización:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = lastSyncTime ?: "Nunca",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Botón de sincronización manual
            Button(
                onClick = {
                    coroutineScope.launch {
                        autoSyncManager.forceSyncNow()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSyncing
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isSyncing) "Sincronizando..." else "Sincronizar ahora")
                }
            }

            // Información
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "¿Qué se sincroniza?",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Text(
                        text = "• Configuración de colores\n" +
                                "• Fecha de nacimiento\n" +
                                "• Metas y desafíos\n" +
                                "• Límites de aplicaciones\n" +
                                "• Apps en lista blanca\n" +
                                "• Frases personalizadas\n" +
                                "• Preferencias del widget",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Nota de sincronización automática
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AutoMode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "La sincronización se realiza automáticamente cada vez que cierras la aplicación.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}
