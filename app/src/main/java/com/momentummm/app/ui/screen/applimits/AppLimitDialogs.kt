package com.momentummm.app.ui.screen.applimits

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import com.momentummm.app.data.repository.AppUsageInfo
import com.momentummm.app.ui.system.*
import androidx.compose.material.icons.filled.Timer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppLimitDialog(
    availableApps: List<AppUsageInfo>,
    onDismiss: () -> Unit,
    onAddApp: (String, String, Int) -> Unit
) {
    var selectedApp by remember { mutableStateOf<AppUsageInfo?>(null) }
    var limitMinutes by remember { mutableStateOf("60") }
    var searchQuery by remember { mutableStateOf("") }
    val isLoading = remember(availableApps) { availableApps.isEmpty() }

    val filteredApps = remember(availableApps, searchQuery) {
        if (searchQuery.isBlank()) {
            availableApps
        } else {
            availableApps.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Agregar Límite de App",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Buscar aplicación") },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // App selection
                Text(
                    text = "Selecciona una aplicación:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Apps list
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .animateContentSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredApps) { app ->
                            AppSelectionItem(
                                app = app,
                                isSelected = selectedApp?.packageName == app.packageName,
                                onSelect = { selectedApp = app }
                            )
                        }
                        
                        if (filteredApps.isEmpty() && searchQuery.isNotEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Filled.SearchOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "No se encontraron apps",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Limit input
                OutlinedTextField(
                    value = limitMinutes,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() } && it.length <= 4) {
                            limitMinutes = it
                        }
                    },
                    label = { Text("Límite diario (minutos)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = {
                        Text("Ejemplo: 60 minutos = 1 hora")
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }

                    MomentumButton(
                        onClick = {
                            selectedApp?.let { app ->
                                val minutes = limitMinutes.toIntOrNull() ?: 60
                                if (minutes > 0) {
                                    onAddApp(app.packageName, app.appName, minutes)
                                }
                            }
                        },
                        enabled = selectedApp != null && limitMinutes.isNotBlank() && (limitMinutes.toIntOrNull() ?: 0) > 0,
                        style = ButtonStyle.Primary,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Agregar Límite")
                    }
                }
            }
        }
    }
}

@Composable
private fun AppSelectionItem(
    app: AppUsageInfo,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val context = LocalContext.current

    // Obtener el icono de la aplicación
    val appIcon = remember(app.packageName) {
        try {
            context.packageManager.getApplicationIcon(app.packageName)
        } catch (e: Exception) {
            null
        }
    }

    MomentumCard(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        containerColor = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon
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

            // App info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Selection indicator
            RadioButton(
                selected = isSelected,
                onClick = onSelect
            )
        }
    }
}

/**
 * Datos de configuración de horario para bloqueo
 */
data class ScheduleLimitConfig(
    val enabled: Boolean = false,
    val startHour: Int = 9,
    val startMinute: Int = 0,
    val endHour: Int = 17,
    val endMinute: Int = 0,
    val daysOfWeek: Set<Int> = setOf(1, 2, 3, 4, 5) // 1=Lun, 7=Dom
)

/**
 * Resultado del diálogo de edición con límite y opcionalmente horario
 */
