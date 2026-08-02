package com.momentummm.app.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.momentummm.app.data.manager.MotivationalNotificationManager
import com.momentummm.app.data.repository.MotivationalMessagesRepository
import com.momentummm.app.notification.MotivationalAlarmScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
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

        private const val WORK_NAME_WATCHDOG = "motivational_notification_watchdog"

        // Nombres antiguos, conservados sólo para poder cancelar trabajos que
        // hubieran quedado encolados por versiones anteriores de la app.
        private const val WORK_NAME_PERIODIC = "motivational_notification_periodic"
        private const val WORK_NAME_MORNING = "motivational_notification_morning"
        private const val WORK_NAME_EVENING = "motivational_notification_evening"

        const val INPUT_TYPE = "notification_type"
        const val INPUT_CONTEXT_INFO = "context_info"
        const val INPUT_GOAL_PROGRESS = "goal_progress"
        const val INPUT_GOAL_TITLE = "goal_title"
        const val INPUT_STREAK_DAYS = "streak_days"

        const val TYPE_WATCHDOG = "watchdog"
        const val TYPE_SCHEDULED = "scheduled"
        const val TYPE_MORNING = "morning"
        const val TYPE_EVENING = "evening"
        const val TYPE_GOAL_PROGRESS = "goal_progress"
        const val TYPE_STREAK = "streak"
        const val TYPE_COMEBACK = "comeback"

        /**
         * Programa el vigilante de las alarmas motivacionales.
         *
         * Los mensajes ya no dependen de WorkManager: los entrega
         * [MotivationalAlarmScheduler] con alarmas exactas. Este trabajo
         * periódico existe sólo como red de seguridad: cada 6 horas comprueba
         * las preferencias y vuelve a poner las alarmas, de modo que si el
         * sistema descarta una alarma (cierre forzado, optimizaciones del
         * fabricante) la cadena se restaura sin que el usuario tenga que abrir
         * la app.
         */
        fun scheduleWatchdog(context: Context) {
            val request = PeriodicWorkRequestBuilder<MotivationalNotificationWorker>(
                6, TimeUnit.HOURS,
                1, TimeUnit.HOURS
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .setInputData(workDataOf(INPUT_TYPE to TYPE_WATCHDOG))
                .addTag(TAG_MOTIVATIONAL)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_WATCHDOG,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )

            // Limpia las cadenas del diseño anterior para que no dupliquen avisos.
            WorkManager.getInstance(context).apply {
                cancelUniqueWork(WORK_NAME_PERIODIC)
                cancelUniqueWork(WORK_NAME_MORNING)
                cancelUniqueWork(WORK_NAME_EVENING)
            }

            Log.d(TAG, "Watchdog de mensajes motivacionales programado")
        }

        const val TAG_MOTIVATIONAL = "motivational"

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
                .addTag(TAG_MOTIVATIONAL)
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
                .addTag(TAG_MOTIVATIONAL)
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
                .addTag(TAG_MOTIVATIONAL)
                .addTag("comeback")
                .build()

            WorkManager.getInstance(context).enqueue(request)

            Log.d(TAG, "Comeback notification triggered")
        }

        /**
         * Cancela los mensajes programados.
         *
         * BUG CORREGIDO: la versión anterior hacía
         * `cancelAllWorkByTag("motivational")`, lo que también borraba las
         * cadenas de mañana y noche. Al volver a activar el interruptor sólo se
         * reprogramaba la periódica, así que los mensajes diarios quedaban
         * cancelados para siempre. Ahora las alarmas se cancelan y se
         * restauran juntas, y el vigilante nunca se cancela para poder repararlas.
         */
        fun cancelAllNotifications(context: Context) {
            MotivationalAlarmScheduler.cancelAll(context)
            Log.d(TAG, "Alarmas de mensajes motivacionales canceladas")
        }
    }
    
    override suspend fun doWork(): Result {
        return try {
            val type = inputData.getString(INPUT_TYPE) ?: TYPE_WATCHDOG

            Log.d(TAG, "Worker started with type: $type")

            // El vigilante se ejecuta ANTES de cualquier comprobación de
            // preferencias: su única misión es que las alarmas sigan en pie.
            // Esto es lo que evita que la entrega de mensajes se pierda para
            // siempre, como ocurría cuando este worker salía con `return`
            // antes de reprogramar.
            if (type == TYPE_WATCHDOG) {
                messagesRepository.applyScheduleChanges()
                Log.d(TAG, "Watchdog: alarmas motivacionales verificadas")
                return Result.success()
            }

            // Las notificaciones puntuales (meta, racha, regreso) sí respetan
            // el interruptor del usuario.
            val prefs = messagesRepository.getPreferencesSync()
            if (prefs == null || !prefs.enabled) {
                Log.d(TAG, "Motivational notifications are disabled")
                return Result.success()
            }

            when (type) {
                TYPE_SCHEDULED -> handleScheduledNotification()
                TYPE_MORNING -> notificationManager.showMorningMessage()
                TYPE_EVENING -> notificationManager.showEveningMessage()
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
