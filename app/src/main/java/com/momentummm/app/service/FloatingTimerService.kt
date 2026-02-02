package com.momentummm.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.momentummm.app.MainActivity
import com.momentummm.app.R
import com.momentummm.app.data.AppDatabase
import com.momentummm.app.data.entity.SmartBlockingConfig
import com.momentummm.app.ui.theme.MomentumTheme
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt

/**
 * Servicio de Timer Flotante que muestra un overlay persistente
 * con el tiempo restante de uso de la app actual.
 * 
 * CARACTERÍSTICAS:
 * - Visible sobre todas las apps
 * - Transparencia ajustable
 * - Posición personalizable (esquinas)
 * - Se puede arrastrar
 * - Muestra countdown en tiempo real
 * - NUNCA desaparece hasta que se cierra la app o se alcanza el límite
 */
class FloatingTimerService : Service(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    companion object {
        private const val TAG = "FloatingTimerService"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "floating_timer_channel"
        
        const val ACTION_START = "com.momentummm.app.action.START_FLOATING_TIMER"
        const val ACTION_STOP = "com.momentummm.app.action.STOP_FLOATING_TIMER"
        const val ACTION_UPDATE = "com.momentummm.app.action.UPDATE_FLOATING_TIMER"
        
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_REMAINING_MINUTES = "remaining_minutes"
        const val EXTRA_TOTAL_MINUTES = "total_minutes"
        const val EXTRA_PACKAGE_NAME = "package_name"
        
        fun start(
            context: Context,
            appName: String,
            packageName: String,
            remainingMinutes: Int,
            totalMinutes: Int
        ) {
            val intent = Intent(context, FloatingTimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_APP_NAME, appName)
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                putExtra(EXTRA_REMAINING_MINUTES, remainingMinutes)
                putExtra(EXTRA_TOTAL_MINUTES, totalMinutes)
            }
            context.startForegroundService(intent)
        }
        
        fun update(context: Context, remainingMinutes: Int) {
            val intent = Intent(context, FloatingTimerService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_REMAINING_MINUTES, remainingMinutes)
            }
            context.startService(intent)
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, FloatingTimerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
        
        fun canDrawOverlays(context: Context): Boolean {
            return Settings.canDrawOverlays(context)
        }
    }

    private var windowManager: WindowManager? = null
    private var floatingView: ComposeView? = null
    private val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Coroutine exception", throwable)
    }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main + exceptionHandler)
    
    // Estado del timer
    private var currentAppName = ""
    private var currentPackageName = ""
    private var remainingMinutes = 0
    private var totalMinutes = 0
    private var opacity = 0.8f
    private var position = "TOP_RIGHT"
    
    // Lifecycle
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
    
    override val viewModelStore: ViewModelStore
        get() = store

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        createNotificationChannel()
        loadConfigFromDatabase()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called with action: ${intent?.action}")
        
        // CRÍTICO: Llamar startForeground inmediatamente para evitar ANR de 5 segundos
        try {
            startForeground(NOTIFICATION_ID, buildNotification())
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground service immediately", e)
        }
        
        when (intent?.action) {
            ACTION_START -> {
                currentAppName = intent.getStringExtra(EXTRA_APP_NAME) ?: ""
                currentPackageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
                remainingMinutes = intent.getIntExtra(EXTRA_REMAINING_MINUTES, 0)
                totalMinutes = intent.getIntExtra(EXTRA_TOTAL_MINUTES, 0)
                
                Log.d(TAG, "Starting floating timer for $currentAppName, remaining: $remainingMinutes/$totalMinutes")
                
                lifecycleRegistry.currentState = Lifecycle.State.STARTED
                try {
                    showFloatingTimer()
                } catch (e: Exception) {
                    Log.e(TAG, "Error showing floating timer", e)
                }
            }
            ACTION_UPDATE -> {
                remainingMinutes = intent.getIntExtra(EXTRA_REMAINING_MINUTES, remainingMinutes)
                Log.d(TAG, "Updating timer: $remainingMinutes min remaining")
                updateFloatingTimer()
            }
            ACTION_STOP -> {
                Log.d(TAG, "Stopping floating timer")
                hideFloatingTimer()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            null -> {
                // Service restarted by system, just keep running
                Log.d(TAG, "Service restarted by system")
            }
            else -> {
                Log.w(TAG, "Unknown action: ${intent.action}")
            }
        }
        return START_STICKY
    }
    
    private var isDestroyed = false

    override fun onDestroy() {
        if (isDestroyed) {
            super.onDestroy()
            return
        }
        isDestroyed = true
        
        hideFloatingTimer()
        
        if (lifecycleRegistry.currentState != Lifecycle.State.DESTROYED) {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }
        
        try {
            store.clear()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing ViewModelStore", e)
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun loadConfigFromDatabase() {
        serviceScope.launch {
            try {
                val database = AppDatabase.getDatabase(applicationContext)
                database.smartBlockingConfigDao().getConfig().collectLatest { config ->
                    config?.let {
                        opacity = it.floatingTimerOpacity
                        position = it.floatingTimerPosition
                        // Actualizar UI si está visible
                        if (floatingView != null) {
                            updateFloatingTimer()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading config", e)
            }
        }
    }

    private fun showFloatingTimer() {
        if (!canDrawOverlays(this)) {
            Log.e(TAG, "No overlay permission")
            return
        }
        
        if (floatingView != null) {
            updateFloatingTimer()
            return
        }

        windowManager = getSystemService(WINDOW_SERVICE) as? WindowManager
        if (windowManager == null) {
            Log.e(TAG, "WindowManager not available")
            return
        }
        
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = getGravityFromPosition(position)
            x = 20
            y = 100
        }

        floatingView = ComposeView(this).apply {
            // Configurar todos los ViewTree owners necesarios para Compose
            setViewTreeLifecycleOwner(this@FloatingTimerService)
            setViewTreeViewModelStoreOwner(this@FloatingTimerService)
            setViewTreeSavedStateRegistryOwner(this@FloatingTimerService)
            
            setContent {
                MomentumTheme {
                    FloatingTimerContent(
                        appName = currentAppName,
                        remainingMinutes = remainingMinutes,
                        totalMinutes = totalMinutes,
                        opacity = opacity,
                        onClose = {
                            stop(this@FloatingTimerService)
                        }
                    )
                }
            }
        }

        try {
            windowManager?.addView(floatingView, layoutParams)
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
            Log.d(TAG, "Floating timer shown for $currentAppName")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing floating timer", e)
        }
    }

    private fun updateFloatingTimer() {
        floatingView?.let { view ->
            view.setContent {
                MomentumTheme {
                    FloatingTimerContent(
                        appName = currentAppName,
                        remainingMinutes = remainingMinutes,
                        totalMinutes = totalMinutes,
                        opacity = opacity,
                        onClose = {
                            stop(this@FloatingTimerService)
                        }
                    )
                }
            }
        }
    }

    private fun hideFloatingTimer() {
        floatingView?.let { view ->
            try {
                // CRÍTICO: Desasociar ViewTree owners ANTES de remover para evitar memory leak
                view.setViewTreeLifecycleOwner(null)
                view.setViewTreeViewModelStoreOwner(null)
                view.setViewTreeSavedStateRegistryOwner(null)
                
                // Dispose la composición para liberar recursos
                (view as? androidx.compose.ui.platform.ComposeView)?.disposeComposition()
                
                windowManager?.removeView(view)
                Log.d(TAG, "Floating timer hidden")
            } catch (e: Exception) {
                Log.e(TAG, "Error hiding floating timer", e)
            }
        }
        floatingView = null
    }

    private fun getGravityFromPosition(pos: String): Int {
        return when (pos) {
            "TOP_LEFT" -> Gravity.TOP or Gravity.START
            "TOP_RIGHT" -> Gravity.TOP or Gravity.END
            "BOTTOM_LEFT" -> Gravity.BOTTOM or Gravity.START
            "BOTTOM_RIGHT" -> Gravity.BOTTOM or Gravity.END
            else -> Gravity.TOP or Gravity.END
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Timer Flotante",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Muestra el timer de uso de apps"
                setShowBadge(false)
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

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Timer activo")
            .setContentText("$currentAppName: $remainingMinutes min restantes")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}

@Composable
private fun FloatingTimerContent(
    appName: String,
    remainingMinutes: Int,
    totalMinutes: Int,
    opacity: Float,
    onClose: () -> Unit
) {
    val progress = if (totalMinutes > 0) {
        remainingMinutes.toFloat() / totalMinutes.toFloat()
    } else 0f
    
    val timerColor = when {
        remainingMinutes <= 5 -> Color(0xFFEF4444)  // Rojo - ¡Urgente!
        remainingMinutes <= 15 -> Color(0xFFF59E0B) // Naranja - Advertencia
        else -> Color(0xFF10B981)                    // Verde - OK
    }
    
    val backgroundColor = Color.Black.copy(alpha = opacity * 0.9f)
    
    Surface(
        modifier = Modifier
            .padding(8.dp)
            .alpha(opacity),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Indicador de color
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(timerColor, RoundedCornerShape(4.dp))
            )
            
            // Tiempo restante
            Column {
                Text(
                    text = formatTime(remainingMinutes),
                    color = timerColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = appName.take(12) + if (appName.length > 12) "..." else "",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
            
            // Emoji de estado
            Text(
                text = when {
                    remainingMinutes <= 5 -> "🔥"
                    remainingMinutes <= 15 -> "⚠️"
                    else -> "⏱️"
                },
                fontSize = 16.sp
            )
        }
    }
}

private fun formatTime(minutes: Int): String {
    return if (minutes >= 60) {
        val hours = minutes / 60
        val mins = minutes % 60
        "${hours}h ${mins}m"
    } else {
        "${minutes}m"
    }
}
