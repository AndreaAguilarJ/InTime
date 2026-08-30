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

    /**
     * Cache corto para no recorrer los eventos del día en cada tick del monitor.
     *
     * Eran 5 segundos. Sumados a un sondeo de 2 s, el usuario podía pasarse
     * hasta 7 segundos del límite antes de que apareciera el bloqueo. Con 2 s
     * el desfase máximo baja a unos 4 s, y quien esté a punto de agotar el
     * límite puede pedir un cálculo fresco con [maxAgeMs] = 0.
     */
    private const val CACHE_TTL_MS = 2_000L

    private data class CacheEntry(val millis: Long, val computedAt: Long, val dayStart: Long)

    private val cache = java.util.concurrent.ConcurrentHashMap<String, CacheEntry>()

    /**
     * Ventana diaria cuyo uso NO debe contarse.
     *
     * Se expresa en minutos desde medianoche y admite cruce de medianoche
     * (23:00–07:00 se guarda como 1380 → 420).
     */
    data class ExcludedWindow(val startMinuteOfDay: Int, val endMinuteOfDay: Int)

    /**
     * Ventana de sueño activa, si el usuario pidió no contar su uso.
     *
     * BUG QUE ESTO CORRIGE: la Ventana de sueño prometía «no contar el uso
     * durante las horas de sueño», pero lo único que hacía era abandonar una
     * comprobación del monitor. Este calculador seguía sumando TODOS los
     * eventos del día, así que al terminar la ventana el uso nocturno
     * reaparecía íntegro en el total y podía bloquear apps por la mañana.
     *
     * La fija [com.momentummm.app.data.manager.SmartBlockingManager] cada vez
     * que la configuración cambia, y es `null` cuando la función está apagada.
     */
    @Volatile
    private var excludedWindow: ExcludedWindow? = null

    /** Ventana excluida vigente. Expuesta para pruebas y diagnóstico. */
    fun excludedWindow(): ExcludedWindow? = excludedWindow

    /**
     * Define (o retira, con `null`) la ventana cuyo uso no se cuenta.
     * Invalida la cache porque el total de cada app cambia al hacerlo.
     */
    fun setExcludedWindow(window: ExcludedWindow?) {
        if (window == excludedWindow) return
        excludedWindow = window
        invalidate()
    }

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

    /**
     * Hora de inicio de día, en cache.
     *
     * [dayStartMillis] se llama en cada consulta de uso —es decir, una o dos
     * veces por segundo mientras el monitor está activo— y cada llamada leía
     * SharedPreferences. StrictMode lo señalaba como lectura de disco en el
     * hilo principal cuando la llamada venía de la creación del servicio.
     */
    @Volatile
    private var dayStartCache: Pair<Int, Int>? = null

    @Volatile
    private var dayStartCacheAt = 0L

    private const val DAY_START_CACHE_TTL_MS = 60_000L

    private fun readDayStart(context: Context): Pair<Int, Int> {
        val now = System.currentTimeMillis()
        dayStartCache?.let { cached ->
            if (now - dayStartCacheAt < DAY_START_CACHE_TTL_MS) return cached
        }

        val value = try {
            val prefs = context.getSharedPreferences(DAY_START_PREFS, Context.MODE_PRIVATE)
            prefs.getInt(KEY_DAY_START_HOUR, 0) to prefs.getInt(KEY_DAY_START_MINUTE, 0)
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo leer la hora de inicio de día, usando medianoche", e)
            0 to 0
        }

        dayStartCache = value
        dayStartCacheAt = now
        return value
    }

    /** Minutos en primer plano hoy. Es el valor que se compara con el límite. */
    fun foregroundMinutesToday(
        context: Context,
        packageName: String,
        maxAgeMs: Long = CACHE_TTL_MS
    ): Int = (foregroundMillisToday(context, packageName, maxAgeMs) / 60_000L).toInt()

    /**
     * Milisegundos en primer plano durante el día actual.
     *
     * @param maxAgeMs antigüedad máxima aceptable del valor cacheado. Con 0 se
     *   fuerza el recálculo: lo usa el monitor cuando el usuario está a punto
     *   de agotar el límite y un valor de hace dos segundos ya no sirve.
     */
    fun foregroundMillisToday(
        context: Context,
        packageName: String,
        maxAgeMs: Long = CACHE_TTL_MS
    ): Long {
        if (packageName.isEmpty()) return 0L

        val now = System.currentTimeMillis()
        val dayStart = dayStartMillis(context)

        // El día forma parte de la clave de validez: al cruzar la hora de
        // inicio del día un valor cacheado pertenece al día anterior.
        if (maxAgeMs > 0) {
            cache[packageName]?.let { entry ->
                if (entry.dayStart == dayStart && now - entry.computedAt < maxAgeMs) {
                    return entry.millis
                }
            }
        }

        val computed = computeForegroundMillis(context, packageName, dayStart, now)
        cache[packageName] = CacheEntry(computed, now, dayStart)
        return computed
    }

    /** Fuerza el recálculo en la siguiente consulta (p. ej. al cambiar de día). */
    fun invalidate(packageName: String? = null) {
        if (packageName == null) {
            cache.clear()
            // La hora de inicio de día puede haber cambiado en los ajustes.
            dayStartCacheAt = 0L
        } else {
            cache.remove(packageName)
        }
    }

    /**
     * Minutos en primer plano desde [sinceMillis] hasta ahora, sin cache.
     *
     * Lo usa el Ayuno digital: su límite se refiere a lo consumido DENTRO de la
     * franja, no al total del día. Medirlo contra el día completo hacía que un
     * usuario que ya había gastado su cuota por la mañana viera la app
     * bloqueada en el primer segundo del ayuno.
     *
     * No se cachea porque `sinceMillis` varía con la franja y una entrada por
     * paquete no podría distinguir dos ventanas distintas.
     */
    fun foregroundMinutesSince(
        context: Context,
        packageName: String,
        sinceMillis: Long
    ): Int {
        if (packageName.isEmpty()) return 0
        val now = System.currentTimeMillis()
        if (sinceMillis >= now) return 0

        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return 0

        // Sólo la vía de eventos sirve aquí: `queryUsageStats` agrega por
        // buckets diarios y no permite acotar una franja arbitraria.
        val breakdown = foregroundFromEvents(manager, packageName, sinceMillis, now)
        val net = (breakdown.totalMillis - breakdown.excludedMillis).coerceAtLeast(0L)
        return (net / 60_000L).toInt()
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
        val events = foregroundFromEvents(manager, packageName, dayStart, now)
        val fromStats = millisFromAggregatedStats(manager, packageName, dayStart, now)

        // La ventana excluida se descuenta de LAS DOS fuentes. Restarla solo de
        // los eventos no serviría: `maxOf` volvería a elegir el agregado, que
        // sigue incluyendo el uso nocturno, y la exclusión quedaría anulada.
        val netFromEvents = (events.totalMillis - events.excludedMillis).coerceAtLeast(0L)
        val netFromStats = (fromStats - events.excludedMillis).coerceAtLeast(0L)
        return maxOf(netFromEvents, netFromStats)
    }

    /** Uso total y porción caída dentro de la ventana excluida. */
    private data class ForegroundBreakdown(val totalMillis: Long, val excludedMillis: Long)

    /**
     * Suma los intervalos RESUMED → PAUSED/STOPPED del paquete.
     * Si la app sigue en primer plano, cuenta hasta [now].
     */
    private fun foregroundFromEvents(
        manager: UsageStatsManager,
        packageName: String,
        dayStart: Long,
        now: Long
    ): ForegroundBreakdown {
        val window = excludedWindow
        return try {
            val events = manager.queryEvents(dayStart, now)
                ?: return ForegroundBreakdown(0L, 0L)
            val event = UsageEvents.Event()

            var total = 0L
            var excluded = 0L
            var resumedAt = 0L

            fun closeInterval(from: Long, to: Long) {
                if (to <= from) return
                total += to - from
                if (window != null) excluded += excludedOverlapMillis(from, to, window)
            }

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
                        if (resumedAt != 0L) {
                            closeInterval(resumedAt, event.timeStamp)
                        }
                        resumedAt = 0L
                    }
                }
            }

            // Intervalo abierto: la app está en primer plano ahora mismo.
            if (resumedAt != 0L) {
                closeInterval(resumedAt, now)
            }

            ForegroundBreakdown(total, excluded)
        } catch (e: Exception) {
            Log.e(TAG, "queryEvents falló para $packageName", e)
            ForegroundBreakdown(0L, 0L)
        }
    }

    /**
     * Milisegundos de `[from, to)` que caen dentro de la ventana diaria dada.
     *
     * La ventana se repite cada día y puede cruzar medianoche, así que se
     * comprueban las apariciones del día anterior, el actual y el siguiente:
     * un intervalo de uso puede empezar antes de la ventana y terminar dentro.
     *
     * El desplazamiento dentro del día se calcula con minutos de 60 s. El
     * inicio de cada día sí usa [Calendar], por lo que solo el día concreto de
     * un cambio de horario de verano puede desviarse una hora; el resto es
     * exacto.
     *
     * `internal` en lugar de `private` para poder probar la aritmética sin
     * necesidad de un `UsageStatsManager` real.
     */
    internal fun excludedOverlapMillis(from: Long, to: Long, window: ExcludedWindow): Long {
        if (to <= from) return 0L
        // Inicio y fin iguales describen una ventana vacía, no un día completo.
        if (window.startMinuteOfDay == window.endMinuteOfDay) return 0L

        val midnight = Calendar.getInstance().apply {
            timeInMillis = from
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        var overlap = 0L
        for (dayOffset in -1..1) {
            val dayMillis = (midnight.clone() as Calendar)
                .apply { add(Calendar.DAY_OF_YEAR, dayOffset) }
                .timeInMillis

            val windowStart = dayMillis + window.startMinuteOfDay * 60_000L
            val endMinute = if (window.endMinuteOfDay > window.startMinuteOfDay) {
                window.endMinuteOfDay
            } else {
                // Cruza medianoche: termina al día siguiente.
                window.endMinuteOfDay + 24 * 60
            }
            val windowEnd = dayMillis + endMinute * 60_000L

            val start = maxOf(from, windowStart)
            val end = minOf(to, windowEnd)
            if (end > start) overlap += end - start
        }
        return overlap
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
