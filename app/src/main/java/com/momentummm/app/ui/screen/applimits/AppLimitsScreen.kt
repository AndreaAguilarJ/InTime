package com.momentummm.app.ui.screen.applimits
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.momentummm.app.data.entity.AppLimit
import com.momentummm.app.data.repository.AppUsageInfo
import com.momentummm.app.data.repository.ProtectedFeature
import com.momentummm.app.ui.password.PasswordProtectionViewModel
import com.momentummm.app.ui.password.PasswordVerificationDialog
import com.momentummm.app.ui.system.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLimitsScreen(
    viewModel: AppLimitsViewModel = hiltViewModel(),
    passwordViewModel: PasswordProtectionViewModel = hiltViewModel(),
    onBackClick: (() -> Unit)? = null,
    onNavigateToWhitelist: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddAppDialog by remember { mutableStateOf(false) }
    var showOverlayPermissionDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Show error message if any
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // Helper para proteger acciones
    val protectAction: (action: () -> Unit) -> Unit = { action ->
        scope.launch {
            val isProtected = passwordViewModel.isFeatureProtected(ProtectedFeature.APP_LIMITS)
            if (isProtected) {
                pendingAction = action
                showPasswordDialog = true
            } else {
                action()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        onBackClick?.let {
            TopAppBar(
                title = { Text("Límites de Aplicaciones") },
                navigationIcon = {
                    IconButton(onClick = it) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    onNavigateToWhitelist?.let { navigate ->
                        IconButton(onClick = navigate) {
                            Icon(Icons.Filled.Shield, contentDescription = "Apps de Emergencia")
                        }
                    }
                }
            )
        }

        if (uiState.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                ),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (onBackClick == null) {
                item {
                    // Header (only show if not in navigation mode)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Límites de Aplicaciones",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Controla el tiempo que pasas en apps distractoras",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Filled.Block,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }

            item {
                // Stats Card
                AppLimitsStatsCard(
                    totalAppsLimited = uiState.appLimits.size,
                    activeBlocks = uiState.appLimits.count { it.isEnabled },
                    avgDailyLimit = if (uiState.appLimits.isNotEmpty()) {
                        uiState.appLimits.map { it.dailyLimitMinutes }.average().toInt()
                    } else 0
                )
            }

            // Agregar botón para whitelist
            item {
                onNavigateToWhitelist?.let { navigate ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        onClick = navigate
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Apps de Emergencia",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    "Apps que nunca se bloquean",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                            Icon(
                                Icons.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            item {
                // Add new app button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Apps con Límites",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    MomentumButton(
                        onClick = { showAddAppDialog = true },
                        style = ButtonStyle.Primary,
                        size = ButtonSize.Small,
                        icon = Icons.Filled.Add
                    ) {
                        Text("Agregar App")
                    }
                }
            }

            if (uiState.appLimits.isNotEmpty()) {
                items(uiState.appLimits) { appLimit ->
                    AppLimitCard(
                        appLimit = appLimit,
                        remainingTime = uiState.remainingTimes[appLimit.packageName] ?: appLimit.dailyLimitMinutes,
                        onToggleEnabled = { enabled ->
                            protectAction {
                                viewModel.toggleAppLimit(appLimit.packageName, enabled)
                            }
                        },
                        onUpdateLimit = { newLimit ->
                            protectAction {
                                viewModel.updateAppLimit(appLimit.packageName, newLimit)
                            }
                        },
                        onRemove = {
                            protectAction {
                                viewModel.removeAppLimit(appLimit)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                item {
                    EmptyAppLimitsCard(
                        onAddApp = { showAddAppDialog = true }
                    )
                }
            }

            item {
                // Monitoring status
                MonitoringStatusCard(
                    isMonitoring = uiState.isMonitoringActive,
                    onToggleMonitoring = { enabled ->
                        if (enabled) {
                            val canOverlay = Settings.canDrawOverlays(context)
                            if (!canOverlay) {
                                showOverlayPermissionDialog = true
                                return@MonitoringStatusCard
                            }
                            viewModel.toggleAppMonitoring(true)
                        } else {
                            viewModel.toggleAppMonitoring(false)
                        }
                    }
                )
            }
        }

        // Add App Dialog
        if (showAddAppDialog) {
            AddAppLimitDialog(
                availableApps = uiState.availableApps,
                onDismiss = { showAddAppDialog = false },
                onAddApp = { packageName, appName, limitMinutes ->
                    viewModel.addAppLimit(packageName, appName, limitMinutes)
                    showAddAppDialog = false
                    scope.launch { snackbarHostState.showSnackbar("Límite agregado") }
                }
            )
        }

        // Overlay permission dialog
        if (showOverlayPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showOverlayPermissionDialog = false },
                title = { Text("Permiso necesario") },
                text = {
                    Text(
                        "Para bloquear apps al exceder el límite, debes permitir 'Mostrar sobre otras apps'."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            try {
                                val intent = android.content.Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:" + context.packageName)
                                )
                                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            } catch (_: Exception) { }
                        }
                    ) { Text("Abrir ajustes") }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = { showOverlayPermissionDialog = false }) {
                            Text("Cancelar")
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                // Reintentar activar si ya concedieron el permiso
                                if (Settings.canDrawOverlays(context)) {
                                    showOverlayPermissionDialog = false
                                    viewModel.toggleAppMonitoring(true)
                                }
                            }
                        ) { Text("Ya lo habilité") }
                    }
                }
            )
        }

        // Password verification dialog
        if (showPasswordDialog) {
            PasswordVerificationDialog(
                onDismiss = {
                    showPasswordDialog = false
                    pendingAction = null
                },
                onVerified = {
                    pendingAction?.invoke()
                    showPasswordDialog = false
                    pendingAction = null
                },
                viewModel = passwordViewModel,
                title = "Verificación Requerida",
                message = "Ingresa tu contraseña para modificar los límites de apps"
            )
        }

        // Snackbar host
        SnackbarHost(hostState = snackbarHostState)
    }
}

