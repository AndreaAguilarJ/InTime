package com.momentummm.shared.util

import kotlinx.datetime.Clock

/**
 * Segundos desde la época, según el reloj del sistema.
 *
 * A diferencia de las fechas de calendario, esto **sí** es seguro con
 * kotlinx-datetime en iOS: un instante no necesita consultar la base de zonas
 * horarias, que es donde está el fallo que cerraba la app.
 */
fun nowEpochSeconds(): Long = Clock.System.now().epochSeconds
