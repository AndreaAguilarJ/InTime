# Esquema de Base de Datos en Appwrite — InTime

Este documento resume la configuración de Appwrite usada por la app InTime: base de datos, colecciones, atributos (con tamaños), campos requeridos/opcionales, valores por defecto, índices y permisos sugeridos. Úsalo como guía al crear o verificar la configuración en Appwrite Console.

## Proyecto Appwrite
- Project ID: `momentum-intime`
- Public Endpoint: `https://sfo.cloud.appwrite.io/v1`
- Database ID: `momentum-db`

## Base de datos
- Única base de datos: `momentum-db`

## Colecciones
A continuación se detallan todas las colecciones utilizadas actualmente por la app.

---

### 1) subscriptions
- Propósito: estado de suscripción y trial por usuario.
- Atributos
  | Nombre       | Tipo                     | Tamaño máx | Requerido | Default     | Notas |
  |--------------|--------------------------|------------|-----------|-------------|-------|
  | userId       | string                   | 128        | Sí        | —           | ID del usuario.
  | status       | enum                     | —          | Sí        | —           | Valores permitidos: `FREE`, `TRIAL`, `PREMIUM_MONTHLY`, `PREMIUM_YEARLY`.
  | expiryDate   | string (ISO-8601)        | 30         | No        | —           | Fecha de expiración.
  | trialEndsAt  | string (ISO-8601)        | 30         | No        | —           | Fin del periodo de prueba.
  | isTrialUsed  | boolean                  | —          | Sí        | false       | Indica si ya usó el trial.
- Consultas/usos: `Query.equal("userId", ...)`.
- Índices:
  - key en ["userId"] (necesario para consultas por usuario).
  - unique en ["userId"] (recomendado para 1 documento por usuario).
- Permisos sugeridos (ACL):
  - create/read/update/delete: `user:$id`.

---

### 2) backups
- Propósito: respaldo JSON completo de datos del usuario.
- Atributos
  | Nombre      | Tipo              | Tamaño máx | Requerido | Default | Notas |
  |-------------|-------------------|------------|-----------|---------|-------|
  | userId      | string            | 128        | Sí        | —       | ID del usuario.
  | backupData  | string (JSON)     | 16384      | Sí        | —       | Respaldo serializado. Para respaldos grandes, considerar Storage.
  | timestamp   | string (ISO-8601) | 30         | Sí        | —       | Usado para ordenar por fecha.
  | version     | string            | 20         | Sí        | "1.0"  | Versión del formato de backup.
- Consultas/usos: `equal("userId", ...)`, `orderDesc("timestamp")`, `limit(1)`.
- Índices:
  - key en ["userId"].
  - key en ["timestamp"].
  - Opcional: compuesto ["userId", "timestamp"] para filtrar y ordenar eficientemente.
- Permisos sugeridos (ACL):
  - create/read/update/delete: `user:$id`.
- Nota: si el tamaño de `backupData` pudiera crecer, usar Storage y guardar aquí solo metadatos (`fileId`).

---

### 3) usage_stats
- Propósito: sincronizar métricas de uso de apps por usuario.
- Atributos
  | Nombre           | Tipo              | Tamaño máx | Requerido | Default | Notas |
  |------------------|-------------------|------------|-----------|---------|-------|
  | userId           | string            | 128        | Sí        | —       | ID del usuario.
  | packageName      | string            | 256        | Sí        | —       | Nombre del paquete de la app.
  | appName          | string            | 256        | No        | —       | Nombre legible de la app.
  | totalTimeInMillis| integer (64-bit)  | —          | Sí        | 0       | Tiempo total en milisegundos.
  | lastTimeUsed     | string (ISO-8601) | 30         | No        | —       | Último uso. Si necesitas ordenar por fecha, considerar tipo datetime.
- Consultas/usos: `equal("userId", ...)`.
- Índices:
  - key en ["userId"] (necesario).
  - Opcional: compuesto ["userId", "packageName"] si consultas por ambos.
- Permisos sugeridos (ACL):
  - create/read/update/delete: `user:$id`.

---

