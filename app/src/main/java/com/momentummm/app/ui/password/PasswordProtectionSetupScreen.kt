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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordProtectionSetupScreen(
    onNavigateBack: () -> Unit,
    viewModel: PasswordProtectionViewModel = hiltViewModel()
) {
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
                title = { Text("Protección por Contraseña") },
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
                            "¿Por qué usar contraseña?",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Protege tus configuraciones de bloqueo para que no puedas desactivarlas fácilmente en momentos de debilidad. Recomendamos una contraseña larga (20-30 dígitos) que escribas en papel y guardes en un lugar seguro.",
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
                label = { Text("Contraseña numérica") },
                placeholder = { Text("Ej: 123456789012345678901234567890") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            "Mostrar/Ocultar contraseña"
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
                label = { Text("Confirmar contraseña") },
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
                "¿Qué funciones deseas proteger?",
                style = MaterialTheme.typography.titleMedium
            )

            Card {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Límites de Apps")
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
                        Text("Bloqueo dentro de Apps")
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
                        Text("Bloqueo de Sitios Web")
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
                        Text("Modo Minimalista")
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
                            errorMessage = "La contraseña debe tener al menos 6 dígitos"
                        }
                        password != confirmPassword -> {
                            showError = true
                            errorMessage = "Las contraseñas no coinciden"
                        }
                        !password.all { it.isDigit() } -> {
                            showError = true
                            errorMessage = "La contraseña solo debe contener números"
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
                Text("Establecer Contraseña")
            }
        }
    }
}
