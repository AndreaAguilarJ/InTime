package com.momentummm.app.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.momentummm.app.data.manager.MotivationalNotificationManager
import com.momentummm.app.data.repository.MotivationalMessagesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Worker for scheduling and showing motivational message notifications.
 * 
 * Features:
 * - Periodic notifications based on user preferences (3-8 per day)
 * - Smart timing to find best notification times
 * - Respects quiet hours and focus mode
 * - Contextual messages based on time of day, goals, and streaks
 * - Automatic rescheduling with adaptive intervals
 */
@HiltWorker
class MotivationalNotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationManager: MotivationalNotificationManager,
    private val messagesRepository: MotivationalMessagesRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "MotivationalNotifWorker"
        
        private const val WORK_NAME_PERIODIC = "motivational_notification_periodic"
        private const val WORK_NAME_MORNING = "motivational_notification_morning"
        private const val WORK_NAME_EVENING = "motivational_notification_evening"
        private const val WORK_NAME_CONTEXTUAL = "motivational_notification_contextual"
        
        const val INPUT_TYPE = "notification_type"
        const val INPUT_CONTEXT_INFO = "context_info"
        const val INPUT_GOAL_PROGRESS = "goal_progress"
        const val INPUT_GOAL_TITLE = "goal_title"
        const val INPUT_STREAK_DAYS = "streak_days"
        
        const val TYPE_SCHEDULED = "scheduled"
        const val TYPE_MORNING = "morning"
        const val TYPE_EVENING = "evening"
        const val TYPE_GOAL_PROGRESS = "goal_progress"
        const val TYPE_STREAK = "streak"
        const val TYPE_COMEBACK = "comeback"
        
        /**
         * Schedule periodic motivational notifications.
         * Notifications will be distributed throughout the day based on user preferences.
         */
        fun schedulePeriodicNotifications(context: Context) {
            // Calculate initial delay to start at a reasonable hour
            val initialDelay = calculateInitialDelay()
            
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()
            
            // Schedule periodic work - runs every 2-3 hours during active hours
            val periodicRequest = PeriodicWorkRequestBuilder<MotivationalNotificationWorker>(
                2, TimeUnit.HOURS,
                30, TimeUnit.MINUTES // flex interval
            )
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .setInputData(workDataOf(INPUT_TYPE to TYPE_SCHEDULED))
                .addTag("motivational")
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicRequest
            )
            
            Log.d(TAG, "Periodic motivational notifications scheduled with initial delay: ${initialDelay}ms")
        }
        
        /**
         * Schedule morning message notification.
         */
        fun scheduleMorningMessage(context: Context, hour: Int = 8, minute: Int = 0) {
            val delay = calculateDelayUntilTime(hour, minute)
            
            val request = OneTimeWorkRequestBuilder<MotivationalNotificationWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(INPUT_TYPE to TYPE_MORNING))
                .addTag("motivational")
                .addTag("morning")
                .build()
            
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_MORNING,
                ExistingWorkPolicy.REPLACE,
                request
            )
            
            Log.d(TAG, "Morning message scheduled at $hour:$minute (delay: ${delay}ms)")
        }
        
        /**
         * Schedule evening message notification.
         */
        fun scheduleEveningMessage(context: Context, hour: Int = 21, minute: Int = 0) {
            val delay = calculateDelayUntilTime(hour, minute)
            
            val request = OneTimeWorkRequestBuilder<MotivationalNotificationWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(INPUT_TYPE to TYPE_EVENING))
                .addTag("motivational")
                .addTag("evening")
                .build()
            
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_EVENING,
                ExistingWorkPolicy.REPLACE,
                request
            )
            
            Log.d(TAG, "Evening message scheduled at $hour:$minute (delay: ${delay}ms)")
        }
        
        /**
         * Trigger a goal progress notification.
         */
        fun triggerGoalProgressNotification(
            context: Context,
            progressPercent: Int,
            goalTitle: String
        ) {
            val request = OneTimeWorkRequestBuilder<MotivationalNotificationWorker>()
                .setInputData(workDataOf(
                    INPUT_TYPE to TYPE_GOAL_PROGRESS,
                    INPUT_GOAL_PROGRESS to progressPercent,
                    INPUT_GOAL_TITLE to goalTitle
                ))
                .addTag("motivational")
                .addTag("goal_progress")
                .build()
            
            WorkManager.getInstance(context).enqueue(request)
            
            Log.d(TAG, "Goal progress notification triggered: $goalTitle ($progressPercent%)")
        }
        
        /**
         * Trigger a streak celebration notification.
         */
        fun triggerStreakNotification(context: Context, streakDays: Int) {
            val request = OneTimeWorkRequestBuilder<MotivationalNotificationWorker>()
                .setInputData(workDataOf(
                    INPUT_TYPE to TYPE_STREAK,
                    INPUT_STREAK_DAYS to streakDays
                ))
                .addTag("motivational")
                .addTag("streak")
                .build()
            
            WorkManager.getInstance(context).enqueue(request)
            
            Log.d(TAG, "Streak notification triggered: $streakDays days")
        }
        
        /**
         * Trigger a comeback notification for returning users.
         */
        fun triggerComebackNotification(context: Context) {
            val request = OneTimeWorkRequestBuilder<MotivationalNotificationWorker>()
                .setInputData(workDataOf(INPUT_TYPE to TYPE_COMEBACK))
                .addTag("motivational")
                .addTag("comeback")
                .build()
            
            WorkManager.getInstance(context).enqueue(request)
            
            Log.d(TAG, "Comeback notification triggered")
        }
        
        /**
         * Cancel all scheduled motivational notifications.
         */
        fun cancelAllNotifications(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag("motivational")
            Log.d(TAG, "All motivational notifications cancelled")
        }
        
        /**
         * Calculate initial delay to start notifications at a reasonable hour.
         */
        private fun calculateInitialDelay(): Long {
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            
            return when {
                // Too early - wait until 8 AM
                currentHour < 8 -> {
                    calendar.set(Calendar.HOUR_OF_DAY, 8)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.timeInMillis - System.currentTimeMillis()
                }
                // Too late - wait until tomorrow 8 AM
                currentHour >= 22 -> {
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 8)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.timeInMillis - System.currentTimeMillis()
                }
                // Good time - start after 1 hour
                else -> TimeUnit.HOURS.toMillis(1)
            }
        }
        
        /**
         * Calculate delay until a specific time.
         */
        private fun calculateDelayUntilTime(hour: Int, minute: Int): Long {
            val calendar = Calendar.getInstance()
            val targetCalendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            // If time has passed today, schedule for tomorrow
            if (targetCalendar.before(calendar)) {
                targetCalendar.add(Calendar.DAY_OF_MONTH, 1)
            }
            
            return targetCalendar.timeInMillis - calendar.timeInMillis
        }
    }
    
    override suspend fun doWork(): Result {
        return try {
            val type = inputData.getString(INPUT_TYPE) ?: TYPE_SCHEDULED
            
            Log.d(TAG, "Worker started with type: $type")
            
            // Check if notifications are enabled
            val prefs = messagesRepository.getPreferencesSync()
            if (prefs == null || !prefs.enabled) {
                Log.d(TAG, "Motivational notifications are disabled")
                return Result.success()
            }
            
            when (type) {
                TYPE_SCHEDULED -> handleScheduledNotification()
                TYPE_MORNING -> handleMorningNotification()
                TYPE_EVENING -> handleEveningNotification()
                TYPE_GOAL_PROGRESS -> handleGoalProgressNotification()
                TYPE_STREAK -> handleStreakNotification()
                TYPE_COMEBACK -> handleComebackNotification()
                else -> {
                    Log.w(TAG, "Unknown notification type: $type")
                    handleScheduledNotification()
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in motivational notification worker", e)
            Result.retry()
        }
    }
    
    private suspend fun handleScheduledNotification() {
        // Check if it's a good time
        if (!messagesRepository.isGoodTimeForNotification()) {
            Log.d(TAG, "Not a good time for notification, skipping")
            return
        }
        
        notificationManager.showMotivationalNotification()
    }
    
    private suspend fun handleMorningNotification() {
        notificationManager.showMorningMessage()
        
        // Reschedule for tomorrow - use default morning time (8:00)
        scheduleMorningMessage(context, 8, 0)
    }
    
    private suspend fun handleEveningNotification() {
        notificationManager.showEveningMessage()
        
        // Reschedule for tomorrow - use default evening time (21:00)
        scheduleEveningMessage(context, 21, 0)
    }
    
    private suspend fun handleGoalProgressNotification() {
        val progressPercent = inputData.getInt(INPUT_GOAL_PROGRESS, 0)
        val goalTitle = inputData.getString(INPUT_GOAL_TITLE) ?: "Meta"
        
        notificationManager.showGoalProgressNotification(progressPercent, goalTitle)
    }
    
    private suspend fun handleStreakNotification() {
        val streakDays = inputData.getInt(INPUT_STREAK_DAYS, 0)
        
        if (streakDays > 0) {
            notificationManager.showStreakNotification(streakDays)
        }
    }
    
    private suspend fun handleComebackNotification() {
        notificationManager.showComebackNotification()
    }
}
