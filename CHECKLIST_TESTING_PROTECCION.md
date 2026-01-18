# ✅ Checklist de Pruebas - Sistema de Protección por Contraseña

## 🧪 Testing Completo del Módulo de Seguridad

### 📋 Pre-requisitos
- [ ] Dispositivo Android con API 26+
- [ ] Dispositivo con sensor biométrico (opcional, para pruebas completas)
- [ ] App compilada en modo Debug
- [ ] Permisos de accesibilidad otorgados (si aplica)

---

## 1️⃣ Configuración Inicial

### Setup de Contraseña
- [ ] Navegar a Settings → "Seguridad y Privacidad"
- [ ] Verificar que redirige a `password_setup` (primera vez)
- [ ] Ingresar contraseña de 4+ caracteres
- [ ] Verificar que la contraseña se guarda correctamente
- [ ] Verificar que `isEnabled = true` en la base de datos
- [ ] Verificar que la contraseña NO está en texto plano (usar Android Studio Database Inspector)

### Verificación de Hash
- [ ] Abrir Database Inspector
- [ ] Navegar a tabla `password_protection`
- [ ] Verificar que `passwordHash` contiene un hash SHA-256 (64 caracteres hex)
- [ ] Verificar que NO hay contraseña en texto plano en ninguna parte

---

## 2️⃣ Bloqueo Automático

### Ciclo de Vida Básico
- [ ] Configurar contraseña
- [ ] Poner app en segundo plano (Home button)
- [ ] Esperar 2-3 segundos
- [ ] Volver a la app
- [ ] **Resultado esperado:** Pantalla de bloqueo aparece

### Excepción: Primer Inicio
- [ ] Desinstalar y reinstalar app
- [ ] Configurar contraseña
- [ ] Poner app en segundo plano
- [ ] Volver a la app
- [ ] **Resultado esperado:** NO debe bloquear en el primer ciclo

### Sin Protección Activa
- [ ] Desactivar protección
- [ ] Poner app en segundo plano
- [ ] Volver a la app
- [ ] **Resultado esperado:** NO debe bloquear

### Múltiples Ciclos
- [ ] Configurar protección
- [ ] Realizar 5 ciclos background → foreground
- [ ] **Resultado esperado:** Bloquea en CADA ciclo (excepto el primero)

---

## 3️⃣ Autenticación con Contraseña

### Contraseña Correcta
- [ ] Llegar a pantalla de bloqueo
- [ ] Ingresar contraseña correcta
- [ ] **Resultado esperado:** App se desbloquea, vuelve a pantalla anterior

### Contraseña Incorrecta
- [ ] Llegar a pantalla de bloqueo
- [ ] Ingresar contraseña incorrecta
- [ ] **Resultado esperado:** 
  - Mensaje "Contraseña incorrecta"
  - Campo de texto se limpia
  - Contador de intentos aumenta en DB

### Múltiples Intentos Fallidos (1-4)
- [ ] Intentar 4 veces con contraseña incorrecta
- [ ] **Resultado esperado:**
  - Cada intento muestra "Contraseña incorrecta"
  - Campo se limpia después de cada intento
  - App NO se bloquea temporalmente

### Bloqueo Temporal (5+ intentos)
- [ ] Intentar 5 veces con contraseña incorrecta
- [ ] **Resultado esperado:**
  - Mensaje cambia a "Demasiados intentos fallidos. Intenta en 5 minutos"
  - Campo de contraseña se deshabilita
  - Botón "Desbloquear" se deshabilita
  - Timer muestra tiempo restante

### Esperar Bloqueo Temporal
- [ ] Activar bloqueo temporal (5 intentos)
- [ ] Esperar 5 minutos (o modificar en código para prueba rápida)
- [ ] **Resultado esperado:**
  - Campo se habilita nuevamente
  - Contador de intentos se resetea
  - Puede intentar ingresar contraseña

---

## 4️⃣ Autenticación Biométrica

### Verificación de Hardware
- [ ] Dispositivo CON sensor biométrico:
  - [ ] Botón "Usar Biometría" visible
  - [ ] Status: `READY` o `AVAILABLE_BUT_NOT_ENROLLED`
- [ ] Dispositivo SIN sensor biométrico:
  - [ ] Botón "Usar Biometría" NO visible
  - [ ] Status: `NOT_AVAILABLE`

