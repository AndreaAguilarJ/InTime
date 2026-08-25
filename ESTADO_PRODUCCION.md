# Estado de preparación para producción

Fecha: 2026-08-24. Rama: `feat/perf-safearea-cleanup`. Sin commit ni push.

**Veredicto: NO lista para producción.** Quedan bloqueos que no se cierran con
código (última sección). Lo que sigue separa lo verificado ejecutando algo de lo
que solo se ha leído.

## Verificado ejecutando

| Comprobación | Cómo | Resultado |
|---|---|---|
| Compilación debug | `./gradlew :app:assembleDebug` | Correcta, APK de 39 MB |
| Pruebas unitarias | `./gradlew :app:testDebugUnitTest` | 73 pruebas, 0 fallando, 10 clases |
| Instalación | `adb install` en el emulador `momentum_test` | Correcta, Android 15 (API 35), arm64 |
| Arranque en frío | `am start -W` | 1,0–2,1 s, sin fallos |
| Ausencia de errores propios | `adb logcat` filtrado por el paquete | 0 líneas de error |
| Pantalla de bienvenida | Captura revisada | Se renderiza bien, en inglés según el idioma del emulador |

Punto de partida: la compilación ya estaba verde antes de tocar nada y había
3 pruebas unitarias. Ahora hay 73.

## Defectos corregidos y verificados

**El análisis de adicción no guardaba nada, para ninguna app.** Una media sobre
lista vacía devolvía `NaN`; SQLite almacena `NaN` como nulo y la columna
`addiction_scores.overallScore` es `NOT NULL`, así que la inserción fallaba con
`SQLiteConstraintException` en cada app analizada. Afectaba a toda instalación
nueva. Corregido en `UsagePatternEngine.kt`, más un saneado de valores no finitos
antes de guardar. Verificado: tras reinstalar desde cero, 0 ocurrencias del error
donde antes aparecía una por app.

**El hash de contraseñas dependía de una API de Android sin necesidad.**
`PasswordProtectionRepository` usaba `android.util.Base64`, que no existe en
pruebas JVM, lo que hacía imposible probar la lógica de contraseñas. Sustituido
por `java.util.Base64`: mismo formato de salida, así que las contraseñas ya
guardadas siguen validando. Las 6 pruebas que fallaban ahora pasan, y una
aserción equivocada que exigía el formato heredado de 64 caracteres se corrigió
al formato PBKDF2 real. Se añadió una prueba de que dos contraseñas iguales
producen hashes distintos por la sal.

**Dos entradas de Ajustes no hacían nada.** «Protección» y «Categorías de apps»
enviaban su clave de navegación, pero el reparto de rutas no tenía rama para
ninguna: el toque se descartaba en silencio y ambas pantallas, completas, eran
inalcanzables. Conectadas, más una rama por defecto que registra en el log
cualquier destino sin cablear para que no vuelva a fallar callado. También se
eliminó una rama muerta hacia una ruta inexistente.

**El tutorial inicial perdía la fecha de nacimiento.** Los dos pasos donde el
usuario introduce su fecha y elige colores empezaban comprobando la sesión de
Appwrite y salían si no existía; el guardado local estaba escrito después, así
que también se saltaba. Un usuario invitado o sin conexión completaba el tutorial
y «Mi Vida en Semanas» se quedaba sin el único dato del que depende. Invertido:
local siempre, nube solo si hay sesión, y un fallo de red ya no revierte lo
local. La fecha se parsea con `Locale.ROOT` para que un calendario no gregoriano
no altere el año.

**El botón de prueba gratuita no hacía nada.** Cableado a `startFreeTrial`.

**Seis pantallas ignoraban el idioma elegido en Android 8 a 12.** El cambio de
idioma usa la API moderna, correcta en Android 13+, pero su compatibilidad con
versiones anteriores solo alcanza a actividades basadas en AppCompat.
`MainActivity` ya lo era; las de bloqueo de apps, bloqueo dentro de apps, bloqueo
de webs, fecha de nacimiento, lanzador mínimo y arranque desde widget heredaban de
`ComponentActivity`, así que un usuario alemán en Android 11 vería las pantallas de
bloqueo en el idioma del sistema. Como el mínimo soportado es Android 8, eso cubre
buena parte del parque. Las seis pasaron a `AppCompatActivity`; se comprobó antes
que su tema desciende de AppCompat para no provocar el crash de tema. Verificado:
cinco no son lanzables desde fuera por seguridad, y la sexta arrancó sin fallos y
se revisó en pantalla.

## Riesgos conocidos