### 4) user_settings
- Propósito: preferencias del usuario, estado de onboarding y flags de auto-backup.
- Atributos
  | Nombre               | Tipo              | Tamaño máx | Requerido | Default    | Notas |
  |----------------------|-------------------|------------|-----------|------------|-------|
  | userId               | string            | 128        | Sí        | —          | ID del usuario.
  | birthDate            | string (ISO-8601) | 30         | No        | —          | Fecha de nacimiento.
  | isOnboardingCompleted| boolean           | —          | Sí        | false      | Estado de onboarding.
  | livedWeeksColor      | string (hex)      | 9          | Sí        | "#FF6B6B" | Color en formato #RRGGBB o #AARRGGBB.
  | futureWeeksColor     | string (hex)      | 9          | Sí        | "#E8E8E8" | —
  | enableBiometric      | boolean           | —          | Sí        | false      | —
  | minimalModeEnabled   | boolean           | —          | Sí        | false      | —
  | allowedApps          | array<string>     | 256 c/elem | Sí        | []         | Lista de paquetes permitidos.
  | dailyQuotesEnabled   | boolean           | —          | Sí        | true       | —
  | onboardingStep       | integer           | —          | Sí        | 0          | Paso actual de onboarding.
  | createdAt            | string (ISO-8601) | 30         | Sí        | —          | Lo rellena la app.
  | updatedAt            | string (ISO-8601) | 30         | Sí        | —          | Lo rellena la app.
  | autoBackupEnabled    | boolean           | —          | No        | false      | Usado por auto-backup.
  | autoBackupInterval   | integer           | —          | No        | 24         | Horas.
  | lastBackupCheck      | string (ISO-8601) | 30         | No        | —          | Última verificación.
- Consultas/usos: `equal("userId", ...)`.
- Índices:
  - key en ["userId"] (necesario).
  - unique en ["userId"] (recomendado para 1 documento por usuario).
- Permisos sugeridos (ACL):
  - create/read/update/delete: `user:$id`.
- Nota: Estandarizar el guardado por atributos (evitar almacenar todo como JSON string) para aprovechar índices/consultas.

---

### 5) quotes
- Propósito: frases motivacionales.
- Atributos (normalizado recomendado)
  | Nombre    | Tipo              | Tamaño máx | Requerido | Default      | Notas |
  |-----------|-------------------|------------|-----------|--------------|-------|
  | id        | string            | 64         | No        | —            | Si usas `ID.unique()` no es necesario.
  | text      | string            | 1000       | Sí        | —            | Texto de la cita.
  | author    | string            | 120        | No        | —            | Autor opcional.
  | category  | string            | 60         | Sí        | "motivation"| Categoría.
  | isActive  | boolean           | —          | Sí        | true         | Activa o no.
  | createdAt | string (ISO-8601) | 30         | Sí        | —            | Lo rellena la app.
- Consultas/usos: listados generales; sin filtros obligatorios hoy.
- Índices:
  - Ninguno obligatorio; opcional key en ["isActive"].
- Permisos sugeridos (ACL):
  - read: `role:all` (si son públicas).
  - create/update/delete: restringido a administradores (p. ej. `role:team:admins`).
- Nota: si temporalmente guardas la cita como JSON string, puedes definir `payload: string (4096–16384)` durante la transición; lo ideal es normalizar en atributos.

---

## Verificación rápida en Appwrite Console
1) Asegúrate de estar en el proyecto `momentum-intime` y en la base de datos `momentum-db`.
2) Revisa cada colección:
   - Atributos: nombres, tipos y tamaños coinciden con las tablas de arriba.
   - Defaults aplicados donde corresponde.
   - Índices creados (y únicos donde se recomienda).
   - Permisos (ACL) configurados según privacidad.
3) Prueba una consulta por `userId` en `subscriptions` y `user_settings` para validar índices.

## Recomendaciones
- Formato de fechas: usa ISO-8601 (ej. `2025-09-03T18:30:00Z`). Si más adelante necesitas ordenaciones robustas por fecha, considera migrar los campos de fecha a tipo `datetime` en Appwrite.
- Para documentos grandes (`backupData`), usar Storage y mantener metadatos en la colección.
- Evita `documentId = "unique()"` como texto; usa `ID.unique()` del SDK.

## Cambios futuros
- Si se añaden nuevas consultas por campos, crear índices adicionales según la necesidad (por ejemplo fulltext sobre `quotes.text`).


