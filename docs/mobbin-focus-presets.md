# Mobbin — Superficie A: pantalla de reposo del temporizador de enfoque (presets) + selección de apps a bloquear

Investigación de referencias reales para el rediseño de la pantalla `Focus` (Android / Jetpack Compose).
Objetivo: eliminar "AI slop" — sin emoji-como-icono, sin glow/cristal/gradientes neón, UNA sola anatomía de tarjeta,
superficies opacas con borde de 1px, paleta restringida, iconos de trazo reales.

Cada patrón está atribuido a la app de la que proviene, con enlace a la pantalla en Mobbin.

---

## Consulta 1 — "focus timer home screen with list of session presets showing duration and break length" (iOS)

### 1.1 Finch — [pantalla en Mobbin](https://mobbin.com/screens/76c07bcc-c7cb-4982-a105-db211fe5bb8d)

Lo que se observa en la imagen:

- **Superficie base opaca, no traslúcida.** Fondo plano de un solo tono claro (crema/lavanda muy desaturado).
  No hay blur de fondo, no hay tarjetas de cristal. Todo el contraste se consigue con **un salto de luminancia
  pequeño entre fondo de pantalla y superficie de tarjeta**, no con sombras difusas ni con glow.
- **Anatomía de fila repetida y única.** Las opciones de sesión se apilan como filas de la misma altura,
  con el mismo padding interno y el mismo radio. No hay una fila "destacada" con tratamiento visual distinto:
  la jerarquía se resuelve con **peso de tipografía**, no con relleno de color.
- **Ilustración contenida en un solo lugar.** El personaje/ilustración vive en un bloque propio en la parte
  superior; **no invade las filas de preset**. Es decir: la app tiene mascota, pero no usa la mascota
  como icono de cada ítem. Esa separación es exactamente la disciplina que buscamos.
- **Acción principal como botón sólido de ancho completo al final del flujo vertical.** El gesto de "empezar"
  no es un tap sobre la tarjeta: seleccionas primero (fila), confirmas después (botón). Lectura en dos pasos.
- **Evita:** glow, gradientes neón, bordes luminosos. Paleta reducida a un neutro + un acento.

**Implementable:** separar `selección de preset` de `confirmación de arranque`. El preset es una fila
seleccionable (estado marcado), y el arranque es un único botón primario persistente al pie.

### 1.2 Me+ — [pantalla en Mobbin](https://mobbin.com/screens/5f46bde8-6275-46da-a62b-239cb4ab1f54)

Lo que se observa en la imagen:

- **Modo oscuro casi monocromo.** Fondo muy oscuro plano; el temporizador es **texto numérico grande**
  en blanco, sin anillo decorativo con degradado. Cuando hay indicador de progreso, es un trazo fino
  de un solo color, no un gradiente.
- **Duración expresada con tipografía, no con iconografía.** El número es el elemento más grande de la
  pantalla; las etiquetas de apoyo (min / descanso) van en un tamaño mucho menor y en color secundario.
  **Cero emoji.** La distinción "trabajo vs descanso" se hace por **etiqueta de texto + color de acento**,
  no por pictograma decorativo.
- **Controles como iconos de trazo dentro de contenedores circulares neutros**, todos del mismo diámetro
  y del mismo grosor de trazo. Un solo set de iconos, coherente.
- **Evita:** cristal, glow, sombras de color. El contraste es puro valor (oscuro vs claro).

**Implementable:** para mostrar duración y descanso sin emoji, usar el patrón
`NÚMERO grande + unidad pequeña` para el foco y una **segunda línea secundaria** tipo
`descanso · N min` en color de texto atenuado. Nada de "🍅"/"☕".

### 1.3 TIDE — [pantalla en Mobbin](https://mobbin.com/screens/f4e64ad4-04d4-4c21-a533-5075e17d05b7)

Lo que se observa en la imagen:

