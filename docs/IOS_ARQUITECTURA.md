# Momentum en iOS — decisión de arquitectura

Rama: `feat/ios-multiplatform`. Base: `feat/perf-safearea-cleanup` (árbol limpio).

## Punto de partida medido

| Medida | Valor |
|---|---|
| Archivos Kotlin | 225 |
| Líneas Kotlin | 71.199 |
| Archivos que importan `android.*` | 115 |
| Archivos sin `android.*` | 110 |
| Gradle / Kotlin / AGP | 8.12 / 2.0.21 / 8.7.3 |
| Xcode local | **ausente** (solo Command Line Tools + swiftc 6.2.3) |

## Camino elegido: Kotlin Multiplatform + Compose Multiplatform

Se reestructura el proyecto en tres módulos y el módulo `:app` de Android sigue
compilando durante todo el proceso.

```
:shared     commonMain  → modelos, entidades, lógica de negocio, motores,
                          repositorios, ViewModels y pantallas Compose
            androidMain → implementaciones Android actuales
            iosMain     → implementaciones iOS (Screen Time, notificaciones)
:app        aplicación Android existente (consume :shared)
iosApp/     proyecto Xcode que aloja la UI Compose
```

### Por qué no las alternativas

- **Reescritura en SwiftUI**: 71.199 líneas a mano. No es entregable y duplica
  cada función futura en dos idiomas.
- **Flutter / React Native**: tira a la basura el código que ya funciona y la
  versión Android que está en revisión para Google Play.

KMP conserva la capa de datos, el motor de gamificación, la analítica y el
sistema de diseño Momentum, y permite que una función nueva salga en las dos
tiendas escribiéndola una vez.

## Restricción real de iOS que hay que decir en voz alta

iOS **no** tiene equivalente libre de `UsageStatsManager` ni de
`AccessibilityService`. Lo que existe es la familia Screen Time:

| Función Android | Equivalente iOS | Requisito |
|---|---|---|
| `UsageStatsManager` (tiempo de pantalla) | `DeviceActivity` | Permiso especial de Apple |
| Bloqueo por accesibilidad | `ManagedSettings` (shield) | Permiso especial de Apple |
| Overlay encima de otra app | `ShieldConfiguration` extension | Permiso especial de Apple |
| Widget de pantalla de inicio | `WidgetKit` | ninguno |
| Notificaciones | `UserNotifications` | ninguno |
| Room | Room KMP 2.7 | ninguno |
| Appwrite | SDK Swift de Appwrite | ninguno |

El permiso se llama **Family Controls** (`com.apple.developer.family-controls`).
Apple lo concede por solicitud, en un formulario aparte del alta de la app. Sin
él, la app compila y se sube, pero las funciones de tiempo de pantalla y bloqueo
quedan inertes. Eso es una gestión que solo puede hacer el titular de la cuenta
de desarrollador; el código sí se escribe ahora y queda listo para el día en que
Apple responda.

## Verificación sin Xcode local

Medido en la rama, no supuesto. La frontera está en un punto muy concreto:

| Tarea Gradle | En este Mac | Evidencia |
|---|---|---|
| `:shared:compileKotlinIosSimulatorArm64` | **funciona** | `BUILD SUCCESSFUL in 1m 22s` |
| `:shared:linkDebugFrameworkIosSimulatorArm64` | **falla** | `MissingXcodeException: /usr/bin/xcrun xcodebuild -version` |

Kotlin/Native descarga su propia cadena LLVM (`~/.konan`), así que **el código
iOS se comprueba de verdad en local**: los tipos se validan contra las
bibliotecas reales de la plataforma (`platform.UIKit`, `platform.Foundation`,
`platform.FamilyControls`…) y un error de API sale en el momento. Lo único que
exige Xcode es el paso final de enlazar el framework y construir el `.app`, y eso
se hace en el sandbox remoto de limrun (`lim xcode create` / `lim xcode build`),
el mismo mecanismo ya usado en otros proyectos de esta máquina.

Regla de trabajo que se deriva: cada etapa cierra con
`:shared:compileKotlinIosSimulatorArm64` y `:app:compileDebugKotlin` en verde. El
enlazado y las capturas del simulador se agrupan al final en limrun, en vez de
pagar una instancia remota por etapa.

## Estado verificado

Todo medido en la rama, no supuesto. Última ronda: 2026-08-31.

