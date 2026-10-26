# 📋 Configuración de Appwrite para Sincronización

## ⚠️ Problema Actual
El error "Error al sincronizar. Tus datos están guardados localmente" ocurre porque la colección `user_settings` no existe en tu base de datos de Appwrite o no tiene los permisos correctos.

## 🔧 Solución: Crear la Colección en Appwrite

### Paso 1: Acceder a tu Proyecto en Appwrite

1. Ve a tu consola de Appwrite: https://cloud.appwrite.io/
2. Abre el proyecto: **momentum-intime**
3. Ve a la sección **Databases**
4. Selecciona la base de datos: **momentum-db**

### Paso 2: Crear la Colección `user_settings`

1. Haz clic en **"Create Collection"**
2. Configura la colección con estos datos:
   - **Collection ID**: `user_settings`
   - **Collection Name**: `User Settings`
   - **Permissions**: Configure los permisos (ver abajo)

### Paso 3: Configurar Permisos de la Colección

En la sección de **Permissions** de la colección:

**Security Type**: Document Security (permite a cada usuario acceder solo a sus documentos)

**Permissions**:
- ✅ **Read**: Role: Users
- ✅ **Create**: Role: Users  
- ✅ **Update**: Role: Users
- ✅ **Delete**: Role: Users

O si prefieres permisos más específicos por usuario:
- ✅ **Read**: Role: user:[USER_ID] (se configura automáticamente con Document Security)
- ✅ **Create**: Role: Users
- ✅ **Update**: Role: user:[USER_ID]
- ✅ **Delete**: Role: user:[USER_ID]

### Paso 4: Crear los Atributos (Campos)

En la sección **Attributes** de la colección, crea los siguientes campos:

| Atributo | Tipo | Tamaño | Requerido | Default | Array |
|----------|------|--------|-----------|---------|-------|
| `userId` | String | 255 | ✅ Sí | - | ❌ No |
| `timestamp` | Integer | - | ✅ Sí | - | ❌ No |
| `enableBiometric` | Boolean | - | ✅ Sí | false | ❌ No |
| `minimalModeEnabled` | Boolean | - | ✅ Sí | false | ❌ No |
| `birthDate` | Integer | - | ❌ No | 0 | ❌ No |
| `isOnboardingCompleted` | Boolean | - | ❌ No | false | ❌ No |
| `hasSeenTutorial` | Boolean | - | ❌ No | false | ❌ No |
| `livedWeeksColor` | String | 50 | ❌ No | "#6366F1" | ❌ No |
| `futureWeeksColor` | String | 50 | ❌ No | "#E5E7EB" | ❌ No |
| `backgroundColor` | String | 50 | ❌ No | "#FFFFFF" | ❌ No |
| `widgetLivedColor` | String | 50 | ❌ No | "#6366F1" | ❌ No |
| `widgetFutureColor` | String | 50 | ❌ No | "#E5E7EB" | ❌ No |
| `goalsCount` | Integer | - | ❌ No | 0 | ❌ No |
| `appLimitsCount` | Integer | - | ❌ No | 0 | ❌ No |
| `whitelistedAppsCount` | Integer | - | ❌ No | 0 | ❌ No |
| `customQuotesCount` | Integer | - | ❌ No | 0 | ❌ No |

**⚠️ IMPORTANTE**: Los campos `userId`, `timestamp`, `enableBiometric` y `minimalModeEnabled` son **REQUERIDOS**. Asegúrate de marcarlos como requeridos cuando los crees.

**📋 RESUMEN DE CAMPOS REQUERIDOS**:
- ✅ `userId` (String)
- ✅ `timestamp` (Integer)  
- ✅ `enableBiometric` (Boolean)
- ✅ `minimalModeEnabled` (Boolean) ← **¡ESTE ES EL NUEVO!**

### Paso 5: Crear Índices (Recomendado)

Para mejorar el rendimiento, crea un índice:

1. Ve a la sección **Indexes**
2. Crea un nuevo índice:
   - **Key**: `userId_index`
   - **Type**: Key
   - **Attributes**: `userId` (ASC)

### Paso 6: Verificar la Configuración

Una vez creada la colección, verifica:

1. ✅ La colección existe y se llama `user_settings`
2. ✅ Todos los atributos están creados
3. ✅ Los permisos permiten a los usuarios autenticados crear y actualizar documentos
4. ✅ El índice está creado

## 🧪 Probar la Sincronización

1. Abre la app InTime
2. Ve a **Configuración** → **Sincronización**
3. Presiona el botón **"Sincronizar ahora"**
4. Deberías ver el mensaje: **"Sincronización exitosa"**

Si aún hay problemas, revisa los logs en Android Studio (Logcat) buscando:
- `AutoSyncManager: ✅ Sincronización exitosa`
- `AutoSyncManager: ❌ Error al...` (te dirá exactamente qué falló)

## 📝 Alternativa: Importar Configuración JSON

Si prefieres, puedes usar esta configuración JSON para crear la colección más rápido:

```json
{
  "$id": "user_settings",
  "name": "User Settings",
  "enabled": true,
  "documentSecurity": true,
  "attributes": [
    { "key": "userId", "type": "string", "size": 255, "required": true },
    { "key": "timestamp", "type": "integer", "required": true },
    { "key": "enableBiometric", "type": "boolean", "required": true, "default": false },
    { "key": "minimalModeEnabled", "type": "boolean", "required": true, "default": false },
    { "key": "birthDate", "type": "integer", "required": false, "default": 0 },
    { "key": "isOnboardingCompleted", "type": "boolean", "required": false, "default": false },
    { "key": "hasSeenTutorial", "type": "boolean", "required": false, "default": false },
    { "key": "livedWeeksColor", "type": "string", "size": 50, "required": false, "default": "#6366F1" },
    { "key": "futureWeeksColor", "type": "string", "size": 50, "required": false, "default": "#E5E7EB" },
    { "key": "backgroundColor", "type": "string", "size": 50, "required": false, "default": "#FFFFFF" },
    { "key": "widgetLivedColor", "type": "string", "size": 50, "required": false, "default": "#6366F1" },
    { "key": "widgetFutureColor", "type": "string", "size": 50, "required": false, "default": "#E5E7EB" },
    { "key": "goalsCount", "type": "integer", "required": false, "default": 0 },
    { "key": "appLimitsCount", "type": "integer", "required": false, "default": 0 },
    { "key": "whitelistedAppsCount", "type": "integer", "required": false, "default": 0 },
    { "key": "customQuotesCount", "type": "integer", "required": false, "default": 0 }
  ],
  "indexes": [
    {
      "key": "userId_index",
      "type": "key",
      "attributes": ["userId"],
      "orders": ["ASC"]
    }
  ]
}
```

## 🔍 Diagnóstico de Problemas

Si después de crear la colección sigues teniendo problemas, verifica:

1. **El usuario está autenticado**: En la app, asegúrate de haber iniciado sesión
2. **Los permisos están correctos**: Verifica que `Role: Users` tenga permisos de Create, Read, Update
3. **El Database ID es correcto**: Debe ser `momentum-db`
4. **Revisa los logs de la app**: Filtrar por `AutoSyncManager` en Logcat te dirá exactamente qué está fallando

## ✅ Mejoras Implementadas en el Código

He actualizado el código para:
- ✅ Agregar logs detallados que te dirán exactamente dónde falla
- ✅ Simplificar los datos enviados para evitar problemas de tipo
- ✅ Mejorar el manejo de errores
- ✅ Mostrar mensajes más claros al usuario

Después de crear la colección en Appwrite, la sincronización debería funcionar perfectamente.