- **Fondo fotográfico / atmosférico a sangre**, con el contenido tipográfico flotando encima.
  Esto es lo **contrario** de lo que necesitamos (dependencia de imagen para la estética), pero
  aporta una lección útil: TIDE **no dibuja tarjetas** sobre la foto. Cuando el fondo es rico,
  la UI se reduce a texto + separadores finos. Es decir: eligen *o* superficie *o* imagen, nunca ambas.
- **Escala tipográfica muy contrastada:** un dígito enorme, todo lo demás pequeño y espaciado.
  El ritmo vertical es amplio; el aire es el que hace de "borde".
- **Selección de duración mediante lista/rueda de valores discretos**, no un slider continuo.
  Valores redondos (5 / 10 / 15 / 25 / 45 …) presentados como texto.

**Implementable (traducido a nuestro sistema):** conservar la **escala tipográfica agresiva**
(un solo elemento dominante por pantalla) pero sustituir la foto por superficie opaca con borde 1px.
Y usar **valores discretos** de duración en lugar de slider.

---

### Síntesis provisional tras la consulta 1

| Decisión | Referencia | Regla concreta |
|---|---|---|
| Anatomía de preset | Finch | Fila/tarjeta única repetida: icono de trazo · nombre · duración. Sin variantes visuales por ítem. |
| Gesto de empezar | Finch | Dos pasos: seleccionar fila (estado) → botón primario sólido al pie. |
| Duración sin emoji | Me+ | `25` grande + `min` pequeño; segunda línea `descanso · 5 min` en texto secundario. |
| Jerarquía | TIDE / Me+ | Un solo elemento dominante; el resto en un único tamaño secundario. |
| Anti-slop confirmado | las tres | Ninguna usa emoji como icono de ítem, ninguna usa glow/gradiente neón en la superficie de la tarjeta. |

---

## Consulta 2 — "screen time app blocking settings list of apps with toggles and app icons and selected count" (iOS)

### 2.1 Opal — [pantalla en Mobbin](https://mobbin.com/screens/99eb07c9-d6f6-435d-a344-931f64c1b2c9)

Opal es *la* referencia directa: su producto entero es bloqueo de apps. Lo que se observa:

- **Fila de app = un solo patrón, repetido sin excepción:**
  `[icono real de la app, cuadrado con esquinas redondeadas] · [nombre de la app] · [control de selección a la derecha]`.
  Todas las filas tienen la misma altura y el mismo padding. Ningún ítem recibe tratamiento especial.
- **El icono de la app es el icono REAL de la app**, no un pictograma inventado ni un emoji.
  Es el único lugar de la pantalla donde entra color arbitrario, y funciona precisamente porque
  el resto de la superficie es neutra. Traducción a Android: `PackageManager.getApplicationIcon()`
  renderizado a tamaño fijo (típicamente 28–32dp) con recorte de esquinas consistente.
- **Agrupación por secciones con cabecera de texto** (categorías tipo *Social*, *Entretenimiento*).
  La cabecera es texto pequeño en mayúsculas o en color secundario, **sin icono y sin fondo propio**.
  Esto evita el "collage de tarjetas" y mantiene una sola anatomía.
- **Control de selección al final de la fila.** Es un control único y consistente en toda la lista
  (círculo con marca / checkbox), no una mezcla de switches y checkboxes según el ítem.
- **Contador de selección visible** en la cabecera o junto a la acción de confirmar
  (`N apps seleccionadas` / `N`). El contador es **texto**, no una badge de color saturado.
- **Buscador arriba** para listas largas — imprescindible cuando el usuario tiene 80+ apps instaladas.
- **Evita:** emoji, glow, cristal. La superficie es opaca; la separación entre filas es una hairline.

**Implementable:**
```
SectionHeader(text)                     // texto secundario, sin icono, sin fondo
AppRow(icon: Drawable, name: String, selected: Boolean)   // altura fija, 1 solo control
BottomBar( "N seleccionadas"  +  botón primario "Guardar" )
```

