package com.momentummm.app.minimal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.momentummm.app.data.AppDatabase
import com.momentummm.app.util.LifeWeeksCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinimalPhoneScreen(
    minimalPhoneManager: MinimalPhoneManager,
    onSettingsClick: () -> Unit,
    onExitMinimalMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val isMinimalModeEnabled by minimalPhoneManager.isMinimalModeEnabled.collectAsState()
    var currentScreen by remember { mutableStateOf<MinimalScreen>(MinimalScreen.Home) }
    var showExitConfirm by remember { mutableStateOf(false) }
    
    if (isMinimalModeEnabled) {
        when (currentScreen) {
            MinimalScreen.Home -> {
                MinimalLauncherContent(
                    minimalPhoneManager = minimalPhoneManager,
                    onSettingsClick = onSettingsClick,
                    onExitMinimalMode = onExitMinimalMode,
                    onDialerClick = { currentScreen = MinimalScreen.Dialer },
                    onAppListClick = { currentScreen = MinimalScreen.AppList },
                    modifier = modifier
                )
            }
            MinimalScreen.Dialer -> {
                MinimalDialerScreen(
                    onCall = { phoneNumber ->
                        minimalPhoneManager.makePhoneCall(phoneNumber)
                        currentScreen = MinimalScreen.Home
                    },
                    onBack = { currentScreen = MinimalScreen.Home },
                    modifier = modifier
                )
            }
            MinimalScreen.AppList -> {
                MinimalAppListScreen(
                    minimalPhoneManager = minimalPhoneManager,
                    onBack = { currentScreen = MinimalScreen.Home },
                    modifier = modifier
                )
            }
        }
    } else {
        MinimalModeToggleScreen(
            onEnableMinimalMode = {
                coroutineScope.launch {
                    minimalPhoneManager.enableMinimalMode()
                }
            },
            modifier = modifier
        )
    }
}

private enum class MinimalScreen {
    Home,
    Dialer,
    AppList
}

