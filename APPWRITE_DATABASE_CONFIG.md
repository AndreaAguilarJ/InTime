# Configuración de Base de Datos Appwrite para Persistencia Completa

## 📋 Resumen
Este documento describe cómo configurar tu base de datos Appwrite para soportar la sincronización automática de TODAS las configuraciones y datos del usuario.

## 🗄️ Colección: `user_settings`

### Información General
- **Nombre**: user_settings
- **ID de Colección**: `user_settings`
- **Permisos**:
  - **Read**: `user:$userId` (solo el usuario puede leer sus datos)
  - **Create**: `user:$userId` 
  - **Update**: `user:$userId`
  - **Delete**: `user:$userId`

### Atributos de la Colección

#### 1. Identificación y Control
| Atributo | Tipo | Tamaño | Requerido | Default | Descripción |
|----------|------|---------|-----------|---------|-------------|
| `userId` | String | 255 | Sí | - | ID del usuario de Appwrite |
| `timestamp` | Integer | - | Sí | - | Timestamp de última actualización |

#### 2. Configuración del Usuario
| Atributo | Tipo | Tamaño | Requerido | Default | Descripción |
|----------|------|---------|-----------|---------|-------------|
| `birthDate` | Integer | - | No | 0 | Fecha de nacimiento en milisegundos |
| `isOnboardingCompleted` | Boolean | - | No | false | Si completó el onboarding |
| `hasSeenTutorial` | Boolean | - | No | false | Si vio el tutorial |

#### 3. Personalización de Colores
| Atributo | Tipo | Tamaño | Requerido | Default | Descripción |
|----------|------|---------|-----------|---------|-------------|
    | `livedWeeksColor` | String | 10 | No | "#6366F1" | Color de semanas vividas |
| `futureWeeksColor` | String | 10 | No | "#E5E7EB" | Color de semanas futuras |
| `backgroundColor` | String | 10 | No | "#FFFFFF" | Color de fondo |
| `widgetLivedColor` | String | 10 | No | "#6366F1" | Color del widget (semanas vividas) |
| `widgetFutureColor` | String | 10 | No | "#E5E7EB" | Color del widget (semanas futuras) |

#### 4. Metas del Usuario
| Atributo | Tipo | Tamaño | Requerido | Default | Descripción |
|----------|------|---------|-----------|---------|-------------|
| `goalsCount` | Integer | - | No | 0 | Cantidad de metas guardadas |
    | `goalsData` | String (JSON) | 65535 | No | "[]" | Array JSON con todas las metas |

**Estructura de goalsData (JSON):**
```json
[
  {
    "id": "goal_123",
    "title": "Reducir uso del celular",
    "description": "Usar menos de 2 horas diarias",
    "targetValue": 120,
    "currentValue": 85,
    "category": "Bienestar",
    "period": "DAILY",
    "endDate": 1234567890000,
    "isActive": true,
    "createdDate": 1234567890000,
    "completionCount": 5,
    "bestStreak": 10,
    "currentStreak": 3
  }
]
```

#### 5. Límites de Aplicaciones
| Atributo | Tipo | Tamaño | Requerido | Default | Descripción |
|----------|------|---------|-----------|---------|-------------|
| `appLimitsCount` | Integer | - | No | 0 | Cantidad de límites configurados |
| `appLimitsData` | String (JSON) | 65535 | No | "[]" | Array JSON con límites de apps |

**Estructura de appLimitsData (JSON):**
```json
[
  {
    "packageName": "com.instagram.android",
    "appName": "Instagram",
    "dailyLimitMinutes": 60,
    "isEnabled": true
  }
]
```

#### 6. Apps en Lista Blanca
| Atributo | Tipo | Tamaño | Requerido | Default | Descripción |
|----------|------|---------|-----------|---------|-------------|
| `whitelistedAppsCount` | Integer | - | No | 0 | Cantidad de apps en whitelist |
| `whitelistedAppsData` | String (JSON) | 65535 | No | "[]" | Array JSON con apps permitidas |

**Estructura de whitelistedAppsData (JSON):**
```json
[
  {
    "packageName": "com.android.phone",
    "appName": "Teléfono",
    "reason": "Emergencias"
  }
]
```

#### 7. Frases Personalizadas
| Atributo | Tipo | Tamaño | Requerido | Default | Descripción |
|----------|------|---------|-----------|---------|-------------|
| `customQuotesCount` | Integer | - | No | 0 | Cantidad de frases personalizadas |
| `customQuotesData` | String (JSON) | 65535 | No | "[]" | Array JSON con frases |

**Estructura de customQuotesData (JSON):**
```json
[
  {
    "text": "El tiempo es el recurso más valioso",
    "author": "Steve Jobs",
    "category": "Motivación"
  }
]
```

## 📊 Índices Requeridos

Crea los siguientes índices para optimizar las consultas:

1. **idx_userId**
   - Tipo: Key
   - Atributo: `userId`
   - Orden: ASC

2. **idx_timestamp**
   - Tipo: Key
   - Atributo: `timestamp`
   - Orden: DESC

## 🔒 Configuración de Permisos

### Permisos de Colección
```
Read: user:$userId
Create: user:$userId
Update: user:$userId
Delete: user:$userId
```

Esto asegura que cada usuario solo puede acceder a sus propios datos.

