package com.momentummm.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.momentummm.app.MainActivity
import com.momentummm.app.R
import com.momentummm.app.data.AppDatabase
import com.momentummm.app.data.manager.SmartBlockingManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Servicio para el Modo Nuclear.
 * 
 * CARACTERÍSTICAS CRÍTICAS:
 * - El timer de desbloqueo SOLO avanza cuando InTime está en primer plano
 * - El usuario DEBE tener la app visible para que el timer progrese
 * - Bloqueo de 1-90 días, no se puede cancelar fácilmente
 * - Muestra el progreso en tiempo real
 */
@AndroidEntryPoint
class NuclearModeService : Service() {
    
    companion object {
        private const val TAG = "NuclearModeService"
        private const val NOTIFICATION_ID = 3001
        private const val CHANNEL_ID = "nuclear_mode_channel"
        
        const val ACTION_START = "com.momentummm.app.action.START_NUCLEAR_TIMER"
        const val ACTION_STOP = "com.momentummm.app.action.STOP_NUCLEAR_TIMER"
        const val ACTION_APP_FOREGROUND = "com.momentummm.app.action.NUCLEAR_APP_FOREGROUND"
        const val ACTION_APP_BACKGROUND = "com.momentummm.app.action.NUCLEAR_APP_BACKGROUND"
        
        fun start(context: Context) {
            val intent = Intent(context, NuclearModeService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, NuclearModeService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
        
        fun notifyAppForeground(context: Context) {
            val intent = Intent(context, NuclearModeService::class.java).apply {
                action = ACTION_APP_FOREGROUND
            }
            context.startService(intent)
        }
        
        fun notifyAppBackground(context: Context) {
            val intent = Intent(context, NuclearModeService::class.java).apply {
                action = ACTION_APP_BACKGROUND
            }
            context.startService(intent)
        }
    }
    
    @Inject
    lateinit var smartBlockingManager: SmartBlockingManager
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var timerJob: Job? = null
    private var isAppInForeground = false
    
    private val _currentWaitSeconds = MutableStateFlow(0)
    val currentWaitSeconds: StateFlow<Int> = _currentWaitSeconds.asStateFlow()
    
    private val _requiredWaitSeconds = MutableStateFlow(0)
    val requiredWaitSeconds: StateFlow<Int> = _requiredWaitSeconds.asStateFlow()
    
    private val _isUnlockAvailable = MutableStateFlow(false)
    val isUnlockAvailable: StateFlow<Boolean> = _isUnlockAvailable.asStateFlow()
    
    private val binder = NuclearBinder()
    
    inner class NuclearBinder : Binder() {
        fun getService(): NuclearModeService = this@NuclearModeService
    }
    
    override fun onBind(intent: Intent): IBinder = binder
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startNuclearTimer()
            }
            ACTION_STOP -> {
                stopNuclearTimer()
                stopSelf()
            }
            ACTION_APP_FOREGROUND -> {
                isAppInForeground = true
                Log.d(TAG, "App en primer plano - timer activo")
            }
            ACTION_APP_BACKGROUND -> {
                isAppInForeground = false
                Log.d(TAG, "App en segundo plano - timer pausado")
            }
        }
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        serviceScope.cancel()
    }
    
    private fun startNuclearTimer() {
        startForeground(NOTIFICATION_ID, buildNotification())
        
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            val config = smartBlockingManager.config.value
            _requiredWaitSeconds.value = config.nuclearModeUnlockWaitMinutes * 60
            
            while (isActive) {
                delay(1000) // Cada segundo
                
                if (isAppInForeground) {
                    // Solo incrementar el timer si la app está en primer plano
                    val completed = smartBlockingManager.updateNuclearWaitProgress(1)
                    
                    val currentConfig = smartBlockingManager.config.value
                    _currentWaitSeconds.value = currentConfig.nuclearModeCurrentWaitSeconds
                    
                    if (completed) {
                        _isUnlockAvailable.value = true
                        Log.d(TAG, "¡Tiempo de espera completado! Desbloqueo disponible")
                        showUnlockAvailableNotification()
                        break
                    }
                    
                    // Actualizar notificación cada 30 segundos
                    if (_currentWaitSeconds.value % 30 == 0) {
                        updateNotification()
                    }
                }
            }
        }
    }
    
    private fun stopNuclearTimer() {
        timerJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Modo Nuclear",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificaciones del Modo Nuclear"
                setShowBadge(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val config = smartBlockingManager.config.value
        val currentSeconds = config.nuclearModeCurrentWaitSeconds
        val requiredSeconds = config.nuclearModeUnlockWaitMinutes * 60
        val remainingSeconds = (requiredSeconds - currentSeconds).coerceAtLeast(0)
        val remainingMinutes = remainingSeconds / 60
        val remainingDays = smartBlockingManager.getNuclearModeRemainingDays()
        
        val progress = if (requiredSeconds > 0) {
            (currentSeconds * 100) / requiredSeconds
        } else 0
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("☢️ Modo Nuclear Activo")
            .setContentText("$remainingDays días restantes | Timer: $remainingMinutes min para desbloqueo")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(100, progress, false)
            .addAction(
                R.drawable.ic_launcher_foreground,
                if (isAppInForeground) "Timer Activo ⏱️" else "Abre InTime para continuar",
                pendingIntent
            )
            .build()
    }
    
    private fun updateNotification() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }
    
    private fun showUnlockAvailableNotification() {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("show_nuclear_unlock", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🎉 ¡Desbloqueo Nuclear Disponible!")
            .setContentText("Has completado el tiempo de espera. Puedes desactivar el Modo Nuclear.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }
}
