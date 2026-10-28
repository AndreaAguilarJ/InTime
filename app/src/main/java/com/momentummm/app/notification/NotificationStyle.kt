package com.momentummm.app.notification

import android.content.Context
import androidx.core.content.ContextCompat
import com.momentummm.app.R

/**
 * Aspecto común de las notificaciones de Momentum.
 *
 * Antes cada aviso usaba un icono distinto (un círculo de «información»
 * genérico, el primer plano del icono adaptativo a 108 dp, iconos del sistema
 * como `star_big_on`) y ninguno llevaba color, así que en la barra de estado no
 * se reconocía de qué app venía. Ahora todos llevan la silueta de la app en
 * 24 dp, monocroma como exige Android, y el índigo de «Mi vida en semanas».
 */
object NotificationStyle {

    val SMALL_ICON: Int = R.drawable.ic_stat_momentum

    fun accentColor(context: Context): Int =
        ContextCompat.getColor(context, R.color.notification_accent)
}
