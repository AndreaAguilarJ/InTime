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
import com.momentummm.app.data.repository.PasswordProtectionSettings
import androidx.compose.ui.res.stringResource
import com.momentummm.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordProtectionSetupScreen(
    onNavigateBack: () -> Unit,
    viewModel: PasswordProtectionViewModel = hiltViewModel()
) {
    // Los tres mensajes se resuelven en composición: se asignan dentro de un onClick,
    // donde stringResource no es válido.
    val errorTooShort = stringResource(R.string.pwd_error_too_short)
    val errorMismatch = stringResource(R.string.pwd_error_mismatch)
    val errorDigitsOnly = stringResource(R.string.pwd_error_digits_only)
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    var protectAppLimits by remember { mutableStateOf(true) }
    var protectInAppBlocking by remember { mutableStateOf(true) }
    var protectWebsiteBlocking by remember { mutableStateOf(true) }
    var protectMinimalMode by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pwd_setup_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Información
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Column {
                        Text(
                            stringResource(R.string.pwd_why_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.pwd_why_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Campo de contraseña
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    showError = false
                },
                label = { Text(stringResource(R.string.pwd_numeric_label)) },
                placeholder = { Text(stringResource(R.string.pwd_numeric_hint)) },
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
                singleLine = true
            )

            // Campo de confirmación
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    showError = false
                },
                label = { Text(stringResource(R.string.pwd_confirm)) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = showError
            )

            if (showError) {
                Text(
                    errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Opciones de protección
            Text(
                stringResource(R.string.pwd_what_to_protect),
                style = MaterialTheme.typography.titleMedium
            )

            Card {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.pwd_feature_app_limits))
                        Switch(
                            checked = protectAppLimits,
                            onCheckedChange = { protectAppLimits = it }
                        )
                    }

                    Divider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.pwd_feature_in_app_block))
                        Switch(
                            checked = protectInAppBlocking,
                            onCheckedChange = { protectInAppBlocking = it }
                        )
                    }

                    Divider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.pwd_feature_web_block))
                        Switch(
                            checked = protectWebsiteBlocking,
                            onCheckedChange = { protectWebsiteBlocking = it }
                        )
                    }

                    Divider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.pwd_feature_minimal))
                        Switch(
                            checked = protectMinimalMode,
                            onCheckedChange = { protectMinimalMode = it }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Botón guardar
            Button(
                onClick = {
                    when {
                        password.length < 6 -> {
                            showError = true
                            errorMessage = errorTooShort
                        }
                        password != confirmPassword -> {
                            showError = true
                            errorMessage = errorMismatch
                        }
                        !password.all { it.isDigit() } -> {
                            showError = true
                            errorMessage = errorDigitsOnly
                        }
                        else -> {
                            val settings = PasswordProtectionSettings(
                                protectAppLimits = protectAppLimits,
                                protectInAppBlocking = protectInAppBlocking,
                                protectWebsiteBlocking = protectWebsiteBlocking,
                                protectMinimalMode = protectMinimalMode
                            )
                            viewModel.setPassword(password, settings)
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = password.isNotEmpty() && confirmPassword.isNotEmpty()
            ) {
                Icon(Icons.Default.Lock, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.pwd_set_button))
            }
        }
    }
}