**El cambio de idioma FUNCIONA; verificado en el emulador.** Con el dispositivo en
en-US se forzo el idioma de la app a en-US y toda la pantalla principal y la
navegacion se tradujeron correctamente: «Today», «Analytics», «Community»,
«Focus», «Minimal Phone», «Settings», «Good afternoon», «Screen time today»,
«Level 1», «Novice», «80 to Level 2», «Total focus». Las traducciones de los cinco
idiomas estan completas y se aplican.

> Correccion de una revision anterior: aqui se afirmo que la pantalla principal
> estaba escrita a mano en espanol porque salia en espanol con el dispositivo en
> ingles. **Era una inferencia equivocada.** La causa real es que Android tenia
> guardado el idioma por app como `[es]` para la instalacion de depuracion
> (`cmd locale get-app-locales` lo confirma), asi que la app hacia lo correcto:
> respetar el idioma elegido. La pantalla principal SI usa recursos de texto.

**Cerrado, NO es un defecto: la app no se fija el idioma a si misma.** Durante la
sesion se observo que la instalacion de depuracion tenia guardado `[es]` sin que
nadie lo hubiera elegido, y se apunto como sospecha a investigar. Se comprobo con
un experimento limpio: borrar los datos de la app (`pm clear`), dejar el idioma por
app vacio y arrancar una vez. Resultado: el idioma sigue vacio despues del
arranque y la app se dibuja en ingles, respetando el sistema. No es reproducible;
aquel `[es]` era residuo de la propia sesion de pruebas, en la que se instalo y
arranco la app muchas veces y se manipularon los idiomas por linea de comandos.
Queda escrito para que nadie persiga un fallo inexistente.

**Las tres pantallas del recorrido principal que filtraban espanol YA ESTAN
TRADUCIDAS.** Se extrajeron 54 textos que estaban escritos a mano en el codigo a
los cinco idiomas, o sea 270 traducciones, en archivos aparte para no mezclarlos
con las 1097 claves existentes:

| Pantalla | Textos extraidos | Archivo de recursos |
|---|---|---|
| Modo minimo | 22 | `strings_minimal.xml` |
| Comunidad | 12 | `strings_community.xml` |
| Enfoque | 20 | `strings_focus.xml` |

Los tres archivos de codigo (`MinimalPhoneScreen.kt`, `CommunityScreen.kt`,
`FocusSessionScreen.kt`) quedan sin un solo literal visible. Comprobado: paridad
exacta de claves en los cinco idiomas, XML valido, cero mojibake, compilacion en
verde, 73 pruebas sin regresion, y los valores presentes en el APK con cada idioma
resuelto (verificado con `aapt2 dump resources`).

Tres textos insertan datos en medio de la frase (nombre de amigo, nombre de sesion,
nivel). Antes se construian pegando el dato dentro de la frase espanola, lo que hace
imposible traducirlas porque cada idioma coloca el dato en otro sitio; ahora usan
argumento posicional `%1$s` / `%1$d`, conservado en los cinco idiomas. Los ejemplos
de los campos de nombre usan un nombre propio de cada cultura en vez de traducir
«Juan Garcia».

NO verificado en pantalla: al borrar los datos de la app durante el experimento del
idioma, esas pantallas quedaron detras del login. La evidencia es el binario, la
paridad y las pruebas, no una captura.

**Quedan textos escritos a mano en las pantallas de segundo nivel**, que no se
recorrieron: `SmartBlockingScreen.kt` (30), `AppCategoriesScreen.kt` (29),
`AppLimitDialogs.kt` (22) y `AppLimitsScreen.kt` (21). Es de esperar que filtren
igual.

**Y hay 106 textos visibles MAS escondidos en la capa de datos.** El recuento
inicial de 301 solo cubria `ui/**` y `minimal/**`, asi que se dejo fuera todo el
texto que generan los motores y repositorios. Aparecio al revisar los avisos de
lint sobre formateo, y son mensajes que el usuario lee de verdad:

| Archivo | Textos visibles en espanol |
|---|---|
| `data/AppDatabase.kt` (frases y semillas) | 51 |
| `data/engine/UsageAnalyticsEngine.kt` | 16 |
| `data/engine/AdaptiveBlockingManager.kt` | 13 |
| `data/appwrite/repository/AppwriteQuotesRepository.kt` | 10 |
| `data/engine/UsagePatternEngine.kt` | 8 |
| `data/entity/AppCategory.kt` | 6 |
| `data/manager/GamificationManager.kt` | 2 |

Ejemplo real de `AdaptiveBlockingManager.kt:681`: «A este ritmo, pasaras X horas
esta semana aqui». Es un mensaje de intervencion que se muestra al usuario y
saldra siempre en espanol.

