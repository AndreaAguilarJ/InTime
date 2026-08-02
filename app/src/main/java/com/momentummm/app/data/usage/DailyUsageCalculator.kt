package com.momentummm.app.data.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import java.util.Calendar

/**
 * Calcula el tiempo REAL en primer plano de una app durante el "día" actual.
 *
 * ─── POR QUÉ EXISTE ESTA CLASE ────────────────────────────────────────────
 * El bloqueo por límite de tiempo nunca se disparaba porque
 * [AppMonitoringService] calculaba el uso así:
 *
 *     queryUsageStats(INTERVAL_DAILY, start, now)
 *         .find { it.packageName == pkg }      // ← BUG
 *         ?.totalTimeInForeground
 *
 * `queryUsageStats` con INTERVAL_DAILY devuelve VARIOS buckets por paquete.
 * `find` se queda con el primero, así que el uso quedaba gravemente
 * subestimado (a menudo casi cero) y `uso >= límite` casi nunca era cierto.
 *
 * Aquí el uso se deriva de [UsageStatsManager.queryEvents], que es la fuente
 * precisa: se suman los intervalos RESUMED → PAUSED/STOPPED. Si el permiso de
 * uso no está concedido o no hay eventos, se cae a `queryUsageStats` pero
 * SUMANDO todos los buckets del paquete.
 */
object DailyUsageCalculator {

    private const val TAG = "DailyUsageCalculator"

    /** Mismas claves que usa UsageStatsRepository, para que el "día" coincida. */
    private const val DAY_START_PREFS = "momentum_user_prefs_cache"
    private const val KEY_DAY_START_HOUR = "day_start_hour"
    private const val KEY_DAY_START_MINUTE = "day_start_minute"

    /** Cache corto para no recorrer los eventos del día en cada tick del monitor. */
    private const val CACHE_TTL_MS = 5_000L

    private data class CacheEntry(val millis: Long, val computedAt: Long, val dayStart: Long)

    private val cache = java.util.concurrent.ConcurrentHashMap<String, CacheEntry>()

    /**
     * Inicio del "día" actual respetando la preferencia de `dayStartHour`
     * del usuario (por defecto medianoche).
     *
     * Si el día empieza a las 4:00 y son las 3:00, el día vigente arrancó
     * ayer a las 4:00.
     */
    fun dayStartMillis(context: Context): Long {
        val (startHour, startMinute) = readDayStart(context)
        val calendar = Calendar.getInstance()

        val nowMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val startMinutes = startHour * 60 + startMinute
        if (nowMinutes < startMinutes) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        calendar.set(Calendar.HOUR_OF_DAY, startHour)
        calendar.set(Calendar.MINUTE, startMinute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun readDayStart(context: Context): Pair<Int, Int> {
        return try {
            val prefs = context.getSharedPreferences(DAY_START_PREFS, Context.MODE_PRIVATE)
            prefs.getInt(KEY_DAY_START_HOUR, 0) to prefs.getInt(KEY_DAY_START_MINUTE, 0)
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo leer la hora de inicio de día, usando medianoche", e)
            0 to 0
        }
    }

    /** Minutos en primer plano hoy. Es el valor que se compara con el límite. */
    fun foregroundMinutesToday(context: Context, packageName: String): Int =
        (foregroundMillisToday(context, packageName) / 60_000L).toInt()

    /**
     * Milisegundos en primer plano durante el día actual, con cache de 5 s.
     */
    fun foregroundMillisToday(context: Context, packageName: String): Long {
        if (packageName.isEmpty()) return 0L

        val now = System.currentTimeMillis()
        val dayStart = dayStartMillis(context)

        // El día forma parte de la clave de validez: al cruzar la hora de
        // inicio del día un valor cacheado pertenece al día anterior.
        cache[packageName]?.let { entry ->
            if (entry.dayStart == dayStart && now - entry.computedAt < CACHE_TTL_MS) {
                return entry.millis
            }
        }

        val computed = computeForegroundMillis(context, packageName, dayStart, now)
        cache[packageName] = CacheEntry(computed, now, dayStart)
        return computed
    }

    /** Fuerza el recálculo en la siguiente consulta (p. ej. al cambiar de día). */
    fun invalidate(packageName: String? = null) {
        if (packageName == null) cache.clear() else cache.remove(packageName)
    }

    private fun computeForegroundMillis(
        context: Context,
        packageName: String,
        dayStart: Long,
        now: Long
    ): Long {
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return 0L

        // Se toma el mayor de las dos fuentes en lugar de quedarse con la
        // primera que devuelva algo. Justo tras un reinicio o al cruzar el
        // límite del día una de las dos puede no tener datos todavía, y
        // subestimar el uso significa no bloquear. Nunca `find`: hay varios
        // buckets por paquete y hay que sumarlos todos.
        val fromEvents = millisFromEvents(manager, packageName, dayStart, now)
        val fromStats = millisFromAggregatedStats(manager, packageName, dayStart, now)
        return maxOf(fromEvents, fromStats)
    }

    /**
     * Suma los intervalos RESUMED → PAUSED/STOPPED del paquete.
     * Si la app sigue en primer plano, cuenta hasta [now].
     */
    private fun millisFromEvents(
        manager: UsageStatsManager,
        packageName: String,
        dayStart: Long,
        now: Long
    ): Long {
        return try {
            val events = manager.queryEvents(dayStart, now) ?: return 0L
            val event = UsageEvents.Event()

            var total = 0L
            var resumedAt = 0L

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.packageName != packageName) continue

                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                        // Sólo el primer RESUMED de una racha abre el intervalo.
                        if (resumedAt == 0L) resumedAt = event.timeStamp
                    }

                    UsageEvents.Event.ACTIVITY_PAUSED,
                    UsageEvents.Event.ACTIVITY_STOPPED -> {
                        if (resumedAt != 0L && event.timeStamp > resumedAt) {
                            total += event.timeStamp - resumedAt
                        }
                        resumedAt = 0L
                    }
                }
            }

            // Intervalo abierto: la app está en primer plano ahora mismo.
            if (resumedAt != 0L && now > resumedAt) {
                total += now - resumedAt
            }

            total
        } catch (e: Exception) {
            Log.e(TAG, "queryEvents falló para $packageName", e)
            0L
        }
    }

    private fun millisFromAggregatedStats(
        manager: UsageStatsManager,
        packageName: String,
        dayStart: Long,
        now: Long
    ): Long {
        return try {
            manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, dayStart, now)
                ?.filter { it.packageName == packageName }
                ?.sumOf { it.totalTimeInForeground }
                ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "queryUsageStats falló para $packageName", e)
            0L
        }
    }
}
