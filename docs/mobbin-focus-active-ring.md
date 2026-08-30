# Mobbin — Superficie B: sesión activa con anillo + estado de descanso

Investigación de patrones reales para rediseñar la pantalla `Focus` (Android / Jetpack Compose).
Objetivo: anatomía del anillo de progreso, diferenciación de estados (corriendo / pausado / descanso)
por **color + etiqueta**, no por decoración, y disposición de controles primario vs destructivo.

Método: 3 consultas en Mobbin (`amount=3`), examinando las imágenes devueltas.

---

## Ronda 1 — "focus timer active session, circular progress ring, countdown in center"

### 1. Tiimo — [pantalla en Mobbin](https://mobbin.com/screens/ffeedfca-2932-4798-9f5a-fccdaff4f095)

Lo que se ve en la captura:

- **Fondo oscuro plano**, casi negro, sin gradiente. El anillo es el único elemento cromático fuerte.
- **Anillo grueso** (no un hairline): el trazo ocupa una fracción notable del radio, del orden de
  10–14 dp a escala móvil. No hay sombra ni glow; es trazo plano sobre superficie plana.
- **Pista vs progreso**: la pista es el mismo color del acento con opacidad muy baja (una versión
  apagada del acento sobre el fondo oscuro), y el progreso es el acento a opacidad plena. No usa
  gris neutro para la pista → la pista es "el acento atenuado". Un solo hue, dos intensidades.
- **Centro del anillo**: SOLO el tiempo restante, en tipografía grande y tabular. Nada más dentro
  del círculo — ni icono, ni porcentaje, ni etiqueta motivacional. El texto de contexto
  (nombre de la tarea / actividad) vive **fuera** del anillo, encima o debajo.
- **Controles**: fila horizontal debajo del anillo, botones circulares del mismo diámetro,
  iconos de trazo. La acción primaria (pausar) es el botón sólido/relleno; las secundarias
  (saltar, terminar) son contorno o solo icono. La destructiva no compite en peso visual.

Reutilizable para nosotros: **un solo hue con dos alfas** (pista = acento @ ~15–20 %, progreso =
acento @ 100 %) evita introducir un color nuevo en la paleta restringida y elimina la tentación
del gradiente.

### 2. Oura — [pantalla en Mobbin](https://mobbin.com/screens/08366dca-8bef-401f-a5d2-2514448fdf7d)

Lo que se ve en la captura:

- Sesión guiada en curso sobre **fondo oscuro**, con la marca temporal grande centrada y el
  progreso como **arco/anillo delgado y contenido**, no como pieza dominante. Aquí el anillo es
  más un *indicador* que el protagonista: el número manda.
- **Jerarquía tipográfica sustituye a la decoración**: el tiempo es enorme; el nombre de la sesión
  y el estado están en tamaños claramente menores encima/debajo. No hay tarjetas apiladas ni
  bloques de "insight" que compitan con el temporizador.
- **Una sola acción visible durante la sesión** (pausar/terminar). El resto de la UI desaparece
  mientras corre. Patrón clave: *la sesión activa es una pantalla mono-tarea*, no un dashboard.

Reutilizable: durante la sesión activa, **oculta** todo lo que no sea tiempo + estado + 1 control.
Nuestra pantalla actual acumula tarjetas; el patorn creíble es lo contrario.

### 3. Toggl Track — [pantalla en Mobbin](https://mobbin.com/screens/44018add-aa29-49a5-9caf-012cc9c6f367)

Lo que se ve en la captura:

- **Superficie clara y opaca**, listas y filas con separadores de 1 px. Ninguna sombra difusa.
- El temporizador corriendo se señala con **color + etiqueta textual**, no con animación: la
  entrada activa lleva el tiempo acumulado en el acento de la app y un control de **stop** que es
  el único elemento en el color de "detener". El resto de las filas son neutras.
- **La acción destructiva (stop) es un botón pequeño, cuadrado/circular, de color distinto**, no un
  botón ancho y prominente. La acción primaria de la pantalla no es detener: es seguir.
- Iconografía: pictogramas de trazo consistentes, mismo grosor, sin relleno mixto. Cero emoji.

Reutilizable: **un único color reservado para "detener/descartar"**, aplicado a un control pequeño.
Todo lo demás en el acento o en neutro.

---

## Síntesis parcial tras la ronda 1