| Comprobación | Evidencia |
|---|---|
| Android sigue compilando | `:app:compileDebugKotlin` BUILD SUCCESSFUL |
| Android sigue pasando sus pruebas | **168 pruebas, 0 fallos** |
| El paquete Android se genera | `app-debug.apk`, 39,7 MB |
| Kotlin compila para iOS | `:shared:compileKotlinIosSimulatorArm64` BUILD SUCCESSFUL |
| Pruebas del núcleo compartido | **47 pruebas, 0 fallos** (`:shared:testDebugUnitTest`) |
| App iOS en Debug | `Build Succeeded` + `Installed to simulator` |
| App iOS en **Release** | `Build Succeeded`, arranca y navega entre secciones |
| «Mi vida en semanas» funciona | Captura: nacido 1990-01-01 → 36 años, 1913 de 4160 semanas, 45 % |
| Cronómetro de enfoque funciona | Captura: 24:55 a los 5 s de arrancar un Pomodoro |
| **Aviso local llega de verdad** | Vídeo del simulador: banner «Sesión de enfoque completada» en el fotograma del segundo 66, con la app en segundo plano |
| Cinco idiomas | Capturas con `-AppleLanguages`: alemán «Fokus / Klassisches Pomodoro / 25 Min · 5 Min», francés «Concentration / Ma vie en semaines», meses «janv., févr., mars» |
| Progreso y racha | Captura: «Lv.1 / 80 to Level 2 / Coins 0 / Streak —» |
| Ajustes y apariencia | Captura: tema oscuro aplicado al instante en toda la app; versión `1.2.0 (14)` leída del paquete |
| Accesibilidad (VoiceOver) | `element-tree` devuelve el árbol con etiquetas reales |
| Manifiesto de privacidad en el paquete | `CopyPlistFile … Momentum.app/PrivacyInfo.xcprivacy` en el log |

**Una comprobación pendiente, declarada:** el texto «+X XP · +Y monedas» que
aparece al completar una sesión no se ha visto en pantalla. Alcanzar la opción de
un minuto exige arrastrar un carril horizontal que la automatización no consigue
mover de forma fiable, y esperar los quince minutos del preset más corto no cabe en
el ciclo. La lógica del premio está cubierta por 9 pruebas y el mecanismo de texto
con argumentos está verificado en pantalla en otros tres sitios, pero eso es
inferencia, no verificación.

## Qué funciona ya en iOS y qué falta

**Funciona**: enfoque con cuenta atrás anclada al reloj, descansos, sesión a
medida recordada, avisos locales, vida en semanas con fecha persistida, progreso
con nivel y racha, ajustes con expectativa de vida y apariencia, navegación de
tres secciones, tema Momentum completo en claro y oscuro, cinco idiomas,
VoiceOver.

**Falta, por orden de valor**:

1. **Tiempo de pantalla y bloqueo de apps.** Requiere el permiso Family Controls
   de Apple. Las APIs fallan incluso en simulador sin él, así que escribir esa
   capa ahora sería escribir código imposible de probar. El paso previo es la
   solicitud, que solo puede hacer el titular de la cuenta: los detalles están en
   `docs/IOS_PARA_PUBLICAR.md`.
2. **Sincronización con Appwrite** en iOS, para que el progreso viaje entre las dos
   plataformas.
3. **Unificar el cronómetro de Android** con el núcleo compartido. Es un servicio
   en primer plano en producción; merece un paso propio y verificación en
   dispositivo real.

## Dos trampas encontradas al ejecutar, no al compilar

**1. Compose Multiplatform aborta sin una clave de `Info.plist`.** La app se
instalaba y moría al instante con
`IllegalStateException: Info.plist doesn't have a valid CADisableMinimumFrameDurationOnPhone entry`.
Es una comprobación deliberada de Compose para pantallas de 120 Hz. Ni el
compilador ni el build lo detectan: solo aparece leyendo la consola con
`lim ios simctl -- launch --console` en primer plano.

**2. Los filtros de sincronización de limrun son EXPRESIONES REGULARES, no
comodines.** `--ignore '.gradle'` parece excluir la carpeta de caché y en realidad
borra del envío `settings.gradle.kts`, `build.gradle.kts` y
`gradle/wrapper/gradle-wrapper.jar`, porque `.` casa con cualquier carácter. El
síntoma es engañoso: Gradle responde
`Directory '...' does not contain a Gradle build`, que parece un proyecto roto. Los
patrones correctos van anclados y con el punto escapado:
`^app/build/`, `^\.gradle/`, `^\.git/`, `^shared/build/`, `^build/`.

## Orden de trabajo

Se cambió a propósito respecto al plan inicial: primero el corte vertical
delgado (una pantalla Compose corriendo en iPhone de verdad), después la anchura.
Migrar 71.199 líneas antes de saber si la app arranca habría sido apostar el
proyecto entero a una suposición.

## Etapas

1. Esqueleto KMP: módulo `:shared` con los tres targets, sin mover código aún, y
   `:app` intacto y verde.
2. Núcleo puro a `commonMain`: modelos, entidades, utilidades y motores sin
   `android.*`.
3. Fronteras de plataforma: `expect/actual` para tiempo de pantalla, bloqueo,
   notificaciones, almacenamiento y ajustes.
4. Datos multiplataforma: Room KMP y el cliente de Appwrite.
5. UI compartida: sistema de diseño Momentum y pantallas a Compose Multiplatform.
6. Implementación iOS: Screen Time, notificaciones, WidgetKit y el proyecto Xcode
   `iosApp/`.
7. Verificación: Android verde, pruebas compartidas en verde y build iOS en
   limrun con capturas del simulador.

Cada etapa termina con el build de Android en verde. Si una etapa lo rompe, se
revierte antes de seguir.