Total corregido de texto sin traducir: alrededor de **400**, no 301. Una parte vive
donde nadie la buscaria, en clases de datos y no de interfaz.

Ojo con el recuento: se hizo buscando literales con mayuscula inicial o acentuada,
asi que mide los que estan en espanol; puede haber mas en otros idiomas (la pantalla
de bienvenida, por ejemplo, no aparecio en ese recuento).

## Analisis estatico: lint no podia ejecutarse, ahora si

`:app:lintDebug` abortaba por completo. Dos detectores de Compose crashean sobre
`MinimalAppListScreen.kt`; la causa de fondo es Kotlin 2.0.21 con Compose BOM
2023.10.01 (de octubre de 2023, pensado para Kotlin 1.9), cuyos analizadores no
entienden el UAST nuevo. Se desactivaron esos dos detectores con el motivo escrito
en `app/build.gradle.kts`. **Es un parche: el arreglo de raiz es subir el Compose
BOM.**

Con lint operativo aparecieron 9 errores. Ocho eran defectos reales y estan
corregidos (ver arriba: tres crashes por API y cinco flujos en composicion). El
noveno se dejo a proposito:

**Descartado con motivo — `StringFormatInvalid` en `values-de`.** Lint pide escapar
el porcentaje de «23 %» como `%%`. Se comprobo el uso real: la cadena se muestra
con `stringResource(...)` SIN argumentos de formato, asi que el porcentaje literal
es inofensivo y escaparlo haria que el usuario leyera «23%%». Seguir la
recomendacion habria creado un defecto visible donde no habia ninguno.

**Otros avisos revisados y descartados con motivo, no por pereza:**

- *8 plurales sin la forma `many`* (fr, pt). Esa forma aplica a partir de un millon
  y las cadenas cuentan dias de racha y «hace X minutos»: un millon de dias son
  2700 anos, y Android ya cae en `other` cuando falta. Anadir texto que no puede
  mostrarse es ruido.
- *24 `DefaultLocale`*. Se inspeccionaron cuatro y todos formatean numeros PARA
  MOSTRAR: horarios «09:00 - 17:00» y cifras compactas «1,2M». Ahi usar el idioma
  del usuario es lo correcto; forzar `Locale.ROOT` haria que un espanol viera
  «1.2M» y un usuario arabe perdiera sus propios digitos. Seria una regresion
  disfrazada de arreglo.
- *7 `SmallSp`*. Todos en los layouts `*_widget_preview.xml`, es decir la miniatura
  del selector de widgets, no la interfaz real.

Quedan 398 avisos sin revisar uno a uno.

**Accesibilidad de la pantalla principal: verificada, sin defectos pendientes.**
La barra inferior esta etiquetada (6 de 6) y los objetivos tactiles miden 144 px a
densidad 480, es decir exactamente los 48 dp minimos: pasa.

> Correccion de una revision anterior: aqui se afirmo que habia «7 elementos
> pulsables sin ninguna etiqueta, invisibles para el lector de pantalla». **6 de
> los 7 eran falsos positivos.** Al inspeccionar los nodos uno a uno se vio que
> cinco son las pestanas inferiores y otro la tarjeta de premium, y que en todos
> la etiqueta vive en un nodo hermano del mismo grupo de semantica, que es
> exactamente como Compose agrupa un control con su texto: el lector de pantalla
> los anuncia bien. El recuento inicial solo miraba si el propio nodo pulsable
> llevaba texto, y esa es una medida equivocada en Compose.

**El septimo si era real y esta corregido:** el control de monedas, nivel y XP de
la cabecera era pulsable pero su `onClick` estaba vacio, asi que el usuario tocaba
y no pasaba nada. Se expuso `onCoinsClick` en `DashboardScreen` y se cableo en
`MomentumApp.kt` hacia la pantalla de gamificacion. Verificado tocandolo en el
emulador: el toque ahora abre el detalle con «Total XP» y «Current streak».

**Los datos de uso son reales, no simulados.** La pantalla principal mostro
10 h 27 m de tiempo total, 5 apps registradas y nombres reales (Instagram,
Facebook, Maps) leidos del sistema.

## Aplicado pero NO verificado en ejecución

Compila y no rompe las pruebas, pero no se ha recorrido en pantalla:

- Unas 123 traducciones añadidas a alemán, francés y portugués.
- Accesibilidad: descripciones para lector de pantalla, semántica de controles,
  tamaños táctiles, contraste y reducción de movimiento, con nuevos archivos
  `strings_a11y.xml` en los cinco idiomas.