| Decisión | Patrón observado |
|---|---|
| Grosor de trazo | Grueso y plano (Tiimo) cuando el anillo es protagonista; delgado (Oura) cuando el número manda. Elegir uno, no ambos. |
| Pista | Acento con alfa baja, no gris nuevo. |
| Centro del anillo | Solo tiempo. Contexto fuera del anillo. |
| Estado | Color + etiqueta de texto. Nunca glow, nunca animación como único indicador. |
| Controles | Primario relleno; destructivo pequeño y en su propio color; secundarios en contorno. |
| Densidad | Sesión activa = pantalla mono-tarea. |

---

## Ronda 2 — "pomodoro break period countdown after focus session ends"

### 4. Vibecode — [pantalla en Mobbin](https://mobbin.com/screens/eb982e90-3655-4048-a0eb-bfbfb9d27894)

Temporizador pomodoro con el ciclo enfoque/descanso expuesto en la propia pantalla:

- **El modo se declara como texto, arriba del anillo**, en un chip / etiqueta pequeña
  (`Focus` vs `Break`). No hay que inferir el estado del color: el color lo *confirma*, la etiqueta
  lo *dice*. Este es exactamente el patrón que nos falta.
- **Segmentación del ciclo**: se muestran los intervalos del ciclo (pomodoros completados /
  pendientes) como una fila de marcas o puntos discretos, separada del anillo. El anillo comunica
  "cuánto queda de ESTE intervalo"; la fila de marcas comunica "en qué parte del ciclo estás".
  Dos indicadores con dos trabajos distintos, no un anillo sobrecargado.
- Un solo par de controles: **primario grande centrado** (start / pause) y una acción secundaria
  discreta (reset / skip) sin relleno.

### 5. Toggl Track (modo Pomodoro) — [pantalla en Mobbin](https://mobbin.com/screens/ba759c0c-be91-4d36-950b-537837011842)

- Configuración y estado del pomodoro expresados como **filas de lista opacas con separador de
  1 px**: `Focus interval` / `Break interval` con su valor a la derecha. Una sola anatomía de fila,
  repetida. Cero tarjetas decorativas.
- La duración del descanso es un **valor editable explícito**, no un número informativo suelto.
  Si la app enseña "5 min de descanso", debe poder ejecutarlos o al menos configurarlos —
  aquí el número es un control, no un adorno.
- Tipografía y peso hacen la jerarquía; el color se reserva para el estado activo.

### 6. Forest — [pantalla en Mobbin](https://mobbin.com/screens/1b152fab-1bc7-4e4e-b778-64645acdebcb)

- Forest cambia **la superficie completa**, no solo un detalle, al cambiar de fase: el fondo y la
  ilustración central cambian de paleta entre "plantando/enfocado" y el estado posterior. El
  temporizador sigue siendo el mismo componente; lo que se conmuta es el **token de color**.
- Aunque Forest sí usa ilustración, la señal de estado es **cromática + textual**, no un efecto:
  no hay glow ni cristal. La ilustración es un asset vectorial plano y consistente.
- La acción destructiva ("Give up" / abandonar) está deliberadamente **degradada**: texto pequeño,
  sin relleno, a veces con confirmación. Nunca es un botón que compita con el primario.

---

## Cómo señalar el DESCANSO (el punto clave)

Tres señales simultáneas, ninguna decorativa:

1. **Token de color conmutado.** Mismo componente de anillo, distinto token semántico:
   `focusAccent` → `breakAccent`. Un segundo hue de la paleta existente (p. ej. el acento
   secundario ya definido), no un color nuevo ni un gradiente. La pista sigue siendo
   `accent @ alfa baja` del hue activo.
2. **Etiqueta textual explícita** encima del tiempo: `Enfoque` / `Descanso`. Sin la palabra, el
   cambio de color es ambiguo para quien no memorizó el código (y para daltonismo es invisible).
3. **Icono de trazo distinto** junto a la etiqueta (p. ej. objetivo/diana para enfoque, taza o
   pausa para descanso), del mismo set y grosor que el resto. Nunca emoji.

Opcional y barato: **invertir el énfasis**. En descanso, el anillo puede pasar a trazo más delgado
y el número a menor tamaño — el descanso no debe pedir la misma atención que el enfoque.

## Implicación directa para nuestra pantalla

Hoy mostramos minutos de descanso pero nunca los ejecutamos. Las apps creíbles no tienen ese hueco:
el descanso es **un intervalo de primera clase** con su propio countdown, su propio color y su
propia etiqueta. Dos salidas honestas:

