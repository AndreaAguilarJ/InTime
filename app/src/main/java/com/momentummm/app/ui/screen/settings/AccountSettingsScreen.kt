package com.momentummm.app.ui.screen.settings

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.momentummm.app.MomentumApplication
import com.momentummm.app.R
import com.momentummm.app.data.UserPreferencesRepository
import com.momentummm.app.data.appwrite.models.AppwriteUserSettings
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.momentummm.app.widget.LifeWeeksWidget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    onBackClick: () -> Unit,
    onCustomizeColors: () -> Unit = {}
) {
    val context = LocalContext.current
    val application = context.applicationContext as MomentumApplication
    val coroutineScope = rememberCoroutineScope()

    val currentUser by application.appwriteService.currentUser.collectAsStateWithLifecycle()

    var userSettings by remember { mutableStateOf<AppwriteUserSettings?>(null) }
    LaunchedEffect(currentUser?.id) {
        val userId = currentUser?.id?.takeIf { it.isNotBlank() }
        if (userId != null) {
            application.appwriteUserRepository.getUserSettings(userId).collect { settings ->
                userSettings = settings
            }
        } else {
            userSettings = null
        }
    }

    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showBirthDatePicker by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Date picker dialog
    val calendar = Calendar.getInstance()

    // Strings para usar en coroutines
    val birthdateUpdatedMsg = stringResource(R.string.account_settings_birthdate_updated)
    val loginRequiredMsg = stringResource(R.string.account_settings_login_required_birthdate)
    val updateErrorMsgFormat = stringResource(R.string.account_settings_update_error)
    val logoutErrorMsgFormat = stringResource(R.string.account_settings_logout_error)
    val deleteAccountErrorMsgFormat = stringResource(R.string.account_settings_delete_account_error)
    val passwordChangedMsg = stringResource(R.string.account_settings_password_changed)
    val passwordChangeErrorMsg = stringResource(R.string.account_settings_password_change_error)

    if (showBirthDatePicker) {
        DisposableEffect(Unit) {
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

                                successMessage = birthdateUpdatedMsg
                            } else {
                                errorMessage = loginRequiredMsg
                            }
                        } catch (e: Exception) {
                            errorMessage = "$updateErrorMsgFormat ${e.message ?: ""}"
                        } finally {
                            isLoading = false
                        }
                    }

                    showBirthDatePicker = false
                },
                calendar.get(Calendar.YEAR) - 25, // Default to 25 years ago
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).apply {
                datePicker.maxDate = System.currentTimeMillis() // No future dates
                setOnDismissListener {
                    showBirthDatePicker = false
                }
                setOnCancelListener {
                    showBirthDatePicker = false
                }
            }

            datePickerDialog.show()

            onDispose {
                if (datePickerDialog.isShowing) {
                    datePickerDialog.dismiss()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.account_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.account_settings_back_cd)
                        )
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
                            Text(stringResource(R.string.account_settings_ok_button))
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
                            Text(stringResource(R.string.account_settings_ok_button))
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
                            text = stringResource(R.string.account_settings_profile_info_title),
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
                                    text = currentUser?.name
                                        ?: stringResource(R.string.account_settings_default_user_name),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = currentUser?.email
                                        ?: stringResource(R.string.account_settings_default_email),
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
                    text = stringResource(R.string.account_settings_section_title),
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
                        // Antes esto era un `// TODO` vacío: la tarjeta se podía
                        // pulsar, mostraba flecha de navegación y no hacía nada.
                        showChangePasswordDialog = true
                    }
                ) {
                    ListItem(
                        headlineContent = {
                            Text(stringResource(R.string.account_settings_change_password_title))
                        },
                        supportingContent = {
                            Text(stringResource(R.string.account_settings_change_password_subtitle))
                        },
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
                            text = stringResource(R.string.account_settings_personal_data_title),
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
                                    text = stringResource(R.string.account_settings_birthdate_label),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = userSettings?.birthDate?.takeIf { it.isNotBlank() }?.let {
                                        try {
                                            val localDate = LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE)
                                            localDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                        } catch (e: Exception) {
                                            stringResource(R.string.account_settings_not_configured)
                                        }
                                    } ?: stringResource(R.string.account_settings_not_configured),
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
                                Text(stringResource(R.string.account_settings_edit_button))
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
                                    text = stringResource(R.string.account_settings_onboarding_completed),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (userSettings?.isOnboardingCompleted == true) {
                                        stringResource(R.string.account_settings_yes)
                                    } else {
                                        stringResource(R.string.account_settings_no)
                                    },
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
                                    Text(stringResource(R.string.account_settings_complete_button))
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
                            text = stringResource(R.string.account_settings_display_preferences_title),
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
                                    text = stringResource(R.string.account_settings_widget_colors_title),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(R.string.account_settings_widget_colors_subtitle),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            TextButton(
                                onClick = onCustomizeColors
                            ) {
                                Text(stringResource(R.string.account_settings_customize_button))
                            }
                        }
                    }
                }
            }

            // Acciones de cuenta
            item {
                Text(
                    text = stringResource(R.string.account_settings_actions_title),
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
                        supportingContent = {
                            Text(stringResource(R.string.account_settings_logout_subtitle))
                        },
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
                                text = stringResource(R.string.account_settings_delete_account_title),
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        supportingContent = {
                            Text(stringResource(R.string.account_settings_delete_account_subtitle))
                        },
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
            title = { Text(stringResource(R.string.account_settings_logout_title)) },
            text = { Text(stringResource(R.string.account_settings_logout_message)) },
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
                                errorMessage = "$logoutErrorMsgFormat ${e.message ?: ""}"
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
                        Text(stringResource(R.string.account_settings_logout_title))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSignOutDialog = false }
                ) {
                    Text(stringResource(R.string.account_settings_cancel_button))
                }
            }
        )
    }

    // Diálogo de eliminar cuenta
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text(stringResource(R.string.account_settings_delete_account_title)) },
            text = {
                Text(stringResource(R.string.account_settings_delete_account_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            try {
                                // BUG CORREGIDO: aquí el borrado estaba
                                // comentado y aun así se cerraba el diálogo y se
                                // volvía atrás, así que el usuario creía que su
                                // cuenta había desaparecido cuando seguía intacta.
                                val result = application.appwriteService.deactivateAccount()
                                if (result.isSuccess) {
                                    showDeleteAccountDialog = false
                                    onBackClick()
                                } else {
                                    errorMessage = "$deleteAccountErrorMsgFormat " +
                                        (result.exceptionOrNull()?.message ?: "")
                                }
                            } catch (e: Exception) {
                                errorMessage = "$deleteAccountErrorMsgFormat ${e.message ?: ""}"
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
                        Text(
                            stringResource(R.string.account_settings_delete_button),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAccountDialog = false }
                ) {
                    Text(stringResource(R.string.account_settings_cancel_button))
                }
            }
        )
    }

    // Diálogo de cambio de contraseña
    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            isLoading = isLoading,
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { current, new ->
                coroutineScope.launch {
                    isLoading = true
                    val result = application.appwriteService.updatePassword(
                        newPassword = new,
                        oldPassword = current
                    )
                    isLoading = false
                    if (result.isSuccess) {
                        showChangePasswordDialog = false
                        successMessage = passwordChangedMsg
                    } else {
                        errorMessage = result.exceptionOrNull()?.message ?: passwordChangeErrorMsg
                    }
                }
            }
        )
    }
}

