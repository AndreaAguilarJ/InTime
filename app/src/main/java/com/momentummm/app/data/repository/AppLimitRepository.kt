package com.momentummm.app.data.repository

import android.content.Context
import com.momentummm.app.data.dao.AppLimitDao
import com.momentummm.app.data.entity.AppLimit
import com.momentummm.app.service.AppMonitoringService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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

    fun getAllEnabledLimits(): Flow<List<AppLimit>> = appLimitDao.getAllEnabledLimits()

    fun getAllLimits(): Flow<List<AppLimit>> = appLimitDao.getAllLimits()

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

    suspend fun updateAppLimit(packageName: String, dailyLimitMinutes: Int) {
        appLimitDao.updateDailyLimit(packageName, dailyLimitMinutes)
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
        val enabledLimits = appLimitDao.getAllEnabledLimits().first()
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
        val limits = appLimitDao.getAllEnabledLimits().first()
        val remainingTimes = mutableMapOf<String, Int>()

        limits.forEach { limit: AppLimit ->
            remainingTimes[limit.packageName] = getRemainingTime(limit.packageName)
        }

        return remainingTimes
    }

    // Verificar si hay apps que han excedido sus límites
    suspend fun getOverLimitApps(): List<AppLimit> {
        val enabledLimits = appLimitDao.getAllEnabledLimits().first()
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
        val allLimits = appLimitDao.getAllLimits().first()
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
}

data class LimitsSummary(
    val totalApps: Int,
    val enabledApps: Int,
    val overLimitApps: Int,
    val averageLimitMinutes: Int,
    val totalLimitMinutes: Int
)
