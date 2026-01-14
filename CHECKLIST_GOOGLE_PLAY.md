# 📱 CHECKLIST PARA PUBLICAR EN GOOGLE PLAY

## ✅ REQUISITOS TÉCNICOS OBLIGATORIOS

### 1. **App Bundle (AAB) - OBLIGATORIO desde 2021**
- [ ] Generar archivo `.aab` en lugar de `.apk`
- **Comando:** `./gradlew bundleRelease`
- **Ubicación:** `app/build/outputs/bundle/release/app-release.aab`

### 2. **Firma de la App (App Signing) - OBLIGATORIO**
Tu app necesita estar firmada con una clave de producción.

#### Crear Keystore:
```bash
keytool -genkey -v -keystore momentum-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias momentum-key
```

#### Agregar a `gradle.properties`:
```properties
MOMENTUM_STORE_FILE=momentum-release-key.jks
MOMENTUM_STORE_PASSWORD=tu_password_aqui
MOMENTUM_KEY_ALIAS=momentum-key
MOMENTUM_KEY_PASSWORD=tu_password_aqui
```

### 3. **Configuración de Release Build - PENDIENTE**
**Estado Actual:** `isMinifyEnabled = false` ❌

**Necesitas actualizar `build.gradle.kts`:**
```kotlin
buildTypes {
    release {
        isMinifyEnabled = true  // ✅ ACTIVAR
        isShrinkResources = true  // ✅ AGREGAR
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
        signingConfig = signingConfigs.getByName("release")
    }
}

signingConfigs {
    create("release") {
        storeFile = file(project.findProperty("MOMENTUM_STORE_FILE") as String? ?: "momentum-release-key.jks")
        storePassword = project.findProperty("MOMENTUM_STORE_PASSWORD") as String?
        keyAlias = project.findProperty("MOMENTUM_KEY_ALIAS") as String?
        keyPassword = project.findProperty("MOMENTUM_KEY_PASSWORD") as String?
    }
}
```

### 4. **Reglas ProGuard - INCOMPLETAS**
**Estado Actual:** Archivo casi vacío ❌

**Necesitas agregar reglas específicas para tus dependencias.**

### 5. **Versioning - LISTO PERO REVISAR**
**Estado Actual:**
- `versionCode = 1` ✅
- `versionName = "1.0"` ✅
- `targetSdk = 34` ⚠️ (Google requiere API 34+ desde agosto 2024)

---

## 📄 REQUISITOS DE CONTENIDO OBLIGATORIOS

### 6. **Política de Privacidad - OBLIGATORIO**
- [ ] Crear documento de Política de Privacidad
- [ ] Publicarlo en una URL accesible públicamente
- [ ] Agregar la URL en Google Play Console
- **Razón:** Tu app solicita permisos sensibles (Estadísticas de uso, Contactos, SMS, etc.)

### 7. **Íconos y Recursos Gráficos - VERIFICAR**
- [ ] **Ícono de app (512x512 px)** - Para Google Play
- [ ] **Ícono adaptativo** - ✅ Ya lo tienes
- [ ] **Screenshots** (mínimo 2, máximo 8)
  - Teléfonos: 1080x1920 px o 1440x2560 px
- [ ] **Gráfico destacado** (1024x500 px)
- [ ] **Video promocional** (opcional pero recomendado)

### 8. **Descripción de la App - CREAR**
- [ ] **Título:** (máximo 50 caracteres)
- [ ] **Descripción corta:** (máximo 80 caracteres)
- [ ] **Descripción completa:** (máximo 4000 caracteres)
- [ ] **Categoría:** Productividad / Estilo de vida
- [ ] **Clasificación de contenido:** Completar cuestionario

---

## 🔒 REQUISITOS DE SEGURIDAD Y PERMISOS

### 9. **Declaración de Permisos Sensibles - OBLIGATORIO**
Tu app solicita **permisos sensibles** que requieren justificación:

**Permisos que Google revisará:**
- ✅ `PACKAGE_USAGE_STATS` - Para estadísticas de uso
- ✅ `READ_CONTACTS` - Para modo teléfono minimalista
- ✅ `READ_SMS` / `SEND_SMS` - Para modo teléfono minimalista
- ✅ `CALL_PHONE` - Para modo teléfono minimalista
- ✅ `SYSTEM_ALERT_WINDOW` - Para bloqueo de apps
- ✅ `FOREGROUND_SERVICE` - Para servicio de monitoreo

**Acción requerida:**
- Completar "Declaración de permisos" en Google Play Console
- Proporcionar video demostrando el uso de cada permiso sensible

### 10. **Cuestionario de Seguridad de Datos - OBLIGATORIO**
- [ ] Completar "Sección de seguridad de datos"
- [ ] Declarar qué datos recopilas
- [ ] Explicar cómo usas los datos
- [ ] Indicar si compartes datos con terceros (Appwrite)

---

## 🏗️ REQUISITOS TÉCNICOS ADICIONALES

### 11. **Arquitecturas de CPU - VERIFICAR**
**Estado Actual:** Probablemente solo incluyes `arm64-v8a` y `armeabi-v7a`
- [ ] Verificar que incluyas las arquitecturas necesarias
- [ ] Considerar incluir `x86` y `x86_64` si es necesario

