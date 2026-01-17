package com.momentummm.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.momentummm.app.R
import com.momentummm.app.data.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max

enum class FocusTimerStatus {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED,
    BREAK
}

data class FocusSessionState(
    val sessionType: String? = null,
    val sessionName: String? = null,
    val totalSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val breakMinutes: Int = 0,
    val startTimeIso: String? = null,
    val blockedApps: List<String> = emptyList(),
    val status: FocusTimerStatus = FocusTimerStatus.IDLE
)

class FocusTimerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickerJob: Job? = null
    private var endTimeMillis: Long? = null
    private var pausedRemainingSeconds: Int = 0

    private val binder = FocusTimerBinder()

    private val _sessionState = MutableStateFlow(FocusSessionState())
    val sessionState: StateFlow<FocusSessionState> = _sessionState.asStateFlow()

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val sessionType = intent.getStringExtra(EXTRA_SESSION_TYPE)
                val sessionName = intent.getStringExtra(EXTRA_SESSION_NAME)
                val durationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 0)
                val breakMinutes = intent.getIntExtra(EXTRA_BREAK_MINUTES, 0)
                val blockedApps = intent.getStringArrayListExtra(EXTRA_BLOCKED_APPS)?.toList() ?: emptyList()
                if (durationMinutes > 0) {
                    startSession(sessionType, sessionName, durationMinutes, breakMinutes, blockedApps)
                }
            }
            ACTION_PAUSE -> pauseSession()
            ACTION_RESUME -> resumeSession()
            ACTION_STOP -> stopSession()
        }
        return Service.START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        tickerJob?.cancel()
        serviceScope.cancel()
    }

    private fun startSession(
        sessionType: String?,
        sessionName: String?,
        durationMinutes: Int,
        breakMinutes: Int,
        blockedApps: List<String>
    ) {
        val totalSeconds = durationMinutes * 60
        val startTimeIso = getCurrentTimestamp()
        val startTimeMillis = System.currentTimeMillis()
        endTimeMillis = startTimeMillis + totalSeconds * 1000L
        pausedRemainingSeconds = 0

        _sessionState.value = FocusSessionState(
            sessionType = sessionType,
            sessionName = sessionName,
            totalSeconds = totalSeconds,
            remainingSeconds = totalSeconds,
            breakMinutes = breakMinutes,
            startTimeIso = startTimeIso,
            blockedApps = blockedApps,
            status = FocusTimerStatus.RUNNING
        )

        serviceScope.launch {
            UserPreferencesRepository.setFocusModeEnabled(applicationContext, true)
            UserPreferencesRepository.setFocusModeBlockedApps(applicationContext, blockedApps)
        }

        val notification = buildNotification(totalSeconds, FocusTimerStatus.RUNNING)
        startForeground(NOTIFICATION_ID, notification)
        startTicker()
    }

    private fun pauseSession() {
        val current = _sessionState.value
        if (current.status != FocusTimerStatus.RUNNING) return

        tickerJob?.cancel()
        pausedRemainingSeconds = computeRemainingSeconds()
        _sessionState.value = current.copy(
            remainingSeconds = pausedRemainingSeconds,
            status = FocusTimerStatus.PAUSED
        )

        updateNotification(pausedRemainingSeconds, FocusTimerStatus.PAUSED)
    }

    private fun resumeSession() {
        val current = _sessionState.value
        if (current.status != FocusTimerStatus.PAUSED) return

        val remainingSeconds = max(1, pausedRemainingSeconds)
        endTimeMillis = System.currentTimeMillis() + remainingSeconds * 1000L
        _sessionState.value = current.copy(
            remainingSeconds = remainingSeconds,
            status = FocusTimerStatus.RUNNING
        )

        updateNotification(remainingSeconds, FocusTimerStatus.RUNNING)
        startTicker()
    }

    private fun stopSession() {
        tickerJob?.cancel()
        endTimeMillis = null
        pausedRemainingSeconds = 0
        _sessionState.value = FocusSessionState()

        serviceScope.launch {
            UserPreferencesRepository.setFocusModeEnabled(this@FocusTimerService, false)
            UserPreferencesRepository.setFocusModeBlockedApps(this@FocusTimerService, emptyList())
        }

        stopForeground(Service.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive) {
                val remainingSeconds = computeRemainingSeconds()
                if (remainingSeconds <= 0) {
                    onSessionCompleted()
                    break
                } else {
                    val current = _sessionState.value
                    if (current.status == FocusTimerStatus.RUNNING) {
                        _sessionState.value = current.copy(remainingSeconds = remainingSeconds)
                        updateNotification(remainingSeconds, FocusTimerStatus.RUNNING)
                    }
                }
                delay(1000)
            }
        }
    }

    private fun onSessionCompleted() {
        tickerJob?.cancel()
        val current = _sessionState.value
        _sessionState.value = current.copy(
            remainingSeconds = 0,
            status = FocusTimerStatus.COMPLETED
        )
        updateNotification(0, FocusTimerStatus.COMPLETED)

        serviceScope.launch {
            UserPreferencesRepository.setFocusModeEnabled(this@FocusTimerService, false)
            UserPreferencesRepository.setFocusModeBlockedApps(this@FocusTimerService, emptyList())
        }
    }

    private fun computeRemainingSeconds(): Int {
        val end = endTimeMillis ?: return 0
        val now = System.currentTimeMillis()
        return max(0, ((end - now) / 1000).toInt())
    }

    private fun updateNotification(remainingSeconds: Int, status: FocusTimerStatus) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(remainingSeconds, status))
    }

    private fun buildNotification(remainingSeconds: Int, status: FocusTimerStatus): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val contentIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FocusTimerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (status) {
            FocusTimerStatus.RUNNING -> "Sesión de enfoque activa"
            FocusTimerStatus.PAUSED -> "Sesión pausada"
            FocusTimerStatus.COMPLETED -> "Sesión completada"
            else -> "Focus Mode"
        }

        val timeText = formatRemainingTime(remainingSeconds)
        val contentText = when (status) {
            FocusTimerStatus.RUNNING -> "Quedan $timeText"
            FocusTimerStatus.PAUSED -> "Pausado en $timeText"
            FocusTimerStatus.COMPLETED -> "¡Completado!"
            else -> ""
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(contentIntent)
            .setOngoing(status == FocusTimerStatus.RUNNING || status == FocusTimerStatus.PAUSED)
            .setSilent(true)
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_pause,
                    "Detener",
                    stopPendingIntent
                )
            )
            .build()
    }

    private fun formatRemainingTime(remainingSeconds: Int): String {
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Temporizador de Enfoque",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notificaciones del temporizador de enfoque"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun getCurrentTimestamp(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return dateFormat.format(Date())
    }

    inner class FocusTimerBinder : Binder() {
        fun getService(): FocusTimerService = this@FocusTimerService
    }

    companion object {
        const val ACTION_START = "com.momentummm.app.action.FOCUS_START"
        const val ACTION_PAUSE = "com.momentummm.app.action.FOCUS_PAUSE"
        const val ACTION_RESUME = "com.momentummm.app.action.FOCUS_RESUME"
        const val ACTION_STOP = "com.momentummm.app.action.FOCUS_STOP"

        const val EXTRA_SESSION_TYPE = "extra_session_type"
        const val EXTRA_SESSION_NAME = "extra_session_name"
        const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"
        const val EXTRA_BREAK_MINUTES = "extra_break_minutes"
        const val EXTRA_BLOCKED_APPS = "extra_blocked_apps"

        private const val CHANNEL_ID = "focus_timer_channel"
        private const val NOTIFICATION_ID = 9101

        fun startForegroundService(context: Context, intent: Intent) {
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
