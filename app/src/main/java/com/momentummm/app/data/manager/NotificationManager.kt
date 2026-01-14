package com.momentummm.app.data.manager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.momentummm.app.MainActivity
import com.momentummm.app.R
import com.momentummm.app.data.repository.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

private val Context.notificationPrefs by preferencesDataStore(name = "notification_preferences")

@Singleton
class SmartNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usageStatsRepository: UsageStatsRepository,
    private val goalsRepository: GoalsRepository,
    private val appLimitRepository: AppLimitRepository,
    private val quotesRepository: QuotesRepository,
    private val userRepository: UserRepository
) {

    companion object {
        // Canales de notificación
        const val CHANNEL_ID_LIMITS = "app_limits_channel"
        const val CHANNEL_ID_GOALS = "goals_achievements_channel"
        const val CHANNEL_ID_MOTIVATION = "daily_motivation_channel"
        const val CHANNEL_ID_WEEKLY_SUMMARY = "weekly_summary_channel"
        const val CHANNEL_ID_REMINDERS = "smart_reminders_channel"

        // IDs de notificaciones
        const val NOTIFICATION_ID_APP_LIMIT_WARNING = 2001
        const val NOTIFICATION_ID_APP_LIMIT_REACHED = 2002
        const val NOTIFICATION_ID_DAILY_MOTIVATION = 2003
        const val NOTIFICATION_ID_WEEKLY_SUMMARY = 2004
        const val NOTIFICATION_ID_GOAL_ACHIEVED = 2005
        const val NOTIFICATION_ID_MILESTONE = 2006
        const val NOTIFICATION_ID_SCREEN_TIME_REMINDER = 2007

        // Umbrales
        const val WARNING_THRESHOLD_PERCENT = 80 // Avisar cuando se usa el 80% del límite
        const val EXCESSIVE_USAGE_HOURS = 6 // Considerar uso excesivo después de 6 horas

        // Keys de preferencias
        val APP_LIMITS_ENABLED = booleanPreferencesKey("app_limits_notifications")
        val DAILY_MOTIVATION_ENABLED = booleanPreferencesKey("daily_motivation")
        val WEEKLY_SUMMARY_ENABLED = booleanPreferencesKey("weekly_summary")
        val ACHIEVEMENTS_ENABLED = booleanPreferencesKey("achievements_notifications")
        val SCREEN_TIME_REMINDERS_ENABLED = booleanPreferencesKey("screen_time_reminders")
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        createNotificationChannels()
        scheduleAllNotifications()
    }

    // Funciones auxiliares para verificar preferencias
    private suspend fun isNotificationEnabled(key: androidx.datastore.preferences.core.Preferences.Key<Boolean>): Boolean {
        return context.notificationPrefs.data.first()[key] ?: true
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_ID_LIMITS,
                    "Límites de Aplicaciones",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alertas cuando te acercas o superas los límites de uso de apps"
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_ID_GOALS,
                    "Metas y Logros",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notificaciones sobre metas alcanzadas y logros desbloqueados"
                },
                NotificationChannel(
                    CHANNEL_ID_MOTIVATION,
                    "Motivación Diaria",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Frases motivacionales para mejorar tu bienestar digital"
                },
                NotificationChannel(
                    CHANNEL_ID_WEEKLY_SUMMARY,
                    "Resumen Semanal",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Resumen de tu uso de dispositivo durante la semana"
                },
                NotificationChannel(
                    CHANNEL_ID_REMINDERS,
                    "Recordatorios Inteligentes",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Recordatorios personalizados sobre tu uso del dispositivo"
                }
            )
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            channels.forEach { notificationManager.createNotificationChannel(it) }
        }
    }
    
    /**
     * 1. Sistema de alertas cuando te acercas a límites de apps
     */
    fun checkAppLimitsAndNotify() {
        scope.launch {
            try {
                if (!isNotificationEnabled(APP_LIMITS_ENABLED)) return@launch

                val enabledLimits = appLimitRepository.getAllEnabledLimits().first()
                val todayUsage = usageStatsRepository.getTodayUsageStats()

                enabledLimits.forEach { limit ->
                    val usage = todayUsage.find { it.packageName == limit.packageName }
                    if (usage != null) {
                        val usageMinutes = (usage.totalTimeInMillis / 60000).toInt()
                        val limitMinutes = limit.dailyLimitMinutes
                        val usagePercent = (usageMinutes * 100) / limitMinutes

                        when {
                            // Límite alcanzado
                            usageMinutes >= limitMinutes -> {
                                showAppLimitReachedNotification(
                                    limit.appName,
                                    usageMinutes,
                                    limitMinutes
                                )
                            }
                            // Advertencia al 80%
                            usagePercent >= WARNING_THRESHOLD_PERCENT && usagePercent < 100 -> {
                                showAppLimitWarningNotification(
                                    limit.appName,
                                    usageMinutes,
                                    limitMinutes,
                                    limitMinutes - usageMinutes
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showAppLimitWarningNotification(
        appName: String,
        usageMinutes: Int,
        limitMinutes: Int,
        remainingMinutes: Int
    ) {
        val title = "⚠️ Acercándote al límite"
        val message = "Has usado $appName durante ${formatMinutes(usageMinutes)}. " +
                "Te quedan ${formatMinutes(remainingMinutes)} antes de alcanzar tu límite."

        showNotification(
            channelId = CHANNEL_ID_LIMITS,
            notificationId = NOTIFICATION_ID_APP_LIMIT_WARNING,
            title = title,
            message = message,
            icon = R.drawable.ic_reminder,
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }
    
    private fun showAppLimitReachedNotification(
        appName: String,
        usageMinutes: Int,
        limitMinutes: Int
    ) {
        val title = "🛑 Límite alcanzado"
        val message = "Has alcanzado tu límite diario de $appName (${formatMinutes(limitMinutes)}). " +
                "¡Es momento de tomar un descanso!"

        showNotification(
            channelId = CHANNEL_ID_LIMITS,
            notificationId = NOTIFICATION_ID_APP_LIMIT_REACHED,
            title = title,
            message = message,
            icon = R.drawable.ic_reminder,
            priority = NotificationCompat.PRIORITY_HIGH,
            autoCancel = false
        )
    }

    /**
     * 2. Resumen semanal de uso
     */
    fun sendWeeklySummary() {
        scope.launch {
            try {
                if (!isNotificationEnabled(WEEKLY_SUMMARY_ENABLED)) return@launch

                val weeklyUsage = usageStatsRepository.getWeeklyUsageStats()
                val totalMinutes = weeklyUsage.sumOf { it.totalTimeInMillis } / 60000
                val topApps = weeklyUsage.take(3)
                val goals = goalsRepository.getAllGoals().first()
                val completedGoals = goals.count { it.currentValue >= it.targetValue }

                val title = "📊 Tu Resumen Semanal"
                val message = buildString {
                    append("Tiempo total: ${formatMinutes(totalMinutes.toInt())}\n")
                    append("Apps más usadas:\n")
                    topApps.forEachIndexed { index, app ->
                        append("${index + 1}. ${app.appName}: ${formatMinutes((app.totalTimeInMillis / 60000).toInt())}\n")
                    }
                    if (completedGoals > 0) {
                        append("\n🎯 Metas completadas: $completedGoals")
                    }
                }

                showExpandableNotification(
                    channelId = CHANNEL_ID_WEEKLY_SUMMARY,
                    notificationId = NOTIFICATION_ID_WEEKLY_SUMMARY,
                    title = title,
                    message = message,
                    icon = R.drawable.ic_goal
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 3. Motivación diaria con frases
     */
    fun sendDailyMotivation() {
        scope.launch {
            try {
                if (!isNotificationEnabled(DAILY_MOTIVATION_ENABLED)) return@launch

                val quote = quotesRepository.getRandomQuote()
                if (quote != null) {
                    val title = "💡 Inspiración del Día"
                    val message = "\"${quote.text}\""

                    showNotification(
                        channelId = CHANNEL_ID_MOTIVATION,
                        notificationId = NOTIFICATION_ID_DAILY_MOTIVATION,
                        title = title,
                        message = message,
                        icon = R.drawable.ic_goal,
                        priority = NotificationCompat.PRIORITY_LOW
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 4. Logros por hitos alcanzados
     */
    fun checkAndNotifyAchievements() {
        scope.launch {
            try {
                if (!isNotificationEnabled(ACHIEVEMENTS_ENABLED)) return@launch

                val goals = goalsRepository.getAllGoals().first()

                goals.forEach { goal ->
                    // Meta completada por primera vez
                    if (goal.currentValue >= goal.targetValue && goal.completionCount == 1) {
                        showAchievementNotification(
                            "🎉 ¡Primera Meta Alcanzada!",
                            "Has completado tu meta: ${goal.title}"
                        )
                    }

                    // Rachas especiales
                    when (goal.currentStreak) {
                        7 -> showMilestoneNotification(
                            "🔥 ¡Racha de 7 Días!",
                            "Una semana completa cumpliendo tu meta: ${goal.title}"
                        )
                        30 -> showMilestoneNotification(
                            "🏆 ¡Racha de 30 Días!",
                            "Un mes entero de consistencia en: ${goal.title}"
                        )
                        100 -> showMilestoneNotification(
                            "💎 ¡Racha de 100 Días!",
                            "¡Increíble dedicación con: ${goal.title}!"
                        )
                    }

                    // Múltiplos de 10 completados
                    if (goal.completionCount > 0 && goal.completionCount % 10 == 0) {
                        showMilestoneNotification(
                            "⭐ Hito Alcanzado",
                            "Has completado ${goal.title} ${goal.completionCount} veces"
                        )
                    }
                }

                // Logro por tiempo total ahorrado
                checkScreenTimeReduction()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private suspend fun checkScreenTimeReduction() {
        try {
            val todayUsage = usageStatsRepository.getTodayUsageStats()
            val totalMinutes = todayUsage.sumOf { it.totalTimeInMillis } / 60000

            // Logro por mantener uso moderado (menos de 3 horas)
            if (totalMinutes < 180) {
                val calendar = Calendar.getInstance()
                if (calendar.get(Calendar.HOUR_OF_DAY) >= 20) { // Solo notificar en la noche
                    showAchievementNotification(
                        "✨ Día Equilibrado",
                        "Has mantenido un uso saludable del dispositivo hoy: ${formatMinutes(totalMinutes.toInt())}"
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showAchievementNotification(title: String, message: String) {
        showNotification(
            channelId = CHANNEL_ID_GOALS,
            notificationId = NOTIFICATION_ID_GOAL_ACHIEVED,
            title = title,
            message = message,
            icon = R.drawable.ic_goal,
            priority = NotificationCompat.PRIORITY_DEFAULT
        )
    }
    
    private fun showMilestoneNotification(title: String, message: String) {
        showNotification(
            channelId = CHANNEL_ID_GOALS,
            notificationId = NOTIFICATION_ID_MILESTONE,
            title = title,
            message = message,
            icon = R.drawable.ic_goal,
            priority = NotificationCompat.PRIORITY_DEFAULT
        )
    }
    
    /**
     * Recordatorio inteligente de tiempo de pantalla
     */
    fun sendSmartScreenTimeReminder() {
        scope.launch {
            try {
                if (!isNotificationEnabled(SCREEN_TIME_REMINDERS_ENABLED)) return@launch

                val todayUsage = usageStatsRepository.getTodayUsageStats()
                val totalMinutes = todayUsage.sumOf { it.totalTimeInMillis } / 60000

                if (totalMinutes > EXCESSIVE_USAGE_HOURS * 60) {
                    val title = "⏰ Recordatorio de Bienestar"
                    val message = "Has usado tu dispositivo durante ${formatMinutes(totalMinutes.toInt())} hoy. " +
                            "Considera tomar un descanso para cuidar tu salud."

                    showNotification(
                        channelId = CHANNEL_ID_REMINDERS,
                        notificationId = NOTIFICATION_ID_SCREEN_TIME_REMINDER,
                        title = title,
                        message = message,
                        icon = R.drawable.ic_reminder,
                        priority = NotificationCompat.PRIORITY_DEFAULT
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Programar todas las notificaciones periódicas
     */
    private fun scheduleAllNotifications() {
        // Chequeo de límites cada 30 minutos
        schedulePeriodicWork(
            workName = "app_limits_check",
            workerClass = AppLimitsCheckWorker::class.java,
            intervalHours = 0,
            intervalMinutes = 30
        )

        // Motivación diaria a las 9 AM
        scheduleDailyWork(
            workName = "daily_motivation",
            workerClass = DailyMotivationWorker::class.java,
            hour = 9,
            minute = 0
        )

        // Resumen semanal los domingos a las 8 PM
        scheduleWeeklyWork(
            workName = "weekly_summary",
            workerClass = WeeklySummaryWorker::class.java,
            dayOfWeek = Calendar.SUNDAY,
            hour = 20,
            minute = 0
        )

        // Chequeo de logros cada 2 horas
        schedulePeriodicWork(
            workName = "achievements_check",
            workerClass = AchievementsCheckWorker::class.java,
            intervalHours = 2
        )

        // Recordatorio de pantalla cada 3 horas (de 9 AM a 9 PM)
        schedulePeriodicWork(
            workName = "screen_time_reminder",
            workerClass = ScreenTimeReminderWorker::class.java,
            intervalHours = 3
        )
    }

    private fun schedulePeriodicWork(
        workName: String,
        workerClass: Class<out ListenableWorker>,
        intervalHours: Long = 1,
        intervalMinutes: Long = 0
    ) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val workRequest = if (intervalMinutes > 0) {
            PeriodicWorkRequestBuilder<ListenableWorker>(intervalMinutes, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
        } else {
            PeriodicWorkRequestBuilder<ListenableWorker>(intervalHours, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
        }

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            workName,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest as PeriodicWorkRequest
        )
    }

    private fun scheduleDailyWork(
        workName: String,
        workerClass: Class<out ListenableWorker>,
        hour: Int,
        minute: Int
    ) {
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(currentDate)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis

        val workRequest = PeriodicWorkRequestBuilder<ListenableWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            workName,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest as PeriodicWorkRequest
        )
    }
    
    private fun scheduleWeeklyWork(
        workName: String,
        workerClass: Class<out ListenableWorker>,
        dayOfWeek: Int,
        hour: Int,
        minute: Int
    ) {
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, dayOfWeek)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(currentDate)) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }

        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis

        val workRequest = PeriodicWorkRequestBuilder<ListenableWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            workName,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest as PeriodicWorkRequest
        )
    }

    // Funciones auxiliares
    private fun showNotification(
        channelId: String,
        notificationId: Int,
        title: String,
        message: String,
        icon: Int,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT,
        autoCancel: Boolean = true
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(icon)
            .setContentIntent(pendingIntent)
            .setAutoCancel(autoCancel)
            .setPriority(priority)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun showExpandableNotification(
        channelId: String,
        notificationId: Int,
        title: String,
        message: String,
        icon: Int
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(icon)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
    
    private fun formatMinutes(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
    }
    
    fun cancelAllNotifications() {
        NotificationManagerCompat.from(context).cancelAll()
    }
    
    fun cancelNotification(notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }
}

// Workers para notificaciones programadas
class AppLimitsCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val notificationManager = (applicationContext as com.momentummm.app.MomentumApplication)
                .smartNotificationManager
            notificationManager.checkAppLimitsAndNotify()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}

class DailyMotivationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val notificationManager = (applicationContext as com.momentummm.app.MomentumApplication)
                .smartNotificationManager
            notificationManager.sendDailyMotivation()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}

class WeeklySummaryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val notificationManager = (applicationContext as com.momentummm.app.MomentumApplication)
                .smartNotificationManager
            notificationManager.sendWeeklySummary()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}

class AchievementsCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val notificationManager = (applicationContext as com.momentummm.app.MomentumApplication)
                .smartNotificationManager
            notificationManager.checkAndNotifyAchievements()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}

class ScreenTimeReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            // Solo enviar recordatorios entre 9 AM y 9 PM
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)

            if (hour in 9..20) {
                val notificationManager = (applicationContext as com.momentummm.app.MomentumApplication)
                    .smartNotificationManager
                notificationManager.sendSmartScreenTimeReminder()
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}