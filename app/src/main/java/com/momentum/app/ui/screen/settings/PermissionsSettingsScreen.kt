package com.momentummm.app.ui.screen.settings

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsSettingsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    // Estados de permisos
    val hasUsageStatsPermission = remember { mutableStateOf(checkUsageStatsPermission(context)) }
    val hasOverlayPermission = remember { mutableStateOf(checkOverlayPermission(context)) }
    val hasNotificationPermission = remember { mutableStateOf(checkNotificationPermission(context)) }
    val hasAccessibilityPermission = remember { mutableStateOf(checkAccessibilityPermission(context)) }
    val isDefaultLauncher = remember { mutableStateOf(checkIsDefaultLauncher(context)) }

    // Refrescar permisos cuando la pantalla obtiene el foco
    DisposableEffect(Unit) {
        onDispose { }
    }

    LaunchedEffect(Unit) {
        // Actualizar estados de permisos periódicamente
        kotlinx.coroutines.delay(500)
        hasUsageStatsPermission.value = checkUsageStatsPermission(context)
        hasOverlayPermission.value = checkOverlayPermission(context)
        hasNotificationPermission.value = checkNotificationPermission(context)
        hasAccessibilityPermission.value = checkAccessibilityPermission(context)
        isDefaultLauncher.value = checkIsDefaultLauncher(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permisos de la aplicación") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Información general
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Column {
                            Text(
                                text = "Permisos necesarios",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Para que Momentum funcione correctamente, necesita acceso a ciertos permisos del sistema. Toca cada opción para configurarlos.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Sección: Permisos esenciales
            item {
                Text(
                    text = "Permisos esenciales",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // 1. Estadísticas de uso
            item {
                PermissionCard(
                    title = "Estadísticas de uso",
                    description = "Necesario para monitorear el tiempo de uso de aplicaciones y establecer límites",
                    icon = Icons.Default.BarChart,
                    isGranted = hasUsageStatsPermission.value,
                    onClick = {
                        openUsageStatsSettings(context)
                    }
                )
            }

            // 2. Superponer sobre otras apps
            item {
                PermissionCard(
                    title = "Superponer sobre otras apps",
                    description = "Necesario para mostrar bloqueos cuando alcances los límites de tiempo",
                    icon = Icons.Default.Layers,
                    isGranted = hasOverlayPermission.value,
                    onClick = {
                        openOverlaySettings(context)
                    }
                )
            }

            // 3. Notificaciones
            item {
                PermissionCard(
                    title = "Notificaciones",
                    description = "Para enviarte recordatorios, alertas de metas y notificaciones de progreso",
                    icon = Icons.Default.Notifications,
                    isGranted = hasNotificationPermission.value,
                    onClick = {
                        openNotificationSettings(context)
                    }
                )
            }

            // 4. Servicio de Accesibilidad - Bloqueo de Sitios Web
            item {
                PermissionCard(
                    title = "Bloqueo de sitios web",
                    description = "Servicio de accesibilidad para detectar y bloquear sitios web en navegadores (contenido adulto, distracciones, etc.)",
                    icon = Icons.Default.Security,
                    isGranted = hasAccessibilityPermission.value,
                    onClick = {
                        openAccessibilitySettings(context)
                    }
                )
            }

            // Sección: Permisos opcionales
            item {
                Text(
                    text = "Permisos opcionales",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            // 5. Launcher predeterminado (Modo teléfono mínimo)
            item {
                PermissionCard(
                    title = "Launcher predeterminado",
                    description = "Para usar el modo teléfono mínimo como pantalla de inicio",
                    icon = Icons.Default.Home,
                    isGranted = isDefaultLauncher.value,
                    onClick = {
                        openDefaultLauncherSettings(context)
                    }
                )
            }

            // 6. Contactos y llamadas
            item {
                PermissionCard(
                    title = "Contactos y llamadas",
                    description = "Para funcionalidades del modo teléfono mínimo (llamadas y SMS)",
                    icon = Icons.Default.Contacts,
                    isGranted = false, // Se puede mejorar checkeando estos permisos
                    onClick = {
                        openAppSettings(context)
                    }
                )
            }

            // 7. Cámara y multimedia
            item {
                PermissionCard(
                    title = "Cámara y multimedia",
                    description = "Para funcionalidades adicionales del modo teléfono mínimo",
                    icon = Icons.Default.CameraAlt,
                    isGranted = false,
                    onClick = {
                        openAppSettings(context)
                    }
                )
            }

            // 8. Acceso completo a configuración
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Configuración completa de permisos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Accede a la configuración del sistema para gestionar todos los permisos de la aplicación manualmente.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = { openAppSettings(context) },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Abrir configuración")
                        }
                    }
                }
            }

            // Botón de refrescar
            item {
                OutlinedButton(
                    onClick = {
                        hasUsageStatsPermission.value = checkUsageStatsPermission(context)
                        hasOverlayPermission.value = checkOverlayPermission(context)
                        hasNotificationPermission.value = checkNotificationPermission(context)
                        hasAccessibilityPermission.value = checkAccessibilityPermission(context)
                        isDefaultLauncher.value = checkIsDefaultLauncher(context)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Actualizar estado de permisos")
                }
            }
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isGranted) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Concedido",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "No concedido",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isGranted) "✓ Concedido" else "✗ No concedido - Toca para configurar",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Ir a configuración",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Funciones auxiliares para verificar permisos
private fun checkUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
    } else {
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

private fun checkOverlayPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else {
        true
    }
}

private fun checkNotificationPermission(context: Context): Boolean {
    return NotificationManagerCompat.from(context).areNotificationsEnabled()
}

private fun checkAccessibilityPermission(context: Context): Boolean {
    val pm = context.packageManager
    val packageName = context.packageName
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        Settings.Secure.getInt(
            context.contentResolver,
            "accessibility_enabled",
            0
        ) == 1
    } else {
        false
    }
}

private fun checkIsDefaultLauncher(context: Context): Boolean {
    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_HOME)
    }
    val resolveInfo = context.packageManager.resolveActivity(intent, 0)
    return resolveInfo?.activityInfo?.packageName == context.packageName
}

// Funciones para abrir configuraciones
private fun openUsageStatsSettings(context: Context) {
    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    context.startActivity(intent)
}

private fun openOverlaySettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
    )
    context.startActivity(intent)
}

private fun openNotificationSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }
    context.startActivity(intent)
}

private fun openDefaultLauncherSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
    } else {
        Intent(Settings.ACTION_HOME_SETTINGS)
    }
    context.startActivity(intent)
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    context.startActivity(intent)
}

private fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    context.startActivity(intent)
}
