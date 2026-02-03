package com.momentummm.app.ui.screen.settings

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.momentummm.app.R
import com.momentummm.app.data.UserPreferencesRepository
import com.momentummm.app.receiver.InTimeDeviceAdminReceiver
import com.momentummm.app.ui.system.MomentumButton
import com.momentummm.app.ui.system.MomentumCard
import com.momentummm.app.ui.system.ButtonStyle
import kotlinx.coroutines.launch

/**
 * Pantalla de configuración de protección avanzada
 * - Device Admin para prevención de desinstalación
 * - Hora de inicio del día personalizable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtectionSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Estados de Device Admin
    var isDeviceAdminActive by remember { mutableStateOf(InTimeDeviceAdminReceiver.isAdminActive(context)) }
    var showDeviceAdminInfo by remember { mutableStateOf(false) }
    
    // Estados de hora de inicio del día
    val dayStartTime by UserPreferencesRepository.getDayStartTimeFlow(context).collectAsState(
        initial = Pair(0, 0)
    )
    var showTimePicker by remember { mutableStateOf(false) }
    
    // Estado de protección anti-desinstalación
    val isUninstallProtectionEnabled by UserPreferencesRepository.getUninstallProtectionFlow(context).collectAsState(
        initial = false
    )
    
    // Refrescar estado del Device Admin
    LaunchedEffect(Unit) {
        isDeviceAdminActive = InTimeDeviceAdminReceiver.isAdminActive(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_protection_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ========================================================================
            // SECCIÓN: Hora de Inicio del Día
            // ========================================================================
            DayStartTimeSection(
                currentHour = dayStartTime.first,
                currentMinute = dayStartTime.second,
                onShowTimePicker = { showTimePicker = true }
            )
            
            // ========================================================================
            // SECCIÓN: Protección Anti-Desinstalación (Device Admin)
            // ========================================================================
            DeviceAdminSection(
                isDeviceAdminActive = isDeviceAdminActive,
                isUninstallProtectionEnabled = isUninstallProtectionEnabled,
                showDeviceAdminInfo = showDeviceAdminInfo,
                onToggleInfo = { showDeviceAdminInfo = !showDeviceAdminInfo },
                onToggleProtection = { enabled ->
                    coroutineScope.launch {
                        UserPreferencesRepository.setUninstallProtectionEnabled(context, enabled)
                    }
                    if (enabled && !isDeviceAdminActive) {
                        InTimeDeviceAdminReceiver.requestAdminActivation(context)
                    }
                },
                onActivateAdmin = {
                    InTimeDeviceAdminReceiver.requestAdminActivation(context)
                    // Refrescar estado después de un delay
                    coroutineScope.launch {
                        kotlinx.coroutines.delay(500)
                        isDeviceAdminActive = InTimeDeviceAdminReceiver.isAdminActive(context)
                    }
                },
                onDeactivateAdmin = {
                    InTimeDeviceAdminReceiver.removeAdmin(context)
                    isDeviceAdminActive = false
                    coroutineScope.launch {
                        UserPreferencesRepository.setUninstallProtectionEnabled(context, false)
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    // TimePicker Dialog
    if (showTimePicker) {
        TimePickerDialog(
            initialHour = dayStartTime.first,
            initialMinute = dayStartTime.second,
            onConfirm = { hour, minute ->
                coroutineScope.launch {
                    UserPreferencesRepository.setDayStartTime(context, hour, minute)
                }
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

@Composable
private fun DayStartTimeSection(
    currentHour: Int,
    currentMinute: Int,
    onShowTimePicker: () -> Unit
) {
    val formattedTime = String.format("%02d:%02d", currentHour, currentMinute)
    
    MomentumCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.day_start_hour_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.day_start_hour_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Hora actual configurada
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.day_start_current),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    MomentumButton(
                        onClick = onShowTimePicker,
                        style = ButtonStyle.Secondary
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_change))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Información adicional
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.day_start_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceAdminSection(
    isDeviceAdminActive: Boolean,
    isUninstallProtectionEnabled: Boolean,
    showDeviceAdminInfo: Boolean,
    onToggleInfo: () -> Unit,
    onToggleProtection: (Boolean) -> Unit,
    onActivateAdmin: () -> Unit,
    onDeactivateAdmin: () -> Unit
) {
    MomentumCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = if (isDeviceAdminActive) MaterialTheme.colorScheme.primary 
                           else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.device_admin_settings_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.device_admin_settings_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onToggleInfo) {
                    Icon(
                        imageVector = if (showDeviceAdminInfo) Icons.Default.ExpandLess else Icons.Default.Info,
                        contentDescription = stringResource(R.string.action_more_info)
                    )
                }
            }
            
            // Información expandible
            AnimatedVisibility(visible = showDeviceAdminInfo) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.device_admin_info_title),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.device_admin_info_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Beneficios
                        BulletPoint(stringResource(R.string.device_admin_benefit_1))
                        BulletPoint(stringResource(R.string.device_admin_benefit_2))
                        BulletPoint(stringResource(R.string.device_admin_benefit_3))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Estado actual
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = if (isDeviceAdminActive) 
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else 
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isDeviceAdminActive) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isDeviceAdminActive) MaterialTheme.colorScheme.primary 
                               else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isDeviceAdminActive) 
                                stringResource(R.string.device_admin_status_active)
                            else 
                                stringResource(R.string.device_admin_status_inactive),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isDeviceAdminActive)
                                stringResource(R.string.device_admin_status_active_desc)
                            else
                                stringResource(R.string.device_admin_status_inactive_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Toggle de protección
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.uninstall_protection_toggle),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.uninstall_protection_toggle_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isUninstallProtectionEnabled && isDeviceAdminActive,
                    onCheckedChange = onToggleProtection,
                    enabled = isDeviceAdminActive
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Botones de acción
            if (!isDeviceAdminActive) {
                MomentumButton(
                    onClick = onActivateAdmin,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.device_admin_activate))
                }
            } else {
                OutlinedButton(
                    onClick = onDeactivateAdmin,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.device_admin_deactivate))
                }
            }
        }
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.day_start_picker_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.day_start_picker_description),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }
            ) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
