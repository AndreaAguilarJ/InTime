# 🔐 Guía de Integración - Sistema de Protección por Contraseña

## 📚 Ejemplos de Uso en tu Código

### 1. Proteger una Acción Específica

```kotlin
@Composable
fun SettingsScreen(
    viewModel: PasswordProtectionViewModel = hiltViewModel()
) {
    var showPasswordDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Button(
        onClick = {
            scope.launch {
                // Verificar si la característica está protegida
                val isProtected = viewModel.isFeatureProtected(
                    ProtectedFeature.APP_LIMITS
                )
                
                if (isProtected) {
                    showPasswordDialog = true
                } else {
                    // Ejecutar acción directamente
                    deleteAllAppLimits()
                }
            }
        }
    ) {
        Text("Eliminar Todos los Límites")
    }

    // Diálogo de verificación
    if (showPasswordDialog) {
        PasswordVerificationDialog(
            onDismiss = { showPasswordDialog = false },
            onSuccess = {
                deleteAllAppLimits()
                showPasswordDialog = false
            },
            title = "Confirmar Eliminación",
            description = "Se eliminarán todos los límites de aplicaciones"
        )
    }
}
```

---

### 2. Verificar Contraseña Antes de Navegar

```kotlin
@Composable
fun NavigateToProtectedScreen(
    navController: NavController,
    viewModel: PasswordProtectionViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()

    Card(
        onClick = {
            scope.launch {
                val isProtected = viewModel.isFeatureProtected(
                    ProtectedFeature.WEBSITE_BLOCKING
                )
                
                if (isProtected) {
                    // Mostrar diálogo de verificación primero
                    showVerificationDialog = true
                } else {
                    // Navegar directamente
                    navController.navigate("website_blocks")
                }
            }
        }
    ) {
        ListItem(
            headlineContent = { Text("Bloqueo de Sitios Web") },
            leadingContent = { Icon(Icons.Default.Security, null) }
        )
    }
}
```

---

### 3. Forzar Bloqueo de la App Manualmente

```kotlin
@Composable
fun QuickLockButton(
    context: Context
) {
    val application = context.applicationContext as MomentumApplication
    
    IconButton(
        onClick = {
            // Bloquear la app inmediatamente
            application.appLockManager.forcelock()
        }
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Bloquear App"
        )
    }
}
```

---

### 4. Verificar Estado de Protección

```kotlin
@Composable
fun SecurityStatusCard(
    viewModel: PasswordProtectionViewModel = hiltViewModel()
) {
    val passwordProtection by viewModel.passwordProtection.collectAsState()
    val isPasswordSet by viewModel.isPasswordSet.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Protección Activa")
                
                if (isPasswordSet) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            if (passwordProtection != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Características protegidas:",
                    style = MaterialTheme.typography.bodySmall
                )
                if (passwordProtection?.protectAppLimits == true) {
                    Text("• Límites de Aplicaciones", style = MaterialTheme.typography.bodySmall)
                }
                if (passwordProtection?.protectInAppBlocking == true) {
                    Text("• Bloqueo dentro de Apps", style = MaterialTheme.typography.bodySmall)
                }
                if (passwordProtection?.protectWebsiteBlocking == true) {
                    Text("• Bloqueo de Sitios Web", style = MaterialTheme.typography.bodySmall)
                }
                if (passwordProtection?.protectMinimalMode == true) {
                    Text("• Modo Minimalista", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
```

---

### 5. Usar Biometría Directamente