### 2.2 stoic. — [pantalla en Mobbin](https://mobbin.com/screens/4ce1783f-9c53-459f-893e-ce4645051055)

- **Minimalismo tipográfico extremo.** La pantalla es prácticamente **texto sobre superficie plana**:
  sin iconos decorativos, sin tarjetas anidadas, separadores hairline o simple aire.
  La jerarquía se construye con **tamaño, peso y color de texto**, nada más.
- **Cero color de acento decorativo.** Cuando hay acento, es un único tono usado solo para el estado
  activo/seleccionado. Esto valida directamente el requisito de "paleta restringida".
- **Lección aplicable:** si una fila no necesita icono para ser comprensible, no lo pongas.
  Esto es el antídoto contra el reflejo de "cada ítem necesita un emoji".

### 2.3 bless. — [pantalla en Mobbin](https://mobbin.com/screens/aef16b24-5e68-4c33-8838-b76f9389e1cd)

- Composición muy plana, de bloques grandes y un solo tono dominante; el contenido se apoya en
  **una sola superficie por pantalla** con mucho aire, en lugar de varias tarjetas compitiendo.
- **Lección aplicable:** una sola superficie contenedora por sección. Si necesitas separar dos
  grupos, sepáralos con espacio y una cabecera de texto — no metiendo cada grupo en su propia
  tarjeta con su propio borde y su propia sombra (ese apilamiento de contenedores es el "slop" visual
  más frecuente).

### Reglas de selección de apps derivadas

1. **Un icono real, no un símbolo inventado** — el icono del paquete, a tamaño fijo, esquinas consistentes.
2. **Un único control de selección** para toda la lista. Elegir: checkbox (multi-selección, es nuestro caso)
   y no mezclar con switches.
3. **Contador en texto**, colocado junto a la acción de confirmar, no como badge llamativa.
4. **Cabeceras de sección en texto puro**, sin contenedor propio.
5. **Buscador** obligatorio si la lista puede pasar de ~15 ítems.
6. **Nada de badges de color por categoría** — el color se reserva al estado seleccionado.

---

## Consulta 3 — "pomodoro focus session cards with icon title work and break duration and a start button" (iOS)

### 3.1 Toggl Track — [pantalla en Mobbin](https://mobbin.com/screens/1c50955c-5b83-490f-a3be-c03a22ce09bf)

La referencia más útil de las tres para la **anatomía de fila + gesto de empezar**:

- **Dos zonas táctiles en UNA sola fila.** El cuerpo de la fila abre/edita la entrada;
  un **botón de reproducción (▶) al extremo derecho** arranca esa entrada inmediatamente.
  Es la respuesta más limpia a "cómo se lee el gesto de empezar": el icono de play a la derecha
  es autoexplicativo y no requiere leer nada.
- **Identidad del ítem con un punto de color, no con un emoji.** Toggl marca el proyecto con un
  **círculo pequeño de color** (~8dp) junto al nombre. Es el marcador de identidad mínimo viable:
  cero pictogramas inventados, cero emoji, y encaja en una paleta restringida.
- **Duración alineada a la derecha, en cifras tabulares**, misma posición en todas las filas.
  Eso crea una columna de lectura vertical limpia.
- **Jerarquía de dos líneas por fila:** línea 1 = nombre (peso medio), línea 2 = metadato
  (proyecto / cliente) en color secundario y tamaño menor. Es exactamente la estructura que
  necesitamos para `nombre` + `enfoque · descanso`.
- **Superficies opacas, separadores hairline, sin sombras de color.** El acento (rojo Toggl) se usa
  solo para el estado de "grabando" y para la acción primaria — un único acento con un único trabajo.

