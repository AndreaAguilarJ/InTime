package com.momentummm.shared

/**
 * Identifica en qué sistema se está ejecutando el código compartido.
 *
 * Existe para dos cosas: comprobar que la cadena de compilación
 * multiplataforma realmente produce binarios para Android y para iOS, y dar a
 * la capa compartida una forma honesta de decir qué plataforma la aloja cuando
 * una función solo existe en una de las dos.
 */
expect object Plataforma {
    /** Nombre legible del sistema, por ejemplo `Android 34` o `iOS 18.0`. */
    val nombre: String

    /** `true` cuando el proceso corre sobre iOS o iPadOS. */
    val esIOS: Boolean
}
