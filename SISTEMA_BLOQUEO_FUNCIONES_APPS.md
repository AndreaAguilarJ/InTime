# Sistema de Bloqueo Dentro de Apps (In-App Blocking)

## Descripción General

Este sistema permite bloquear funciones específicas dentro de aplicaciones de redes sociales sin bloquear completamente la app. Por ejemplo, puedes bloquear Instagram Reels o YouTube Shorts mientras sigues teniendo acceso al resto de la aplicación.

## Características Implementadas

### Funciones Bloqueables por Defecto

1. **Instagram**
   - ✅ Reels (videos cortos verticales)
   - ✅ Explorar (descubre contenido nuevo)

2. **YouTube**
   - ✅ Shorts (videos cortos)
   - ✅ Búsqueda (search dentro de la app)

3. **Facebook**
   - ✅ Reels

4. **Snapchat**
   - ✅ Discover

5. **TikTok**
   - ✅ For You Page

6. **X (Twitter)**
   - ✅ Explorar

## Arquitectura del Sistema

### 1. Base de Datos

**Entidad: InAppBlockRule**
- `id`: ID único de la regla
- `ruleId`: Identificador de la regla (ej: "instagram_reels")
- `packageName`: Paquete de la app (ej: "com.instagram.android")
- `appName`: Nombre de la app
- `blockType`: Tipo de bloqueo (REELS, SHORTS, EXPLORE, etc.)
- `featureName`: Nombre descriptivo de la función
- `isEnabled`: Si la regla está habilitada
- `detectionPatterns`: Patrones para detectar la función (JSON)

**DAO: InAppBlockRuleDao**
- Métodos para CRUD de reglas
- Consultas para obtener reglas habilitadas por paquete

**Repository: InAppBlockRepository**
- Maneja la lógica de negocio de las reglas
- Inicializa reglas predeterminadas
- Proporciona métodos para activar/desactivar reglas

### 2. Servicio de Accesibilidad

**MomentumAccessibilityService**
- Detecta cuando el usuario navega a funciones bloqueadas
- Utiliza el árbol de accesibilidad para identificar elementos
- Métodos de detección específicos para cada plataforma:
  - `detectInstagramReels()`: Busca elementos relacionados con Reels
  - `detectYouTubeShorts()`: Detecta cuando se abre Shorts
  - `detectInstagramExplore()`: Identifica la sección Explorar
  - etc.

**Métodos de Detección:**
- Búsqueda por texto visible
- Búsqueda por ID de vista
- Búsqueda por clase de componente
- Búsqueda por descripción de contenido

### 3. Interfaz de Usuario

**InAppBlockScreen**
- Pantalla principal de configuración
- Muestra reglas agrupadas por aplicación
- Permite activar/desactivar reglas con switches
- Iconos y colores personalizados por app

**InAppBlockedActivity**
- Pantalla que se muestra cuando se bloquea una función
- Countdown de 3 segundos antes de permitir cerrar
- Diseño atractivo con mensaje motivacional
- No se puede cerrar con botón atrás (previene evitar el bloqueo)

**InAppBlockViewModel**
- Maneja el estado de las reglas
- Inicializa reglas predeterminadas al primer uso
- Proporciona métodos para toggle y eliminar reglas

### 4. Integración

**DatabaseModule (Hilt)**
- Proveedor del DAO de InAppBlockRule
- Inyección de dependencias para el repositorio

**AndroidManifest**
- Declaración de InAppBlockedActivity
- Configuración del servicio de accesibilidad

**Navegación**
- Ruta "in_app_blocking" agregada al NavHost
- Botón en Settings para acceder a la configuración
- Icono VideoLibrary para identificar la función

## Cómo Funciona

### Flujo de Detección y Bloqueo

1. **Usuario navega en una app**: El usuario abre Instagram y va a Reels
2. **Servicio detecta**: MomentumAccessibilityService recibe eventos de accesibilidad
3. **Verifica reglas**: Obtiene reglas habilitadas para Instagram
4. **Analiza contenido**: Busca en el árbol de accesibilidad elementos de Reels
5. **Coincidencia encontrada**: Si detecta Reels, verifica el cooldown
6. **Muestra bloqueo**: Lanza InAppBlockedActivity con mensaje de bloqueo
7. **Usuario espera**: Countdown de 3 segundos antes de poder cerrar
8. **Retorna a home**: Usuario vuelve a la app pero es consciente del tiempo

