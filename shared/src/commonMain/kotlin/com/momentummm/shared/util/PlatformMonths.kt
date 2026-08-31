package com.momentummm.shared.util

/**
 * Nombres abreviados de los meses, en el idioma y formato del propio sistema.
 *
 * Se piden a la plataforma en vez de traducirlos: son doce por idioma, ya existen
 * en Android y en iOS, y así la app los muestra correctamente incluso en idiomas
 * a los que la interfaz no está traducida. Además respeta la convención regional
 * de cada país, que no siempre coincide con el idioma.
 */
expect fun shortMonthNames(): List<String>