- Endurecimiento de ProGuard, manifiesto y configuración de release.
- Endurecimiento del guardado de contraseñas y de rutas de error.

## Release: de imposible de generar a verificado en verde

El release era el riesgo numero uno y ahora esta comprobado de punta a punta. Dos
bloqueos reales impedian generar cualquier artefacto publicable, y ninguno era
visible desde la version de depuracion:

1. **`:app:minifyReleaseWithR8` fallaba** con «Missing classes detected while
   running R8» por clases que iText referencia y que no existen en Android
   (`java.awt.*`, `javax.imageio.*`) y por utilidades JSON opcionales basadas en
   Jackson. Corregido anadiendo a `app/proguard-rules.pro` exactamente las reglas
   `-dontwarn` que genero R8, con el motivo de cada grupo documentado en el propio
   archivo. Verificado en verde.
2. **`:app:lintVitalRelease` fallaba** por `RemoveWorkManagerInitializer`, y detras
   del aviso habia un defecto funcional (ver el apartado de WorkManager en los
   defectos corregidos). Corregido y verificado en verde.

Con eso, `:app:assembleRelease` produce APK y **funciona**:

| Comprobacion | Resultado |
|---|---|
| Tamano | 18,4 MB minificado frente a 37,2 MB en depuracion |
| Instalacion | Correcta |
| Arranque en frio | 763 ms |
| Excepciones de ProGuard | Ninguna: ni ClassNotFound, ni NoSuchMethod, ni fallo de serializacion |
| Interfaz | Renderiza correctamente; verificado con captura y con el arbol de accesibilidad, que devuelve los 6 nodos de texto esperados |

Para producir un APK instalable se firmo con la clave de depuracion estandar del
SDK pasando las propiedades por linea de comandos, sin modificar ningun archivo ni
incluir credencial alguna. El keystore real sigue siendo un bloqueo (mas abajo).

> Nota de honestidad sobre este apartado: en una revision anterior se documento
> aqui que el release minificado arrancaba en negro sin dibujar nada. **Era falso.**
> La captura se tomo con la pantalla del emulador en reposo, y la comparacion de
> control estaba viciada: se contrasto esa captura contra una de depuracion tomada
> tras despertar la pantalla, y ademas el emulador habia cambiado a tema oscuro, asi
> que el archivo pesaba poco por dos motivos a la vez. Lo que destapo el error fue
> volcar el arbol de accesibilidad, que mostraba toda la interfaz presente. Queda
> escrito porque una limitacion falsa documentada cierra vias que si funcionaban.

**Sin pruebas instrumentadas ni recorrido completo de la interfaz.** Se verifico
el arranque y la pantalla de bienvenida; el resto de pantallas no se recorrió.

## Bloqueos que no se resuelven con código

- **La compra de la suscripción no existe.** `BillingManager` tiene un flujo de
  compra real implementado, pero ninguna pantalla lo invoca: el botón de
  suscribirse llamaba a una función vacía. Deliberadamente NO se conectó a
  `upgradeToPremium`, porque eso concedería premium a cualquiera que pulse el
  botón sin cobrar. Requiere crear los productos en Play Console, lanzar el flujo
  real y verificar la compra antes de conceder nada.
- **Política de privacidad y términos** apuntan a direcciones que no existen.
- **La generación de mensajes con IA** necesita una clave que no está en el
  proyecto; la función queda apagada hasta que se aporte.
- **Appwrite real** no se ha validado contra un servidor de verdad.
- **Firma de release**: hacen falta el keystore y sus contraseñas.

---

## Funciones construidas que no llegan a la pantalla

Trazando la cadena completa del análisis de uso aparece un bloque grande de código
que funciona por dentro y no tiene salida a la interfaz:

```
UsagePatternEngine   calcula los scores de adicción     → los guarda en la BD
UsageAnalyticsEngine los lee y genera hallazgos y logros → devuelve la lista
SmartBlockingManager expone getInsights() y getWeeklyReport()
        ↓
   NADIE los llama
```

Comprobado punto por punto:

- Ninguna pantalla consulta la tabla `addiction_scores`.
- `suggestedAction` solo lo lee `UsageAnalyticsEngine`, que a su vez no se muestra.
- `getInsights()` y `getWeeklyReport()` no tienen ni una llamada en todo el proyecto.
- La única invocación real (`SmartBlockingManager` línea 278) genera los hallazgos
  cada día en segundo plano y **descarta el resultado**.
