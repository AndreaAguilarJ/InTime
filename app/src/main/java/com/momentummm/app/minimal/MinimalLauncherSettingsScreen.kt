package com.momentummm.app.minimal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinimalLauncherSettingsScreen(
    launcherManager: LauncherManager,
    minimalPhoneManager: MinimalPhoneManager,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isDefaultLauncher by launcherManager.isDefaultLauncher.collectAsState()
    val autoEnableMinimal by launcherManager.autoEnableMinimal.collectAsState()
    val isMinimalModeEnabled by minimalPhoneManager.isMinimalModeEnabled.collectAsState()

    var showLauncherList by remember { mutableStateOf(false) }
    var installedLaunchers by remember { mutableStateOf<List<LauncherInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        installedLaunchers = launcherManager.getInstalledLaunchers()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBackClick) {
                Text("← Volver")
            }
            Text(
                text = "Configuración del Launcher",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (showLauncherList) {
            LauncherSelectionList(
                launchers = installedLaunchers,
                onLauncherSelected = { launcher ->
                    if (launcher.isCurrentApp) {
                        coroutineScope.launch {
                            launcherManager.setAsDefaultLauncher()
                        }
                    }
                    showLauncherList = false
                },
                onBackClick = { showLauncherList = false }
            )
        } else {
            LauncherSettingsContent(
                isDefaultLauncher = isDefaultLauncher,
                autoEnableMinimal = autoEnableMinimal,
                isMinimalModeEnabled = isMinimalModeEnabled,
                onSetAsDefaultClick = {
                    coroutineScope.launch {
                        launcherManager.setAsDefaultLauncher()
                    }
                },
                onToggleAutoMinimal = { enabled ->
                    coroutineScope.launch {
                        launcherManager.setAutoEnableMinimal(enabled)
                    }
                },
                onToggleMinimalMode = { enabled ->
                    coroutineScope.launch {
                        if (enabled) {
                            minimalPhoneManager.enableMinimalMode()
                        } else {
                            minimalPhoneManager.disableMinimalMode()
                        }
                    }
                },
                onViewLaunchersClick = { showLauncherList = true },
                onOpenSystemSettingsClick = {
                    launcherManager.openLauncherSettings()
                }
            )
        }
    }
}

@Composable
private fun LauncherSettingsContent(
    isDefaultLauncher: Boolean,
    autoEnableMinimal: Boolean,
    isMinimalModeEnabled: Boolean,
    onSetAsDefaultClick: () -> Unit,
    onToggleAutoMinimal: (Boolean) -> Unit,
    onToggleMinimalMode: (Boolean) -> Unit,
    onViewLaunchersClick: () -> Unit,
    onOpenSystemSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDefaultLauncher) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isDefaultLauncher) "Launcher por Defecto" else "No es Launcher por Defecto",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isDefaultLauncher) {
                                "Momentum es tu launcher principal"
                            } else {
                                "Configura Momentum como launcher predeterminado"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (isDefaultLauncher) Icons.Default.Check else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isDefaultLauncher) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
        }

        item {
            // Set as Default Button
            if (!isDefaultLauncher) {
                Button(
                    onClick = onSetAsDefaultClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Home, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Establecer como Launcher por Defecto")
                }
            }
        }

        item {
            // Minimal Mode Settings
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Configuración del Modo Mínimo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Current minimal mode status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Modo Mínimo Activo",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Activar/desactivar el modo mínimo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isMinimalModeEnabled,
                            onCheckedChange = onToggleMinimalMode
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Auto enable minimal mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-activar Modo Mínimo",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Activar automáticamente al abrir el launcher",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoEnableMinimal,
                            onCheckedChange = onToggleAutoMinimal
                        )
                    }
                }
            }
        }

        item {
            // Launcher Management
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Gestión de Launchers",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = onViewLaunchersClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ver Launchers Instalados")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onOpenSystemSettingsClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Abrir Configuración del Sistema")
                    }
                }
            }
        }

        item {
            // Instructions
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📱 Instrucciones",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "1. Presiona 'Establecer como Launcher por Defecto'\n" +
                                "2. Selecciona 'Momentum' en la lista\n" +
                                "3. Marca 'Usar siempre' si quieres que sea permanente\n" +
                                "4. Activa el modo mínimo para una experiencia simplificada",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LauncherSelectionList(
    launchers: List<LauncherInfo>,
    onLauncherSelected: (LauncherInfo) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBackClick) {
                Text("← Volver")
            }
            Text(
                text = "Launchers Instalados",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(launchers) { launcher ->
                LauncherItem(
                    launcher = launcher,
                    onClick = { onLauncherSelected(launcher) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LauncherItem(
    launcher: LauncherInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (launcher.isDefault) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = launcher.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (launcher.isCurrentApp) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = launcher.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (launcher.isCurrentApp) {
                    Text(
                        text = "Esta aplicación",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (launcher.isDefault) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Por defecto",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