**Anatomía trasladable directamente a Compose:**
```
Row(height = 64.dp) {
  ColorDot(8.dp)                     // o icono de trazo 20dp — UN solo tipo, no mezclar
  Column(weight = 1f) {
    Text(name,  style = titleMedium)
    Text("25 min · 5 min descanso", style = bodySmall, color = onSurfaceVariant)
  }
  Text(duration, style = labelLarge)  // opcional, cifras tabulares
  IconButton(PlayArrow)               // el gesto de empezar, explícito
}
```

### 3.2 Life Reset — [pantalla en Mobbin](https://mobbin.com/screens/b027cc71-18bc-4fb1-bdfe-b434a6b5a724)

- Pantalla de tipo "programa/sesión" con bloques grandes y **un único CTA dominante**.
  El resto de la pantalla no compite con él: no hay tres botones del mismo peso visual.
- **Lección aplicable:** exactamente un botón primario por pantalla. Si el preset ya tiene su ▶,
  el botón inferior debe ser el arranque del preset seleccionado — no un segundo "Empezar" ambiguo.
  **Elige uno de los dos modelos, no ambos:** (a) ▶ por fila, sin botón inferior; o
  (b) selección de fila + un único botón primario al pie.

### 3.3 Me+ (revisitada) — [pantalla en Mobbin](https://mobbin.com/screens/5f46bde8-6275-46da-a62b-239cb4ab1f54)

Reaparece en esta consulta y confirma el patrón numérico: **dígitos grandes, unidad pequeña,
etiqueta de descanso en texto secundario, cero emoji, cero anillo con degradado**.

---

## Decisiones finales para la pantalla Focus

### Tarjeta / fila de preset — anatomía única

```
┌──────────────────────────────────────────────┐
│ ◦  Trabajo profundo                      ▶   │   ← 1 fila, 64dp, borde 1px, superficie opaca
│    50 min · 10 min descanso                  │
└──────────────────────────────────────────────┘
```

| Elemento | Regla | Origen |
|---|---|---|
| Marcador de identidad | punto de color 8dp **o** icono de trazo 20dp — uno de los dos, nunca ambos, nunca emoji | Toggl Track |
| Nombre | `titleMedium`, una línea, truncado con ellipsis | Toggl Track / Finch |
| Duración + descanso | segunda línea, `bodySmall`, `onSurfaceVariant`, formato `50 min · 10 min descanso` | Me+ / Toggl |
| Gesto de empezar | `IconButton(PlayArrow)` al final de la fila (modelo a) **o** selección + botón único al pie (modelo b) | Toggl Track / Finch |
| Superficie | opaca, borde 1px, sin sombra de color, sin gradiente | las 6 referencias |
| Estado seleccionado | único acento de la paleta, aplicado al borde y/o al marcador — no relleno saturado | stoic. |

### Selección de apps a bloquear

```
BUSCADOR
─────────────────────────────
REDES SOCIALES                      ← cabecera: texto secundario, sin fondo
[icono real] Instagram          [✓]
[icono real] X                  [ ]
─────────────────────────────
                 3 seleccionadas  [ Guardar ]
```

| Elemento | Regla | Origen |
|---|---|---|
| Icono | icono real del paquete, 28–32dp, esquinas consistentes | Opal |
| Control | un solo tipo (checkbox) para toda la lista | Opal |
| Cabecera de sección | texto puro, sin contenedor ni icono | Opal |
| Contador | texto (`N seleccionadas`) junto al botón, no badge | Opal |
| Buscador | obligatorio (listas de 80+ apps) | Opal |
| Densidad | filas de altura fija, separador hairline, una sola superficie | bless. / stoic. |

### Prohibiciones verificadas contra las referencias

Ninguna de las 6 apps revisadas usa: emoji como icono de ítem, glow, efecto cristal,
gradientes neón, sombras de color, ni más de un radio de esquina en la misma pantalla.
TIDE es la única que apuesta por imagen a sangre — y precisamente **por eso elimina las tarjetas**:
o superficie o imagen, nunca las dos apiladas.

