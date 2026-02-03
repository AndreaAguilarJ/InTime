package com.momentummm.app.data.repository

import android.content.Context
import com.momentummm.app.data.dao.AppLimitDao
import com.momentummm.app.data.entity.AppLimit
import com.momentummm.app.service.AppMonitoringService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class AppLimitRepository @Inject constructor(
    private val appLimitDao: AppLimitDao,
    @ApplicationContext private val context: Context,
    private val usageStatsRepository: UsageStatsRepository,
    private val appWhitelistRepository: AppWhitelistRepository
) {

    fun getAllEnabledLimits(): Flow<List<AppLimit>> = 
        appLimitDao.getAllEnabledLimits().distinctUntilChanged()

    fun getAllLimits(): Flow<List<AppLimit>> = 
        appLimitDao.getAllLimits().distinctUntilChanged()

    suspend fun getLimitByPackage(packageName: String): AppLimit? =
        appLimitDao.getLimitByPackage(packageName)

    suspend fun addAppLimit(packageName: String, appName: String, dailyLimitMinutes: Int) {
        val appLimit = AppLimit(
            packageName = packageName,
            appName = appName,
            dailyLimitMinutes = dailyLimitMinutes,
            isEnabled = true
        )
        appLimitDao.insertOrUpdateLimit(appLimit)

        // Iniciar el servicio de monitoreo automáticamente
        ensureMonitoringServiceRunning()
    }

    /**
     * Actualiza el límite diario de una app.
     * 
     * IMPORTANTE: Esta función verifica si el límite ya fue excedido hoy.
     * Si fue excedido, NO permite la edición (los usuarios se quejan de 
     * poder cambiar los límites cuando ya se han pasado).
     * 
     * @return true si se actualizó exitosamente, false si la edición está bloqueada
     */
    suspend fun updateAppLimit(packageName: String, dailyLimitMinutes: Int): Boolean {
        // Verificar si el límite ya fue excedido hoy
        val currentLimit = getLimitByPackage(packageName)
        if (currentLimit?.isEditBlocked() == true) {
            // El límite ya fue excedido hoy, no permitir edición
            android.util.Log.w("AppLimitRepository", 
                "Edición bloqueada para $packageName: límite ya excedido hoy")
            return false
        }
        
        appLimitDao.updateDailyLimit(packageName, dailyLimitMinutes)
        return true
    }
    
    /**
     * Actualiza el límite diario de una app, forzando la actualización
     * incluso si el límite ya fue excedido. Usar con cuidado.
     */
    suspend fun forceUpdateAppLimit(packageName: String, dailyLimitMinutes: Int) {
        appLimitDao.updateDailyLimit(packageName, dailyLimitMinutes)
    }
    
    /**
     * Verifica si la edición del límite está bloqueada para una app.
     */
    suspend fun isEditBlocked(packageName: String): Boolean {
        val limit = getLimitByPackage(packageName) ?: return false
        return limit.isEditBlocked()
    }
    
    /**
     * Marca una app como que ha excedido su límite.
     * Esto bloquea la edición del límite por el resto del día.
     */
    suspend fun markAsExceeded(packageName: String) {
        val currentLimit = getLimitByPackage(packageName) ?: return
        val updatedLimit = currentLimit.withExceeded()
        appLimitDao.insertOrUpdateLimit(updatedLimit)
    }

    suspend fun toggleAppLimit(packageName: String, enabled: Boolean) {
        appLimitDao.updateLimitEnabled(packageName, enabled)

        // Si se habilita un límite, asegurar que el servicio esté corriendo
        if (enabled) {
            ensureMonitoringServiceRunning()
        }
    }

    suspend fun removeAppLimit(appLimit: AppLimit) {
        appLimitDao.deleteLimit(appLimit)
    }

    /**
     * Asegura que el servicio de monitoreo esté corriendo si hay límites habilitados
     */
    private suspend fun ensureMonitoringServiceRunning() {
        val enabledLimits = withTimeoutOrNull(5000L) { 
            appLimitDao.getAllEnabledLimits().first() 
        } ?: emptyList()
        if (enabledLimits.isNotEmpty()) {
            AppMonitoringService.startService(context)
        }
    }

    // Obtener apps instaladas que pueden ser bloqueadas
    suspend fun getInstallableApps(): List<AppUsageInfo> {
        val packageManager = context.packageManager
        val launcherIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val activities = packageManager.queryIntentActivities(launcherIntent, 0)

        return activities
            .mapNotNull { resolveInfo ->
                try {
                    val packageName = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                    val appName = packageManager.getApplicationLabel(
                        packageManager.getApplicationInfo(packageName, 0)
                    ).toString()
                    AppUsageInfo(
                        packageName = packageName,
                        appName = appName,
                        totalTimeInMillis = 0L,
                        lastTimeUsed = 0L
                    )
                } catch (_: Exception) {
                    null
                }
            }
            .distinctBy { it.packageName }
            .sortedBy { it.appName }
    }

    // Verificar si una app ha excedido su límite diario
    suspend fun isAppOverLimit(packageName: String): Boolean {
        // Verificar primero si está en whitelist
        if (appWhitelistRepository.isAppWhitelisted(packageName)) {
            return false
        }

        val limit = getLimitByPackage(packageName) ?: return false
        if (!limit.isEnabled) return false

        val todayUsageStats = usageStatsRepository.getTodayUsageStats()
        val appUsage = todayUsageStats.find { it.packageName == packageName }

        if (appUsage != null) {
            val usageMinutes = appUsage.totalTimeInMillis / (1000 * 60)
            return usageMinutes >= limit.dailyLimitMinutes
        }

        return false
    }

    // Obtener tiempo restante para una app
    suspend fun getRemainingTime(packageName: String): Int {
        val limit = getLimitByPackage(packageName) ?: return Int.MAX_VALUE
        if (!limit.isEnabled) return Int.MAX_VALUE

        val todayUsageStats = usageStatsRepository.getTodayUsageStats()
        val appUsage = todayUsageStats.find { it.packageName == packageName }

        if (appUsage != null) {
            val usageMinutes = appUsage.totalTimeInMillis / (1000 * 60)
            return maxOf(0, limit.dailyLimitMinutes - usageMinutes.toInt())
        }

        return limit.dailyLimitMinutes
    }

    // Obtener estadísticas de tiempo restante para todas las apps con límites
    suspend fun getAllRemainingTimes(): Map<String, Int> {
        val limits = withTimeoutOrNull(5000L) { 
            appLimitDao.getAllEnabledLimits().first() 
        } ?: emptyList()
        val remainingTimes = mutableMapOf<String, Int>()

        limits.forEach { limit: AppLimit ->
            remainingTimes[limit.packageName] = getRemainingTime(limit.packageName)
        }

        return remainingTimes
    }

    // Verificar si hay apps que han excedido sus límites
    suspend fun getOverLimitApps(): List<AppLimit> {
        val enabledLimits = withTimeoutOrNull(5000L) { 
            appLimitDao.getAllEnabledLimits().first() 
        } ?: emptyList()
        return enabledLimits.filter { limit: AppLimit ->
            isAppOverLimit(limit.packageName)
        }
    }

    // Obtener el progreso de uso de una app (0.0 a 1.0)
    suspend fun getUsageProgress(packageName: String): Float {
        val limit = getLimitByPackage(packageName) ?: return 0f
        if (!limit.isEnabled) return 0f

        val todayUsageStats = usageStatsRepository.getTodayUsageStats()
        val appUsage = todayUsageStats.find { it.packageName == packageName }

        return if (appUsage != null && limit.dailyLimitMinutes > 0) {
            val usageMinutes = appUsage.totalTimeInMillis / (1000 * 60)
            (usageMinutes.toFloat() / limit.dailyLimitMinutes.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    // Obtener resumen de límites
    suspend fun getLimitsSummary(): LimitsSummary {
        val allLimits = withTimeoutOrNull(5000L) { 
            appLimitDao.getAllLimits().first() 
        } ?: emptyList()
        val enabledLimits = allLimits.filter { it.isEnabled }
        val overLimitApps = getOverLimitApps()

        val totalTimeLimit = enabledLimits.sumOf { it.dailyLimitMinutes }
        val averageLimit = if (enabledLimits.isNotEmpty()) {
            totalTimeLimit / enabledLimits.size
        } else 0

        return LimitsSummary(
            totalApps = allLimits.size,
            enabledApps = enabledLimits.size,
            overLimitApps = overLimitApps.size,
            averageLimitMinutes = averageLimit,
            totalLimitMinutes = totalTimeLimit
        )
    }
    
    // ============================================================
    // BLOQUEO POR HORARIO
    // ============================================================
    // Feature solicitada: "block certain apps... at a certain time"
    
    /**
     * Verifica si una app está bloqueada por horario.
     * Esto es independiente del límite de tiempo diario.
     */
    suspend fun isAppBlockedBySchedule(packageName: String): Boolean {
        val limit = getLimitByPackage(packageName) ?: return false
        if (!limit.isEnabled) return false
        return limit.isWithinScheduleBlock()
    }
    
    /**
     * Obtiene todas las apps actualmente bloqueadas por horario.
     */
    suspend fun getAppsBlockedBySchedule(): List<AppLimit> {
        val enabledLimits = withTimeoutOrNull(5000L) {
            appLimitDao.getAllEnabledLimits().first()
        } ?: emptyList()
        
        return enabledLimits.filter { it.isWithinScheduleBlock() }
    }
    
    /**
     * Actualiza el horario de bloqueo de una app.
     */
    suspend fun updateScheduleLimit(
        packageName: String,
        hasScheduleLimit: Boolean,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        daysOfWeek: String
    ) {
        val currentLimit = getLimitByPackage(packageName) ?: return
        val updatedLimit = currentLimit.withScheduleLimit(
            enabled = hasScheduleLimit,
            startHour = startHour,
            startMinute = startMinute,
            endHour = endHour,
            endMinute = endMinute,
            daysOfWeek = daysOfWeek
        )
        appLimitDao.insertOrUpdateLimit(updatedLimit)
    }
    
    /**
     * Verifica si una app debería estar bloqueada (por cualquier razón).
     * Combina verificación de límite diario + bloqueo por horario.
     */
    suspend fun shouldAppBeBlocked(packageName: String): Boolean {
        // Primero verificar whitelist
        if (appWhitelistRepository.isAppWhitelisted(packageName)) {
            return false
        }
        
        val limit = getLimitByPackage(packageName) ?: return false
        if (!limit.isEnabled) return false
        
        // Verificar bloqueo por horario
        if (limit.isWithinScheduleBlock()) {
            return true
        }
        
        // Verificar límite diario
        return isAppOverLimit(packageName)
    }
    
    /**
     * Obtiene el motivo por el cual una app está bloqueada.
     */
    suspend fun getBlockReason(packageName: String): BlockReason? {
        val limit = getLimitByPackage(packageName) ?: return null
        if (!limit.isEnabled) return null
        
        // Verificar bloqueo por horario primero
        if (limit.isWithinScheduleBlock()) {
            return BlockReason.SCHEDULE_BLOCK(
                startTime = limit.getScheduleFormatted().split(" - ").first(),
                endTime = limit.getScheduleFormatted().split(" - ").last()
            )
        }
        
        // Verificar límite diario
        if (isAppOverLimit(packageName)) {
            return BlockReason.DAILY_LIMIT_EXCEEDED(limit.dailyLimitMinutes)
        }
        
        return null
    }
}

/**
 * Razón por la cual una app está bloqueada.
 */
sealed class BlockReason {
    data class DAILY_LIMIT_EXCEEDED(val limitMinutes: Int) : BlockReason()
    data class SCHEDULE_BLOCK(val startTime: String, val endTime: String) : BlockReason()
    data class CATEGORY_LIMIT(val categoryName: String, val limitMinutes: Int) : BlockReason()
    object FOCUS_MODE : BlockReason()
    object NUCLEAR_MODE : BlockReason()
}

data class LimitsSummary(
    val totalApps: Int,
    val enabledApps: Int,
    val overLimitApps: Int,
    val averageLimitMinutes: Int,
    val totalLimitMinutes: Int
)