- **A) Ejecutarlo**: al terminar el enfoque, el mismo anillo arranca el intervalo de descanso con
  el token `breakAccent` y la etiqueta `Descanso`. Es el patrón de Vibecode.
- **B) No mostrarlo como si corriera**: si no vamos a ejecutarlo, el número de descanso debe vivir
  como **fila de configuración** (patrón Toggl), no como parte del temporizador. Mostrar un contador
  que no cuenta es precisamente el tipo de detalle que delata una UI generada.

---

## Ronda 3 — "session paused, resume / end session below a countdown ring"

### 7. stoic. — [pantalla en Mobbin](https://mobbin.com/screens/41c30896-a507-47ca-b2b0-250c48669d13)

- **Restricción cromática extrema**: superficie clara casi monocroma, tipografía como único recurso
  jerárquico. Cuando la paleta es tan estrecha, el estado se comunica con **palabra + posición**,
  no con color. Demuestra que la etiqueta textual es suficiente por sí sola; el color es refuerzo.
- Bordes/divisores finos de 1 px, sin sombras. Una sola anatomía de contenedor repetida.
- La acción de salir es **texto plano**, no un botón rojo. Nada grita.

### 8. Alan — [pantalla en Mobbin](https://mobbin.com/screens/e11fcc45-b238-43f6-ad35-1dc6040d153a)

- Sesión en curso con el **progreso como arco/anillo alrededor de un núcleo central** y las acciones
  agrupadas en una **fila inferior fija**: la posición de los controles no se mueve entre estados.
  Al pausar cambia el *icono y la etiqueta del botón*, no su tamaño ni su sitio.
- Ese detalle es importante: **el layout no salta** al cambiar de estado. Un botón que cambia de
  ancho o de posición al pausar es otro tell de UI generada.

### 9. pushr — [pantalla en Mobbin](https://mobbin.com/screens/3d789eaf-22c1-477d-aeb5-2604955e046a)

Patrón de **descanso** más literal de todo el conjunto (temporizador de descanso entre series):

- El descanso es una **fase con su propio countdown y su propia etiqueta** (`Rest`), no un dato
  adjunto. Ocupa el mismo componente visual que la fase de trabajo.
- Ofrece controles propios del descanso: **+/− tiempo** y **saltar**. Un descanso real es editable
  y salteable; un número decorativo no.
- Contraste de estado por **peso y color de acento**, no por decoración añadida.

---

## Reglas finales, listas para implementar

**Anillo**
- Un solo `Canvas` / `CircularProgressIndicator` custom con `strokeWidth` constante entre estados
  (8–12 dp si es protagonista; `StrokeCap.Round` opcional, pero elige uno y no lo mezcles).
- Pista = color de acento activo con alfa 0.15–0.20. Progreso = acento a alfa 1.0. Sin `Brush`
  de gradiente, sin `shadow`, sin `blur`.
- Centro: **solo** el tiempo (`mm:ss`), tipografía tabular para que no baile. Nada más dentro.

**Estados (color + etiqueta, ambos)**
| Estado | Token de color | Etiqueta | Icono de trazo |
|---|---|---|---|
| Corriendo | `focusAccent` | `Enfoque` | diana / objetivo |
| Pausado | `focusAccent` @ alfa reducida | `Pausado` | pausa |
| Descanso | `breakAccent` (2.º hue existente) | `Descanso` | taza / pausa larga |

Pausado no cambia de hue: **atenúa** el mismo. Descanso sí cambia de hue. Así el usuario distingue
"detenido" de "otra fase".

**Controles**
- Fila inferior de **posición fija**. Al cambiar de estado cambian icono y etiqueta, nunca la
  geometría.
- Primario: relleno, el más grande (iniciar / pausar / reanudar).
- Secundario: contorno o solo icono (saltar, +1 min).
- Destructivo (terminar / descartar): el **más pequeño**, en su color reservado, y con confirmación.
  Nunca a la misma escala que el primario.

**Prohibiciones que estas apps respetan sin excepción**
- Cero emoji como icono. Todos usan sets vectoriales de trazo uniforme.
- Cero glow / cristal / gradiente neón. El color plano y el alfa hacen todo el trabajo.
- Cero radios "mágicos": un único radio de esquina reutilizado.
- Una sola anatomía de tarjeta/fila, repetida.


