package com.momentummm.app.ui.password

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.momentummm.app.R
import com.momentummm.app.security.AppLockManager
import com.momentummm.app.security.BiometricPromptManager
import com.momentummm.app.security.BiometricResult
import kotlinx.coroutines.launch
import androidx.fragment.app.FragmentActivity

/**
 * Pantalla de bloqueo que aparece cuando la app está bloqueada
 * Requiere contraseña o biometría para desbloquear
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockScreen(
    appLockManager: AppLockManager,
    viewModel: PasswordProtectionViewModel = hiltViewModel(),
    biometricPromptManager: BiometricPromptManager
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    
    val remainingLockoutTime by viewModel.remainingLockoutTime.collectAsState()
    val isBiometricAvailable = viewModel.isBiometricAvailable()
    val biometricFailedMessage = stringResource(R.string.lock_screen_biometric_failed)
    val tooManyAttemptsMessage = stringResource(
        R.string.lock_screen_too_many_attempts,
        remainingLockoutTime / 1000 / 60
    )
    val incorrectPasswordMessage = stringResource(R.string.lock_screen_password_incorrect)

    // Observar resultados de biometría
    LaunchedEffect(Unit) {
        viewModel.biometricResults.collect { result ->
            when (result) {
                is BiometricResult.AuthenticationSuccess -> {
                    appLockManager.unlockApp()
                }
                is BiometricResult.AuthenticationFailed -> {
                    errorMessage = biometricFailedMessage
                }
                is BiometricResult.AuthenticationError -> {
                    errorMessage = result.error
                }
            }
        }
    }

    // Actualizar tiempo de bloqueo
    LaunchedEffect(Unit) {
        viewModel.updateRemainingLockoutTime()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icono de candado
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = stringResource(R.string.lock_screen_locked_cd),
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = stringResource(R.string.lock_screen_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = stringResource(R.string.lock_screen_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Campo de contraseña
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text(stringResource(R.string.lock_screen_password_label)) },
                    placeholder = { Text(stringResource(R.string.lock_screen_password_placeholder)) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (password.isNotEmpty() && remainingLockoutTime == 0L) {
                                scope.launch {
                                    isLoading = true
                                    val isCorrect = viewModel.verifyPassword(password)
                                    isLoading = false

                                    if (isCorrect) {
                                        appLockManager.unlockApp()
                                        password = ""
                                    } else {
                                        viewModel.updateRemainingLockoutTime()
                                        if (remainingLockoutTime > 0) {
                                            errorMessage = tooManyAttemptsMessage
                                        } else {
                                            errorMessage = incorrectPasswordMessage
                                        }
                                        password = ""
                                    }
                                }
                            }
                        }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) {
                                    stringResource(R.string.lock_screen_hide_password)
                                } else {
                                    stringResource(R.string.lock_screen_show_password)
                                }
                            )
                        }
                    },
                    enabled = remainingLockoutTime == 0L && !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errorMessage != null
                )

                // Mensaje de error
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }

                // Mensaje de bloqueo temporal
                if (remainingLockoutTime > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = stringResource(
                                R.string.lock_screen_temporarily_locked,
                                remainingLockoutTime / 1000 / 60
                            ),
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Botón de desbloqueo
                Button(
                    onClick = {
                        if (password.isNotEmpty() && remainingLockoutTime == 0L) {
                            scope.launch {
                                isLoading = true
                                val isCorrect = viewModel.verifyPassword(password)
                                isLoading = false

                                if (isCorrect) {
                                    appLockManager.unlockApp()
                                    password = ""
                                } else {
                                    viewModel.updateRemainingLockoutTime()
                                    if (remainingLockoutTime > 0) {
                                        errorMessage = tooManyAttemptsMessage
                                    } else {
                                        errorMessage = incorrectPasswordMessage
                                    }
                                    password = ""
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = password.isNotEmpty() && remainingLockoutTime == 0L && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(stringResource(R.string.lock_screen_unlock))
                    }
                }

                // Botón de biometría (si está disponible)
                if (isBiometricAvailable && activity != null && remainingLockoutTime == 0L) {
                    val biometricTitle = stringResource(R.string.lock_screen_biometric_title)
                    val biometricSubtitle = stringResource(R.string.lock_screen_biometric_subtitle)
                    val biometricDescription = stringResource(R.string.lock_screen_biometric_description)
                    OutlinedButton(
                        onClick = {
                            biometricPromptManager.showBiometricPrompt(
                                activity = activity,
                                title = biometricTitle,
                                subtitle = biometricSubtitle,
                                description = biometricDescription
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.lock_screen_use_biometric))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Nota de ayuda
                Text(
                    text = stringResource(R.string.lock_screen_help_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
