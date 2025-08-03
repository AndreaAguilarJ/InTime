# Momentum: Tu Vida en Semanas

Una aplicación Android moderna que combina el bienestar digital con la visualización inspiracional de la vida, basada en la famosa charla TED de Tim Urban sobre la procrastinación.

## 🚀 Características

### 📱 Monitor de Bienestar Digital
- **Panel Principal**: Muestra el tiempo total de pantalla del día
- **Estadísticas de Apps**: Lista de aplicaciones más usadas con tiempo de uso
- **Límites de Uso**: Configuración de límites diarios para aplicaciones específicas
- **Historial**: Gráficos de uso semanal y mensual

### 📅 Mi Vida en Semanas
- **Visualización Única**: Cuadrícula de 4,160 recuadros representando 80 años de vida
- **Personalización**: Colores customizables para semanas vividas y futuras
- **Generador de Fondos**: Crea imágenes personalizadas de tu vida en semanas
- **Widget de Pantalla**: Widget actualizable automáticamente

### 💡 Sistema de Motivación
- **Biblioteca de Frases**: Más de 50 citas inspiradoras sobre tiempo y productividad
- **Frase del Día**: Motivación diaria en el panel principal
- **Widget de Frases**: Widget redimensionable con citas rotativas

## 🛠 Tecnologías

### Arquitectura
- **MVVM Pattern**: Separación clara entre UI y lógica de negocio
- **Jetpack Compose**: UI moderna y declarativa
- **Material Design 3**: Diseño adaptable con Material You

### Persistencia y Datos
- **Room Database**: Almacenamiento local eficiente
- **Coroutines**: Operaciones asíncronas fluidas
- **StateFlow/LiveData**: Gestión reactiva del estado

### Funcionalidades Avanzadas
- **WorkManager**: Tareas en segundo plano para widgets
- **Usage Stats API**: Monitoreo del tiempo de pantalla
- **Canvas Drawing**: Visualización personalizada de la cuadrícula de vida

## 📋 Requisitos

- Android API 26+ (Android 8.0)
- Permiso de acceso a datos de uso (Usage Stats)
- Kotlin 1.9+
- Gradle 8.4+

## 🎨 Diseño

La aplicación implementa Material Design 3 con:
- **Colores Dinámicos**: Adaptación automática al fondo de pantalla del usuario
- **Navegación Intuitiva**: Bottom Navigation con 3 pestañas principales
- **Tipografía Clara**: Jerarquía visual bien definida
- **Espacios Generosos**: Diseño minimalista y limpio

## 📱 Navegación

### 🏠 Hoy
- Tiempo de pantalla actual
- Aplicaciones más usadas
- Frase motivacional del día

### 👤 Mi Vida
- Visualización de semanas vividas
- Herramientas de personalización
- Generación de fondos de pantalla

### ⚙️ Ajustes
- Límites de aplicaciones
- Configuración de notificaciones
- Personalización de widgets

## 🔧 Configuración del Proyecto

### Dependencias Principales

```kotlin
// Jetpack Compose
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")

// Arquitectura
implementation("androidx.lifecycle:lifecycle-viewmodel-compose")
implementation("androidx.navigation:navigation-compose")

// Persistencia
implementation("androidx.room:room-runtime")
implementation("androidx.room:room-ktx")

// Trabajo en Segundo Plano
implementation("androidx.work:work-runtime-ktx")

// Widgets
implementation("androidx.glance:glance-appwidget")
```

### Estructura del Proyecto

```
app/
├── src/main/java/com/momentum/app/
│   ├── data/                  # Capa de datos
│   │   ├── entity/           # Entidades Room
│   │   ├── dao/              # Data Access Objects
│   │   └── repository/       # Repositorios
│   ├── ui/                   # Capa de UI
│   │   ├── screen/          # Pantallas Compose
│   │   ├── viewmodel/       # ViewModels
│   │   ├── component/       # Componentes reutilizables
│   │   └── theme/           # Tema y colores
│   ├── util/                # Utilidades
│   ├── widget/              # Widgets de pantalla de inicio
│   └── worker/              # Workers de WorkManager
```

## 🌟 Inspiración

Esta aplicación está inspirada en la charla TED "Inside the mind of a master procrastinator" de Tim Urban, que popularizó el concepto de visualizar la vida como una cuadrícula de semanas para generar conciencia sobre el tiempo y motivar la acción.

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Consulta el archivo `LICENSE` para más detalles.

---

**Momentum** - Transforma tu relación con el tiempo. Una semana a la vez. ⏰✨