data class EditLimitResult(
    val limitMinutes: Int,
    val scheduleConfig: ScheduleLimitConfig? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAppLimitDialog(
    appName: String,
    currentLimit: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    // Wrapper para compatibilidad con código existente
    EditAppLimitDialogExtended(
        appName = appName,
        currentLimit = currentLimit,
        hasScheduleLimit = false,
        currentScheduleConfig = null,
        onDismiss = onDismiss,
        onSave = { result -> onSave(result.limitMinutes) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAppLimitDialogExtended(
    appName: String,
    currentLimit: Int,
    hasScheduleLimit: Boolean = false,
    currentScheduleConfig: ScheduleLimitConfig? = null,
    onDismiss: () -> Unit,
    onSave: (EditLimitResult) -> Unit
) {
    var limitMinutes by remember { mutableStateOf(currentLimit.toString()) }
    var showScheduleSection by remember { mutableStateOf(hasScheduleLimit) }
    var scheduleEnabled by remember { mutableStateOf(currentScheduleConfig?.enabled ?: false) }
    var scheduleStartHour by remember { mutableIntStateOf(currentScheduleConfig?.startHour ?: 9) }
    var scheduleStartMinute by remember { mutableIntStateOf(currentScheduleConfig?.startMinute ?: 0) }
    var scheduleEndHour by remember { mutableIntStateOf(currentScheduleConfig?.endHour ?: 17) }
    var scheduleEndMinute by remember { mutableIntStateOf(currentScheduleConfig?.endMinute ?: 0) }
    var selectedDays by remember { mutableStateOf(currentScheduleConfig?.daysOfWeek ?: setOf(1, 2, 3, 4, 5)) }
    
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Editar Límite",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // App name
                Text(
                    text = "Aplicación: $appName",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ================================================================
                // SECCIÓN: Límite Diario
                // ================================================================
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Límite de Tiempo Diario",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                // Limit input
                OutlinedTextField(
                    value = limitMinutes,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() } && it.length <= 4) {
                            limitMinutes = it
                        }
                    },
                    label = { Text("Límite diario (minutos)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = {
                        Text("Límite actual: $currentLimit minutos")
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Preset buttons
                Text(
                    text = "Límites predefinidos:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(15, 30, 60, 120)
                    presets.forEach { preset ->
                        OutlinedButton(
                            onClick = { limitMinutes = preset.toString() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("${preset}m")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                
                Spacer(modifier = Modifier.height(16.dp))

                // ================================================================
                // SECCIÓN: Bloqueo por Horario
                // ================================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Bloqueo por Horario",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = scheduleEnabled,
                        onCheckedChange = { 
                            scheduleEnabled = it
                            if (it) showScheduleSection = true
                        }
                    )
                }
                
                Text(
                    text = "Bloquear esta app durante horarios específicos, independiente del límite de tiempo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Contenido expandible de horario
                androidx.compose.animation.AnimatedVisibility(visible = scheduleEnabled) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        // Selector de horario
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Hora de inicio
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Hora de inicio:",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    TextButton(onClick = { showStartTimePicker = true }) {
                                        Text(
                                            text = String.format("%02d:%02d", scheduleStartHour, scheduleStartMinute),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Hora de fin
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Hora de fin:",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    TextButton(onClick = { showEndTimePicker = true }) {
                                        Text(
                                            text = String.format("%02d:%02d", scheduleEndHour, scheduleEndMinute),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Días de la semana
                        Text(
                            text = "Días activos:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        DayOfWeekSelector(
                            selectedDays = selectedDays,
                            onDaysChanged = { selectedDays = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }

                    MomentumButton(
                        onClick = {
                            val minutes = limitMinutes.toIntOrNull() ?: currentLimit
                            if (minutes > 0) {
                                val scheduleConfig = if (scheduleEnabled) {
                                    ScheduleLimitConfig(
                                        enabled = true,
                                        startHour = scheduleStartHour,
                                        startMinute = scheduleStartMinute,
                                        endHour = scheduleEndHour,
                                        endMinute = scheduleEndMinute,
                                        daysOfWeek = selectedDays
                                    )
                                } else null
                                
                                onSave(EditLimitResult(minutes, scheduleConfig))
                            }
                        },
                        enabled = limitMinutes.isNotBlank() && (limitMinutes.toIntOrNull() ?: 0) > 0,
                        style = ButtonStyle.Primary,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
    
    // Time Pickers
    if (showStartTimePicker) {
        ScheduleTimePickerDialog(
            title = "Hora de Inicio",
            initialHour = scheduleStartHour,
            initialMinute = scheduleStartMinute,
            onConfirm = { hour, minute ->
                scheduleStartHour = hour
                scheduleStartMinute = minute
                showStartTimePicker = false
            },
            onDismiss = { showStartTimePicker = false }
        )
    }
    
    if (showEndTimePicker) {
        ScheduleTimePickerDialog(
            title = "Hora de Fin",
            initialHour = scheduleEndHour,
            initialMinute = scheduleEndMinute,
            onConfirm = { hour, minute ->
                scheduleEndHour = hour
                scheduleEndMinute = minute
                showEndTimePicker = false
            },
            onDismiss = { showEndTimePicker = false }
        )
    }
}

@Composable
private fun DayOfWeekSelector(
    selectedDays: Set<Int>,
    onDaysChanged: (Set<Int>) -> Unit
) {
    val days = listOf(
        1 to "L",
        2 to "M", 
        3 to "X",
        4 to "J",
        5 to "V",
        6 to "S",
        7 to "D"
    )
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        days.forEach { (dayNum, dayLabel) ->
            val isSelected = dayNum in selectedDays
            
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        val newDays = if (isSelected) {
                            selectedDays - dayNum
                        } else {
                            selectedDays + dayNum
                        }
                        onDaysChanged(newDays)
                    },
                color = if (isSelected) MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleTimePickerDialog(
    title: String,
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
        title = { Text(title) },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
