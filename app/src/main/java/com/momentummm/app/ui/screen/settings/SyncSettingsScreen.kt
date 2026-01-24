package com.momentummm.app.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.momentummm.app.R
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
                title = { Text(stringResource(R.string.sync_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.sync_settings_back_cd)
                        )
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
                            text = stringResource(R.string.sync_settings_status_title),
                            style = MaterialTheme.typography.titleMedium
                        )

                        when (syncStatus) {
                            AutoSyncManager.SyncStatus.Idle -> {
                                Icon(
                                    Icons.Default.CloudOff,
                                    contentDescription = stringResource(R.string.sync_settings_status_idle_cd),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                            AutoSyncManager.SyncStatus.Syncing -> {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                            AutoSyncManager.SyncStatus.Success -> {
                                Icon(
                                    Icons.Default.CloudDone,
                                    contentDescription = stringResource(R.string.sync_settings_status_success_cd),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            AutoSyncManager.SyncStatus.Failed -> {
                                Icon(
                                    Icons.Default.CloudOff,
                                    contentDescription = stringResource(R.string.sync_settings_status_failed_cd),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Divider()

                    // Mensaje de estado
                    Text(
                        text = when (syncStatus) {
                            AutoSyncManager.SyncStatus.Idle -> stringResource(R.string.sync_settings_status_idle)
                            AutoSyncManager.SyncStatus.Syncing -> stringResource(R.string.sync_settings_status_syncing)
                            AutoSyncManager.SyncStatus.Success -> stringResource(R.string.sync_settings_status_success)
                            AutoSyncManager.SyncStatus.Failed -> stringResource(R.string.sync_settings_status_failed)
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
                            text = stringResource(R.string.sync_settings_last_sync_label),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = lastSyncTime ?: stringResource(R.string.sync_settings_never),
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
                    Text(
                        if (isSyncing) {
                            stringResource(R.string.sync_settings_status_syncing)
                        } else {
                            stringResource(R.string.sync_settings_sync_now)
                        }
                    )
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
                            text = stringResource(R.string.sync_settings_info_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Text(
                        text = stringResource(R.string.sync_settings_info_list),
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
                        text = stringResource(R.string.sync_settings_auto_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}
