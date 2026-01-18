package com.momentummm.app.ui.screen.applimits

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAppLimitDialog(
    appName: String,
    currentLimit: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var limitMinutes by remember { mutableStateOf(currentLimit.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
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

                Spacer(modifier = Modifier.height(24.dp))

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
                                onSave(minutes)
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
}
