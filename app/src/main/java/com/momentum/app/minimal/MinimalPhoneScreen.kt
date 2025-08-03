package com.momentum.app.minimal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinimalPhoneScreen(
    minimalPhoneManager: MinimalPhoneManager,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMinimalModeEnabled by minimalPhoneManager.isMinimalModeEnabled.collectAsState()
    
    if (isMinimalModeEnabled) {
        MinimalLauncherContent(
            minimalPhoneManager = minimalPhoneManager,
            onSettingsClick = onSettingsClick,
            modifier = modifier
        )
    } else {
        MinimalModeToggleScreen(
            onEnableMinimalMode = { minimalPhoneManager.enableMinimalMode() },
            modifier = modifier
        )
    }
}

@Composable
private fun MinimalLauncherContent(
    minimalPhoneManager: MinimalPhoneManager,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val essentialApps = listOf(
        EssentialApp("Teléfono", Icons.Default.Phone) {
            // Open phone dialer
        },
        EssentialApp("Mensajes", Icons.Default.Message) {
            // Open messages
        },
        EssentialApp("Contactos", Icons.Default.Contacts) {
            minimalPhoneManager.openContacts()
        },
        EssentialApp("Configuración", Icons.Default.Settings) {
            onSettingsClick()
        }
    )
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Momentum Minimal",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Current time display
        Text(
            text = getCurrentTime(),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Essential apps grid
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            essentialApps.chunked(2).forEach { rowApps ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    rowApps.forEach { app ->
                        EssentialAppCard(
                            app = app,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Add empty space if odd number of apps
                    if (rowApps.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedButton(
            onClick = { minimalPhoneManager.disableMinimalMode() }
        ) {
            Text("Salir del modo mínimo")
        }
    }
}

@Composable
private fun MinimalModeToggleScreen(
    onEnableMinimalMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📱",
            style = MaterialTheme.typography.displayLarge
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Modo Teléfono Mínimo",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Limita tu teléfono a las funciones esenciales: llamadas, mensajes y contactos.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Características:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("• Solo apps esenciales disponibles")
                Text("• Interfaz simplificada")
                Text("• Menos distracciones")
                Text("• Enfoque en comunicación")
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onEnableMinimalMode,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Activar modo mínimo")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EssentialAppCard(
    app: EssentialApp,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = app.onClick,
        modifier = modifier.aspectRatio(1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = app.icon,
                contentDescription = app.name,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = app.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun AllowedAppsManagementScreen(
    minimalPhoneManager: MinimalPhoneManager,
    modifier: Modifier = Modifier
) {
    val installedApps by remember { mutableStateOf(minimalPhoneManager.getInstalledApps()) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Gestionar apps permitidas",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Selecciona qué apps estarán disponibles en modo mínimo:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(installedApps) { appInfo ->
                AppPermissionItem(
                    appInfo = appInfo,
                    onToggle = { isAllowed ->
                        if (isAllowed) {
                            minimalPhoneManager.addAllowedApp(appInfo.packageName)
                        } else {
                            minimalPhoneManager.removeAllowedApp(appInfo.packageName)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AppPermissionItem(
    appInfo: AppInfo,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = appInfo.appName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = appInfo.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Switch(
                checked = appInfo.isAllowed,
                onCheckedChange = onToggle
            )
        }
    }
}

private data class EssentialApp(
    val name: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

private fun getCurrentTime(): String {
    return java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        .format(java.util.Date())
}