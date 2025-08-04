package com.momentum.app.data.manager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.momentum.app.MainActivity
import com.momentum.app.R
import java.util.concurrent.TimeUnit

class NotificationManager(private val context: Context) {
    
    companion object {
        const val CHANNEL_ID_GOALS = "goals_channel"
        const val CHANNEL_ID_CHALLENGES = "challenges_channel"
        const val CHANNEL_ID_FOCUS = "focus_channel"
        const val CHANNEL_ID_REMINDERS = "reminders_channel"
        
        const val NOTIFICATION_ID_DAILY_GOAL = 1001
        const val NOTIFICATION_ID_WEEKLY_GOAL = 1002
        const val NOTIFICATION_ID_CHALLENGE = 1003
        const val NOTIFICATION_ID_FOCUS_BREAK = 1004
        const val NOTIFICATION_ID_FOCUS_END = 1005
        const val NOTIFICATION_ID_SCREEN_TIME_REMINDER = 1006
    }
    
    init {
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_ID_GOALS,
                    "Metas y Objetivos",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notificaciones sobre el progreso de tus metas diarias y semanales"
                },
                NotificationChannel(
                    CHANNEL_ID_CHALLENGES,
                    "Desafíos",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notificaciones sobre desafíos de bienestar digital"
                },
                NotificationChannel(
                    CHANNEL_ID_FOCUS,
                    "Sesiones de Enfoque",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notificaciones sobre sesiones de enfoque y descansos"
                },
                NotificationChannel(
                    CHANNEL_ID_REMINDERS,
                    "Recordatorios",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Recordatorios generales sobre el uso del dispositivo"
                }
            )
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            channels.forEach { notificationManager.createNotificationChannel(it) }
        }
    }
    
    fun showDailyGoalProgress(progress: Int, target: Int) {
        val progressPercentage = (progress * 100) / target
        val title = if (progressPercentage >= 100) {
            "¡Meta diaria alcanzada! 🎉"
        } else {
            "Progreso de meta diaria"
        }
        
        val message = if (progressPercentage >= 100) {
            "Has cumplido tu meta de tiempo de pantalla de hoy. ¡Excelente trabajo!"
        } else {
            "Llevas ${progress}h de ${target}h objetivo (${progressPercentage}%)"
        }
        
        showNotification(
            channelId = CHANNEL_ID_GOALS,
            notificationId = NOTIFICATION_ID_DAILY_GOAL,
            title = title,
            message = message,
            icon = R.drawable.ic_goal
        )
    }
    
    fun showWeeklyGoalProgress(progress: Int, target: Int) {
        val progressPercentage = (progress * 100) / target
        val title = if (progressPercentage >= 100) {
            "¡Meta semanal completada! 🏆"
        } else {
            "Progreso de meta semanal"
        }
        
        val message = if (progressPercentage >= 100) {
            "Has completado tu meta semanal. ¡Sigue así!"
        } else {
            "Progreso semanal: ${progressPercentage}% completado"
        }
        
        showNotification(
            channelId = CHANNEL_ID_GOALS,
            notificationId = NOTIFICATION_ID_WEEKLY_GOAL,
            title = title,
            message = message,
            icon = R.drawable.ic_goal
        )
    }
    
    fun showChallengeNotification(challengeTitle: String, progress: String) {
        showNotification(
            channelId = CHANNEL_ID_CHALLENGES,
            notificationId = NOTIFICATION_ID_CHALLENGE,
            title = "Desafío: $challengeTitle",
            message = progress,
            icon = R.drawable.ic_challenge
        )
    }
    
    fun showFocusBreakNotification(sessionType: String, breakDuration: Int) {
        showNotification(
            channelId = CHANNEL_ID_FOCUS,
            notificationId = NOTIFICATION_ID_FOCUS_BREAK,
            title = "Tiempo de descanso ⏰",
            message = "Tu sesión de $sessionType ha terminado. Descansa $breakDuration minutos.",
            icon = R.drawable.ic_focus,
            autoCancel = false
        )
    }
    
    fun showFocusSessionEnd(sessionType: String, duration: Int) {
        showNotification(
            channelId = CHANNEL_ID_FOCUS,
            notificationId = NOTIFICATION_ID_FOCUS_END,
            title = "Sesión completada ✅",
            message = "Has completado $duration minutos de $sessionType. ¡Buen trabajo!",
            icon = R.drawable.ic_focus
        )
    }
    
    fun showScreenTimeReminder(currentUsage: String) {
        showNotification(
            channelId = CHANNEL_ID_REMINDERS,
            notificationId = NOTIFICATION_ID_SCREEN_TIME_REMINDER,
            title = "Recordatorio de tiempo de pantalla",
            message = "Llevas $currentUsage de uso hoy. ¿Qué tal un descanso?",
            icon = R.drawable.ic_reminder
        )
    }
    
    private fun showNotification(
        channelId: String,
        notificationId: Int,
        title: String,
        message: String,
        icon: Int,
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
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Handle permission not granted
        }
    }
    
    fun scheduleDailyGoalReminder() {
        val workRequest = PeriodicWorkRequestBuilder<DailyGoalReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(12, TimeUnit.HOURS) // Start checking at noon
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "daily_goal_reminder",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }
    
    fun scheduleScreenTimeReminder() {
        val workRequest = PeriodicWorkRequestBuilder<ScreenTimeReminderWorker>(2, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "screen_time_reminder",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }
    
    fun cancelAllNotifications() {
        NotificationManagerCompat.from(context).cancelAll()
    }
    
    fun cancelNotification(notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }
}

// Worker classes for scheduled notifications
class DailyGoalReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        // Check goal progress and send notification if needed
        // This would integrate with your goals repository
        return Result.success()
    }
}

class ScreenTimeReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        // Check screen time usage and send reminder if needed
        // This would integrate with your usage stats repository
        return Result.success()
    }
}