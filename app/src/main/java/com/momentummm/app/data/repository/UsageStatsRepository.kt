package com.momentummm.app.data.repository

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.momentummm.app.data.entity.AppUsage
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

    fun getTodayUsageStats(): List<AppUsageInfo> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        return getUsageStats(startTime, endTime)
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
        
        return usageStats
            .filter { it.totalTimeInForeground > 60000 } // Filtrar apps con menos de 1 minuto de uso
            .mapNotNull { stats ->
                try {
                    val appInfo = packageManager.getApplicationInfo(stats.packageName, 0)
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    
                    // Incluir apps importantes y apps no del sistema
                    if (shouldIncludeApp(stats.packageName, appInfo)) {
                        AppUsageInfo(
                            packageName = stats.packageName,
                            appName = appName,
                            totalTimeInMillis = stats.totalTimeInForeground,
                            lastTimeUsed = stats.lastTimeUsed
                        )
                    } else null
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
            }
            .sortedByDescending { it.totalTimeInMillis }
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
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
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