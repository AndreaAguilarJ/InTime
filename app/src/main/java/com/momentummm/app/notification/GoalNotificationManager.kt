package com.momentummm.app.notification
import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.momentummm.app.MainActivity
import com.momentummm.app.R
import com.momentummm.app.data.entity.Challenge
import com.momentummm.app.data.entity.Goal
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class GoalNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID_GOALS = "goals_notifications"
        const val CHANNEL_ID_CHALLENGES = "challenges_notifications"
        const val CHANNEL_ID_ACHIEVEMENTS = "achievements_notifications"

        const val NOTIFICATION_ID_GOAL_REMINDER = 1001
        const val NOTIFICATION_ID_CHALLENGE_REMINDER = 1002
        const val NOTIFICATION_ID_ACHIEVEMENT = 1003
        const val NOTIFICATION_ID_STREAK_REMINDER = 1004

        private const val TAG = "GoalNotificationManager"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val goalsChannel = NotificationChannel(
                CHANNEL_ID_GOALS,
                context.getString(R.string.goal_ch_goals_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.goal_ch_goals_desc)
            }

            val challengesChannel = NotificationChannel(
                CHANNEL_ID_CHALLENGES,
                context.getString(R.string.goal_ch_challenges_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.goal_ch_challenges_desc)
            }

            val achievementsChannel = NotificationChannel(
                CHANNEL_ID_ACHIEVEMENTS,
                context.getString(R.string.goal_ch_achievements_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.goal_ch_achievements_desc)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            manager.createNotificationChannels(
                listOf(goalsChannel, challengesChannel, achievementsChannel)
            )
        }
    }

    /**
     * Id de notificación estable y único por entidad, acotado para no invadir
     * los rangos que usan las demás notificaciones de la app.
     */
    private fun notificationIdFor(base: Int, entityId: String): Int {
        val hash = entityId.hashCode()
        val offset = if (hash == Int.MIN_VALUE) 0 else kotlin.math.abs(hash) % 90
        return base * 100 + offset
    }

    fun showGoalReminderNotification(goal: Goal) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "goals")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val progressPercentage = if (goal.targetValue > 0) {
            ((goal.currentValue.toFloat() / goal.targetValue.toFloat()) * 100).toInt()
        } else 0

        val bigText = buildString {
            append(goal.description ?: "")
            append("\nProgreso actual: ${goal.currentValue}/${goal.targetValue}")
            append(" (${progressPercentage}%)")
        }

        val notification: Notification = NotificationCompat.Builder(context, CHANNEL_ID_GOALS)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Cambia por tu drawable si tienes uno propio para metas
            .setContentTitle("Recordatorio de Meta")
            .setContentText("${goal.title} - $progressPercentage% completado")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // BUG CORREGIDO: todos los recordatorios de metas compartían el mismo
        // id, así que con dos metas activas sólo se veía la última: las demás se
        // sobrescribían en silencio. Los retos ya usaban un id por entidad.
        notifySafe(notificationIdFor(NOTIFICATION_ID_GOAL_REMINDER, goal.id.toString()), notification)
    }

    fun showChallengeReminderNotification(challenge: Challenge) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "challenges")
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val daysRemaining = challenge.durationDays - challenge.daysCompleted
        val progressPercentage = (challenge.progress * 100).toInt()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_CHALLENGES)
            .setSmallIcon(R.drawable.ic_challenge) // Asegúrate de que exista este recurso
            .setContentTitle(context.getString(R.string.goal_challenge_title))
            .setContentText(context.getString(R.string.goal_challenge_text, challenge.title, progressPercentage))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.goal_challenge_bigtext, challenge.description, daysRemaining))
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_media_play,
                context.getString(R.string.goal_action_continue),
                pendingIntent
            )
            .build()

        // `base + hashCode()` podía desbordar y aterrizar en el id de otra
        // notificación de la app; se acota el desplazamiento.
        notifySafe(
            notificationIdFor(NOTIFICATION_ID_CHALLENGE_REMINDER, challenge.id.toString()),
            notification
        )
    }

    fun showAchievementNotification(achievementTitle: String, achievementDescription: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "achievements")
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ACHIEVEMENTS)
            .setSmallIcon(android.R.drawable.star_big_on)
            .setContentTitle(context.getString(R.string.goal_achievement_title))
            .setContentText(achievementTitle)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("🎉 $achievementTitle\n$achievementDescription")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notifySafe(NOTIFICATION_ID_ACHIEVEMENT, notification)
    }

    fun showStreakReminderNotification(streakDays: Int, goalTitle: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "goals")
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_GOALS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.goal_streak_title))
            .setContentText(context.getString(R.string.goal_streak_text, streakDays, goalTitle))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.goal_streak_bigtext, streakDays))
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notifySafe(NOTIFICATION_ID_STREAK_REMINDER, notification)
    }

    fun cancelAllNotifications() {
        NotificationManagerCompat.from(context).cancelAll()
    }

    fun cancelGoalNotifications() {
        val manager = NotificationManagerCompat.from(context)
        manager.cancel(NOTIFICATION_ID_GOAL_REMINDER)
        manager.cancel(NOTIFICATION_ID_STREAK_REMINDER)
    }

    fun cancelChallengeNotifications() {
        NotificationManagerCompat.from(context)
            .cancel(NOTIFICATION_ID_CHALLENGE_REMINDER)
    }

    private fun canPostNotifications(): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun notifySafe(id: Int, notification: Notification) {
        if (!canPostNotifications()) {
            Log.w(TAG, "Permiso POST_NOTIFICATIONS no concedido. Notificación $id no enviada.")
            return
        }
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (se: SecurityException) {
            Log.e(TAG, "Fallo al notificar (SecurityException): ${se.message}")
        }
    }
}