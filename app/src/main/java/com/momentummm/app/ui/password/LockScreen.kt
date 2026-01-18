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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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

    // Observar resultados de biometría
    LaunchedEffect(Unit) {
        viewModel.biometricResults.collect { result ->
            when (result) {
                is BiometricResult.AuthenticationSuccess -> {
                    appLockManager.unlockApp()
                }
                is BiometricResult.AuthenticationFailed -> {
                    errorMessage = "Autenticación biométrica fallida"
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
                    contentDescription = "Bloqueado",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Aplicación Bloqueada",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Ingresa tu contraseña para continuar",
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
                    label = { Text("Contraseña") },
                    placeholder = { Text("Ingresa tu contraseña") },
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
                                            errorMessage = "Demasiados intentos fallidos. Intenta en ${remainingLockoutTime / 1000 / 60} minutos"
                                        } else {
                                            errorMessage = "Contraseña incorrecta"
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
                                contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
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
                            text = "Bloqueado por ${remainingLockoutTime / 1000 / 60} minuto(s)",
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
                                        errorMessage = "Demasiados intentos fallidos. Intenta en ${remainingLockoutTime / 1000 / 60} minutos"
                                    } else {
                                        errorMessage = "Contraseña incorrecta"
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
                        Text("Desbloquear")
                    }
                }

                // Botón de biometría (si está disponible)
                if (isBiometricAvailable && activity != null && remainingLockoutTime == 0L) {
                    OutlinedButton(
                        onClick = {
                            biometricPromptManager.showBiometricPrompt(
                                activity = activity,
                                title = "Desbloquear InTime",
                                subtitle = "Usa tu huella o face ID",
                                description = "Verifica tu identidad para continuar"
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
                        Text("Usar Biometría")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Nota de ayuda
                Text(
                    text = "Si olvidaste tu contraseña, desinstala y reinstala la aplicación",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