### Autenticación Exitosa
- [ ] Llegar a pantalla de bloqueo
- [ ] Tocar "Usar Biometría"
- [ ] Colocar huella registrada / mirar cámara Face ID
- [ ] **Resultado esperado:**
  - Prompt del sistema aparece
  - Autenticación exitosa
  - App se desbloquea inmediatamente

### Autenticación Fallida
- [ ] Llegar a pantalla de bloqueo
- [ ] Tocar "Usar Biometría"
- [ ] Intentar con huella NO registrada / cara incorrecta
- [ ] **Resultado esperado:**
  - Mensaje "Autenticación biométrica fallida"
  - Vuelve a pantalla de bloqueo
  - Puede intentar nuevamente

### Cancelación de Biometría
- [ ] Llegar a pantalla de bloqueo
- [ ] Tocar "Usar Biometría"
- [ ] Cancelar el prompt del sistema
- [ ] **Resultado esperado:**
  - Vuelve a pantalla de bloqueo
  - Sin errores mostrados
  - Puede intentar con contraseña o biometría nuevamente

---

## 5️⃣ Navegación y Rutas

### Primera Vez (Sin Contraseña)
- [ ] Settings → "Seguridad y Privacidad"
- [ ] **Resultado esperado:** Navega a `password_setup`

### Con Contraseña Configurada
- [ ] Settings → "Seguridad y Privacidad"
- [ ] **Resultado esperado:** Navega a `password_manage`

### Indicador Visual en Settings
- [ ] Sin contraseña:
  - [ ] Texto: "Protege tu configuración con contraseña"
  - [ ] Ícono gris
- [ ] Con contraseña:
  - [ ] Texto: "Protección activa - Gestionar configuración"
  - [ ] Ícono azul (primary color)

---

## 6️⃣ Cambio de Contraseña

### Cambio Exitoso
- [ ] Navegar a "password_manage"
- [ ] Seleccionar "Cambiar Contraseña"
- [ ] Ingresar contraseña actual correcta
- [ ] Ingresar nueva contraseña válida
- [ ] Confirmar nueva contraseña
- [ ] **Resultado esperado:**
  - Mensaje de éxito
  - Hash actualizado en DB
  - Intentos fallidos reseteados

### Contraseña Actual Incorrecta
- [ ] Intentar cambiar con contraseña actual incorrecta
- [ ] **Resultado esperado:**
  - Error: "Contraseña actual incorrecta"
  - No se actualiza la contraseña

### Contraseñas No Coinciden
- [ ] Ingresar contraseña actual correcta
- [ ] Nueva contraseña y confirmación diferentes
- [ ] **Resultado esperado:**
  - Error: "Las contraseñas no coinciden"
  - No se actualiza

---

## 7️⃣ Desactivar Protección

### Desactivación Exitosa
- [ ] Navegar a "password_manage"
- [ ] Seleccionar "Desactivar Protección"
- [ ] Ingresar contraseña correcta
- [ ] **Resultado esperado:**
  - Protección desactivada
  - `isEnabled = false` en DB
  - App ya no se bloquea automáticamente
  - Settings muestra "Sin protección"

### Contraseña Incorrecta
- [ ] Intentar desactivar con contraseña incorrecta
- [ ] **Resultado esperado:**
  - Error: "Contraseña incorrecta"
  - Protección sigue activa

---

## 8️⃣ Protección de Características

### Verificar Feature Protection
- [ ] Configurar protección
- [ ] Habilitar protección para "Límites de Apps"
- [ ] Intentar modificar límites
- [ ] **Resultado esperado:**
  - Diálogo de verificación aparece
  - Sin contraseña correcta, no permite modificar

### Sin Feature Protection
- [ ] Deshabilitar protección para característica
- [ ] Intentar modificar la característica
- [ ] **Resultado esperado:**
  - NO pide contraseña
  - Modificación directa permitida

---

## 9️⃣ Edge Cases y Manejo de Errores

### App Reiniciada (Force Stop)
- [ ] Configurar protección
- [ ] Force stop de la app (Settings del sistema)
- [ ] Abrir app nuevamente
- [ ] **Resultado esperado:**
  - NO debe bloquear en el primer inicio después de force stop
  - Protección sigue activa

