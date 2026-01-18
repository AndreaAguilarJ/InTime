package com.momentummm.app.ui.screen.settings

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.momentummm.app.MomentumApplication
import com.momentummm.app.R
import kotlinx.coroutines.flow.Flow
import com.momentummm.app.data.UserPreferencesRepository
import com.momentummm.app.data.appwrite.models.AppwriteUserSettings
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import androidx.glance.appwidget.updateAll
import com.momentummm.app.widget.LifeWeeksWidget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as MomentumApplication
    val coroutineScope = rememberCoroutineScope()

    val currentUser by application.appwriteService.currentUser.collectAsState()

    var userSettingsFlow by remember { mutableStateOf<Flow<AppwriteUserSettings?>?>(null) }
    LaunchedEffect(currentUser?.id) {
        val userId = currentUser?.id?.takeIf { it.isNotBlank() }
        userSettingsFlow = if (userId != null) {
            application.appwriteUserRepository.getUserSettings(userId)
        } else {
            null
        }
    }
    val userSettings = userSettingsFlow?.collectAsState(initial = null)?.value

    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showBirthDatePicker by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Date picker dialog
    val calendar = Calendar.getInstance()

    if (showBirthDatePicker) {
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)

                // Convert to LocalDate and ISO format
                val localDate = LocalDate.of(year, month + 1, dayOfMonth)
                val iso = localDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

                coroutineScope.launch {
                    try {
                        isLoading = true
                        errorMessage = null

                        val userId = currentUser?.id
                        if (userId != null) {
                            // Update in Appwrite
                            val existing = userSettings ?: AppwriteUserSettings(
                                userId = userId,
                                birthDate = ""
                            )
                            val updated = existing.copy(birthDate = iso)
                            application.appwriteUserRepository.updateUserSettings(userId, updated)

                            // Save locally for widget
                            UserPreferencesRepository.setDobIso(context, iso)

                            // Save in local Room database
                            val dateFormatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            val date = dateFormatter.parse(iso)
                            if (date != null) {
                                application.userRepository.setBirthDate(date)
                            }

                            // Update widget - trigger update
                            try {
                                LifeWeeksWidget().updateAll(context)
                            } catch (_: Exception) {
                                // Widget update failed, but data is saved
                            }

                            successMessage = "Fecha de nacimiento actualizada correctamente"
                        } else {
                            errorMessage = "Debes iniciar sesión para actualizar tu fecha de nacimiento"
                        }
                    } catch (e: Exception) {
                        errorMessage = "Error al actualizar: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }

                showBirthDatePicker = false
            },
            calendar.get(Calendar.YEAR) - 25, // Default to 25 years ago
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.datePicker.maxDate = System.currentTimeMillis() // No future dates
        datePickerDialog.setOnDismissListener {
            showBirthDatePicker = false
        }

        LaunchedEffect(Unit) {
            datePickerDialog.show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.account_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = {
            // Show success or error messages
            successMessage?.let { message ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { successMessage = null }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(message)
                }
            }

            errorMessage?.let { message ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.error,
                    action = {
                        TextButton(onClick = { errorMessage = null }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(message)
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Show loading indicator
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            // Información del perfil
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Información del perfil",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = currentUser?.name ?: "Usuario",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = currentUser?.email ?: "email@ejemplo.com",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Información de la cuenta
            item {
                Text(
                    text = "Configuración de cuenta",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Cambiar contraseña
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        // TODO: Implementar cambio de contraseña
                    }
                ) {
                    ListItem(
                        headlineContent = { Text("Cambiar contraseña") },
                        supportingContent = { Text("Actualiza tu contraseña de acceso") },
                        leadingContent = {
                            Icon(Icons.Default.Lock, contentDescription = null)
                        },
                        trailingContent = {
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    )
                }
            }

            // Datos de onboarding
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Datos personales",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Fecha de nacimiento",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = userSettings?.birthDate?.takeIf { it.isNotBlank() }?.let {
                                        try {
                                            val localDate = LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE)
                                            localDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                        } catch (e: Exception) {
                                            "No configurada"
                                        }
                                    } ?: "No configurada",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            TextButton(
                                onClick = {
                                    showBirthDatePicker = true
                                },
                                enabled = !isLoading
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Editar")
                            }
                        }

                        Divider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Onboarding completado",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (userSettings?.isOnboardingCompleted == true) "Sí" else "No",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }

                            if (userSettings?.isOnboardingCompleted != true) {
                                TextButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val userId = currentUser?.id ?: return@launch
                                            application.appwriteUserRepository.completeOnboarding(userId)
                                        }
                                    }
                                ) {
                                    Text("Completar")
                                }
                            }
                        }
                    }
                }
            }

            // Preferencias de colores
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Preferencias de visualización",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Colores del widget",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Personaliza los colores de tu visualización",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            TextButton(
                                onClick = {
                                    // TODO: Abrir configuración de colores
                                }
                            ) {
                                Text("Personalizar")
                            }
                        }
                    }
                }
            }

            // Acciones de cuenta
            item {
                Text(
                    text = "Acciones de cuenta",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Cerrar sesión
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showSignOutDialog = true }
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(R.string.logout),
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        supportingContent = { Text("Cerrar sesión en este dispositivo") },
                        leadingContent = {
                            Icon(
                                Icons.Default.Logout,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }

            // Eliminar cuenta
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showDeleteAccountDialog = true }
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = "Eliminar cuenta",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        supportingContent = { Text("Esta acción no se puede deshacer") },
                        leadingContent = {
                            Icon(
                                Icons.Default.DeleteForever,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }

    // Diálogo de cerrar sesión
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Cerrar sesión") },
            text = { Text("¿Estás seguro de que quieres cerrar sesión?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            try {
                                application.appwriteService.logout()
                                showSignOutDialog = false
                                onBackClick() // Esto debería llevar de vuelta al flujo de autenticación
                            } catch (e: Exception) {
                                errorMessage = "Error al cerrar sesión: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text("Cerrar sesión")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSignOutDialog = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de eliminar cuenta
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Eliminar cuenta") },
            text = {
                Text("¿Estás seguro de que quieres eliminar tu cuenta? Esta acción no se puede deshacer y perderás todos tus datos.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            try {
                                // TODO: Implementar eliminación de cuenta
                                // application.appwriteService.deleteAccount()
                                showDeleteAccountDialog = false
                                onBackClick()
                            } catch (e: Exception) {
                                errorMessage = "Error al eliminar cuenta: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAccountDialog = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Mostrar error si existe
    errorMessage?.let { message ->
        LaunchedEffect(message) {
            // TODO: Mostrar snackbar con el error
        }
    }
}