/**
 * Diálogo para cambiar la contraseña.
 *
 * Valida en el cliente lo mínimo para no gastar una llamada de red: que los
 * campos no estén vacíos, que la nueva tenga al menos 8 caracteres (el mínimo
 * de Appwrite) y que la confirmación coincida.
 */
@Composable
private fun ChangePasswordDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (currentPassword: String, newPassword: String) -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val tooShort = newPassword.isNotEmpty() && newPassword.length < MIN_PASSWORD_LENGTH
    val mismatch = confirmPassword.isNotEmpty() && confirmPassword != newPassword
    val canSubmit = currentPassword.isNotEmpty() &&
        newPassword.length >= MIN_PASSWORD_LENGTH &&
        confirmPassword == newPassword &&
        !isLoading

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        icon = { Icon(Icons.Default.Lock, contentDescription = null) },
        title = { Text(stringResource(R.string.account_settings_change_password_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text(stringResource(R.string.account_settings_current_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(stringResource(R.string.account_settings_new_password)) },
                    singleLine = true,
                    isError = tooShort,
                    supportingText = if (tooShort) {
                        { Text(stringResource(R.string.account_settings_password_too_short)) }
                    } else null,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(stringResource(R.string.account_settings_confirm_password)) },
                    singleLine = true,
                    isError = mismatch,
                    supportingText = if (mismatch) {
                        { Text(stringResource(R.string.account_settings_passwords_dont_match)) }
                    } else null,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(currentPassword, newPassword) },
                enabled = canSubmit
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text(stringResource(R.string.account_settings_save_button))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text(stringResource(R.string.account_settings_cancel_button))
            }
        }
    )
}

/** Mínimo que exige Appwrite para una contraseña. */
private const val MIN_PASSWORD_LENGTH = 8
