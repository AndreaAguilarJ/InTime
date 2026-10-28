package com.momentummm.app.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.updateAll
import com.momentummm.app.worker.WidgetUpdateWorker

/** Vuelve a pintar los cuatro widgets de Momentum. */
object WidgetRefresher {

    private const val TAG = "WidgetRefresher"

    private fun all(): List<GlanceAppWidget> = listOf(
        LifeWeeksWidget(),
        YearProgressWidget(),
        QuoteWidget(),
        MotivationalMessageWidget(),
    )

    /**
     * Cada widget por separado: antes un fallo en uno (la base de datos ocupada,
     * por ejemplo) cortaba el bucle y los que venían detrás no se actualizaban.
     *
     * @return cuántos widgets fallaron.
     */
    suspend fun refreshAll(context: Context): Int = all().count { widget ->
        runCatching { widget.updateAll(context) }
            .onFailure { Log.e(TAG, "No se pudo actualizar ${widget.javaClass.simpleName}", it) }
            .isFailure
    }
}

/**
 * Cambios del sistema que dejan los widgets desfasados al instante:
 * - idioma del sistema: los textos se resolvieron en el idioma anterior;
 * - hora o zona horaria: el día del año, la frase del día y la medianoche
 *   programada se calcularon con el reloj anterior.
 *
 * Las tres difusiones están exentas de las restricciones de Android 8, así que
 * se pueden declarar en el manifiesto.
 */
class WidgetSystemChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED) return
        WidgetUpdateWorker.requestImmediateUpdate(context)
        WidgetUpdateWorker.scheduleMidnightRefresh(context, WidgetUpdateWorker.MidnightReason.CLOCK_CHANGED)
    }

    private companion object {
        val HANDLED = setOf(
            Intent.ACTION_LOCALE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
    }
}