- La pantalla de Análisis que sí ve el usuario tiene su propia función
  `generateInsights` en `AdvancedAnalyticsViewModel` línea 312, independiente del
  motor. Hay dos implementaciones y solo una llega a pantalla.

Los 44 textos en español de `UsageAnalyticsEngine` (títulos de hallazgos, mensajes
con datos, logros como "Voluntad de Acero") **no se han traducido a propósito**:
serían 220 traducciones invertidas en texto que nadie lee, y dejarían el fichero
con aspecto de terminado cuando en realidad está desconectado.

### Corrección a un informe anterior de esta sesión

El primer defecto que documenté decía que el análisis de adicción "no guardaba nada
para ninguna app en ninguna instalación nueva". Eso es exacto y el arreglo es real:
un NaN convertido en NULL rompía la inserción, y ahora los datos se guardan.

Lo que no dije, porque entonces no lo sabía, es que **ese dato no se muestra en
ninguna pantalla**. El arreglo corrige la base de datos y deja la función lista
para cuando se conecte, pero no cambió nada de lo que el usuario ve. Merecía esa
matización desde el principio.

### Decisión pendiente del propietario

Conectar el motor a la pantalla sería añadir funcionalidad, no pulir la existente,
y contradiría la instrucción de no rediseñar. Retirarlo borraría trabajo que quizá
se quiere conservar. Queda anotado sin tocar.

### Mapa de funciones públicas sin invocación conocida

Barrido sobre motores y gestores: por cada función pública se buscó una llamada
`.nombre(` en todo el proyecto, excluyendo el fichero que la declara.

| Clase | Sin invocar / totales |
|---|---|
| NotificationManager | 28 / 36 |
| AdaptiveBlockingManager | 30 / 43 |
| UsagePatternEngine | 20 / 28 |
| SmartBlockingManager | 10 / 46 |
| GamificationManager | 6 / 15 |
| CommunityManager | 5 / 18 |
| BackupSyncManager | 5 / 8 |
| AdvancedDetectionEngine | 5 / 7 |
| BillingManager | 3 / 6 |
| ThemeManager, ExportManager, UsageAnalyticsEngine | 0 (todas en uso) |

**Esto es un mapa de candidatos, no un veredicto.** El método tiene límites
conocidos: no ve llamadas sin receptor explícito dentro de la misma clase, ni las
que llegan por una interfaz o con otro nombre de variable. Solo un caso se ha
verificado de extremo a extremo (`getInsights`, ciclo 41): cero apariciones de
`.getInsights(` en todo el proyecto, confirmado a mano.

Sirve para priorizar dónde mirar, no para borrar nada. Cada entrada necesita su
propia comprobación antes de tocarla.

### Biblioteca de mensajes motivacionales — contenido solo en español (decisión del propietario)

La función de mensajes motivacionales es **contenido monolingüe** y no un hueco de
traducción mecánica:

- **584 mensajes sembrados**, todos en español fijo, en
  `MotivationalMessagesSeed.kt`. Se cargan en la base de datos para todos los
  usuarios, sea cual sea su idioma.
- El **generador de IA** también fuerza el español: el prompt dice literalmente
  *"Genera UN SOLO mensaje motivacional en español"*
  (`MotivationalMessageGenerator.kt:279`). Aunque el usuario tenga el móvil en
  francés, la IA respondería en español. Además, este camino está **muerto sin
  `GEMINI_API_KEY`** (bloqueo ya documentado), así que en la práctica los únicos
  mensajes que se ven son los 584 en español.
- Los enums `MessageCategory` (18 valores) y `MessageTone` (8 valores) llevan su
  `displayName` en español fijo y se muestran como etiquetas en la biblioteca.

**Por qué no traduje solo las 26 etiquetas:** dejaría 584 mensajes en español bajo
etiquetas traducidas — una función monolingüe con aspecto de estar a medio
traducir, el mismo error que traducir el subsistema de análisis desconectado.
Traducir el corpus completo (584 mensajes × 4 idiomas más el prompt de la IA) es
trabajo de redacción creativa, no extracción mecánica, y es una **decisión del
propietario**: o se traduce el corpus entero, o se asume que la app entrega
motivación en español. Localizar el prompt de la IA es trivial, pero no cambia
nada observable mientras falte la clave.

#### Nota sobre el método

La primera versión de este barrido devolvió "ningún problema" y era falso: un
`|| echo 0` producía dos líneas donde se esperaba un número, la comparación
numérica fallaba en cada iteración y el error estaba silenciado. Un resultado
limpio obtenido con una herramienta que no puede fallar visiblemente no es
evidencia de nada. Se detectó probando el barrido contra un caso cuya respuesta
ya se conocía.
