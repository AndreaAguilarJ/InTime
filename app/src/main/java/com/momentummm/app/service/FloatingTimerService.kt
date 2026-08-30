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
import com.momentummm.app.ui.theme.momentum
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

        // Estado mínimo para reconstruir el overlay si el sistema mata el
        // servicio y lo recrea con START_STICKY (intent nulo).
        private const val STATE_PREFS = "floating_timer_state"
        private const val KEY_APP_NAME = "app_name"
        private const val KEY_PACKAGE_NAME = "package_name"
        private const val KEY_REMAINING = "remaining_minutes"
        private const val KEY_TOTAL = "total_minutes"

        /** Margen del overlay respecto a la esquina elegida. */
        private const val EDGE_MARGIN_X = 20
        private const val EDGE_MARGIN_Y = 100
        
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

    /**
     * Se conservan los LayoutParams para poder mover la ventana con
     * `updateViewLayout` cuando el usuario cambia la esquina. Antes la posición
     * sólo se aplicaba al crear la vista, así que cambiarla no movía nada.
     */
    private var layoutParams: WindowManager.LayoutParams? = null

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
    private var sizeName = "MEDIUM"

    /**
     * `true` cuando ya se leyó la configuración al menos una vez.
     *
     * La carga desde Room es asíncrona y `onStartCommand` es síncrono, así que
     * antes el overlay se dibujaba con los valores por defecto y la posición
     * guardada llegaba demasiado tarde para aplicarse.
     */
    @Volatile
    private var configLoaded = false
    
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
                saveState()
                // Se espera a tener la configuración ANTES de dibujar, para que
                // la primera aparición ya use la esquina y el tamaño elegidos.
                showWhenConfigReady()
            }
            ACTION_UPDATE -> {
                remainingMinutes = intent.getIntExtra(EXTRA_REMAINING_MINUTES, remainingMinutes)
                Log.d(TAG, "Updating timer: $remainingMinutes min remaining")
                saveState()
                if (floatingView == null) {
                    // El sistema pudo matar el servicio y recrearlo por
                    // START_STICKY sin overlay. Antes esta rama recomponía una
                    // vista inexistente y el contador no volvía nunca.
                    Log.d(TAG, "Overlay ausente en ACTION_UPDATE: se vuelve a dibujar")
                    showWhenConfigReady()
                } else {
                    updateFloatingTimer()
                }
            }
            ACTION_STOP -> {
                Log.d(TAG, "Stopping floating timer")
                clearState()
                hideFloatingTimer()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            null -> {
                // Reinicio por START_STICKY: recuperar el estado guardado y
                // volver a dibujar. Sin esto el servicio quedaba vivo pero
                // vacío, con el usuario dentro de una app limitada y sin timer.
                if (restoreState()) {
                    Log.d(TAG, "Servicio recreado por el sistema: restaurando overlay")
                    lifecycleRegistry.currentState = Lifecycle.State.STARTED
                    showWhenConfigReady()
                } else {
                    Log.d(TAG, "Servicio recreado sin estado guardado: se detiene")
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
            else -> {
                Log.w(TAG, "Unknown action: ${intent.action}")
            }
        }
        return START_STICKY
    }

    /**
     * Dibuja el overlay en cuanto la configuración esté disponible.
     * Si ya se cargó antes, se dibuja de inmediato sin esperar a Room.
     */
    private fun showWhenConfigReady() {
        serviceScope.launch {
            try {
                ensureConfigLoaded()
                showFloatingTimer()
            } catch (e: Exception) {
                Log.e(TAG, "Error showing floating timer", e)
            }
        }
    }

    private suspend fun ensureConfigLoaded() {
        if (configLoaded) return
        try {
            val config = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(applicationContext).smartBlockingConfigDao().getConfigSync()
            }
            config?.let { applyConfig(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo configuración inicial del timer", e)
        }
        configLoaded = true
    }

    private fun applyConfig(config: SmartBlockingConfig) {
        opacity = config.floatingTimerOpacity
        position = config.floatingTimerPosition
        sizeName = config.floatingTimerSize
    }

    // ── Estado persistido ─────────────────────────────────────────────────
    // Los cuatro datos del overlay vivían sólo en memoria, así que un reinicio
    // del servicio los perdía. Se guardan en SharedPreferences para poder
    // reconstruir la vista tal como estaba.

    private val statePrefs by lazy {
        applicationContext.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE)
    }

    private fun saveState() {
        try {
            statePrefs.edit()
                .putString(KEY_APP_NAME, currentAppName)
                .putString(KEY_PACKAGE_NAME, currentPackageName)
                .putInt(KEY_REMAINING, remainingMinutes)
                .putInt(KEY_TOTAL, totalMinutes)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando estado del timer", e)
        }
    }

    /** Devuelve `true` si había un estado utilizable que restaurar. */
    private fun restoreState(): Boolean {
        return try {
            val pkg = statePrefs.getString(KEY_PACKAGE_NAME, "") ?: ""
            if (pkg.isEmpty()) return false
            currentAppName = statePrefs.getString(KEY_APP_NAME, "") ?: ""
            currentPackageName = pkg
            remainingMinutes = statePrefs.getInt(KEY_REMAINING, 0)
            totalMinutes = statePrefs.getInt(KEY_TOTAL, 0)
            // Sin minutos restantes no hay nada que mostrar: el límite ya cayó.
            remainingMinutes > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error restaurando estado del timer", e)
            false
        }
    }

    private fun clearState() {
        try {
            statePrefs.edit().clear().apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error limpiando estado del timer", e)
        }
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
                        val positionChanged = it.floatingTimerPosition != position
                        applyConfig(it)
                        configLoaded = true
                        if (floatingView != null) {
                            // Mover la ventana requiere updateViewLayout: volver
                            // a componer Compose no cambia la gravedad del
                            // WindowManager, que es lo que decide la esquina.
                            if (positionChanged) applyPositionToWindow()
                            updateFloatingTimer()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading config", e)
            }
        }
    }

    private fun applyPositionToWindow() {
        val view = floatingView ?: return
        val params = layoutParams ?: return
        params.gravity = getGravityFromPosition(position)
        // Se reinician los desplazamientos: si el usuario había arrastrado el
        // overlay, conservar el offset anterior lo dejaría fuera de la esquina
        // que acaba de elegir.
        params.x = EDGE_MARGIN_X
        params.y = EDGE_MARGIN_Y
        try {
            windowManager?.updateViewLayout(view, params)
            Log.d(TAG, "Timer flotante movido a $position")
        } catch (e: Exception) {
            Log.e(TAG, "Error moviendo el timer flotante", e)
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
        
        val params = WindowManager.LayoutParams(
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
            x = EDGE_MARGIN_X
            y = EDGE_MARGIN_Y
        }
        layoutParams = params

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
                        scale = scaleForSize(sizeName),
                        onClose = {
                            stop(this@FloatingTimerService)
                        }
                    )
                }
            }
        }

        try {
            windowManager?.addView(floatingView, params)
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
                        scale = scaleForSize(sizeName),
                        onClose = {
                            stop(this@FloatingTimerService)
                        }
                    )
                }
            }
        }
    }

    /**
     * Factor de escala del overlay.
     *
     * `floatingTimerSize` se guardaba pero nadie lo leía: elegir Pequeño,
     * Mediano o Grande no cambiaba nada en pantalla.
     */
    private fun scaleForSize(size: String): Float = when (size) {
        "SMALL" -> 0.8f
        "LARGE" -> 1.3f
        else -> 1f
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
        layoutParams = null
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
                getString(R.string.svc_floating_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.svc_floating_channel_desc)
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
            .setContentTitle(getString(R.string.svc_floating_title))
            .setContentText(getString(R.string.svc_floating_text, currentAppName, remainingMinutes))
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
    scale: Float,
    onClose: () -> Unit
) {
    val timerColor = when {
        remainingMinutes <= 5 -> MaterialTheme.momentum.danger   // Urgente
        remainingMinutes <= 15 -> MaterialTheme.momentum.warning // Advertencia
        else -> MaterialTheme.momentum.success                   // OK
    }

    // La opacidad se aplica una sola vez, en el Surface, para evitar el efecto
    // cuadrático que dejaba el overlay casi invisible. El color de superficie
    // sigue el tema en vez de un negro fijo.
    val backgroundColor = MaterialTheme.momentum.surfaceElevated

    Surface(
        modifier = Modifier
            .padding((8 * scale).dp)
            .alpha(opacity),
        shape = RoundedCornerShape((16 * scale).dp),
        color = backgroundColor,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = (12 * scale).dp, vertical = (8 * scale).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((8 * scale).dp)
        ) {
            // Punto de estado: el color comunica la urgencia, sin emoji.
            Box(
                modifier = Modifier
                    .size((8 * scale).dp)
                    .background(timerColor, RoundedCornerShape((4 * scale).dp))
            )

            // Tiempo restante
            Column {
                Text(
                    text = formatTime(remainingMinutes),
                    color = timerColor,
                    fontSize = (18 * scale).sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = appName.take(12) + if (appName.length > 12) "..." else "",
                    color = MaterialTheme.momentum.textSecondary,
                    fontSize = (10 * scale).sp
                )
            }
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
