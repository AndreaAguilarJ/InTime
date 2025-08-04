package com.momentum.app.data.repository

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.momentum.app.data.entity.AppUsage
import java.util.*

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val totalTimeInMillis: Long,
    val lastTimeUsed: Long
)

class UsageStatsRepository(private val context: Context) {

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
            .filter { it.totalTimeInForeground > 0 }
            .mapNotNull { stats ->
                try {
                    val appInfo = packageManager.getApplicationInfo(stats.packageName, 0)
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    
                    // Filter out system apps if desired
                    if (appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0 || isImportantSystemApp(stats.packageName)) {
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

    private fun isImportantSystemApp(packageName: String): Boolean {
        val importantSystemApps = setOf(
            "com.android.chrome",
            "com.google.android.youtube",
            "com.google.android.gm", // Gmail
            "com.google.android.apps.maps",
            "com.android.dialer",
            "com.android.mms", // Messages
            "com.android.camera2"
        )
        return importantSystemApps.contains(packageName)
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

        return usageStats.sumOf { it.totalTimeInForeground }
    }
}