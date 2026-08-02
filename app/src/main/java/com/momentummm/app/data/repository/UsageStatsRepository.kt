package com.momentummm.app.data.repository

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import com.momentummm.app.data.entity.AppUsage
import com.momentummm.app.data.UserPreferencesRepository
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val totalTimeInMillis: Long,
    val lastTimeUsed: Long
)

@Singleton
class UsageStatsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val usageStatsManager: UsageStatsManager? =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    // Cache para evitar consultas repetidas
    @Volatile
    private var cachedTodayStats: List<AppUsageInfo>? = null
    @Volatile
    private var lastCacheTime: Long = 0L
    private val CACHE_DURATION_MS = 30_000L // 30 segundos de caché

    // Cache del PackageManager para evitar lookups repetidos
    // Usar ConcurrentHashMap para evitar ConcurrentModificationException
    private val appNameCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    /**
     * Obtiene la hora de inicio del día configurada por el usuario.
     * 
     * CRITICAL FIX: Antes usaba runBlocking que causaba ANR en el main thread.
     * Ahora usa un valor cacheado que se actualiza periódicamente.
     * Por defecto es 0:00 (medianoche).
     */
    @Volatile
    private var cachedDayStartHour: Int = 0
    @Volatile  
    private var cachedDayStartMinute: Int = 0
    @Volatile
    private var dayStartLastLoaded: Long = 0L
    private val DAY_START_CACHE_MS = 60_000L // Recargar cada 60 segundos
    
    private fun getDayStartTime(): Pair<Int, Int> {
        val now = System.currentTimeMillis()
        if (now - dayStartLastLoaded > DAY_START_CACHE_MS) {
            // Cargar desde SharedPreferences directamente (no DataStore) para evitar suspend
            try {
                val prefs = context.getSharedPreferences("momentum_user_prefs_cache", android.content.Context.MODE_PRIVATE)
                cachedDayStartHour = prefs.getInt("day_start_hour", 0)
                cachedDayStartMinute = prefs.getInt("day_start_minute", 0)
                dayStartLastLoaded = now
            } catch (e: Exception) {
                // Fallback a medianoche si hay error
                cachedDayStartHour = 0
                cachedDayStartMinute = 0
            }
        }
        return cachedDayStartHour to cachedDayStartMinute
    }
    
    /**
     * Actualiza el cache de dayStart cuando se cambia la preferencia.
     * Debe ser llamado desde UserPreferencesRepository.setDayStartTime()
     */
    fun updateDayStartCache(hour: Int, minute: Int) {
        cachedDayStartHour = hour
        cachedDayStartMinute = minute
        dayStartLastLoaded = System.currentTimeMillis()
        // También guardar en SharedPreferences para persistencia
        try {
            context.getSharedPreferences("momentum_user_prefs_cache", android.content.Context.MODE_PRIVATE)
                .edit()
                .putInt("day_start_hour", hour)
                .putInt("day_start_minute", minute)
                .apply()
        } catch (e: Exception) {
            Log.e("UsageStatsRepository", "Error saving day start cache", e)
        }
        // Invalidar el cache de stats para que se recalcule con el nuevo horario
        invalidateCache()
        // DailyUsageCalculator mantiene su propio cache de la hora de inicio de
        // día (se consulta una o dos veces por segundo desde el monitor). Sin
        // esto, cambiar el ajuste tardaría hasta un minuto en aplicarse al
        // cálculo del límite diario.
        com.momentummm.app.data.usage.DailyUsageCalculator.invalidate()
    }

    /**
     * Calcula el timestamp de inicio del "día" actual basándose en la 
     * preferencia del usuario para dayStartHour.
     * 
     * Por ejemplo, si dayStartHour = 4:
     * - A las 3:00 AM, el "día" aún es del día anterior (empezó a las 4:00 AM ayer)
     * - A las 5:00 AM, el "día" es de hoy (empezó a las 4:00 AM hoy)
     */
    private fun calculateDayStartTimestamp(): Long {
        val (startHour, startMinute) = getDayStartTime()
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        
        // Si la hora actual es antes de la hora de inicio del día,
        // entonces el "día" empezó ayer a esa hora
        val currentTimeInMinutes = currentHour * 60 + currentMinute
        val startTimeInMinutes = startHour * 60 + startMinute
        
        if (currentTimeInMinutes < startTimeInMinutes) {
            // Retroceder un día
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        
        calendar.set(Calendar.HOUR_OF_DAY, startHour)
        calendar.set(Calendar.MINUTE, startMinute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        return calendar.timeInMillis
    }

    fun getTodayUsageStats(): List<AppUsageInfo> {
        val now = System.currentTimeMillis()
        cachedTodayStats?.let { cached ->
            if (now - lastCacheTime < CACHE_DURATION_MS) {
                return cached
            }
        }
        
        // Usar la hora de inicio del día personalizada en lugar de 00:00
        val startTime = calculateDayStartTimestamp()
        val endTime = System.currentTimeMillis()

        val stats = getUsageStats(startTime, endTime)
        cachedTodayStats = stats
        lastCacheTime = System.currentTimeMillis()
        return stats
    }

    fun invalidateCache() {
        cachedTodayStats = null
        lastCacheTime = 0L
    }

    fun getWeeklyUsageStats(): List<AppUsageInfo> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        return getUsageStats(startTime, endTime)
    }

    private fun getUsageStats(startTime: Long, endTime: Long): List<AppUsageInfo> {
        val usageStats = usageStatsManager?.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: return emptyList()

        val packageManager = context.packageManager
        
        // Agrupar por packageName para sumar todos los usos de la misma app
        val aggregatedStats = usageStats
            .filter { it.totalTimeInForeground > 60000 } // Filtrar apps con menos de 1 minuto de uso
            .groupBy { it.packageName }
            .mapNotNull { (packageName, statsList) ->
                try {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    
                    // Incluir apps importantes y apps no del sistema
                    if (shouldIncludeApp(packageName, appInfo)) {
                        // Usar caché para nombres de apps
                        val appName = appNameCache.getOrPut(packageName) {
                            packageManager.getApplicationLabel(appInfo).toString()
                        }
                        
                        // Sumar todo el tiempo de uso de todas las entradas de esta app
                        val totalTime = statsList.sumOf { it.totalTimeInForeground }
                        // Usar el último tiempo de uso más reciente
                        val lastUsed = statsList.maxOf { it.lastTimeUsed }
                        
                        AppUsageInfo(
                            packageName = packageName,
                            appName = appName,
                            totalTimeInMillis = totalTime,
                            lastTimeUsed = lastUsed
                        )
                    } else null
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
            }
            .sortedByDescending { it.totalTimeInMillis }
        
        return aggregatedStats
    }

    private fun shouldIncludeApp(packageName: String, appInfo: ApplicationInfo): Boolean {
        // Excluir aplicaciones específicas que no son relevantes
        val excludedApps = setOf(
            "android",
            "com.android.systemui",
            "com.android.launcher",
            "com.android.settings",
            context.packageName // Excluir nuestra propia app
        )

        if (excludedApps.contains(packageName)) {
            return false
        }

        // Incluir apps importantes del sistema
        val importantSystemApps = setOf(
            "com.android.chrome",
            "com.google.android.youtube",
            "com.google.android.gm", // Gmail
            "com.google.android.apps.maps",
            "com.android.dialer",
            "com.android.mms", // Messages
            "com.android.camera2",
            "com.google.android.apps.photos",
            "com.whatsapp",
            "com.instagram.android",
            "com.facebook.katana",
            "com.twitter.android",
            "com.spotify.music",
            "com.netflix.mediaclient"
        )

        // Si es una app importante del sistema, incluirla
        if (importantSystemApps.contains(packageName)) {
            return true
        }

        // Si no es una app del sistema, incluirla
        return (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
    }

    fun getTotalScreenTime(): Long {
        // Usar la hora de inicio del día personalizada
        val startTime = calculateDayStartTimestamp()
        val endTime = System.currentTimeMillis()

        return getTotalScreenTime(startTime, endTime)
    }

    private fun getTotalScreenTime(startTime: Long, endTime: Long): Long {
        val usageStats = usageStatsManager?.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: return 0L

        return usageStats
            .filter { it.totalTimeInForeground > 0 }
            .filter { stats ->
                try {
                    val appInfo = context.packageManager.getApplicationInfo(stats.packageName, 0)
                    shouldIncludeApp(stats.packageName, appInfo)
                } catch (e: PackageManager.NameNotFoundException) {
                    false
                }
            }
            .sumOf { it.totalTimeInForeground }
    }
}