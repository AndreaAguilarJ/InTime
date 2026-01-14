# Sistema de Bloqueo de Sitios Web - Documentación

## Resumen
Se ha implementado un sistema completo de bloqueo de sitios web en la aplicación Momentum InTime. Esta funcionalidad permite a los usuarios bloquear sitios web distractores o no deseados, incluyendo sitios de contenido para adultos.

## Características Implementadas

### 1. Gestión de Sitios Bloqueados
- **Pantalla de configuración**: Nueva pantalla accesible desde Configuración > Bloqueo de sitios web
- **Agregar sitios personalizados**: Los usuarios pueden agregar cualquier URL o dominio que deseen bloquear
- **Categorías predefinidas**:
  - Contenido para adultos (20+ sitios populares)
  - Redes sociales (Facebook, Instagram, Twitter, TikTok, etc.)
  - Entretenimiento (YouTube, Netflix, etc.)
  - Juegos online
  - Noticias
  - Compras

### 2. Base de Datos
- Nueva tabla `website_blocks` con campos:
  - `id`: Identificador único
  - `url`: URL o dominio a bloquear
  - `displayName`: Nombre para mostrar
  - `category`: Categoría del sitio
  - `isEnabled`: Estado de activación
  - `createdAt` / `updatedAt`: Timestamps

### 3. Interfaz de Usuario
- **WebsiteBlockScreen**: Pantalla principal con:
  - Tarjeta de estadísticas (total, activos, adultos bloqueados)
  - Lista agrupada por categorías
  - Toggle individual para cada sitio
  - Opciones de eliminación
  
- **Diálogos**:
  - Agregar sitio personalizado (URL + nombre + categoría)
  - Agregar categoría predefinida completa
  - Confirmación de eliminación

### 4. Servicio de Accesibilidad
- **WebsiteBlockService**: Servicio que detecta URLs en navegadores
- Navegadores soportados:
  - Chrome
  - Firefox
  - Opera
  - Brave
  - Microsoft Edge
  - Samsung Internet
  - DuckDuckGo
  - Y más...

### 5. Bloqueo Activo
- Cuando un usuario intenta acceder a un sitio bloqueado:
  - El navegador se cierra automáticamente
  - Se muestra una notificación
  - Se puede abrir una pantalla explicativa
  
### 6. Actividad de Bloqueo
- **WebsiteBlockedActivity**: Pantalla que se muestra cuando se bloquea un sitio
  - Icono de advertencia
  - URL bloqueada
  - Mensaje informativo
  - Botón para cerrar

## Archivos Creados

### Entidades y DAOs
1. `WebsiteBlock.kt` - Entidad de base de datos
2. `WebsiteBlockDao.kt` - Data Access Object
3. `WebsiteBlockRepository.kt` - Repositorio con lógica de negocio

### UI y ViewModels
4. `WebsiteBlockViewModel.kt` - ViewModel para la pantalla
5. `WebsiteBlockScreen.kt` - Pantalla principal Compose
6. `WebsiteBlockDialogs.kt` - Diálogos de la UI
7. `WebsiteBlockedActivity.kt` - Actividad cuando se bloquea un sitio

### Servicios
8. `WebsiteBlockService.kt` - Servicio de accesibilidad

### Configuración
9. `website_block_accessibility_service.xml` - Configuración del servicio

## Archivos Modificados

1. **AppDatabase.kt** - Agregada nueva tabla `website_blocks` (versión 6)
2. **AppModule.kt** - Proveedor de `WebsiteBlockRepository`
3. **MomentumApp.kt** - Ruta de navegación `website_blocks`
4. **SettingsScreen.kt** - Nueva opción "Bloqueo de sitios web"
5. **AndroidManifest.xml** - Registrado servicio y actividad
6. **strings.xml** - Nuevos textos en español

## Cómo Usar

### Para el Usuario:
1. Ir a **Configuración > Bloqueo de sitios web**
2. Agregar sitios de dos formas:
   - **Categoría predefinida**: Tap en el icono de categoría, seleccionar (ej: Contenido Adulto)
   - **Sitio personalizado**: Tap en +, ingresar URL (ej: facebook.com) y nombre
3. Activar/desactivar sitios con el switch
4. El sistema bloqueará automáticamente el acceso en navegadores

### Habilitar el Servicio de Accesibilidad:
1. Ir a **Configuración del Sistema > Accesibilidad**
2. Buscar **"Momentum - Website Block"**
3. Activar el servicio
4. Aceptar permisos

## Sitios de Contenido Adulto Predefinidos

El sistema incluye 20+ sitios populares de contenido adulto que se pueden bloquear con un solo tap:
- Pornhub, XVideos, XNXX, xHamster
- RedTube, YouPorn, Tube8, SpankBang
- Y más...

## Funcionamiento Técnico

### Detección de URLs
1. El servicio de accesibilidad monitorea eventos de ventana en navegadores
2. Extrae la URL de la barra de direcciones
3. Compara con la lista de sitios bloqueados en la base de datos
4. Si coincide, ejecuta el bloqueo

### Normalización de URLs
- Elimina protocolos (https://, http://)
- Elimina www.
- Elimina rutas y parámetros
- Compara dominios base

### Bloqueo Multi-Método
- Acción `GLOBAL_ACTION_BACK` para cerrar navegador
- Acción `GLOBAL_ACTION_HOME` para ir al inicio
- Notificación informativa
- Pantalla de bloqueo opcional

## Privacidad y Seguridad

- **Datos locales**: Toda la información se almacena localmente en la base de datos Room
- **Sin rastreo**: El servicio solo lee URLs cuando detecta navegadores activos
- **Control total**: El usuario decide qué sitios bloquear y puede desactivar todo en cualquier momento

## Extensibilidad

El sistema está diseñado para ser extensible:
- Fácil agregar más categorías predefinidas
- Soporte para patrones de URL avanzados
- Posibilidad de horarios de bloqueo (futuro)
- Estadísticas de intentos de acceso (futuro)

## Notas Importantes

1. **Permiso de Accesibilidad**: El usuario debe habilitar manualmente el servicio de accesibilidad
2. **Navegadores no root**: El bloqueo funciona sin necesidad de root
3. **Privacidad**: El servicio solo actúa en navegadores, no monitorea otras apps
4. **Rendimiento**: Impacto mínimo en la batería y rendimiento

## Próximas Mejoras Sugeridas

1. Horarios de bloqueo (ej: bloquear solo de noche)
2. Bloqueo temporal con temporizador
3. Estadísticas de intentos bloqueados
4. Exportar/importar lista de sitios
5. Modo estricto (sin posibilidad de desbloqueo fácil)
6. Sincronización con Appwrite para multi-dispositivo

