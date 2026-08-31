package com.momentummm.shared.util

import kotlinx.datetime.LocalDate

/**
 * Fecha de hoy según el calendario del propio sistema.
 *
 * Existe por un fallo concreto, no por gusto: `TimeZone.currentSystemDefault()` de
 * kotlinx-datetime **lanza excepción en iOS** (`IllegalTimeZoneException: Invalid
 * zone ID: Europe/Amsterdam`) porque su base de zonas horarias no resuelve la del
 * sistema en el simulador, y la app se cerraba al arrancar. Compilaba
 * perfectamente: el fallo solo aparece en ejecución.
 *
 * Preguntar la fecha al calendario nativo --`java.time` en Android, `NSCalendar` en
 * iOS-- es además más correcto, porque respeta la configuración regional de la
 * persona sin depender de una tabla de zonas empaquetada aparte.
 */
expect fun todayLocalDate(): LocalDate