```kotlin
@Composable
fun BiometricButton(
    activity: FragmentActivity,
    viewModel: PasswordProtectionViewModel = hiltViewModel()
) {
    val biometricAuthStatus = viewModel.getBiometricAuthStatus()
    
    LaunchedEffect(Unit) {
        viewModel.biometricResults.collect { result ->
            when (result) {
                is BiometricResult.AuthenticationSuccess -> {
                    // Biometría exitosa
                    performSecureAction()
                }
                is BiometricResult.AuthenticationFailed -> {
                    // Mostrar error
                }
                is BiometricResult.AuthenticationError -> {
                    // Mostrar error con mensaje
                }
            }
        }
    }

    when (biometricAuthStatus) {
        BiometricAuthStatus.READY -> {
            Button(
                onClick = {
                    val application = activity.application as MomentumApplication
                    application.biometricPromptManager.showBiometricPrompt(
                        activity = activity,
                        title = "Verificar Identidad",
                        subtitle = "Usa tu huella o Face ID",
                        description = "Confirma tu identidad para continuar"
                    )
                }
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Autenticar con Biometría")
            }
        }
        BiometricAuthStatus.AVAILABLE_BUT_NOT_ENROLLED -> {
            Text(
                "Configura tu huella o Face ID en los ajustes del dispositivo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        BiometricAuthStatus.NOT_AVAILABLE -> {
            Text(
                "Biometría no disponible en este dispositivo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        BiometricAuthStatus.TEMPORARY_NOT_AVAILABLE -> {
            Text(
                "Biometría temporalmente no disponible",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
```

---

### 6. Verificar Contraseña con Suspend Function

```kotlin
suspend fun verifyPasswordAndExecute(
    password: String,
    viewModel: PasswordProtectionViewModel,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    val isCorrect = viewModel.verifyPassword(password)
    
    if (isCorrect) {
        onSuccess()
    } else {
        viewModel.updateRemainingLockoutTime()
        val lockoutTime = viewModel.remainingLockoutTime.value
        
        if (lockoutTime > 0) {
            val minutes = lockoutTime / 1000 / 60
            onFailure("Bloqueado por $minutes minutos")
        } else {
            onFailure("Contraseña incorrecta")
        }
    }
}

// Uso
@Composable
fun Example() {
    val viewModel: PasswordProtectionViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()
    
    Button(
        onClick = {
            scope.launch {
                verifyPasswordAndExecute(
                    password = "1234",
                    viewModel = viewModel,
                    onSuccess = {
                        // Acción exitosa
                    },
                    onFailure = { error ->
                        // Mostrar error
                    }
                )
            }
        }
    ) {
        Text("Verificar")
    }
}
```

---

### 7. Proteger Cambios de Configuración Crítica

```kotlin
@Composable
fun CriticalSettingToggle(
    title: String,
    currentValue: Boolean,
    onValueChange: (Boolean) -> Unit,
    viewModel: PasswordProtectionViewModel = hiltViewModel()
) {
    var showPasswordDialog by remember { mutableStateOf(false) }
    var pendingValue by remember { mutableStateOf(currentValue) }
    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title)
        
        Switch(
            checked = currentValue,
            onCheckedChange = { newValue ->
                scope.launch {
                    val isProtected = viewModel.isFeatureProtected(
                        ProtectedFeature.APP_LIMITS
                    )
                    
                    if (isProtected) {
                        pendingValue = newValue
                        showPasswordDialog = true
                    } else {
                        onValueChange(newValue)
                    }
                }
            }
        )
    }

    if (showPasswordDialog) {
        PasswordVerificationDialog(
            onDismiss = { 
                showPasswordDialog = false
                pendingValue = currentValue // Revertir
            },
            onSuccess = {
                onValueChange(pendingValue)
                showPasswordDialog = false
            },
            title = "Verificar Cambio",
            description = "Esta configuración está protegida"
        )
    }
}
```

---

### 8. Detectar si App Está Bloqueada

```kotlin
@Composable
fun AppLockIndicator(
    context: Context
) {
    val application = context.applicationContext as MomentumApplication
    val isLocked by application.appLockManager.isLocked.collectAsState()

    if (isLocked) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "App Bloqueada",
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
```

---

### 9. Cambiar Contraseña

```kotlin
@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: PasswordProtectionViewModel = hiltViewModel()
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar Contraseña") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { 
                        oldPassword = it
                        errorMessage = null
                    },
                    label = { Text("Contraseña Actual") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { 
                        newPassword = it
                        errorMessage = null
                    },
                    label = { Text("Nueva Contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { 
                        confirmPassword = it
                        errorMessage = null
                    },
                    label = { Text("Confirmar Nueva Contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
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
                        newPassword != confirmPassword -> {
                            errorMessage = "Las contraseñas no coinciden"
                        }
                        newPassword.length < 4 -> {
                            errorMessage = "La contraseña debe tener al menos 4 caracteres"
                        }
                        else -> {
                            viewModel.changePassword(oldPassword, newPassword) { success ->
                                if (success) {
                                    onSuccess()
                                } else {
                                    errorMessage = "Contraseña actual incorrecta"
                                }
                            }
                        }
                    }
                }
            ) {
                Text("Cambiar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
```

