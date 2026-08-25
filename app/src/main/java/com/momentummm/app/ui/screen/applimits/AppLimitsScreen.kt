package com.momentummm.app.ui.screen.applimits
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.momentummm.app.ui.theme.momentum
import com.momentummm.app.R
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
                title = { Text(stringResource(R.string.limits_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = it) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.a11y_back))
                    }
                },
                actions = {
                    onNavigateToWhitelist?.let { navigate ->
                        IconButton(onClick = navigate) {
                            Icon(Icons.Filled.Shield, contentDescription = stringResource(R.string.a11y_emergency_apps))
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
                                text = stringResource(R.string.limits_screen_title),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.limits_screen_subtitle),
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

            // Nueva Sección: Apps Comunes para Bloquear (Sugerencias)
            if (uiState.suggestedApps.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.limits_common_apps),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stringResource(R.string.limits_common_apps_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Filled.Recommend,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(
                                items = uiState.suggestedApps,
                                key = { it.packageName }
                            ) { app ->
                                val hasLimit = uiState.appLimits.any { it.packageName == app.packageName }
                                SuggestedAppCard(
                                    app = app,
                                    hasLimit = hasLimit,
                                    onAddLimit = { packageName, appName, limitMinutes ->
                                        protectAction {
                                            viewModel.addAppLimit(packageName, appName, limitMinutes)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
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
                                    stringResource(R.string.limits_emergency_apps),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    stringResource(R.string.limits_emergency_apps_desc),
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
                        text = stringResource(R.string.limits_apps_with_limits),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    MomentumButton(
                        onClick = { showAddAppDialog = true },
                        style = ButtonStyle.Primary,
                        size = ButtonSize.Small,
                        icon = Icons.Filled.Add
                    ) {
                        Text(stringResource(R.string.limits_add_app))
                    }
                }
            }

            if (uiState.appLimits.isNotEmpty()) {
                items(
                    items = uiState.appLimits,
                    key = { it.packageName }
                ) { appLimit ->
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
                    // getString y no stringResource: esto corre dentro de una corrutina,
                    // no en composición, y stringResource solo es válido en composables.
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.limits_added)
                        )
                    }
                }
            )
        }

        // Overlay permission dialog
        if (showOverlayPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showOverlayPermissionDialog = false },
                title = { Text(stringResource(R.string.limits_permission_needed)) },
                text = {
                    Text(
                        stringResource(R.string.limits_permission_message)
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
                    ) { Text(stringResource(R.string.limits_open_settings)) }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = { showOverlayPermissionDialog = false }) {
                            Text(stringResource(R.string.limit_dialog_cancel))
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
                        ) { Text(stringResource(R.string.limits_already_enabled)) }
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
                title = stringResource(R.string.limits_verification_required),
                message = stringResource(R.string.limits_verification_message)
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
                text = stringResource(R.string.limits_summary_title),
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
                    label = stringResource(R.string.limits_summary_limited_apps),
                    icon = Icons.Filled.Apps,
                    color = MaterialTheme.colorScheme.primary
                )

                StatItem(
                    value = activeBlocks.toString(),
                    label = stringResource(R.string.limits_summary_active),
                    icon = Icons.Filled.Block,
                    color = MaterialTheme.colorScheme.secondary
                )

                StatItem(
                    value = "${avgDailyLimit}m",
                    label = stringResource(R.string.limits_summary_average),
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

    val appIcon = remember(appLimit.packageName) {
        runCatching { context.packageManager.getApplicationIcon(appLimit.packageName) }.getOrNull()
    }

    val isOverLimit = remainingTime <= 0
    val progressPercentage = if (appLimit.dailyLimitMinutes > 0) {
        ((appLimit.dailyLimitMinutes - remainingTime).toFloat() / appLimit.dailyLimitMinutes.toFloat())
            .coerceIn(0f, 1f)
    } else 0f

    // Semáforo de consumo: verde hasta el 60%, ámbar al acercarse y rojo al pasarse.
    // El color lo llevan la barra y la cifra restante, que son lo que se mira primero.
    val stateColor = when {
        !appLimit.isEnabled -> MaterialTheme.momentum.textTertiary
        isOverLimit -> MaterialTheme.momentum.danger
        progressPercentage > 0.8f -> MaterialTheme.momentum.warning
        else -> MaterialTheme.momentum.success
    }

    MomentumCard(modifier = modifier) {
        Column(modifier = Modifier.padding(MomentumDesign.Spacing.medium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (appIcon != null) {
                    Image(
                        painter = BitmapPainter(appIcon.toBitmap().asImageBitmap()),
                        contentDescription = null,
                        modifier = Modifier
                            .size(MomentumDesign.Size.iconTile)
                            .clip(RoundedCornerShape(MomentumDesign.CornerRadius.medium))
                    )
                } else {
                    IconTile(icon = Icons.Filled.Apps)
                }

                Spacer(modifier = Modifier.width(MomentumDesign.Spacing.compact))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appLimit.appName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.momentum.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(MomentumDesign.Spacing.hairline))
                    Text(
                        text = stringResource(R.string.app_limits_daily_limit_value, appLimit.dailyLimitMinutes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.momentum.textSecondary
                    )
                }

                Spacer(modifier = Modifier.width(MomentumDesign.Spacing.small))

                Switch(
                    checked = appLimit.isEnabled,
                    onCheckedChange = onToggleEnabled
                )
            }

            Spacer(modifier = Modifier.height(MomentumDesign.Spacing.medium))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = if (isOverLimit) {
                        stringResource(R.string.app_limits_exceeded)
                    } else {
                        stringResource(R.string.app_limits_remaining_value, remainingTime)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = stateColor,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${(progressPercentage * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.momentum.textTertiary
                )
            }

            Spacer(modifier = Modifier.height(MomentumDesign.Spacing.small))

            ProgressBar(
                progress = progressPercentage,
                color = stateColor,
                height = 8.dp,
            )

            Spacer(modifier = Modifier.height(MomentumDesign.Spacing.medium))

            // Acciones como tiles compactos: dos TextButton anchos competían con el
            // switch por la atención en una tarjeta que ya es densa.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MomentumDesign.Spacing.small)
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(MomentumDesign.Size.iconTileSmall)
                        .clip(MomentumDesign.Shapes.pill)
                        .background(MaterialTheme.momentum.surfaceSunken)
                        .clickable { showEditDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.edit),
                        tint = MaterialTheme.momentum.textSecondary,
                        modifier = Modifier.size(MomentumDesign.Size.icon)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(MomentumDesign.Size.iconTileSmall)
                        .clip(MomentumDesign.Shapes.pill)
                        .background(MaterialTheme.momentum.danger.copy(alpha = MomentumDesign.Alpha.soft))
                        .clickable(onClick = onRemove),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.momentum.danger,
                        modifier = Modifier.size(MomentumDesign.Size.icon)
                    )
                }
            }
        }
    }

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
                text = stringResource(R.string.limits_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.limits_empty_message),
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
                Text(stringResource(R.string.limits_empty_action))
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
                    text = if (isMonitoring) "Monitoreo Activo" else stringResource(R.string.limits_monitoring_inactive),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isMonitoring) {
                        stringResource(R.string.limits_monitoring_active_desc)
                    } else {
                        stringResource(R.string.limits_monitoring_inactive_desc)
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

/**
 * Card compacta para mostrar apps sugeridas en scroll horizontal
 */
@Composable
private fun SuggestedAppCard(
    app: AppUsageInfo,
    hasLimit: Boolean,
    onAddLimit: (String, String, Int) -> Unit
) {
    val context = LocalContext.current
    var showQuickDialog by remember { mutableStateOf(false) }
    
    // Load app icon outside of composable calls
    val appIcon = remember(app.packageName) {
        try {
            context.packageManager.getApplicationIcon(app.packageName).toBitmap()
        } catch (e: Exception) {
            null
        }
    }
    
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(180.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasLimit) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        onClick = { if (!hasLimit) showQuickDialog = true }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // App Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (appIcon != null) {
                    Image(
                        painter = BitmapPainter(appIcon.asImageBitmap()),
                        contentDescription = app.appName,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Icon(
                        Icons.Filled.Apps,
                        contentDescription = app.appName,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // App Name
            Text(
                text = app.appName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                color = if (hasLimit) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            // Action Button
            if (hasLimit) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.limits_badge_limited),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = { showQuickDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.limits_action_limit),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Quick Limit Dialog
    if (showQuickDialog) {
        QuickLimitDialog(
            appName = app.appName,
            onDismiss = { showQuickDialog = false },
            onConfirm = { minutes ->
                onAddLimit(app.packageName, app.appName, minutes)
                showQuickDialog = false
            }
        )
    }
}

/**
 * Diálogo rápido para establecer límites comunes
 */
@Composable
private fun QuickLimitDialog(
    appName: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedMinutes by remember { mutableStateOf(30) }
    
    val quickOptions = listOf(
        15 to "15 min",
        30 to "30 min",
        45 to "45 min",
        60 to "1 hora",
        90 to "1.5 horas",
        120 to "2 horas"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Filled.Timer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.limits_set_limit),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.limits_set_limit_question, appName),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickOptions.forEach { (minutes, label) ->
                        FilterChip(
                            selected = selectedMinutes == minutes,
                            onClick = { selectedMinutes = minutes },
                            label = { Text(label) },
                            leadingIcon = if (selectedMinutes == minutes) {
                                {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedMinutes) }
            ) {
                Text(stringResource(R.string.limits_set_limit))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.limit_dialog_cancel))
            }
        }
    )
}
