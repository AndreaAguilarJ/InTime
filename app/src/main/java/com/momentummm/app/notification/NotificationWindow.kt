package com.momentummm.app.notification

/**
 * Decide si un instante cae dentro de la ventana horaria en la que el usuario
 * quiere recibir mensajes.
 *
 * ─── POR QUÉ ES UNA CLASE APARTE ─────────────────────────────────────────
 * Esta comprobación descartaba mensajes legítimos y estaba enterrada dentro de
 * una función que necesita Room, así que no se podía probar. El caso que
 * fallaba: sin permiso de alarmas exactas el sistema puede retrasar una alarma
 * hasta una hora, de modo que un mensaje programado a las 21:40 llegaba a las
 * 22:10 y se tiraba a la basura por "fuera de horario" —el usuario simplemente
 * no recibía nada y no había forma de saber por qué—.
 *
 * El margen [graceMinutes] amplía el final de la ventana para esos retrasos.
 */
object NotificationWindow {

    /**
     * @param nowMinutes minuto del día actual (0..1439).
     * @param startMinutes inicio de la ventana en minutos desde medianoche.
     * @param endMinutes fin de la ventana en minutos desde medianoche.
     * @param graceMinutes margen extra después del fin, para alarmas retrasadas.
     */
    fun isWithin(
        nowMinutes: Int,
        startMinutes: Int,
        endMinutes: Int,
        graceMinutes: Int = 0
    ): Boolean {
        val end = endMinutes + graceMinutes.coerceAtLeast(0)

        // Ventana normal (empieza antes de acabar).
        if (startMinutes < end) {
            return nowMinutes in startMinutes..end
        }

        // Ventana que cruza medianoche (p. ej. 22:00 → 06:00).
        return nowMinutes >= startMinutes || nowMinutes <= end
    }
}