### 12. **App Size Optimization - RECOMENDADO**
- [ ] Activar `splits` por ABI para reducir tamaño de descarga
- [ ] Optimizar imágenes y recursos
- [ ] Eliminar recursos no utilizados

### 13. **Crash Reporting - RECOMENDADO**
- [ ] Integrar Firebase Crashlytics o similar
- [ ] Para detectar errores en producción

### 14. **Testing - OBLIGATORIO**
- [ ] Probar en diferentes dispositivos/emuladores
- [ ] Probar en diferentes versiones de Android (26-34)
- [ ] Probar todos los flujos principales
- [ ] Verificar que no haya crashes

---

## 📱 CONFIGURACIÓN DE GOOGLE PLAY CONSOLE

### 15. **Cuenta de Google Play Console - REQUERIDO**
- [ ] Crear cuenta de desarrollador ($25 USD única vez)
- [ ] Verificar identidad
- [ ] Configurar información de pago

### 16. **Configuración de la App en Console**
- [ ] Crear nueva aplicación
- [ ] Completar información básica
- [ ] Configurar países de distribución
- [ ] Establecer precio (gratuita/pago)

### 17. **Configuración de In-App Purchases - SI APLICA**
**Tu app tiene:** `com.android.vending.BILLING`
- [ ] Configurar productos/suscripciones en Google Play Console
- [ ] Probar compras con cuenta de prueba

### 18. **Testing Tracks - RECOMENDADO**
- [ ] Configurar Internal Testing (probar con hasta 100 usuarios)
- [ ] Luego Closed Testing (Beta)
- [ ] Finalmente Open Testing o Producción

---

## 📋 CONTENIDO LEGAL Y COMPLIANCE

### 19. **Términos y Condiciones - RECOMENDADO**
- [ ] Crear documento de Términos de Servicio
- [ ] Incluir enlace en la app y/o Play Store

### 20. **Clasificación de Contenido - OBLIGATORIO**
- [ ] Completar cuestionario IARC
- [ ] Obtener clasificaciones por edad

### 21. **Cumplimiento COPPA - OBLIGATORIO**
- [ ] Declarar si la app está dirigida a niños
- [ ] Probablemente **NO** en tu caso

### 22. **Declaración de Anuncios - OBLIGATORIO**
- [ ] Indicar si la app contiene anuncios
- [ ] En tu caso: **NO** (no vi implementación de ads)

---

## 🔧 OPTIMIZACIONES RECOMENDADAS

### 23. **App Quality**
- [ ] Pasar todas las verificaciones de Pre-launch Report
- [ ] Resolver todos los warnings de Android Vitals
- [ ] Optimizar tamaño de la app (<100 MB ideal)

### 24. **Accesibilidad**
- [ ] Agregar descripciones de contenido
- [ ] Soportar TalkBack
- [ ] Tamaños de fuente escalables

### 25. **Internacionalización - OPCIONAL**
- [ ] Traducir a otros idiomas
- [ ] Actualmente solo español

---

## ⚡ PRIORIDADES INMEDIATAS

### 🚨 CRÍTICO (Sin esto NO puedes publicar):
1. ✅ **Crear y firmar Keystore**
2. ✅ **Actualizar build.gradle.kts con firma**
3. ✅ **Generar App Bundle (.aab)**
4. ✅ **Crear Política de Privacidad**
5. ✅ **Completar reglas ProGuard**
6. ✅ **Crear cuenta Google Play Console**
7. ✅ **Preparar recursos gráficos (íconos, screenshots)**
8. ✅ **Completar cuestionario de seguridad de datos**
9. ✅ **Justificar permisos sensibles**

### ⚠️ IMPORTANTE (Antes de lanzar):
10. Testing exhaustivo
11. Clasificación de contenido
12. Descripción y textos de marketing
13. Configurar productos In-App (si aplica)

### 💡 RECOMENDADO (Para mejor experiencia):
14. Crash reporting
15. Beta testing
16. Optimización de tamaño
17. Múltiples screenshots de calidad

---

## 📚 RECURSOS ÚTILES

**Documentación Google Play:**
- [Requisitos de publicación](https://support.google.com/googleplay/android-developer/answer/9859152)
- [Política de privacidad](https://support.google.com/googleplay/android-developer/answer/113469)
- [Permisos sensibles](https://support.google.com/googleplay/android-developer/answer/9888170)
- [App Bundle](https://developer.android.com/guide/app-bundle)

**Generador de Política de Privacidad:**
- https://www.privacypolicygenerator.info/
- https://app-privacy-policy-generator.firebaseapp.com/

---

## 🎯 PRÓXIMOS PASOS INMEDIATOS

1. **HOY:** Crear keystore y configurar firma
2. **HOY:** Actualizar ProGuard rules
3. **HOY:** Generar Política de Privacidad
4. **MAÑANA:** Crear recursos gráficos (screenshots, ícono 512x512)
5. **MAÑANA:** Testing exhaustivo
6. **DÍA 3:** Crear cuenta Google Play Console
7. **DÍA 3:** Generar AAB y subir a Internal Testing
8. **DÍA 4-5:** Completar toda la información en Play Console
9. **DÍA 6-7:** Enviar a revisión

**Tiempo estimado:** 5-7 días hasta envío a revisión
**Tiempo de revisión Google:** 1-7 días adicionales

