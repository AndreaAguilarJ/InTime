# Sistema de Notificaciones Inteligentes - InTime

## 📱 Características Implementadas

### 1. **Recordatorios de Límites de Apps**
- ⚠️ Advertencia al alcanzar el 80% del límite configurado
- 🛑 Alerta cuando se alcanza el 100% del límite
- Verificación automática cada 30 minutos
- Notificaciones de alta prioridad con vibración

### 2. **Resumen Semanal de Uso**
- 📊 Enviado cada domingo a las 8 PM
- Incluye:
  - Tiempo total de uso semanal
  - Top 3 apps más usadas
  - Número de metas completadas
- Notificación expandible con toda la información

### 3. **Motivación Diaria**
- 💡 Frase inspiradora cada mañana a las 9 AM
- Frases extraídas de tu base de datos de quotes
- Prioridad baja para no interrumpir

### 4. **Logros por Hitos Alcanzados**
Notificaciones automáticas cuando logras:
- 🎉 Primera vez completando una meta
- 🔥 Racha de 7 días consecutivos
- 🏆 Racha de 30 días
- 💎 Racha de 100 días
- ⭐ Múltiplos de 10 metas completadas
- ✨ Día equilibrado (menos de 3 horas de uso)

### 5. **Recordatorios Inteligentes de Tiempo de Pantalla**
- ⏰ Recordatorio cuando usas más de 6 horas diarias
- Solo se envían entre 9 AM y 9 PM
- Verificación cada 3 horas
- Mensajes personalizados con el tiempo exacto usado

## 🎯 Configuración Personalizable

Los usuarios pueden activar/desactivar cada tipo de notificación desde la pantalla de configuración:
- `NotificationSettingsScreen.kt`

Preferencias almacenadas en DataStore:
- `app_limits_notifications`
- `daily_motivation`
- `weekly_summary`
- `achievements_notifications`
- `screen_time_reminders`

## 🔧 Arquitectura Técnica

### Componentes Principales

1. **SmartNotificationManager**
   - Ubicación: `data/manager/NotificationManager.kt`
   - Singleton inyectado con Hilt
   - Gestiona todos los tipos de notificaciones
   - Respeta las preferencias del usuario

2. **Workers de Background**
   - `AppLimitsCheckWorker` - Cada 30 minutos
   - `DailyMotivationWorker` - Diario a las 9 AM
   - `WeeklySummaryWorker` - Domingos a las 8 PM
   - `AchievementsCheckWorker` - Cada 2 horas
   - `ScreenTimeReminderWorker` - Cada 3 horas

3. **Canales de Notificación (Android O+)**
   - Límites de Aplicaciones (Alta prioridad)
   - Metas y Logros (Prioridad normal)
   - Motivación Diaria (Baja prioridad)
   - Resumen Semanal (Prioridad normal)
   - Recordatorios Inteligentes (Prioridad normal)

## 📋 Integración

El sistema se inicializa automáticamente en `MomentumApplication.onCreate()`:

```kotlin
// Sistema de notificaciones inteligentes
val smartNotificationManager by lazy {
    SmartNotificationManager(
        this,
        usageStatsRepository,
        goalsRepository,
        appLimitRepository,
        quotesRepository,
        userRepository
    )
}
```

## 🎨 Interfaz de Usuario

### Pantalla de Configuración
`ui/settings/NotificationSettingsScreen.kt`

Características:
- Switch para cada tipo de notificación
- Descripciones claras de cada categoría
- Botón de prueba para enviar notificación inmediata
- Íconos Material Design 3
- Diseño moderno y adaptativo

## 📊 Umbrales y Configuración

```kotlin
WARNING_THRESHOLD_PERCENT = 80      // Advertencia al 80% del límite
EXCESSIVE_USAGE_HOURS = 6           // Uso excesivo después de 6 horas
```

## ⏰ Horarios de Notificaciones

| Tipo | Frecuencia | Horario |
|------|-----------|---------|
| Límites de Apps | Cada 30 min | Todo el día |
| Motivación Diaria | Diaria | 9:00 AM |
| Resumen Semanal | Semanal | Domingo 8:00 PM |
| Logros | Cada 2 horas | Todo el día |
| Recordatorios | Cada 3 horas | 9 AM - 9 PM |

## 🔒 Permisos Requeridos

Ya están incluidos en `AndroidManifest.xml`:
- `POST_NOTIFICATIONS` (Android 13+)
- `VIBRATE`

## 🚀 Uso

### Enviar Notificación Manual
```kotlin
val app = applicationContext as MomentumApplication
app.smartNotificationManager.sendDailyMotivation()
```

### Verificar Límites de Apps
```kotlin
app.smartNotificationManager.checkAppLimitsAndNotify()
```

### Enviar Resumen Semanal
```kotlin
app.smartNotificationManager.sendWeeklySummary()
```

## 📈 Próximas Mejoras Sugeridas

1. ✅ **Notificaciones personalizadas por hora del día**
2. ✅ **Machine Learning para predecir mejor momento para notificar**
3. ✅ **Notificaciones adaptativas según el contexto**
4. ✅ **Estadísticas de interacción con notificaciones**
5. ✅ **Notificaciones con acciones rápidas (snooze, completar meta, etc.)**

## 🐛 Debugging

Los logs del sistema usan el tag: `SmartNotificationManager`

```bash
adb logcat | grep "SmartNotificationManager"
```

## ✨ Ventajas del Sistema

1. **No Intrusivo**: Respeta las preferencias del usuario
2. **Inteligente**: Se adapta al uso real del dispositivo
3. **Motivador**: Celebra logros y mantiene engagement
4. **Personalizable**: Control completo del usuario
5. **Eficiente**: Usa WorkManager para optimizar batería
6. **Profesional**: Siguiendo mejores prácticas de Android

---

**Implementado por**: Sistema de Notificaciones Inteligentes v1.0
**Última actualización**: 2025-10-08

