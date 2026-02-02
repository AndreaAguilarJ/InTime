package com.momentummm.app.data.manager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.momentummm.app.MainActivity
import com.momentummm.app.R
import com.momentummm.app.data.entity.MessageCategory
import com.momentummm.app.data.entity.MotivationalMessage
import com.momentummm.app.data.repository.MotivationalMessagesRepository
import com.momentummm.app.receiver.MotivationalNotificationReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages motivational message notifications with smart timing and interactive actions.
 * 
 * Features:
 * - High priority notification channel for motivational messages
 * - BigTextStyle for longer messages
 * - Interactive actions: "❤️ Me encantó", "🔄 Otro mensaje", "⚙️ Configurar"
 * - Smart timing integration with user preferences
 * - Integration with goals and streaks for contextual messages
 */
@Singleton
class MotivationalNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val messagesRepository: MotivationalMessagesRepository
) {

    companion object {
        private const val TAG = "MotivationalNotifMgr"
        
        // Channel ID
        const val CHANNEL_ID_MOTIVATIONAL = "motivational_messages_channel"
        
        // Notification IDs
        const val NOTIFICATION_ID_MOTIVATIONAL = 3001
        const val NOTIFICATION_ID_GOAL_PROGRESS = 3002
        const val NOTIFICATION_ID_STREAK = 3003
        const val NOTIFICATION_ID_COMEBACK = 3004
        
        // Action request codes
        const val REQUEST_CODE_LOVE = 3101
        const val REQUEST_CODE_ANOTHER = 3102
        const val REQUEST_CODE_SETTINGS = 3103
        const val REQUEST_CODE_OPEN = 3104
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Create the motivational messages notification channel with high priority.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_MOTIVATIONAL,
                context.getString(R.string.notification_channel_motivational_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_motivational_desc)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 100, 250)
                setShowBadge(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }
    
    /**
     * Show a motivational message notification with actions.
     */
    suspend fun showMotivationalNotification(
        message: MotivationalMessage? = null,
        contextInfo: String? = null
    ) {
        try {
            // Get message to show
            val messageToShow = message ?: messagesRepository.getNextMessageToShow()
            
            if (messageToShow == null) {
                Log.w(TAG, "No message available to show")
                return
            }
            
            // Check if it's a good time
            if (!messagesRepository.isGoodTimeForNotification()) {
                Log.d(TAG, "Not a good time for notification, skipping")
                return
            }
            
            // Mark as shown
            messagesRepository.markMessageAsShown(messageToShow.id, contextInfo)
            
            // Build and show notification
            showNotificationInternal(messageToShow, NOTIFICATION_ID_MOTIVATIONAL)
            
            Log.d(TAG, "Showed motivational notification: ${messageToShow.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing motivational notification", e)
        }
    }
    
    /**
     * Show notification for goal progress milestone.
     */
    suspend fun showGoalProgressNotification(progressPercent: Int, goalTitle: String) {
        try {
            val message = messagesRepository.getGoalProgressMessage(progressPercent, goalTitle)
            
            if (message != null) {
                val contextInfo = "goal_${progressPercent}%"
                messagesRepository.markMessageAsShown(message.id, contextInfo)
                
                // Customize title based on progress
                val title = when {
                    progressPercent >= 100 -> "🎉 ¡Meta completada: $goalTitle!"
                    progressPercent >= 75 -> "🚀 ¡75% completado: $goalTitle!"
                    progressPercent >= 50 -> "💪 ¡Mitad del camino: $goalTitle!"
                    progressPercent >= 25 -> "🌟 ¡25% completado: $goalTitle!"
                    else -> "📈 Progreso en: $goalTitle"
                }
                
                showNotificationInternal(
                    message = message,
                    notificationId = NOTIFICATION_ID_GOAL_PROGRESS,
                    customTitle = title
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing goal progress notification", e)
        }
    }
    
    /**
     * Show notification for streak celebration.
     */
    suspend fun showStreakNotification(streakDays: Int) {
        try {
            val message = messagesRepository.getStreakMessage(streakDays)
            
            if (message != null) {
                val contextInfo = "streak_${streakDays}_days"
                messagesRepository.markMessageAsShown(message.id, contextInfo)
                
                val title = when {
                    streakDays >= 100 -> "🔥🔥🔥 ¡$streakDays días de racha! ¡Legendario!"
                    streakDays >= 30 -> "🔥🔥 ¡$streakDays días de racha! ¡Increíble!"
                    streakDays >= 7 -> "🔥 ¡$streakDays días de racha! ¡Sigue así!"
                    else -> "🔥 ¡$streakDays días de racha!"
                }
                
                showNotificationInternal(
                    message = message,
                    notificationId = NOTIFICATION_ID_STREAK,
                    customTitle = title
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing streak notification", e)
        }
    }
    
    /**
     * Show notification for user comeback (returning after inactivity).
     */
    suspend fun showComebackNotification() {
        try {
            val message = messagesRepository.getContextualMessage(MessageCategory.COMEBACK)
            
            if (message != null) {
                messagesRepository.markMessageAsShown(message.id, "comeback")
                
                showNotificationInternal(
                    message = message,
                    notificationId = NOTIFICATION_ID_COMEBACK,
                    customTitle = "🔄 ¡Bienvenido de vuelta!"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing comeback notification", e)
        }
    }
    
    /**
     * Show morning message notification.
     */
    suspend fun showMorningMessage() {
        try {
            val message = messagesRepository.getContextualMessage(MessageCategory.MORNING)
            
            if (message != null) {
                messagesRepository.markMessageAsShown(message.id, "morning")
                showNotificationInternal(message, NOTIFICATION_ID_MOTIVATIONAL)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing morning message", e)
        }
    }
    
    /**
     * Show evening message notification.
     */
    suspend fun showEveningMessage() {
        try {
            val message = messagesRepository.getContextualMessage(MessageCategory.EVENING)
            
            if (message != null) {
                messagesRepository.markMessageAsShown(message.id, "evening")
                showNotificationInternal(message, NOTIFICATION_ID_MOTIVATIONAL)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing evening message", e)
        }
    }
    
    /**
     * Handle "Love" action from notification.
     */
    fun handleLoveAction(messageId: String) {
        scope.launch {
            try {
                messagesRepository.loveMessage(messageId)
                Log.d(TAG, "Message loved: $messageId")
            } catch (e: Exception) {
                Log.e(TAG, "Error handling love action", e)
            }
        }
    }
    
    /**
     * Handle "Another Message" action from notification.
     */
    fun handleAnotherMessageAction(messageId: String) {
        scope.launch {
            try {
                messagesRepository.recordAnotherMessageRequested(messageId)
                
                // Show a new message
                showMotivationalNotification()
                
                Log.d(TAG, "Another message requested after: $messageId")
            } catch (e: Exception) {
                Log.e(TAG, "Error handling another message action", e)
            }
        }
    }
    
    /**
     * Internal method to build and show the notification.
     */
    private fun showNotificationInternal(
        message: MotivationalMessage,
        notificationId: Int,
        customTitle: String? = null
    ) {
        try {
            // Content intent - opens app
            val contentIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("message_id", message.id)
                putExtra("source", "notification")
            }
            val contentPendingIntent = PendingIntent.getActivity(
                context,
                REQUEST_CODE_OPEN,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Action: Love
            val loveIntent = Intent(context, MotivationalNotificationReceiver::class.java).apply {
                action = MotivationalNotificationReceiver.ACTION_LOVE
                putExtra(MotivationalNotificationReceiver.EXTRA_MESSAGE_ID, message.id)
            }
            val lovePendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_LOVE,
                loveIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Action: Another message
            val anotherIntent = Intent(context, MotivationalNotificationReceiver::class.java).apply {
                action = MotivationalNotificationReceiver.ACTION_ANOTHER
                putExtra(MotivationalNotificationReceiver.EXTRA_MESSAGE_ID, message.id)
            }
            val anotherPendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_ANOTHER,
                anotherIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Action: Settings
            val settingsIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", "motivational_settings")
            }
            val settingsPendingIntent = PendingIntent.getActivity(
                context,
                REQUEST_CODE_SETTINGS,
                settingsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Build notification title
            val title = customTitle ?: buildTitle(message)
            
            // Build notification
            val notification = NotificationCompat.Builder(context, CHANNEL_ID_MOTIVATIONAL)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message.content)
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText(message.content)
                    .setBigContentTitle(title))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(contentPendingIntent)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .addAction(0, "❤️ Me encantó", lovePendingIntent)
                .addAction(0, "🔄 Otro", anotherPendingIntent)
                .addAction(0, "⚙️", settingsPendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()
            
            // Show notification
            try {
                NotificationManagerCompat.from(context).notify(notificationId, notification)
            } catch (e: SecurityException) {
                Log.w(TAG, "Missing POST_NOTIFICATIONS permission", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error building notification", e)
        }
    }
    
    /**
     * Build a title based on message category.
     */
    private fun buildTitle(message: MotivationalMessage): String {
        val emoji = message.emoji ?: message.category.emoji
        val categoryName = when (message.category) {
            MessageCategory.MORNING -> "Buenos días"
            MessageCategory.EVENING -> "Buenas noches"
            MessageCategory.ACHIEVEMENT -> "¡Logro!"
            MessageCategory.FOCUS -> "Momento de enfoque"
            MessageCategory.GRATITUDE -> "Momento de gratitud"
            MessageCategory.CHALLENGE -> "¡Ánimo!"
            MessageCategory.STREAK -> "Tu racha"
            MessageCategory.CELEBRATION -> "¡A celebrar!"
            MessageCategory.MOTIVATION -> "Motivación"
            MessageCategory.PRODUCTIVITY -> "Productividad"
            MessageCategory.MINDFULNESS -> "Mindfulness"
            MessageCategory.SELF_CARE -> "Cuídate"
            MessageCategory.GROWTH -> "Crecimiento"
            MessageCategory.RESILIENCE -> "Resiliencia"
            MessageCategory.WEEKEND -> "¡Fin de semana!"
            MessageCategory.MONDAY -> "¡Nuevo lunes!"
            MessageCategory.MILESTONE -> "¡Hito alcanzado!"
            MessageCategory.COMEBACK -> "¡De vuelta!"
        }
        return "$emoji $categoryName"
    }
    
    /**
     * Cancel all motivational notifications.
     */
    fun cancelAllMotivationalNotifications() {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(NOTIFICATION_ID_MOTIVATIONAL)
        notificationManager.cancel(NOTIFICATION_ID_GOAL_PROGRESS)
        notificationManager.cancel(NOTIFICATION_ID_STREAK)
        notificationManager.cancel(NOTIFICATION_ID_COMEBACK)
    }
}