@Composable
private fun AppLimitsStatsCard(
    totalAppsLimited: Int,
    activeBlocks: Int,
    avgDailyLimit: Int
) {
    MomentumCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Resumen de Límites",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = totalAppsLimited.toString(),
                    label = "Apps\nlimitadas",
                    icon = Icons.Filled.Apps,
                    color = MaterialTheme.colorScheme.primary
                )

                StatItem(
                    value = activeBlocks.toString(),
                    label = "Límites\nactivos",
                    icon = Icons.Filled.Block,
                    color = MaterialTheme.colorScheme.secondary
                )

                StatItem(
                    value = "${avgDailyLimit}m",
                    label = "Límite\npromedio",
                    icon = Icons.Filled.Schedule,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AppLimitCard(
    appLimit: AppLimit,
    remainingTime: Int,
    onToggleEnabled: (Boolean) -> Unit,
    onUpdateLimit: (Int) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }

    // Obtener el icono de la aplicación
    val appIcon = remember(appLimit.packageName) {
        try {
            context.packageManager.getApplicationIcon(appLimit.packageName)
        } catch (e: Exception) {
            null
        }
    }

    val isOverLimit = remainingTime <= 0
    val progressPercentage = if (appLimit.dailyLimitMinutes > 0) {
        ((appLimit.dailyLimitMinutes - remainingTime).toFloat() / appLimit.dailyLimitMinutes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    MomentumCard(
        modifier = modifier,
        containerColor = if (isOverLimit && appLimit.isEnabled) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icono de la app
                if (appIcon != null) {
                    Image(
                        painter = BitmapPainter(appIcon.toBitmap().asImageBitmap()),
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Icon(
                            Icons.Filled.Apps,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = appLimit.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Límite: ${appLimit.dailyLimitMinutes} min/día",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isOverLimit && appLimit.isEnabled) {
                        Text(
                            text = "⚠️ Límite excedido",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = "Restante: ${remainingTime} min",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (remainingTime < 30) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Switch para habilitar/deshabilitar
                Switch(
                    checked = appLimit.isEnabled,
                    onCheckedChange = onToggleEnabled
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Barra de progreso
            LinearProgressIndicator(
                progress = progressPercentage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when {
                    isOverLimit -> MaterialTheme.colorScheme.error
                    progressPercentage > 0.8f -> MaterialTheme.colorScheme.tertiary
                    progressPercentage > 0.6f -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.primary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Botones de acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { showEditDialog = true }
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Editar")
                }

                TextButton(
                    onClick = onRemove,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Eliminar")
                }
            }
        }
    }

    // Edit Dialog
    if (showEditDialog) {
        EditAppLimitDialog(
            appName = appLimit.appName,
            currentLimit = appLimit.dailyLimitMinutes,
            onDismiss = { showEditDialog = false },
            onSave = { newLimit ->
                onUpdateLimit(newLimit)
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun EmptyAppLimitsCard(
    onAddApp: () -> Unit
) {
    MomentumCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onAddApp
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.PhoneAndroid,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No hay límites configurados",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Agrega aplicaciones distractoras para controlar el tiempo que pasas en ellas",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            MomentumButton(
                onClick = onAddApp,
                style = ButtonStyle.Primary,
                icon = Icons.Filled.Add
            ) {
                Text("Agregar Primera App")
            }
        }
    }
}

@Composable
private fun MonitoringStatusCard(
    isMonitoring: Boolean,
    onToggleMonitoring: (Boolean) -> Unit
) {
    MomentumCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isMonitoring) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                contentDescription = null,
                tint = if (isMonitoring) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (isMonitoring) "Monitoreo Activo" else "Monitoreo Inactivo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isMonitoring) {
                        "Las apps serán bloqueadas automáticamente al exceder sus límites"
                    } else {
                        "Activa el monitoreo para bloquear apps automáticamente"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = isMonitoring,
                onCheckedChange = onToggleMonitoring
            )
        }
    }
}
