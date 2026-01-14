# Guía de Protección por Contraseña

## Descripción General

El sistema de protección por contraseña permite a los usuarios proteger configuraciones críticas de la aplicación con una contraseña numérica larga (recomendado 20-30 dígitos). Esto previene que en momentos de debilidad puedan desactivar fácilmente los bloqueos y límites configurados.

## Características Implementadas

### 1. Entidad de Base de Datos
- **PasswordProtection**: Almacena la contraseña hasheada (SHA-256) y configuraciones de protección
- Campos protegibles:
  - Límites de Apps
  - Bloqueo dentro de Apps (Reels, Shorts, etc.)
  - Bloqueo de Sitios Web
  - Modo Minimalista

### 2. Seguridad
- Contraseñas hasheadas con SHA-256 (nunca se almacena en texto plano)
- Bloqueo temporal después de 5 intentos fallidos (5 minutos)
- Solo contraseñas numéricas
- Mínimo 6 dígitos (recomendado 20-30)

### 3. Pantallas de Usuario
- **PasswordProtectionManageScreen**: Gestión principal, ver estado, cambiar o desactivar
- **PasswordProtectionSetupScreen**: Configuración inicial de contraseña
- **PasswordVerificationDialog**: Diálogo reutilizable para verificar contraseña

## Cómo Usar en tus Pantallas

### Ejemplo 1: Proteger una acción específica (Desactivar límite de app)

```kotlin
import androidx.hilt.navigation.compose.hiltViewModel
import com.momentummm.app.ui.password.PasswordProtectionViewModel
import com.momentummm.app.ui.password.PasswordVerificationDialog
import com.momentummm.app.data.repository.ProtectedFeature

@Composable
fun AppLimitScreen(
    passwordViewModel: PasswordProtectionViewModel = hiltViewModel()
) {
    var showPasswordDialog by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    
    val onDeleteLimit = { limitId: String ->
        // Verificar si esta función está protegida
        kotlinx.coroutines.MainScope().launch {
            val isProtected = passwordViewModel.isFeatureProtected(ProtectedFeature.APP_LIMITS)
            
            if (isProtected) {
                // Guardar la acción pendiente y mostrar diálogo
                pendingAction = {
                    // Aquí va el código para eliminar el límite
                    deleteLimitFromDatabase(limitId)
                }
                showPasswordDialog = true
            } else {
                // No está protegido, ejecutar directamente
                deleteLimitFromDatabase(limitId)
            }
        }
    }
    
    // Tu UI aquí...
    Button(onClick = { onDeleteLimit("app123") }) {
        Text("Eliminar Límite")
    }
    
    // Diálogo de verificación
    if (showPasswordDialog) {
        PasswordVerificationDialog(
            onDismiss = { 
                showPasswordDialog = false
                pendingAction = null
            },
            onVerified = { 
                pendingAction?.invoke()
                showPasswordDialog = false
                pendingAction = null
            },
            viewModel = passwordViewModel,
            title = "Verificación Requerida",
            message = "Ingresa tu contraseña para eliminar este límite"
        )
    }
}
```

### Ejemplo 2: Usar el componente ProtectedAction (más simple)

```kotlin
import com.momentummm.app.ui.password.ProtectedAction

@Composable
fun WebsiteBlockScreen(
    passwordViewModel: PasswordProtectionViewModel = hiltViewModel()
) {
    ProtectedAction(
        viewModel = passwordViewModel,
        featureProtected = { 
            passwordViewModel.isFeatureProtected(ProtectedFeature.WEBSITE_BLOCKING)
        },
        onActionAllowed = {
            // Acción a ejecutar después de verificación exitosa
            disableWebsiteBlock()
        }
    ) { triggerAction ->
        // Tu UI - triggerAction iniciará la verificación
        Button(onClick = triggerAction) {
            Text("Desactivar Bloqueo")
        }
    }
}
```

### Ejemplo 3: Proteger toda una pantalla

```kotlin
@Composable
fun MinimalModeSettingsScreen(
    passwordViewModel: PasswordProtectionViewModel = hiltViewModel()
) {
    var hasAccess by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        val isProtected = passwordViewModel.isFeatureProtected(ProtectedFeature.MINIMAL_MODE)
        if (!isProtected) {
            hasAccess = true
            showPasswordDialog = false
        }
    }
    
    if (!hasAccess) {
        if (showPasswordDialog) {
            PasswordVerificationDialog(
                onDismiss = { 
                    // Volver atrás si cancela
                    onNavigateBack()
                },
                onVerified = { 
                    hasAccess = true
                    showPasswordDialog = false
                },
                viewModel = passwordViewModel,
                title = "Configuración Protegida",
                message = "Esta configuración está protegida por contraseña"
            )
        }
    } else {
        // Mostrar el contenido de la pantalla
        YourActualScreenContent()
    }
}
```

## Tipos de Funciones Protegibles

```kotlin
enum class ProtectedFeature {
    APP_LIMITS,           // Límites de tiempo en apps
    IN_APP_BLOCKING,      // Bloqueo de Reels, Shorts, etc.
    WEBSITE_BLOCKING,     // Bloqueo de sitios web
    MINIMAL_MODE          // Modo teléfono minimalista
}
```

## Flujo de Usuario Recomendado

1. **Configuración Inicial**:
   - Usuario va a Settings → Protección por Contraseña
   - Crea una contraseña numérica larga (20-30 dígitos)
   - Se recomienda escribirla en papel y guardarla en lugar seguro
   - Selecciona qué funciones proteger

2. **Uso Diario**:
   - Usuario intenta desactivar un límite/bloqueo
   - Si está protegido, se muestra diálogo de verificación
   - Debe ingresar la contraseña correcta
   - Si falla 5 veces, bloqueo temporal de 5 minutos

3. **Gestión**:
   - Cambiar contraseña: requiere contraseña actual
   - Desactivar protección: requiere contraseña actual
   - Ver funciones protegidas: no requiere contraseña

## Casos de Uso Recomendados

### Caso 1: Protección Familiar
Un esposo crea una contraseña de 30 dígitos y la escribe en un papel que guardan en una caja de seguridad. Ambos pueden usarla cuando realmente necesitan acceder a funciones bloqueadas, pero la fricción de tener que ir por el papel previene usos impulsivos.

### Caso 2: Autocontrol
Un usuario que lucha con adicción a redes sociales configura bloqueos y los protege con contraseña. Le pide a un amigo o familiar que guarde la contraseña, creando una capa adicional de accountability.

### Caso 3: Modo "Dumb Phone"
Usuario convierte su teléfono en un "dumb phone" bloqueando apps distractoras y protegiendo con contraseña, pero mantiene acceso a herramientas necesarias como llamadas, mapas, etc.

## Mejoras Futuras Sugeridas

1. **Pregunta de Seguridad**: En caso de olvido de contraseña
2. **Recuperación por Email**: Enviar código de recuperación
3. **Tiempo de Espera Progresivo**: Aumentar bloqueo con más intentos fallidos
4. **Modo Strict**: No permitir desactivar protección durante horarios específicos
5. **Compartir Contraseña**: Permitir que un "accountability partner" gestione la contraseña
6. **Biometría como Segundo Factor**: Huella/Face ID además de contraseña
7. **Historial de Intentos**: Registro de cuándo se ingresó la contraseña

## Notas de Implementación

- La contraseña se hashea usando SHA-256
- El hash se almacena en Room Database local
- Los intentos fallidos se rastrean para prevenir ataques de fuerza bruta
- El bloqueo temporal usa timestamps del sistema
- La verificación es asíncrona para no bloquear la UI

