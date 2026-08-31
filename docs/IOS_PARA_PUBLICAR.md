# Subir Momentum a la App Store: lo que falta y quién puede hacerlo

Estado del código: la app iOS existe, compila en Debug y en Release, arranca en el
simulador y funciona en cinco idiomas. Lo que queda son trámites y firmas que
**solo puede hacer el titular de la cuenta de desarrollador**, más una decisión de
producto sobre el tiempo de pantalla.

Esta lista está en orden. El paso 1 es el más lento, así que conviene empezar por
él aunque el resto no esté listo.

---

## 1. Solicitar el permiso Family Controls a Apple (empieza YA)

**Por qué importa.** En iOS, medir el tiempo de uso de otras apps y bloquearlas
está detrás de un permiso especial. No es una opción que se active en el proyecto:
Apple lo concede a mano, app por app. Sin él, esas funciones de Momentum no
existen en iPhone. Las demás --enfoque, vida en semanas, progreso, avisos-- sí
funcionan.

**Qué hay que hacer.**

1. Entra en el [formulario de solicitud de Family Controls (distribución)](https://developer.apple.com/contact/request/family-controls-distribution).
2. Lo tiene que enviar **el titular de la cuenta de organización**. Ninguna otra
   persona del equipo puede.
3. Se solicita **por cada objetivo** que lo necesite. Para Momentum serían cuatro
   identificadores: la app y sus tres extensiones (el monitor de actividad, la
   acción del escudo y su apariencia).
4. Explica en la solicitud para qué se usa: una app de bienestar digital que
   ayuda a la persona a limitar su propio uso, con los datos en el dispositivo.

**Cuánto tarda.** Entre **1 y 5 semanas** según la propia documentación y los
foros de Apple. No es inmediato y no se puede acelerar.

**Mientras esperas.** La app se puede publicar sin esas funciones. Lo que NO tiene
sentido es escribir ahora su código: las APIs de Apple fallan incluso en el
simulador sin el permiso, así que sería código imposible de probar. En cuanto
llegue la aprobación, esa capa se escribe y se verifica de verdad.

---

## 2. Cuenta de desarrollador y ficha de la app

- Cuenta de Apple Developer Program activa (99 USD al año). La de Google Play no
  sirve.
- Crear la app en App Store Connect con el identificador **`com.momentummm.app`**,
  que es el que ya lleva el proyecto.
- Nombre, subtítulo, categoría y descripción. La descripción de Google Play sirve
  de base, pero conviene quitar cualquier mención a funciones que iOS no tenga
  todavía.

---

## 3. Firma

Momentum ya pasó por esto en Android y salió caro: la clave de subida se perdió y
hubo que pedir a Google que la restableciera. En iOS el riesgo es el mismo.

- Apple ofrece **firma gestionada** («Xcode Managed Signing»), que es la opción
  recomendada precisamente porque Apple guarda la clave y no se puede perder.
- Si en cambio se genera un certificado propio, hay que **guardar copia de
  seguridad del certificado y su clave privada** fuera del ordenador. Sin ellos no
  se puede volver a subir una actualización.

En el proyecto la configuración de firma está en automático, así que basta con
elegir el equipo de desarrollo al abrirlo en Xcode.

---

## 4. Material de la ficha

- **Icono**: ya está listo, 1024×1024 sin transparencia, derivado del logo de la
  app.
- **Capturas**: Apple pide al menos una por tamaño de pantalla. Se pueden generar
  del simulador; las que hay en el repositorio sirven de punto de partida.
- **Política de privacidad**: hay una en el repositorio (`PRIVACY_POLICY.md`) que
  hay que publicar en una dirección web accesible, porque App Store Connect pide
  el enlace.
- **Cuestionario de privacidad**: hay que declarar qué datos recoge la app. Hoy la
  versión iOS guarda la fecha de nacimiento y las preferencias **en el
  dispositivo** y no recoge nada más; el manifiesto del proyecto ya lo declara así
  y el rastreo publicitario está en «no».

---

## 5. Qué falta en el código (no bloquea la subida)

Por orden de valor:

1. **Tiempo de pantalla y bloqueo de apps**, en cuanto Apple conceda el permiso
   del paso 1.
2. **Pantalla de ajustes** en iOS: cambiar la expectativa de vida de la
   cuadrícula, el tema y el idioma.
3. **Sincronización con la cuenta** (Appwrite), para que el progreso viaje entre
   Android e iOS. El servidor ya está vivo y accesible.
4. **Unificar el cronómetro de Android** con el compartido: hoy el servicio de
   Android conserva su propia copia de las reglas. Funciona, pero son dos sitios
   donde arreglar lo mismo.

---

## Resumen honesto

Puedes subir Momentum a la App Store en cuanto tengas la cuenta y el material de
la ficha. La app que subirías tendría el enfoque con descansos y avisos, la vida
en semanas y el progreso con racha, en cinco idiomas y accesible con VoiceOver.

Las funciones de tiempo de pantalla y bloqueo llegarán en una segunda versión,
cuando Apple conceda el permiso. Esa espera empieza el día que envíes el
formulario del paso 1, así que ese es el primer movimiento.