@Composable
private fun MinimalLauncherContent(
    minimalPhoneManager: MinimalPhoneManager,
    onSettingsClick: () -> Unit,
    onExitMinimalMode: () -> Unit,
    onDialerClick: () -> Unit,
    onAppListClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var showExitConfirm by remember { mutableStateOf(false) }
    val customApp by minimalPhoneManager.customApp.collectAsState()
    val customAppInfo = remember(customApp) { minimalPhoneManager.getCustomAppInfo() }
    var showAppSelector by remember { mutableStateOf(false) }

    // Obtener datos de semanas de vida
    val context = LocalContext.current
    var lifeWeeksData by remember { mutableStateOf<LifeWeeksCalculator.LifeWeeksData?>(null) }

    LaunchedEffect(Unit) {
        val database = AppDatabase.getDatabase(context)
        val userSettings = withContext(Dispatchers.IO) {
            database.userDao().getUserSettingsSync()
        }
        lifeWeeksData = userSettings?.birthDate?.let { birthDate ->
            LifeWeeksCalculator.calculateLifeWeeks(birthDate)
        }
    }

    val essentialApps = listOf(
        EssentialApp("Teléfono", Icons.Default.Phone) {
            if (!minimalPhoneManager.openDialer()) {
                // Fallback al marcador interno
                onDialerClick()
            }
        },
        EssentialApp("Mensajes", Icons.Default.Message) {
            minimalPhoneManager.openMessages()
        },
        EssentialApp("Contactos", Icons.Default.Contacts) {
            minimalPhoneManager.openContacts()
        },
        EssentialApp("Configuración", Icons.Default.Settings) {
            if (!minimalPhoneManager.openSettings()) {
                // Fallback: abrir configuración de la app
                onSettingsClick()
            }
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

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = { showExitConfirm = true }) {
            Icon(Icons.Default.Close, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Salir del modo mínimo")
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Reloj con barra de progreso circular de semanas de vida
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(240.dp)
        ) {
            // Barra de progreso circular de semanas de vida
            lifeWeeksData?.let { data ->
                CircularLifeWeeksProgress(
                    progressPercentage = data.progressPercentage,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Reloj en el centro
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hora actual
                var currentTime by remember { mutableStateOf(getCurrentTime()) }

                LaunchedEffect(Unit) {
                    while (true) {
                        currentTime = getCurrentTime()
                        kotlinx.coroutines.delay(1000) // Actualizar cada segundo
                    }
                }

                Text(
                    text = currentTime,
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                lifeWeeksData?.let { data ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${data.progressPercentage.toInt()}% vivido",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        // Essential apps grid (4 apps + 1 custom slot)
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Primera fila: Teléfono y Mensajes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EssentialAppCard(
                    app = essentialApps[0],
                    modifier = Modifier.weight(1f)
                )
                EssentialAppCard(
                    app = essentialApps[1],
                    modifier = Modifier.weight(1f)
                )
            }

            // Segunda fila: Contactos y Configuración
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EssentialAppCard(
                    app = essentialApps[2],
                    modifier = Modifier.weight(1f)
                )
                EssentialAppCard(
                    app = essentialApps[3],
                    modifier = Modifier.weight(1f)
                )
            }

            // Tercera fila: App personalizada (centrada)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                if (customAppInfo != null) {
                    CustomAppCard(
                        appInfo = customAppInfo,
                        onClick = { minimalPhoneManager.openCustomApp() },
                        onLongClick = { showAppSelector = true },
                        modifier = Modifier.width(160.dp)
                    )
                } else {
                    AddCustomAppCard(
                        onClick = { showAppSelector = true },
                        modifier = Modifier.width(160.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        TextButton(
            onClick = onAppListClick
        ) {
            Text("Todas las apps", style = MaterialTheme.typography.titleMedium)
        }
        
    }

    if (showAppSelector) {
        AppSelectorDialog(
            minimalPhoneManager = minimalPhoneManager,
            currentSelection = customApp,
            onDismiss = { showAppSelector = false },
            onAppSelected = { packageName ->
                coroutineScope.launch {
                    minimalPhoneManager.setCustomApp(packageName)
                    showAppSelector = false
                }
            }
        )
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text("Salir del modo mínimo") },
            text = { Text("¿Seguro que quieres salir? Se restaurará la experiencia completa de Momentum.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitConfirm = false
                        onExitMinimalMode()
                    }
                ) {
                    Text("Salir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun CircularLifeWeeksProgress(
    progressPercentage: Float,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = modifier) {
        val strokeWidth = 12.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val radius = diameter / 2
        val topLeft = Offset(
            x = (size.width - diameter) / 2,
            y = (size.height - diameter) / 2
        )

        // Círculo de fondo (semanas restantes)
        drawArc(
            color = surfaceVariant,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = Size(diameter, diameter),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Círculo de progreso (semanas vividas)
        val sweepAngle = (progressPercentage / 100f) * 360f
        drawArc(
            color = primaryColor,
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = topLeft,
            size = Size(diameter, diameter),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Punto indicador en el progreso actual
        val angleInRadians = Math.toRadians((sweepAngle - 90).toDouble())
        val indicatorX = (size.width / 2) + (radius * Math.cos(angleInRadians)).toFloat()
        val indicatorY = (size.height / 2) + (radius * Math.sin(angleInRadians)).toFloat()

        drawCircle(
            color = primaryColor,
            radius = strokeWidth / 2,
            center = Offset(indicatorX, indicatorY)
        )
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
    val coroutineScope = rememberCoroutineScope()
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
                        coroutineScope.launch {
                            if (isAllowed) {
                                minimalPhoneManager.addAllowedApp(appInfo.packageName)
                            } else {
                                minimalPhoneManager.removeAllowedApp(appInfo.packageName)
                            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomAppCard(
    appInfo: AppInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "📱",
                style = MaterialTheme.typography.displaySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = appInfo.appName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onLongClick) {
                Text(
                    text = "Cambiar",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCustomAppCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Agregar app",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Agregar app",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AppSelectorDialog(
    minimalPhoneManager: MinimalPhoneManager,
    currentSelection: String?,
    onDismiss: () -> Unit,
    onAppSelected: (String?) -> Unit
) {
    val installedApps = remember {
        minimalPhoneManager.getInstalledApps().filter {
            !it.isCore // Excluir apps del sistema
        }
    }
    var searchQuery by remember { mutableStateOf("") }

    val filteredApps = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            installedApps
        } else {
            installedApps.filter {
                it.appName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Seleccionar aplicación adicional")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Elige una app para agregar al teléfono mínimo (solo una):",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Buscar app") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (currentSelection != null) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Quitar app actual",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.Medium
                                    )
                                    TextButton(onClick = { onAppSelected(null) }) {
                                        Text("Quitar")
                                    }
                                }
                            }
                        }
                    }

                    items(filteredApps) { appInfo ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = if (appInfo.packageName == currentSelection) {
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            } else {
                                CardDefaults.cardColors()
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = appInfo.appName,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = appInfo.packageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (appInfo.packageName == currentSelection) {
                                    Text(
                                        text = "✓",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    TextButton(
                                        onClick = { onAppSelected(appInfo.packageName) }
                                    ) {
                                        Text("Seleccionar")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

private fun getCurrentTime(): String {
    return java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        .format(java.util.Date())
}