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
import com.momentummm.app.data.AppDatabase
import com.momentummm.app.data.UserPreferencesRepository
import com.momentummm.app.data.manager.GamificationManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
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
import kotlinx.coroutines.flow.update
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
    val status: FocusTimerStatus = FocusTimerStatus.IDLE,
    // Gamification
    val minutesCompleted: Int = 0,
    val xpEarned: Int = 0,
    val coinsEarned: Int = 0
)

/**
 * ─── POR QUÉ ESTE SERVICIO USA HILT ──────────────────────────────────────
 * BUG CORREGIDO: en `onCreate` se construía un GamificationManager propio
 * (`GamificationManager(database.userDao(), applicationContext)`). Ese objeto
 * era una instancia distinta de la del resto de la app, y a la de la app se le
 * conecta el SmartNotificationManager en `MomentumApplication.onCreate`
 * (`gamificationManager.setNotificationManager(...)`). Resultado: al terminar
 * una sesión de enfoque se otorgaban XP y monedas, pero las notificaciones de
 * subida de nivel y de logros nunca salían, porque las emitía un manager sin
 * notificador conectado.
 *
 * Con `@AndroidEntryPoint` + `@Inject` se recibe el singleton de Hilt, que es
 * el mismo que usa el resto de la aplicación.
 */
@AndroidEntryPoint
class FocusTimerService : Service() {

