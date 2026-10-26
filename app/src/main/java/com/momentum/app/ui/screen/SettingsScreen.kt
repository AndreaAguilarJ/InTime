package com.momentummm.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.momentummm.app.MomentumApplication
import com.momentummm.app.R
import com.momentummm.app.minimal.AllowedAppsManagementScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToScreen: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val application = context.applicationContext as MomentumApplication
    val coroutineScope = rememberCoroutineScope()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAppsManagement by remember { mutableStateOf(false) }
    
    if (showAppsManagement) {
        AllowedAppsManagementScreen(
            minimalPhoneManager = application.minimalPhoneManager,
            modifier = Modifier.fillMaxSize()
        )
    } else {
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
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Account Section
            item {
                Text(
                    text = "Cuenta",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { 
                        // Navigate to account settings screen (premium feature)
                        onNavigateToScreen("account_settings")
                    }
                ) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.account_settings)) },
                        supportingContent = { Text("Gestionar tu perfil y configuración") },
                        leadingContent = { Icon(Icons.Default.AccountCircle, contentDescription = null) }
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showLogoutDialog = true }
                ) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.logout)) },
                        supportingContent = { Text("Cerrar sesión actual") },
                        leadingContent = { Icon(Icons.Default.Logout, contentDescription = null) }
                    )
                }
            }

            // Help Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Ayuda",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { 
                        // Navigate to tutorial
                        onNavigateToScreen("tutorial")
                    }
                ) {
                    ListItem(
                        headlineContent = { Text("Tutorial de la app") },
                        supportingContent = { Text("Aprende a usar todas las funciones de Momentum") },
                        leadingContent = { Icon(Icons.Default.School, contentDescription = null) }
                    )
                }
            }

            // App Features Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Funciones",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onNavigateToScreen("password_protection")
                    }
                ) {
                    ListItem(
                        headlineContent = { Text("Protección por Contraseña") },
                        supportingContent = { Text("Protege tus configuraciones con contraseña numérica") },
                        leadingContent = { Icon(Icons.Default.Lock, contentDescription = null) }
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onNavigateToScreen("app_limits")
                    }
                ) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.app_limits)) },
                        supportingContent = { Text("Configurar límites de tiempo para apps distractoras") },
                        leadingContent = { Icon(Icons.Default.Block, contentDescription = null) }
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onNavigateToScreen("in_app_blocking")
                    }
                ) {
                    ListItem(
                        headlineContent = { Text("Bloqueo dentro de Apps") },
                        supportingContent = { Text("Bloquea Reels, Shorts y más sin bloquear toda la app") },
                        leadingContent = { Icon(Icons.Default.VideoLibrary, contentDescription = null) }
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onNavigateToScreen("website_blocks")
                    }
                ) {
                    ListItem(
                        headlineContent = { Text("Bloqueo de sitios web") },
                        supportingContent = { Text("Bloquear sitios web distractores o no deseados") },
                        leadingContent = { Icon(Icons.Default.Security, contentDescription = null) }
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showAppsManagement = true }
                ) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.manage_apps)) },
                        supportingContent = { Text("Gestionar apps permitidas en modo mínimo") },
                        leadingContent = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) }
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { 
                        // Navigate to notification settings
                        onNavigateToScreen("notification_settings")
                    }
                ) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.notifications)) },
                        supportingContent = { Text("Configurar notificaciones y recordatorios") },
                        leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null) }
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        // Navigate to "Mi vida en semanas" screen
                        onNavigateToScreen("mi_vida_en_semanas")
                    }
                ) {
                    ListItem(
                        headlineContent = { Text("Mi vida en semanas") },
                        supportingContent = { Text("Visualiza tu vida y reflexiona sobre el tiempo") },
                        leadingContent = { Icon(Icons.Default.Person, contentDescription = null) }
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        // Navigate to theme settings
                        onNavigateToScreen("theme_settings")
                    }
                ) {
                    ListItem(
                        headlineContent = { Text("Personalización") },
                        supportingContent = { Text("Temas y apariencia de la aplicación") },
                        leadingContent = { Icon(Icons.Default.Apps, contentDescription = null) }
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { 
                        // Navigate to export/backup settings
                        onNavigateToScreen("backup_settings")
                    }
                ) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.widget_settings)) },
                        supportingContent = { Text("Personalizar widgets de pantalla de inicio") },
                        leadingContent = { Icon(Icons.Default.ViewModule, contentDescription = null) }
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onNavigateToScreen("sync_settings")
                    }
                ) {
                    ListItem(
                        headlineContent = { Text("Sincronización") },
                        supportingContent = { Text("Ver estado y sincronizar datos") },
                        leadingContent = { Icon(Icons.Default.Sync, contentDescription = null) }
                    )
                }
            }

            // About Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Información",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { 
                        // Navigate to about screen
                        onNavigateToScreen("about")
                    }
                ) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.about)) },
                        supportingContent = { Text("Información sobre Momentum v1.0") },
                        leadingContent = { Icon(Icons.Default.Info, contentDescription = null) }
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        // Navigate to permissions settings
                        onNavigateToScreen("permissions_settings")
                    }
                ) {
                    ListItem(
                        headlineContent = { Text("Permisos") },
                        supportingContent = { Text("Configurar permisos de la aplicación") },
                        leadingContent = { Icon(Icons.Default.Security, contentDescription = null) }
                    )
                }
            }
        }
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Cerrar sesión") },
            text = { Text("¿Estás seguro de que quieres cerrar sesión?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            application.appwriteService.logout()
                            showLogoutDialog = false
                        }
                    }
                ) {
                    Text("Cerrar sesión")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}