### Cambio de Orientación
- [ ] Llegar a pantalla de bloqueo
- [ ] Rotar dispositivo
- [ ] **Resultado esperado:**
  - Pantalla se mantiene
  - Estado se preserva
  - Sin crashes

### Proceso en Background Matado
- [ ] Configurar protección
- [ ] Abrir muchas apps para forzar kill del proceso
- [ ] Volver a la app
- [ ] **Resultado esperado:**
  - App se reinicia
  - Protección sigue activa
  - Bloquea si corresponde

### Base de Datos Vacía/Corrupta
- [ ] Borrar datos de la app (Clear data)
- [ ] Abrir app
- [ ] **Resultado esperado:**
  - Sin crashes
  - Inicializa con protección deshabilitada
  - Permite configurar nueva contraseña

---

## 🔟 Performance y UX

### Tiempo de Respuesta
- [ ] Verificar contraseña toma < 500ms
- [ ] Bloqueo automático es instantáneo al volver a foreground
- [ ] Biometría responde inmediatamente

### Animaciones
- [ ] Pantalla de bloqueo tiene transición suave
- [ ] Campo de texto tiene feedback visual
- [ ] Botones tienen ripple effect

### Mensajes de Error
- [ ] Todos los mensajes de error son claros
- [ ] Sin textos hardcodeados (usar strings.xml)
- [ ] Colores consistentes (error = rojo)

---

## 1️⃣1️⃣ Integración con Resto de la App

### Inyección de Dependencias (Hilt)
- [ ] `AppLockManager` se inyecta correctamente
- [ ] `BiometricPromptManager` se inyecta correctamente
- [ ] `PasswordProtectionViewModel` funciona con @HiltViewModel

### Acceso desde MomentumApplication
- [ ] `application.appLockManager` accesible
- [ ] `application.biometricPromptManager` accesible
- [ ] Sin null pointer exceptions

---

## 1️⃣2️⃣ Regresión Testing

### Funcionalidad Existente No Afectada
- [ ] Dashboard carga correctamente
- [ ] Navegación entre pantallas funciona
- [ ] Settings sigue funcionando
- [ ] Minimal Mode no se ve afectado
- [ ] Limits de Apps funcionan normalmente
- [ ] Website Blocking funciona

---

## 📊 Resultados Esperados

### ✅ Todos los Checks Pasados
- Sistema de protección funcional al 100%
- Sin crashes
- UX fluida
- Seguridad robusta

### ⚠️ Algunos Checks Fallidos
- Revisar logs de Android Studio
- Verificar implementación de la feature que falló
- Repasar documentación en `SISTEMA_PROTECCION_CONTRASENA_COMPLETO.md`

### ❌ Muchos Checks Fallidos
- Verificar que todas las dependencias estén instaladas
- Sincronizar Gradle
- Limpiar y reconstruir proyecto
- Verificar versión de Android y API level

---

## 🐛 Debugging Tips

### Logs Útiles
```kotlin
// En AppLockManager.kt
Log.d("AppLockManager", "App moved to foreground")
Log.d("AppLockManager", "Password protection is enabled, locking app")
```

### Database Inspector
- View → Tool Windows → App Inspection
- Seleccionar app en ejecución
- Pestaña "Database Inspector"
- Verificar tabla `password_protection`

### Logcat Filters
```
tag:AppLockManager
tag:BiometricPromptManager
tag:PasswordProtection
```

---

## 📝 Notas de Testing

### Modificar Tiempo de Bloqueo para Testing
```kotlin
// En PasswordProtectionRepository.kt, línea ~75
val lockoutTime = if (newAttempts >= 5) {
    currentTime + (5 * 60 * 1000) // Cambiar a (30 * 1000) para 30 segundos
} else {
    0L
}
```

### Resetear Intentos Fallidos Manualmente
```kotlin
// Ejecutar en Repository o ViewModel
passwordProtectionDao.resetFailedAttempts()
```

---

## ✅ Firma del QA

- **Tester:** _________________
- **Fecha:** _________________
- **Build:** _________________
- **Resultado:** ☐ Pass  ☐ Fail
- **Notas adicionales:**

---

---

## 🚀 Listo para Producción

Si todos los checks pasan:
- [ ] Código revisado
- [ ] Testing completo exitoso
- [ ] Performance aceptable
- [ ] UX validada
- [ ] Sin crashes reportados

**El sistema de protección por contraseña está listo para producción.** 🎉