    private val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
        android.util.Log.e("FocusTimerService", "Coroutine exception", throwable)
    }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + exceptionHandler)
    private var tickerJob: Job? = null
    private var xpTrackerJob: Job? = null
    private var endTimeMillis: Long? = null
    private var pausedRemainingSeconds: Int = 0
    private var lastMinuteAwarded: Int = 0

    private val binder = FocusTimerBinder()

    private val _sessionState = MutableStateFlow(FocusSessionState())
    val sessionState: StateFlow<FocusSessionState> = _sessionState.asStateFlow()

    // Singleton compartido con el resto de la app (ver comentario de la clase).
    @Inject
    lateinit var gamificationManagerInstance: GamificationManager

    private val gamificationManager: GamificationManager?
        get() = if (::gamificationManagerInstance.isInitialized) gamificationManagerInstance else null

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // CRÍTICO: Si se inicia como foreground service, DEBE llamar startForeground en 5 segundos
        // Llamar startForeground inmediatamente para evitar ANR/crash
        try {
            val defaultNotification = buildNotification(0, FocusTimerStatus.IDLE)
            startForeground(NOTIFICATION_ID, defaultNotification)
        } catch (e: Exception) {
            android.util.Log.e("FocusTimerService", "Error starting foreground", e)
        }
        
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
            null -> {
                // CRITICAL FIX: Service restarted by system with null intent
                // Si hay una sesión activa (RUNNING o PAUSED), dejar el servicio vivo
                // pero sin timer activo. El usuario verá la notificación y podrá retomar.
                val currentStatus = _sessionState.value.status
                if (currentStatus == FocusTimerStatus.IDLE) {
                    android.util.Log.d("FocusTimerService", "Restarted with no active session, stopping")
                    stopForeground(Service.STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    android.util.Log.d("FocusTimerService", "Restarted with session in $currentStatus state")
                }
            }
        }
        return Service.START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        tickerJob?.cancel()
        xpTrackerJob?.cancel()
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
        lastMinuteAwarded = 0

        _sessionState.value = FocusSessionState(
            sessionType = sessionType,
            sessionName = sessionName,
            totalSeconds = totalSeconds,
            remainingSeconds = totalSeconds,
            breakMinutes = breakMinutes,
            startTimeIso = startTimeIso,
            blockedApps = blockedApps,
            status = FocusTimerStatus.RUNNING,
            minutesCompleted = 0,
            xpEarned = 0,
            coinsEarned = 0
        )

        serviceScope.launch {
            UserPreferencesRepository.setFocusModeEnabled(applicationContext, true)
            UserPreferencesRepository.setFocusModeBlockedApps(applicationContext, blockedApps)
        }

        val notification = buildNotification(totalSeconds, FocusTimerStatus.RUNNING)
        startForeground(NOTIFICATION_ID, notification)
        startTicker()
        startXpTracker()
    }

    /**
     * Tracker de XP - Otorga XP cada minuto completado de foco
     */
    private fun startXpTracker() {
        xpTrackerJob?.cancel()
        xpTrackerJob = serviceScope.launch {
            while (isActive) {
                delay(60_000) // Cada minuto
                
                val current = _sessionState.value
                if (current.status == FocusTimerStatus.RUNNING) {
                    val minutesCompleted = current.minutesCompleted + 1
                    
                    // Otorgar XP por minuto - usar safe call para evitar crash
                    val event = gamificationManager?.awardFocusMinuteXp(1)
                    
                    android.util.Log.d("FocusTimerService", "XP otorgado por minuto: xp=${event?.xpGained}, coins=${event?.coinsGained}, minutos=${minutesCompleted}")
                    
                    _sessionState.value = current.copy(
                        minutesCompleted = minutesCompleted,
                        xpEarned = current.xpEarned + (event?.xpGained ?: 0),
                        coinsEarned = current.coinsEarned + (event?.coinsGained ?: 0)
                    )
                    
                    lastMinuteAwarded = minutesCompleted
                }
            }
        }
    }

    private fun pauseSession() {
        val current = _sessionState.value
        if (current.status != FocusTimerStatus.RUNNING) return

        tickerJob?.cancel()
        xpTrackerJob?.cancel() // CRÍTICO: Cancelar XP tracker también para evitar battery drain
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
        // CRITICAL FIX: Reiniciar el XP tracker que fue cancelado en pauseSession()
        // Antes, después de pause/resume, el usuario no ganaba más XP
        startXpTracker()
    }

    private fun stopSession() {
        tickerJob?.cancel()
        xpTrackerJob?.cancel()
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
                    // CRITICAL FIX: Usar update{} para operación atómica
                    // Antes, read-then-write podía ser sobreescrito por xpTrackerJob
                    _sessionState.update { current ->
                        if (current.status == FocusTimerStatus.RUNNING) {
                            current.copy(remainingSeconds = remainingSeconds)
                        } else current
                    }
                    updateNotification(remainingSeconds, FocusTimerStatus.RUNNING)
                }
                delay(1000)
            }
        }
    }

    private fun onSessionCompleted() {
        tickerJob?.cancel()
        xpTrackerJob?.cancel()
        
        val current = _sessionState.value
        
        // Otorgar bonus por completar sesión - usar safe call para evitar crash
        serviceScope.launch {
            val bonusEvent = gamificationManager?.awardSessionCompletionBonus()
            
            android.util.Log.d("FocusTimerService", "Sesión completada - Bonus: xp=${bonusEvent?.xpGained}, coins=${bonusEvent?.coinsGained}")
            android.util.Log.d("FocusTimerService", "XP total acumulado en sesión: ${current.xpEarned + (bonusEvent?.xpGained ?: 0)}")
            
            _sessionState.value = current.copy(
                remainingSeconds = 0,
                status = FocusTimerStatus.COMPLETED,
                xpEarned = current.xpEarned + (bonusEvent?.xpGained ?: 0),
                coinsEarned = current.coinsEarned + (bonusEvent?.coinsGained ?: 0)
            )
            
            UserPreferencesRepository.setFocusModeEnabled(this@FocusTimerService, false)
            UserPreferencesRepository.setFocusModeBlockedApps(this@FocusTimerService, emptyList())
        }
        
        updateNotification(0, FocusTimerStatus.COMPLETED)
    }

    private fun computeRemainingSeconds(): Int {
        val end = endTimeMillis ?: return 0
        val now = System.currentTimeMillis()
        return max(0, ((end - now) / 1000).toInt())
    }

    private fun updateNotification(remainingSeconds: Int, status: FocusTimerStatus) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
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

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
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
