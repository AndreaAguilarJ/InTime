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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PasswordVerificationDialog(
    onDismiss: () -> Unit,
    onVerified: () -> Unit,
    viewModel: PasswordProtectionViewModel,
    title: String = "Verificación Requerida",
    message: String = "Ingresa tu contraseña para continuar"
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    var isLockedOut by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val remainingLockoutTime by viewModel.remainingLockoutTime.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.updateRemainingLockoutTime()
    }

    LaunchedEffect(remainingLockoutTime) {
        isLockedOut = remainingLockoutTime > 0
        if (isLockedOut) {
            // Actualizar cada segundo mientras esté bloqueado
            while (isLockedOut && remainingLockoutTime > 0) {
                delay(1000)
                viewModel.updateRemainingLockoutTime()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                if (isLockedOut) Icons.Default.Lock else Icons.Default.Security,
                contentDescription = null
            )
        },
        title = { Text(if (isLockedOut) "Bloqueado Temporalmente" else title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isLockedOut) {
                    Text(
                        "Has excedido el número de intentos permitidos. Por favor espera ${remainingLockoutTime / 1000 / 60} minutos antes de intentar nuevamente.",
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(message)

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            showError = false
                        },
                        label = { Text("Contraseña") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    "Mostrar/Ocultar"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = showError,
                        enabled = !isVerifying
                    )

                    if (showError) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Contraseña incorrecta",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    if (isVerifying) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {
            if (!isLockedOut) {
                Button(
                    onClick = {
                        isVerifying = true
                        coroutineScope.launch {
                            val isValid = viewModel.verifyPassword(password)
                            isVerifying = false
                            if (isValid) {
                                onVerified()
                                onDismiss()
                            } else {
                                showError = true
                                password = ""
                                viewModel.updateRemainingLockoutTime()
                            }
                        }
                    },
                    enabled = password.isNotEmpty() && !isVerifying
                ) {
                    Text("Verificar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * Composable helper para proteger acciones con contraseña
 */
@Composable
fun ProtectedAction(
    viewModel: PasswordProtectionViewModel,
    featureProtected: suspend () -> Boolean,
    onActionAllowed: () -> Unit,
    content: @Composable (triggerAction: () -> Unit) -> Unit
) {
    var showPasswordDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val triggerAction: () -> Unit = {
        coroutineScope.launch {
            val needsPassword = featureProtected()
            if (needsPassword) {
                showPasswordDialog = true
            } else {
                onActionAllowed()
            }
        }
        Unit
    }

    content(triggerAction)

    if (showPasswordDialog) {
        PasswordVerificationDialog(
            onDismiss = { showPasswordDialog = false },
            onVerified = { onActionAllowed() },
            viewModel = viewModel
        )
    }
}