### Cooldown System

- **2 segundos** entre bloqueos para evitar spam
- **Última app bloqueada** se guarda en memoria
- **Tiempo del último bloqueo** se registra

## Configuración para el Usuario

### Activar Servicio de Accesibilidad

1. Ir a **Configuración > Accesibilidad**
2. Buscar **InTime** o **Momentum**
3. Activar el servicio
4. Conceder permisos

### Configurar Reglas de Bloqueo

1. Abrir InTime
2. Ir a **Configuración (Settings)**
3. Seleccionar **"Bloqueo dentro de Apps"**
4. Activar/desactivar las funciones deseadas:
   - Instagram Reels ✓
   - YouTube Shorts ✓
   - Instagram Explorar ✓
   - etc.

## Ventajas del Sistema

✅ **No bloquea toda la app**: Puedes usar mensajes de Instagram sin ver Reels
✅ **Granularidad**: Control fino sobre qué funciones bloquear
✅ **Flexible**: Activa/desactiva reglas en cualquier momento
✅ **Consciente**: Mensaje de reflexión cuando intentas acceder
✅ **Extensible**: Fácil agregar nuevas apps y funciones

## Limitaciones Conocidas

⚠️ **Detección imperfecta**: Los patrones pueden cambiar con actualizaciones
⚠️ **Requiere accesibilidad**: El usuario debe activar el servicio
⚠️ **Dependiente de UI**: Si la app cambia su interfaz, puede no detectar
⚠️ **Idioma**: Algunos patrones dependen del texto en español/inglés

## Próximas Mejoras

📋 **Roadmap:**
- [ ] Soporte para más apps (Reddit, Pinterest, LinkedIn)
- [ ] Detección basada en OCR para mayor precisión
- [ ] Estadísticas de intentos de acceso bloqueados
- [ ] Configuración de horarios de bloqueo
- [ ] Whitelist temporal (permitir por X minutos)
- [ ] Detección por machine learning

## Archivos Modificados/Creados

### Nuevos Archivos
```
app/src/main/java/com/momentum/app/
├── data/
│   ├── entity/InAppBlockRule.kt
│   ├── dao/InAppBlockRuleDao.kt
│   └── repository/InAppBlockRepository.kt
├── ui/
│   ├── InAppBlockedActivity.kt
│   └── inappblock/
│       ├── InAppBlockScreen.kt
│       └── InAppBlockViewModel.kt
└── accessibility/MomentumAccessibilityService.kt (actualizado)
```

### Archivos Modificados
```
- AppDatabase.kt (versión 7, nueva tabla)
- DatabaseModule.kt (nuevo DAO provider)
- AndroidManifest.xml (nueva activity)
- SettingsScreen.kt (nueva opción de menú)
- MomentumApp.kt (nueva ruta de navegación)
```

## Testing

### Probar Manualmente

1. **Activar el servicio de accesibilidad**
2. **Configurar reglas**: Activar "Instagram Reels"
3. **Abrir Instagram**
4. **Navegar a Reels**
5. **Verificar bloqueo**: Debe aparecer la pantalla de bloqueo
6. **Esperar countdown**: 3 segundos
7. **Cerrar**: Botón "Entendido" debe funcionar

### Casos de Prueba

- ✓ Activar/desactivar reglas desde UI
- ✓ Bloqueo aparece cuando se accede a función bloqueada
- ✓ Cooldown previene spam de bloqueos
- ✓ Botón de atrás no cierra pantalla de bloqueo
- ✓ Reglas persisten después de reiniciar app
- ✓ Múltiples reglas para la misma app funcionan
- ✓ Desactivar regla permite acceso inmediato

## Soporte y Documentación

Para más información sobre el sistema de accesibilidad de Android:
- [Accessibility Service Guide](https://developer.android.com/guide/topics/ui/accessibility/service)
- [AccessibilityNodeInfo API](https://developer.android.com/reference/android/view/accessibility/AccessibilityNodeInfo)

---

**Autor**: Sistema de Bloqueo InTime  
**Versión**: 1.0.0  
**Fecha**: Octubre 2025

