package com.momentummm.app.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.momentummm.app.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Destino pedido por una notificación que todavía no se ha abierto.
 *
 * MainActivity lo rellena desde el Intent (en `onCreate` y en `onNewIntent`) y
 * `MainAppContent` lo consume en cuanto su grafo de navegación existe. Vive
 * fuera de la composición porque el Intent llega antes de que haya NavHost:
 * el usuario puede estar aún en la pantalla de carga, en el bloqueo de la app
 * o en el modo minimalista, y el destino debe esperar hasta poder aplicarse.
 */
object PendingNavigation {

    private val _destination = MutableStateFlow<String?>(null)
    val destination: StateFlow<String?> = _destination.asStateFlow()

    /** Lee el destino del Intent, lo valida y lo retira para no repetirlo. */
    fun offer(intent: Intent?) {
        val raw = intent?.getStringExtra(NotificationRoutes.EXTRA_DESTINATION) ?: return
        // Se retira del Intent: al girar la pantalla la actividad se recrea con
        // el mismo Intent y volvería a saltar a la pantalla de la notificación.
        intent.removeExtra(NotificationRoutes.EXTRA_DESTINATION)
        NotificationRoutes.sanitize(raw)?.let { _destination.value = it }
    }

    /** Marca [route] como aplicado; no borra un destino más reciente. */
    fun consume(route: String) {
        _destination.compareAndSet(route, null)
    }
}

/** PendingIntents que abren la app en la pantalla adecuada. */
object NotificationIntents {

    /**
     * @param requestCode debe ser único por notificación: dos PendingIntents
     *   con el mismo código y la misma acción son el MISMO para el sistema, y
     *   `FLAG_UPDATE_CURRENT` haría que todos apuntaran al último destino.
     */
    fun open(context: Context, destination: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            // La acción incluye el destino para que dos avisos con distinto
            // destino nunca compartan PendingIntent aunque coincida el código.
            action = "$ACTION_OPEN_PREFIX$destination"
            putExtra(NotificationRoutes.EXTRA_DESTINATION, destination)
            // SINGLE_TOP + CLEAR_TOP reutilizan la actividad abierta (llega a
            // onNewIntent) en vez de destruir la tarea como hacía CLEAR_TASK.
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private const val ACTION_OPEN_PREFIX = "com.momentummm.app.action.OPEN_FROM_NOTIFICATION."
}
