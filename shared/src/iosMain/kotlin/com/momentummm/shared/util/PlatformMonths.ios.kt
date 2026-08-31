package com.momentummm.shared.util

import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

actual fun shortMonthNames(): List<String> {
    // Se usa la PREFERENCIA DE IDIOMA y no `currentLocale`, por dos razones
    // comprobadas en el simulador:
    //   1. Un `NSDateFormatter` recién creado no hereda idioma alguno y devolvía
    //      los meses en inglés dentro de una app en francés.
    //   2. `currentLocale` mezcla idioma y región, así que a quien tiene el
    //      teléfono en francés con región de Estados Unidos le habrían salido los
    //      meses en inglés aunque el resto de la app estuviera en francés.
    // `preferredLanguages` es la misma fuente que elige el idioma de la interfaz,
    // así que los meses y los textos siempre coinciden.
    val idioma = (NSLocale.preferredLanguages.firstOrNull() as? String) ?: "en"
    val formatter = NSDateFormatter().apply {
        setLocale(NSLocale(localeIdentifier = idioma))
    }
    val simbolos = formatter.shortMonthSymbols
    // En teoría son doce. Si alguna vez llegara vacío o incompleto, es mejor
    // mostrar el número del mes que dejar el selector en blanco.
    return (0 until 12).map { indice ->
        (simbolos.getOrNull(indice) as? String) ?: (indice + 1).toString()
    }
}
