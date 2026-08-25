package com.momentummm.app.ui.password

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.momentummm.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordProtectionManageScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSetup: () -> Unit,
    viewModel: PasswordProtectionViewModel = hiltViewModel()
) {
    val passwordProtection by viewModel.passwordProtection.collectAsStateWithLifecycle()

    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showDisableDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pwd_manage_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        val protection = passwordProtection
        if (protection == null || !protection.isEnabled) {
            // No hay contraseña configurada
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.LockOpen,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    stringResource(R.string.pwd_none_set),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.pwd_none_set_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onNavigateToSetup,
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.pwd_setup))
                }
            }
        } else {
            // Contraseña configurada
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Estado
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Column {
                            Text(
                                stringResource(R.string.pwd_active),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                stringResource(R.string.pwd_active_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Funciones protegidas
                Text(
                    stringResource(R.string.pwd_protected_features),
                    style = MaterialTheme.typography.titleMedium
                )

                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ProtectionStatusItem(
                            label = stringResource(R.string.pwd_feature_app_limits),
                            isProtected = protection.protectAppLimits
                        )
                        ProtectionStatusItem(
                            label = stringResource(R.string.pwd_feature_in_app_block),
                            isProtected = protection.protectInAppBlocking
                        )
                        ProtectionStatusItem(
                            label = stringResource(R.string.pwd_feature_web_block),
                            isProtected = protection.protectWebsiteBlocking
                        )
                        ProtectionStatusItem(
                            label = stringResource(R.string.pwd_feature_minimal),
                            isProtected = protection.protectMinimalMode
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Acciones
                OutlinedButton(
                    onClick = { showChangePasswordDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.pwd_change_title))
                }

                Button(
                    onClick = { showDisableDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.pwd_disable_action))
                }
            }
        }
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            viewModel = viewModel
        )
    }

    if (showDisableDialog) {
        DisablePasswordDialog(
            onDismiss = { showDisableDialog = false },
            viewModel = viewModel
        )
    }
}

@Composable
private fun ProtectionStatusItem(label: String, isProtected: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isProtected) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = if (isProtected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
            Text(
                if (isProtected) "Protegido" else stringResource(R.string.pwd_not_protected),
                style = MaterialTheme.typography.bodySmall,
                color = if (isProtected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    viewModel: PasswordProtectionViewModel
) {
    // Los cuatro mensajes se resuelven en composición: se asignan dentro de un
    // onClick, donde stringResource no es válido.
    val errorTooShort = stringResource(R.string.pwd_error_too_short)
    val errorMismatch = stringResource(R.string.pwd_error_mismatch)
    val errorDigitsOnly = stringResource(R.string.pwd_error_digits_only)
    val errorCurrentWrong = stringResource(R.string.pwd_error_current_wrong)
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pwd_change_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = {
                        oldPassword = it
                        showError = false
                    },
                    label = { Text(stringResource(R.string.pwd_current)) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        showError = false
                    },
                    label = { Text(stringResource(R.string.pwd_new)) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        showError = false
                    },
                    label = { Text(stringResource(R.string.pwd_confirm_new)) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showError
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Checkbox(
                        checked = passwordVisible,
                        onCheckedChange = { passwordVisible = it }
                    )
                    Text(stringResource(R.string.pwd_show_passwords), style = MaterialTheme.typography.bodySmall)
                }

                if (showError) {
                    Text(
                        errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        newPassword.length < 6 -> {
                            showError = true
                            errorMessage = errorTooShort
                        }
                        newPassword != confirmPassword -> {
                            showError = true
                            errorMessage = errorMismatch
                        }
                        !newPassword.all { it.isDigit() } -> {
                            showError = true
                            errorMessage = errorDigitsOnly
                        }
                        else -> {
                            viewModel.changePassword(oldPassword, newPassword) { success ->
                                if (success) {
                                    onDismiss()
                                } else {
                                    showError = true
                                    errorMessage = errorCurrentWrong
                                }
                            }
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.pwd_change_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.pwd_cancel))
            }
        }
    )
}

@Composable
private fun DisablePasswordDialog(
    onDismiss: () -> Unit,
    viewModel: PasswordProtectionViewModel
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null) },
        title = { Text(stringResource(R.string.pwd_disable_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.pwd_disable_message))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        showError = false
                    },
                    label = { Text(stringResource(R.string.pwd_enter)) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                stringResource(R.string.pwd_toggle_visibility)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showError
                )

                if (showError) {
                    Text(
                        stringResource(R.string.pwd_error_wrong),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.disablePasswordProtection(password) { success ->
                        if (success) {
                            onDismiss()
                        } else {
                            showError = true
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.pwd_disable_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.pwd_cancel))
            }
        }
    )
}