## 🚀 Pasos para Configurar en Appwrite Console

### 1. Crear la Colección
1. Ve a tu proyecto en Appwrite Console
2. Navega a **Databases** → Tu Base de Datos
3. Click en **"Create Collection"**
4. Nombre: `user_settings`
5. ID: `user_settings` (o deja que se genere automáticamente)

### 2. Agregar los Atributos
Para cada atributo en las tablas de arriba:

1. Click en **"Create Attribute"**
2. Selecciona el tipo (String, Integer, Boolean)
3. Ingresa el nombre exacto del atributo
4. Configura el tamaño (para String)
5. Marca si es requerido
6. Agrega el valor por defecto si aplica
7. Click en **"Create"**

### 3. Configurar los Índices
1. Ve a la pestaña **"Indexes"**
2. Click en **"Create Index"**
3. Agrega cada índice según la tabla de arriba

### 4. Configurar Permisos
1. Ve a la pestaña **"Settings"** de la colección
2. En **"Permissions"**, configura:
   - Read: `user:$userId`
   - Create: `user:$userId`
   - Update: `user:$userId`
   - Delete: `user:$userId`

## 📝 Script SQL para Referencia

Si prefieres usar un enfoque programático, aquí está la estructura equivalente:

```javascript
// Crear colección
const collection = await databases.createCollection(
  databaseId,
  'user_settings',
  'user_settings',
  [
    Permission.read(Role.user("USER_ID")),
    Permission.create(Role.user("USER_ID")),
    Permission.update(Role.user("USER_ID")),
    Permission.delete(Role.user("USER_ID"))
  ]
);

// Agregar atributos String
await databases.createStringAttribute(databaseId, 'user_settings', 'userId', 255, true);
await databases.createStringAttribute(databaseId, 'user_settings', 'livedWeeksColor', 10, false, '#6366F1');
await databases.createStringAttribute(databaseId, 'user_settings', 'futureWeeksColor', 10, false, '#E5E7EB');
await databases.createStringAttribute(databaseId, 'user_settings', 'backgroundColor', 10, false, '#FFFFFF');
await databases.createStringAttribute(databaseId, 'user_settings', 'widgetLivedColor', 10, false, '#6366F1');
await databases.createStringAttribute(databaseId, 'user_settings', 'widgetFutureColor', 10, false, '#E5E7EB');

// JSON strings
await databases.createStringAttribute(databaseId, 'user_settings', 'goalsData', 65535, false, '[]');
await databases.createStringAttribute(databaseId, 'user_settings', 'appLimitsData', 65535, false, '[]');
await databases.createStringAttribute(databaseId, 'user_settings', 'whitelistedAppsData', 65535, false, '[]');
await databases.createStringAttribute(databaseId, 'user_settings', 'customQuotesData', 65535, false, '[]');

// Atributos Integer
await databases.createIntegerAttribute(databaseId, 'user_settings', 'timestamp', true);
await databases.createIntegerAttribute(databaseId, 'user_settings', 'birthDate', false, 0);
await databases.createIntegerAttribute(databaseId, 'user_settings', 'goalsCount', false, 0);
await databases.createIntegerAttribute(databaseId, 'user_settings', 'appLimitsCount', false, 0);
await databases.createIntegerAttribute(databaseId, 'user_settings', 'whitelistedAppsCount', false, 0);
await databases.createIntegerAttribute(databaseId, 'user_settings', 'customQuotesCount', false, 0);

// Atributos Boolean
await databases.createBooleanAttribute(databaseId, 'user_settings', 'isOnboardingCompleted', false, false);
await databases.createBooleanAttribute(databaseId, 'user_settings', 'hasSeenTutorial', false, false);

// Crear índices
await databases.createIndex(databaseId, 'user_settings', 'idx_userId', 'key', ['userId'], ['ASC']);
await databases.createIndex(databaseId, 'user_settings', 'idx_timestamp', 'key', ['timestamp'], ['DESC']);
```

## ✅ Verificación

Después de configurar todo, verifica:

1. ✅ La colección `user_settings` existe
2. ✅ Todos los atributos están creados con los tipos correctos
3. ✅ Los índices están configurados
4. ✅ Los permisos están establecidos correctamente
5. ✅ Puedes crear un documento de prueba manualmente

## 🔄 Sincronización Automática

Una vez configurada la base de datos, el `AutoSyncManager` se encargará automáticamente de:

- ✅ Guardar todos los cambios cuando cierras la app
- ✅ Sincronizar al iniciar la app
- ✅ Mantener una copia local en Room Database
- ✅ Sincronizar con Appwrite cuando hay conexión

## 🎯 Beneficios

- **Persistencia Total**: Todos tus datos se guardan automáticamente
- **Multi-dispositivo**: Accede a tu configuración desde cualquier dispositivo
- **Respaldo Automático**: Nunca pierdas tu configuración
- **Offline First**: La app funciona sin internet, sincroniza cuando hay conexión
- **Seguridad**: Cada usuario solo accede a sus propios datos

## 📱 Listo para Producción

Con esta configuración, tu app está lista para producción con:
- ✅ Persistencia completa de datos
- ✅ Sincronización en la nube
- ✅ Respaldo automático
- ✅ Funcionalidad offline
- ✅ Seguridad implementada