---

### 10. Desactivar Protección

```kotlin
@Composable
fun DisableProtectionDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: PasswordProtectionViewModel = hiltViewModel()
) {
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { 
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Desactivar Protección") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Esto desactivará la protección por contraseña. " +
                    "Tus configuraciones quedarán desprotegidas.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        errorMessage = null
                    },
                    label = { Text("Contraseña Actual") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
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
                            onSuccess()
                        } else {
                            errorMessage = "Contraseña incorrecta"
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Desactivar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
```

---

## 🎯 Features Protegidas Disponibles

```kotlin
enum class ProtectedFeature {
    APP_LIMITS,          // Límites de aplicaciones
    IN_APP_BLOCKING,     // Bloqueo dentro de apps (Reels, Shorts, etc.)
    WEBSITE_BLOCKING,    // Bloqueo de sitios web
    MINIMAL_MODE         // Modo minimalista/launcher
}
```

---

## 🔧 Configuración en ViewModel

Para usar el sistema de protección en tu ViewModel:

```kotlin
@HiltViewModel
class MyFeatureViewModel @Inject constructor(
    private val passwordProtectionRepository: PasswordProtectionRepository
) : ViewModel() {

    suspend fun performProtectedAction() {
        val isProtected = passwordProtectionRepository.isFeatureProtected(
            ProtectedFeature.APP_LIMITS
        )
        
        if (isProtected) {
            // Emitir evento para mostrar diálogo de verificación
            _showPasswordDialog.emit(true)
        } else {
            // Ejecutar acción
            executeAction()
        }
    }
}
```

---

## ⚡ Tips y Mejores Prácticas

1. **Siempre verifica primero si una característica está protegida**
   ```kotlin
   if (viewModel.isFeatureProtected(feature)) {
       showPasswordDialog = true
   }
   ```

2. **Usa LaunchedEffect para observar resultados de biometría**
   ```kotlin
   LaunchedEffect(Unit) {
       viewModel.biometricResults.collect { result ->
           // Manejar resultado
       }
   }
   ```

3. **Actualiza el tiempo de bloqueo antes de mostrar el diálogo**
   ```kotlin
   LaunchedEffect(Unit) {
       viewModel.updateRemainingLockoutTime()
   }
   ```

4. **Maneja el estado de carga durante la verificación**
   ```kotlin
   var isLoading by remember { mutableStateOf(false) }
   
   scope.launch {
       isLoading = true
       val result = viewModel.verifyPassword(password)
       isLoading = false
   }
   ```

5. **Limpia el campo de contraseña después de intentos fallidos**
   ```kotlin
   if (!isCorrect) {
       password = ""
   }
   ```

---

## 🚀 Integración Rápida

Para integrar rápidamente en una pantalla existente:

```kotlin
@Composable
fun YourExistingScreen(
    viewModel: PasswordProtectionViewModel = hiltViewModel()
) {
    var showPasswordDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Tu UI existente...

    // Agregar verificación
    Button(
        onClick = {
            scope.launch {
                if (viewModel.isFeatureProtected(ProtectedFeature.APP_LIMITS)) {
                    showPasswordDialog = true
                } else {
                    performAction()
                }
            }
        }
    ) {
        Text("Acción Protegida")
    }

    // Agregar diálogo
    if (showPasswordDialog) {
        PasswordVerificationDialog(
            onDismiss = { showPasswordDialog = false },
            onSuccess = {
                performAction()
                showPasswordDialog = false
            }
        )
    }
}
```

---

## 📝 Notas Importantes

- El hashing SHA-256 se maneja automáticamente en el Repository
- Los intentos fallidos se rastrean automáticamente
- El bloqueo temporal (5 minutos) se activa después de 5 intentos fallidos
- La biometría requiere `FragmentActivity` como contexto
- El sistema es totalmente compatible con Jetpack Compose y Hilt
