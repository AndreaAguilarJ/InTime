# Mobbin — Superficie C: fin de sesión, bloque de estadísticas y estado vacío

Investigación de referencia para el rediseño de la pantalla **Focus** (InTime, Android / Jetpack Compose).
Objetivo: eliminar el "AI slop" (glow, cristal, gradientes neón, emoji-como-icono, confeti) y quedarnos con
**una sola anatomía de tarjeta**, superficies opacas con borde de 1px, paleta restringida e iconos de trazo real.

> Método: búsquedas en Mobbin (iOS), examinando las imágenes devueltas. Cada patrón se cita con la app y el enlace de la pantalla.

---

## Consulta 1 — "focus session complete summary screen with duration and completion message"

### 1.1 TIDE — pantalla de sesión / cierre de foco
[Ver en Mobbin](https://mobbin.com/screens/390d64f4-3b90-4189-9de0-9845e46442fa)

Lo que se observa:

- **La duración ES la ilustración.** No hay tarjeta de celebración: el número de minutos ocupa el centro óptico
  de la pantalla en un peso tipográfico muy alto y tamaño enorme (≈ 64–80sp), y todo lo demás es texto secundario
  pequeño alrededor. La "recompensa" es leer el número grande, no un badge.
- **Fondo fotográfico a sangre completa, sin gradiente sintético.** El único "efecto" es un oscurecimiento plano
  (scrim opaco) para garantizar contraste del texto. Es un scrim, no un glow.
- **Jerarquía de tres niveles y nada más**: (1) dato numérico gigante, (2) etiqueta corta en mayúsculas/tracking
  amplio debajo del número, (3) una sola acción primaria al pie.
- **Cero iconografía decorativa.** Los pocos iconos presentes son de trazo fino, monocromo, del mismo grosor.

Aplicable a InTime:
- Para el fin de sesión, sustituir cualquier celebración por **el dato**: `42` + `MINUTOS ENFOCADOS`.
- Si queremos un fondo expresivo, usar imagen/color plano + scrim opaco, nunca gradiente radial con glow.

### 1.2 Brick — cierre de bloqueo / confirmación
[Ver en Mobbin](https://mobbin.com/screens/eb2562e9-5f2c-4e6a-87da-8ca18fdc29eb)

Lo que se observa:

- **Celebración por tipografía, no por confeti.** El estado "hecho" se comunica con una frase corta declarativa
  en tipografía grande y un lienzo de un solo color muy saturado y **plano**. Ni partículas, ni destellos, ni sombras.
- **Superficies opacas de un color, borde/contorno definido, radios consistentes.** Los contenedores no tienen
  elevación ni blur: se distinguen por color de relleno y por un contorno nítido.
- **Un único botón de ancho completo** al pie, con etiqueta verbal ("hecho / continuar"), sin icono acompañante.
- **El color hace el trabajo emocional.** Es el mecanismo de celebración más barato y el que no envejece: un cambio
  de color de superficie a nivel pantalla en lugar de una animación de recompensa.

Aplicable a InTime:
- Patrón de celebración recomendado: **cambio de color de la superficie completa** (accent opaco) + frase corta +
  el dato de la sesión. Coste de implementación ~0, cero assets, cero animación.
- Reutiliza nuestra anatomía única de tarjeta: la "tarjeta de logro" es la misma tarjeta con el color de acento
  como relleno.

### 1.3 Bevel — resumen con métricas post-actividad
[Ver en Mobbin](https://mobbin.com/screens/5f7837bf-96da-4c5d-a614-655893f9cbf3)

Lo que se observa:

- **Anatomía de estadística: número dominante + etiqueta minúscula, apilados verticalmente.** El número usa
  tipografía tabular/condensada; la etiqueta va debajo en tamaño pequeño y color atenuado. Los iconos, cuando
  aparecen, son de trazo fino y **acompañan a la etiqueta**, no al número.
- **Filas separadas por divisores de 1px, no por tarjetas.** Cuando hay varias métricas relacionadas, se agrupan
  en un solo contenedor y se separan con hairlines. Esto evita el "mar de tarjetas" que produce el look de AI slop.
- **Unidades siempre en tamaño menor que el número** (`42` grande + `min` pequeño), en la misma línea base.
- Paleta muy restringida: fondo neutro, texto de dos niveles de opacidad, un único acento para el valor actual.

Aplicable a InTime:
- **Regla de anatomía de métrica**: `NÚMERO (display, tabular) / ETIQUETA (label, 60–70% opacidad) / icono opcional
  de 16dp junto a la etiqueta`. Nunca icono grande sobre el número.
- **Regla de agrupación**: 2–4 métricas → **un contenedor con divisores de 1px**. Solo usar tarjetas separadas
  cuando las métricas son navegables por separado.

---

## Consulta 2 — "productivity stats screen with daily streak count and total time focused"

### 2.1 Open — panel de estadísticas de práctica
[Ver en Mobbin](https://mobbin.com/screens/520499f8-5d63-41c7-9b66-787efd6a06ce)

Lo que se observa:

- **Retícula de 2 columnas con un solo contenedor por métrica, todos del MISMO tamaño.** Esa es la "única
  anatomía de tarjeta": rectángulo de relleno opaco ligeramente distinto al fondo, radio moderado y constante,
  sin sombra. Ninguna tarjeta es "hero", ninguna tiene tratamiento especial.
- **Dentro de cada celda, el orden es fijo**: etiqueta arriba (pequeña, atenuada) → número grande abajo → unidad
  pequeña. Ese orden repetido en las 4 celdas es lo que hace que se lea como sistema y no como collage.
- **El icono, cuando existe, es de 16–20px, trazo de 1.5–2px, monocromo y alineado con la etiqueta.** No hay
  emoji, no hay icono relleno de color.
- La única jerarquía de color es: fondo / superficie / texto primario / texto secundario / **un** acento.

Aplicable a InTime:
- Nuestro bloque "hoy / racha / tiempo total" cabe exactamente en esta retícula: 3 celdas idénticas (o 2+1),
  todas con `label → valor → unidad`, mismas dimensiones, mismo borde de 1px.

### 2.2 TIDE — estadísticas de foco con serie temporal
[Ver en Mobbin](https://mobbin.com/screens/fd5c7391-2040-4ac8-bb4a-a1d30459189f)

Lo que se observa:

- **Fila de métricas "resumen" en la cabecera + gráfico debajo.** Las 3–4 cifras clave van en una fila horizontal
  compartiendo un mismo contenedor, **separadas por divisores verticales de 1px**, no por tarjetas independientes.
  Es el patrón más económico visualmente y el que mejor escala a 3 items.
- **Gráfico de barras sobrio**: barras de un solo color, sin gradiente, sin bordes redondeados exagerados, ejes
  reducidos a etiquetas de texto minúsculas. El eje Y suele omitirse y el valor aparece solo en la barra activa.
- **Selector de rango (semana / mes / año) como segmented control plano** encima del gráfico. Sin píldoras con glow.
- Cifras alineadas en tipografía de cifras tabulares para que no "salten" al actualizarse.

Aplicable a InTime:
- Para "hoy / racha / total": **una fila, un contenedor, dos divisores verticales de 1px**. Cero tarjetas sueltas.
- Si añadimos histórico, barras de un color plano y etiquetas de día en texto pequeño. Nada más.

### 2.3 Me+ — representación de la racha
[Ver en Mobbin](https://mobbin.com/screens/ae0da7df-b2fd-41d5-a89a-33ea2f4352f4)

Lo que se observa — **cómo se representa una racha de forma creíble**:

- **La racha se muestra como una fila de días, no como una llama.** Siete casillas / puntos en línea, una por día
  de la semana, con la inicial del día debajo. El estado se codifica por **relleno**: día cumplido = superficie
  de acento opaca; día pendiente = superficie vacía con borde de 1px; hoy = borde más marcado o punto interior.
- **El número de racha acompaña, no protagoniza**: `12` + `días seguidos` en la misma anatomía de métrica que
  el resto. No hay badge dorado, ni llama, ni brillo.
- **Progreso circular / anular con grosor fino y un solo color** para el objetivo del día; el track es el mismo
  color al 15–20% de opacidad, no un gradiente.
- Cuando hace falta iconografía de logro, es un **check de trazo** dentro de la casilla — el mismo icono del sistema.

Aplicable a InTime:
- **Racha = 7 casillas + inicial del día + contador numérico**, codificando el estado con relleno/borde.
  Es legítimo, informativo (el usuario ve *cuándo* falló) y no requiere ningún asset.
- Si queremos un solo icono de trazo junto a la etiqueta de racha, usar un icono de calendario o de rayo del set
  vectorial existente, a 16dp, monocromo. **Nunca un emoji 🔥.**

---

## Consulta 3 — "empty state with no data yet illustration and call to action"

> Nota: esta consulta devolvió apps fuera del dominio de bienestar (banca, viajes, delivery). Aun así la
> **anatomía del estado vacío** es transferible y es exactamente lo que nos falta, así que la documento como tal.

### 3.1 Ubank Money App — estado sin datos
[Ver en Mobbin](https://mobbin.com/screens/0f38d793-9dc5-4c87-b22f-c4c0290797fa)

Lo que se observa:

- **Pila vertical centrada de 4 elementos y nada más**: (1) gráfico pequeño y contenido, (2) titular de una
  línea, (3) una única frase de apoyo, (4) un botón primario. Sin decoración periférica.
- **El estado vacío vive DENTRO del mismo contenedor que ocuparán los datos.** El marco de la tarjeta/sección no
  desaparece: cambia su contenido. Eso mantiene la pantalla reconocible entre "vacío" y "con datos".
- **El gráfico es geométrico y monocromo**, del mismo grosor de trazo que los iconos del sistema. No es una
  ilustración 3D, ni un personaje, ni un render con luces.
- **El titular nombra lo que falta; la frase de apoyo dice qué hacer.** Nunca "0%", nunca un gráfico vacío con ejes.

### 3.2 Skyscanner — lista guardada vacía
[Ver en Mobbin](https://mobbin.com/screens/ff6fd519-92bd-4a84-8727-45907ace1206)

Lo que se observa:

- **Icono de trazo grande (≈48–64px) en color atenuado**, no el color de acento: el estado vacío no compite por
  atención con la acción primaria.
- **Dos niveles de texto con jerarquía clara** (título semibold + cuerpo secundario) y ancho de línea limitado
  (≈28–36 caracteres) para que el bloque quede compacto y centrado.
- **La acción es la MISMA acción primaria de la pantalla llena**, no un botón especial de onboarding. Es decir:
  el estado vacío no es una pantalla aparte, es la pantalla normal sin filas.
- Espaciado generoso arriba y abajo del bloque; el bloque no se estira a pantalla completa.

### 3.3 Swiggy — sin resultados / sin contenido
[Ver en Mobbin](https://mobbin.com/screens/49e36f4c-1aa5-4b48-82d9-a9f91fb219cd)

Lo que se observa:

- **La cabecera y la navegación permanecen intactas.** Solo el área de contenido se sustituye. El usuario nunca
  pierde la orientación ni ve un spinner a pantalla completa.
- **Cuando hay carga real, se usan placeholders de forma (skeleton) con la silueta de las filas finales**, no un
  spinner centrado. El skeleton es un rectángulo de relleno neutro con el mismo radio que la tarjeta real.
- Texto de estado en una sola línea, con enlace/botón de recuperación al lado.

---

## Síntesis aplicable a InTime

### A. Fin de sesión — celebrar sin confeti
1. **El dato es la celebración.** Duración en tipografía display (`48`) + etiqueta (`minutos enfocados`). (TIDE)
2. **La señal de éxito es el color de la superficie**, no una animación: la pantalla/tarjeta de cierre se rellena
   con el acento opaco. Cero partículas, cero glow, cero sombras. (Brick)
3. **Un solo botón primario de ancho completo** con verbo (`Hecho`). Acción secundaria como texto plano debajo.
4. Debajo del dato principal, máximo 2 métricas de contexto en la misma anatomía de métrica. (Bevel)

### B. Bloque de estadísticas — hoy / racha / tiempo total
- **Un contenedor, tres columnas, dos divisores verticales de 1px.** Sin tarjetas independientes. (TIDE)
- Anatomía por columna, orden invariable: `ETIQUETA (label, atenuada)` → `VALOR (display, cifras tabulares)` →
  `unidad (pequeña)`. Icono opcional de 16dp, trazo, monocromo, junto a la etiqueta. (Open, Bevel)
- **Racha = 7 casillas + inicial del día + contador.** Cumplido = relleno de acento; pendiente = borde de 1px;
  hoy = borde reforzado. Nada de llamas ni emoji. (Me+)

### C. Estado vacío — el reemplazo del spinner eterno y de los ceros crudos
Tres estados distintos, no uno:

| Estado | Qué mostrar | Qué NO mostrar |
|---|---|---|
| **Cargando** (sesión de usuario resolviéndose) | Skeleton con la silueta exacta del bloque de estadísticas: 3 rectángulos neutros del tamaño de los valores, dentro del contenedor real, borde de 1px visible | Spinner a pantalla completa, spinner indefinido |
| **Usuario nuevo / cero datos** | Bloque centrado dentro del mismo contenedor: icono de trazo 48dp atenuado → titular (`Aún no hay sesiones`) → una frase (`Inicia tu primera sesión de enfoque para empezar a medir tu tiempo.`) → el botón primario normal de la pantalla | `0 min`, `0 días`, `0%`, gráfico con ejes vacíos |
| **Con datos pero día sin actividad** | Valores reales con `0` SOLO en la columna "hoy", el resto con sus datos; la racha muestra la casilla de hoy pendiente | Sustituir toda la pantalla por el estado vacío |

Reglas duras derivadas de las tres apps:
- **La cabecera y la navegación nunca se sustituyen.** Solo cambia el área de contenido. (Swiggy)
- **El estado vacío usa el mismo contenedor y el mismo botón primario** que el estado con datos. (Skyscanner)
- **Nunca un cero crudo sin contexto**: si no hay ninguna sesión histórica, se muestra el bloque vacío; los ceros
  solo son legítimos cuando existen datos con los que compararlos.
- **Timeout explícito**: si la sesión de usuario no resuelve, el skeleton pasa a un estado de error accionable
  (`No pudimos cargar tus datos` + `Reintentar`), nunca a un spinner permanente.

### D. Lista de "no hacer" confirmada por las referencias
- Ningún emoji como icono — todas las apps usan iconos vectoriales de trazo consistente.
- Ningún gradiente neón, glow ni cristal — todas usan relleno opaco + borde/divisor de 1px.
- Ninguna tarjeta "hero" con tratamiento distinto — una sola anatomía repetida.
- Ningún radio arbitrario — el mismo radio en tarjeta, skeleton, casilla de racha y botón.
- Ningún confeti ni animación de recompensa — el color y el número hacen ese trabajo.
