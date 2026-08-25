package com.momentummm.app.ui.screen.applimits

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.momentummm.app.data.entity.AppWhitelist
import com.momentummm.app.data.repository.AppUsageInfo
import com.momentummm.app.ui.system.*
import androidx.compose.ui.res.stringResource
import com.momentummm.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppWhitelistScreen(
    onNavigateBack: () -> Unit,
    viewModel: AppWhitelistViewModel = hiltViewModel()
) {
    val whitelistedApps by viewModel.whitelistedApps.collectAsStateWithLifecycle()
    val uiState = viewModel.uiState
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.whitelist_emergency_apps)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.a11y_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.a11y_add_app))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Información
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            stringResource(R.string.whitelist_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.whitelist_desc),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Botón para agregar apps de emergencia predeterminadas
            if (whitelistedApps.isEmpty()) {
                OutlinedButton(
                    onClick = { viewModel.addDefaultEmergencyApps() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Filled.LocalHospital, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.whitelist_add_defaults))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Lista de apps en whitelist
            if (whitelistedApps.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.SecurityUpdate,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            stringResource(R.string.whitelist_empty),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            stringResource(R.string.whitelist_empty_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(whitelistedApps, key = { it.packageName }) { app ->
                        WhitelistAppCard(
                            app = app,
                            onRemove = { viewModel.removeFromWhitelist(app) }
                        )
                    }
                }
            }
        }
    }

    // Diálogo para agregar app
    if (showAddDialog) {
        AddToWhitelistDialog(
            availableApps = uiState.availableApps.filter { availableApp ->
                whitelistedApps.none { it.packageName == availableApp.packageName }
            },
            onDismiss = { showAddDialog = false },
            onConfirm = { packageName, appName, reason ->
                viewModel.addToWhitelist(packageName, appName, reason)
                showAddDialog = false
            }
        )
    }

    // Mostrar error si existe
    uiState.error?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text(stringResource(R.string.whitelist_error)) },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun WhitelistAppCard(
    app: AppWhitelist,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (app.reason.isNotEmpty()) {
                    Text(
                        text = app.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.a11y_remove_from_whitelist),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToWhitelistDialog(
    availableApps: List<AppUsageInfo>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var selectedApp by remember { mutableStateOf<AppUsageInfo?>(null) }
    // "Emergencias" es el VALOR que se guarda, no un texto de interfaz: no se traduce.
    // La etiqueta visible sale de predefinedReasons más abajo.
    var reason by remember { mutableStateOf("Emergencias") }
    var showAppPicker by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredApps = remember(availableApps, searchQuery) {
        if (searchQuery.isBlank()) {
            availableApps
        } else {
            availableApps.filter {
                it.appName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Pares (valor que se guarda, etiqueta que se muestra).
    // El valor guardado sigue siendo el mismo de siempre para que las entradas
    // existentes en la base de datos no se rompan; solo cambia lo que se ve.
    val predefinedReasons = listOf(
        "Emergencias" to stringResource(R.string.whitelist_reason_emergency),
        "Trabajo" to stringResource(R.string.whitelist_reason_work),
        "Educación" to stringResource(R.string.whitelist_reason_education),
        "Salud" to stringResource(R.string.whitelist_reason_health),
        "Comunicación Familiar" to stringResource(R.string.whitelist_reason_family),
        "Otro" to stringResource(R.string.whitelist_reason_other)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.whitelist_add_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    stringResource(R.string.whitelist_add_desc),
                    style = MaterialTheme.typography.bodyMedium
                )

                // Selector de app
                OutlinedButton(
                    onClick = { showAppPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Apps, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(selectedApp?.appName ?: stringResource(R.string.whitelist_select_app))
                }

                // Selector de razón
                Text(stringResource(R.string.whitelist_reason_label), style = MaterialTheme.typography.labelMedium)
                predefinedReasons.forEach { (reasonValue, reasonLabel) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = reason == reasonValue,
                            onClick = { reason = reasonValue }
                        )
                        Text(
                            text = reasonLabel,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            MomentumButton(
                onClick = {
                    selectedApp?.let { app ->
                        onConfirm(app.packageName, app.appName, reason)
                    }
                },
                enabled = selectedApp != null,
                style = ButtonStyle.Primary
            ) {
                Text(stringResource(R.string.whitelist_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.whitelist_cancel))
            }
        }
    )

    // Diálogo para seleccionar app
    if (showAppPicker) {
        AlertDialog(
            onDismissRequest = { showAppPicker = false },
            title = { Text(stringResource(R.string.whitelist_picker_title)) },
            text = {
                Column(modifier = Modifier.heightIn(max = 400.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text(stringResource(R.string.whitelist_search)) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (filteredApps.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Filled.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    stringResource(R.string.whitelist_no_apps_found),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.animateContentSize()) {
                            items(filteredApps, key = { it.packageName }) { app ->
                                TextButton(
                                    onClick = {
                                        selectedApp = app
                                        showAppPicker = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = app.appName,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppPicker = false }) {
                    Text(stringResource(R.string.whitelist_close))
                }
            }
        )
    }
}
