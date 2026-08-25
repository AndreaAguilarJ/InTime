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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.momentummm.app.R

@Composable
fun PasswordVerificationDialog(
    onDismiss: () -> Unit,
    onVerified: () -> Unit,
    viewModel: PasswordProtectionViewModel,
    title: String = stringResource(R.string.pwd_verify_title),
    message: String = stringResource(R.string.pwd_verify_subtitle)
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    var isLockedOut by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val remainingLockoutTime by viewModel.remainingLockoutTime.collectAsStateWithLifecycle()

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
        title = { Text(if (isLockedOut) stringResource(R.string.pwd_locked_title) else title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isLockedOut) {
                    Text(
                        run { val m = (remainingLockoutTime / 1000 / 60).toInt(); pluralStringResource(R.plurals.pwd_locked_message, m, m) },
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
                        label = { Text(stringResource(R.string.pwd_password_label)) },
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
                                stringResource(R.string.pwd_error_wrong),
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
                    Text(stringResource(R.string.pwd_verify_button))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.pwd_cancel))
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
