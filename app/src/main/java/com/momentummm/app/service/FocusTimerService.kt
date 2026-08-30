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
    /**
     * `true` mientras la sesión está en su fase de DESCANSO, incluso si está
     * pausada. La pantalla lo necesita porque `PAUSED` por sí solo no dice de
     * qué fase viene, y las métricas del enfoque no deben reaparecer en medio
     * de un descanso.
     */
    val onBreak: Boolean = false,
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

    /**
     * `true` cuando la pausa se hizo durante el DESCANSO.
     *
     * Sin esto, reanudar siempre volvía a RUNNING: un descanso pausado se
     * convertía en tiempo de enfoque (y volvía a otorgar XP), que es justo lo
     * que el descanso no debe premiar.
     */
    private var pausedFromBreak: Boolean = false

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
        restoreSnapshot()
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
            ACTION_START_BREAK -> startBreak()
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
        saveSnapshot()
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
        // El descanso también se puede pausar: es una cuenta atrás real.
        if (current.status != FocusTimerStatus.RUNNING && current.status != FocusTimerStatus.BREAK) return

        pausedFromBreak = current.status == FocusTimerStatus.BREAK
        tickerJob?.cancel()
        xpTrackerJob?.cancel() // CRÍTICO: Cancelar XP tracker también para evitar battery drain
        pausedRemainingSeconds = computeRemainingSeconds()
        _sessionState.value = current.copy(
            remainingSeconds = pausedRemainingSeconds,
            status = FocusTimerStatus.PAUSED
        )

        updateNotification(pausedRemainingSeconds, FocusTimerStatus.PAUSED)
        saveSnapshot()
    }

    private fun resumeSession() {
        val current = _sessionState.value
        if (current.status != FocusTimerStatus.PAUSED) return

        val remainingSeconds = max(1, pausedRemainingSeconds)
        val resumedStatus = if (pausedFromBreak) FocusTimerStatus.BREAK else FocusTimerStatus.RUNNING
        endTimeMillis = System.currentTimeMillis() + remainingSeconds * 1000L
        _sessionState.value = current.copy(
            remainingSeconds = remainingSeconds,
            status = resumedStatus
        )

        updateNotification(remainingSeconds, resumedStatus)
        saveSnapshot()
        startTicker()
        // CRITICAL FIX: Reiniciar el XP tracker que fue cancelado en pauseSession()
        // Antes, después de pause/resume, el usuario no ganaba más XP.
        // En el descanso NO se reinicia: descansar no es tiempo de enfoque.
        if (resumedStatus == FocusTimerStatus.RUNNING) {
            startXpTracker()
        }
    }

    /**
     * Arranca el DESCANSO como intervalo de primera clase: cuenta atrás propia,
     * etiqueta y color propios, y sin XP.
     *
     * Antes los minutos de descanso eran decorativos: se mostraban en la tarjeta
     * y se guardaban en el historial, pero el cronómetro nunca entraba en esta
     * fase. El enfoque ya terminó y quedó guardado, así que aquí no se vuelve a
     * tocar el registro de la sesión ni el bloqueo de apps (durante el descanso
     * el teléfono queda libre a propósito).
     */
    private fun startBreak() {
        val current = _sessionState.value
        if (current.status != FocusTimerStatus.COMPLETED) return
        val breakSeconds = current.breakMinutes * 60
        if (breakSeconds <= 0) return

        endTimeMillis = System.currentTimeMillis() + breakSeconds * 1000L
        pausedRemainingSeconds = 0
        pausedFromBreak = false

        _sessionState.value = current.copy(
            totalSeconds = breakSeconds,
            remainingSeconds = breakSeconds,
            status = FocusTimerStatus.BREAK,
            onBreak = true
        )

        updateNotification(breakSeconds, FocusTimerStatus.BREAK)
        saveSnapshot()
        startTicker()
    }

    private fun stopSession() {
        tickerJob?.cancel()
        xpTrackerJob?.cancel()
        endTimeMillis = null
        pausedRemainingSeconds = 0
        pausedFromBreak = false
        _sessionState.value = FocusSessionState()
        clearSnapshot()

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
                // La fase se lee en cada tick: el mismo ticker sirve al enfoque y
                // al descanso, y cada uno termina en un sitio distinto.
                val phase = _sessionState.value.status
                if (remainingSeconds <= 0) {
                    if (phase == FocusTimerStatus.BREAK) {
                        onBreakCompleted()
                    } else {
                        onSessionCompleted()
                    }
                    break
                } else {
                    // CRITICAL FIX: Usar update{} para operación atómica
                    // Antes, read-then-write podía ser sobreescrito por xpTrackerJob
                    _sessionState.update { current ->
                        if (current.status == FocusTimerStatus.RUNNING || current.status == FocusTimerStatus.BREAK) {
                            current.copy(remainingSeconds = remainingSeconds)
                        } else current
                    }
                    updateNotification(remainingSeconds, phase)
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

            // La sesión terminó: el estado guardado ya no vale y no debe
            // restaurarse en el próximo arranque del servicio.
            clearSnapshot()
            
            UserPreferencesRepository.setFocusModeEnabled(this@FocusTimerService, false)
            UserPreferencesRepository.setFocusModeBlockedApps(this@FocusTimerService, emptyList())
        }
        
        updateNotification(0, FocusTimerStatus.COMPLETED)
    }

    /**
     * El descanso llegó a cero. La sesión de enfoque ya se registró al
     * completarse, así que aquí no se guarda nada ni se otorgan recompensas:
     * solo se avisa con una notificación que se puede descartar y se libera el
     * servicio, dejando la pantalla lista para empezar otra sesión.
     */
    private fun onBreakCompleted() {
        tickerJob?.cancel()
        xpTrackerJob?.cancel()
        endTimeMillis = null
        pausedRemainingSeconds = 0
        pausedFromBreak = false
        _sessionState.value = FocusSessionState()
        clearSnapshot()

        // REMOVE retira la notificación del temporizador; el aviso de "descanso
        // terminado" viaja con su PROPIO id para que ningún startForeground
        // posterior lo sobreescriba y quede pegado como residuo persistente.
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
        notifyBreakOver()
        stopSelf()
    }

    private fun notifyBreakOver() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val contentIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.svc_focus_title_break_over))
            .setContentText(getString(R.string.svc_focus_text_break_over))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setOngoing(false)
            .build()
        notificationManager.notify(BREAK_OVER_NOTIFICATION_ID, notification)
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
            FocusTimerStatus.RUNNING -> getString(R.string.svc_focus_title_running)
            FocusTimerStatus.BREAK -> getString(R.string.svc_focus_title_break)
            FocusTimerStatus.PAUSED -> getString(R.string.svc_focus_title_paused)
            FocusTimerStatus.COMPLETED -> getString(R.string.svc_focus_title_completed)
            else -> getString(R.string.svc_focus_title_default)
        }

        val timeText = formatRemainingTime(remainingSeconds)
        val contentText = when (status) {
            FocusTimerStatus.RUNNING -> getString(R.string.svc_focus_text_running, timeText)
            FocusTimerStatus.BREAK -> getString(R.string.svc_focus_text_break, timeText)
            FocusTimerStatus.PAUSED -> getString(R.string.svc_focus_text_paused, timeText)
            FocusTimerStatus.COMPLETED -> getString(R.string.svc_focus_text_completed)
            else -> ""
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(contentIntent)
            .setOngoing(status == FocusTimerStatus.RUNNING || status == FocusTimerStatus.PAUSED || status == FocusTimerStatus.BREAK)
            .setSilent(true)
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_pause,
                    getString(R.string.svc_focus_action_stop),
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
            getString(R.string.svc_focus_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.svc_focus_channel_desc)
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        notificationManager.createNotificationChannel(channel)
    }

    // ─── Persistencia del estado ──────────────────────────────────────────
    //
    // El servicio guardaba el temporizador sólo en memoria, así que cualquier
    // muerte del proceso (force-stop, presión de memoria, crash) perdía en
    // silencio una sesión en curso: al volver, la pantalla mostraba de nuevo la
    // lista como si nunca hubiera empezado. La rama `null` de `onStartCommand`
    // ya intentaba sobrevivir a un reinicio del sistema, pero no podía
    // funcionar: sin estado guardado, `status` siempre era IDLE ahí.
    //
    // No hace falta escribir en cada tick. `endTimeMillis` es un instante
    // absoluto y el tiempo restante se deriva de él, así que basta con guardar
    // en los cambios de estado.

    private val snapshot by lazy {
        getSharedPreferences("focus_timer_snapshot", Context.MODE_PRIVATE)
    }

    private fun saveSnapshot() {
        val state = _sessionState.value
        if (state.status == FocusTimerStatus.IDLE) {
            clearSnapshot()
            return
        }
        snapshot.edit()
            .putString(KEY_SESSION_TYPE, state.sessionType)
            .putString(KEY_SESSION_NAME, state.sessionName)
            .putInt(KEY_TOTAL_SECONDS, state.totalSeconds)
            .putInt(KEY_BREAK_MINUTES, state.breakMinutes)
            .putString(KEY_START_TIME_ISO, state.startTimeIso)
            .putStringSet(KEY_BLOCKED_APPS, state.blockedApps.toSet())
            .putString(KEY_STATUS, state.status.name)
            .putInt(KEY_MINUTES_COMPLETED, state.minutesCompleted)
            .putInt(KEY_XP_EARNED, state.xpEarned)
            .putInt(KEY_COINS_EARNED, state.coinsEarned)
            .putLong(KEY_END_TIME_MILLIS, endTimeMillis ?: 0L)
            .putInt(KEY_PAUSED_REMAINING, pausedRemainingSeconds)
            .putInt(KEY_LAST_MINUTE_AWARDED, lastMinuteAwarded)
            .putBoolean(KEY_PAUSED_FROM_BREAK, pausedFromBreak)
            .apply()
    }

    private fun clearSnapshot() {
        snapshot.edit().clear().apply()
    }

    private fun restoreSnapshot() {
        val statusName = snapshot.getString(KEY_STATUS, null) ?: return
        val status = runCatching { FocusTimerStatus.valueOf(statusName) }.getOrNull() ?: return
        if (status == FocusTimerStatus.IDLE) {
            clearSnapshot()
            return
        }

        val storedEnd = snapshot.getLong(KEY_END_TIME_MILLIS, 0L)
        val storedPaused = snapshot.getInt(KEY_PAUSED_REMAINING, 0)

        val remainingSeconds = when (status) {
            // El descanso también corre contra un instante absoluto.
            FocusTimerStatus.RUNNING, FocusTimerStatus.BREAK ->
                max(0, ((storedEnd - System.currentTimeMillis()) / 1000).toInt())
            else -> storedPaused
        }

        // El plazo venció mientras el proceso estaba muerto. No se restaura ni
        // se otorgan XP: nadie estuvo vigilando esos minutos, y premiar tiempo
        // no observado vacía de sentido la gamificación. Se descarta y el
        // usuario empieza limpio.
        if (remainingSeconds <= 0) {
            clearSnapshot()
            return
        }

        endTimeMillis = if (status == FocusTimerStatus.RUNNING || status == FocusTimerStatus.BREAK) storedEnd else null
        pausedRemainingSeconds = storedPaused
        lastMinuteAwarded = snapshot.getInt(KEY_LAST_MINUTE_AWARDED, 0)
        pausedFromBreak = snapshot.getBoolean(KEY_PAUSED_FROM_BREAK, false)

        _sessionState.value = FocusSessionState(
            sessionType = snapshot.getString(KEY_SESSION_TYPE, null),
            sessionName = snapshot.getString(KEY_SESSION_NAME, null),
            totalSeconds = snapshot.getInt(KEY_TOTAL_SECONDS, 0),
            remainingSeconds = remainingSeconds,
            breakMinutes = snapshot.getInt(KEY_BREAK_MINUTES, 0),
            startTimeIso = snapshot.getString(KEY_START_TIME_ISO, null),
            blockedApps = snapshot.getStringSet(KEY_BLOCKED_APPS, emptySet())?.toList() ?: emptyList(),
            status = status,
            onBreak = status == FocusTimerStatus.BREAK ||
                (status == FocusTimerStatus.PAUSED && pausedFromBreak),
            minutesCompleted = snapshot.getInt(KEY_MINUTES_COMPLETED, 0),
            xpEarned = snapshot.getInt(KEY_XP_EARNED, 0),
            coinsEarned = snapshot.getInt(KEY_COINS_EARNED, 0)
        )

        updateNotification(remainingSeconds, status)

        if (status == FocusTimerStatus.RUNNING) {
            startTicker()
            startXpTracker()
        } else if (status == FocusTimerStatus.BREAK) {
            // El descanso se reanuda solo: no reparte XP.
            startTicker()
        }
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
        const val ACTION_START_BREAK = "com.momentummm.app.action.FOCUS_START_BREAK"
        const val ACTION_STOP = "com.momentummm.app.action.FOCUS_STOP"

        const val EXTRA_SESSION_TYPE = "extra_session_type"
        const val EXTRA_SESSION_NAME = "extra_session_name"
        const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"
        const val EXTRA_BREAK_MINUTES = "extra_break_minutes"
        const val EXTRA_BLOCKED_APPS = "extra_blocked_apps"

        private const val CHANNEL_ID = "focus_timer_channel"
        private const val NOTIFICATION_ID = 9101

        /** Aviso de "descanso terminado": id propio, no es una notificación en primer plano. */
        private const val BREAK_OVER_NOTIFICATION_ID = 9102

        // Claves del estado guardado (ver saveSnapshot/restoreSnapshot).
        private const val KEY_SESSION_TYPE = "session_type"
        private const val KEY_SESSION_NAME = "session_name"
        private const val KEY_TOTAL_SECONDS = "total_seconds"
        private const val KEY_BREAK_MINUTES = "break_minutes"
        private const val KEY_START_TIME_ISO = "start_time_iso"
        private const val KEY_BLOCKED_APPS = "blocked_apps"
        private const val KEY_STATUS = "status"
        private const val KEY_MINUTES_COMPLETED = "minutes_completed"
        private const val KEY_XP_EARNED = "xp_earned"
        private const val KEY_COINS_EARNED = "coins_earned"
        private const val KEY_END_TIME_MILLIS = "end_time_millis"
        private const val KEY_PAUSED_REMAINING = "paused_remaining"
        private const val KEY_LAST_MINUTE_AWARDED = "last_minute_awarded"
        private const val KEY_PAUSED_FROM_BREAK = "paused_from_break"

        fun startForegroundService(context: Context, intent: Intent) {
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
