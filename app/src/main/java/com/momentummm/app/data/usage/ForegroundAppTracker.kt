package com.momentummm.app.data.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log

/**
 * Fuente única de verdad sobre "qué app está en primer plano ahora".
 *
 * ─── POR QUÉ EXISTE ESTA CLASE ────────────────────────────────────────────
 * [AppMonitoringService] resolvía la app en primer plano con
 * `queryEvents(now - 5000, now)`. Si el usuario llevaba más de 5 segundos
 * dentro de la misma app —el caso normal— esa ventana no contenía ningún
 * evento MOVE_TO_FOREGROUND y la detección caía a un fallback basado en
 * `lastTimeUsed`, poco fiable en Android moderno. Resultado: la app en
 * primer plano se detectaba de forma intermitente y el bloqueo se disparaba
 * sólo por casualidad.
 *
 * Aquí se combinan dos fuentes:
 *
 *  1. **AccessibilityService** (preferente): entrega el paquete de forma
 *     instantánea y exacta en cada cambio de ventana. Si hay un reporte
 *     reciente, se usa.
 *  2. **UsageEvents con ventana amplia** (respaldo): se busca el último
 *     ACTIVITY_RESUMED de los últimos minutos, no de los últimos 5 s.
 */
object ForegroundAppTracker {

    private const val TAG = "ForegroundAppTracker"

    /** Un reporte del servicio de accesibilidad se considera vigente este tiempo. */
    private const val ACCESSIBILITY_REPORT_TTL_MS = 20_000L

    /** Ventana de respaldo: suficiente para cubrir sesiones largas en una misma app. */
    private const val EVENTS_LOOKBACK_MS = 15 * 60_000L

    /**
     * Paquete y momento del último reporte, en una sola referencia atómica.
     *
     * Con dos campos @Volatile separados existía una carrera: un lector podía
     * leer el paquete anterior junto con la marca de tiempo nueva y considerar
     * fresco un dato obsoleto. Un único swap atómico lo hace imposible.
     */
    private val lastReport = java.util.concurrent.atomic.AtomicReference(Report("", 0L))

    private data class Report(val packageName: String, val timestamp: Long)

    /**
     * Lo llama [com.momentummm.app.accessibility.MomentumAccessibilityService]
     * en cada TYPE_WINDOW_STATE_CHANGED.
     */
    fun reportFromAccessibility(packageName: String) {
        if (packageName.isEmpty()) return
        lastReport.set(Report(packageName, System.currentTimeMillis()))
    }

    /** true si el servicio de accesibilidad está alimentando datos frescos. */
    fun hasFreshAccessibilityReport(): Boolean {
        val report = lastReport.get()
        return report.packageName.isNotEmpty() &&
            System.currentTimeMillis() - report.timestamp < ACCESSIBILITY_REPORT_TTL_MS
    }

    /**
     * Paquete en primer plano, o cadena vacía si no se puede determinar.
     */
    fun current(context: Context): String {
        val report = lastReport.get()
        val isFresh = report.packageName.isNotEmpty() &&
            System.currentTimeMillis() - report.timestamp < ACCESSIBILITY_REPORT_TTL_MS
        if (isFresh) return report.packageName
        return lastResumedPackage(context)
    }

    private fun lastResumedPackage(context: Context): String {
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return ""

        return try {
            val now = System.currentTimeMillis()
            val events = manager.queryEvents(now - EVENTS_LOOKBACK_MS, now) ?: return ""
            val event = UsageEvents.Event()

            var latestPackage = ""
            var latestTimestamp = 0L

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val isForegroundEvent = event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                    event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
                if (isForegroundEvent && event.timeStamp >= latestTimestamp) {
                    latestTimestamp = event.timeStamp
                    latestPackage = event.packageName ?: ""
                }
            }

            latestPackage
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo resolver la app en primer plano", e)
            ""
        }
    }
}
