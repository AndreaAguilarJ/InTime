package com.momentummm.app.ui.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.momentummm.app.data.usage.ForegroundAppTracker
import com.momentummm.app.ui.system.*
import com.momentummm.app.ui.theme.MomentumTheme
import com.momentummm.app.util.BlockingCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.momentummm.app.ui.theme.*
import androidx.compose.ui.res.stringResource
import com.momentummm.app.R

class AppBlockOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "blocked_app_package"
        const val EXTRA_BLOCKED_APP_NAME = "blocked_app_name"
        const val EXTRA_DAILY_LIMIT = "daily_limit"
        const val EXTRA_REASON = "block_reason"

        private const val NOTIFICATION_ID = 1003
        private const val CHANNEL_ID = "app_block_overlay_channel"

        /** Cada cuánto se comprueba si la app bloqueada sigue en primer plano. */
        private const val WATCH_INTERVAL_MS = 700L

        /**
         * Margen antes de empezar a vigilar. La app en primer plano se deriva
         * de eventos de UsageStats, que pueden tardar en propagarse; sin este
         * margen el overlay podría retirarse justo después de aparecer.
         */
        private const val GRACE_PERIOD_MS = 1_500L
    }

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Vigila que la app bloqueada siga en primer plano. */
    private var watchJob: Job? = null

    /** Paquete que el overlay está tapando ahora mismo. */
    private var currentBlockedPackage: String = ""

    // Lifecycle management
    private lateinit var lifecycleRegistry: LifecycleRegistry
    private lateinit var store: ViewModelStore
    private lateinit var savedStateRegistryController: SavedStateRegistryController

    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry = LifecycleRegistry(this)
        store = ViewModelStore()
        savedStateRegistryController = SavedStateRegistryController.create(this)
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        // Se arranca con startForegroundService: hay que promoverlo de inmediato
        // o el sistema mata el proceso. Además garantiza que el overlay no se
        // destruya mientras la app bloqueada sigue abierta.
        promoteToForeground()
    }

    private fun promoteToForeground() {
        try {
            val manager = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(com.momentummm.app.R.string.notification_channel_monitoring_name),
                    NotificationManager.IMPORTANCE_MIN
                ).apply { setShowBadge(false) }
            )

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(com.momentummm.app.R.string.app_blocked_time_up_title))
                .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
                .setOngoing(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build()

            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            // Si startForeground falla, el servicio queda "arrancado pero no
            // en primer plano" y Android 14+ lo mata con
            // ForegroundServiceDidNotStartInTimeException, tumbando la app.
            // Mejor rendirse aquí: el llamador tiene otras vías de bloqueo.
            android.util.Log.e("AppBlockOverlayService", "No se pudo promover a foreground", e)
            stopSelf()
        }
    }

    /**
     * Android puede imponer un límite de tiempo a los foreground services de
     * tipo specialUse. Si llega, se retira el overlay de forma ordenada en
     * lugar de dejar que el sistema lance una excepción.
     */
    override fun onTimeout(startId: Int) {
        android.util.Log.w("AppBlockOverlayService", "onTimeout: retirando overlay")
        dismissOverlay()
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val blockedAppPackage = intent?.getStringExtra(EXTRA_BLOCKED_PACKAGE) ?: ""
        val blockedAppName = intent?.getStringExtra(EXTRA_BLOCKED_APP_NAME) ?: "Aplicación"
        val dailyLimit = intent?.getIntExtra(EXTRA_DAILY_LIMIT, 0) ?: 0
        val reason = intent?.getStringExtra(EXTRA_REASON)

        lifecycleRegistry.currentState = Lifecycle.State.STARTED

        // Si ya hay un overlay para OTRA app, se recrea con los datos nuevos.
        // Antes `showOverlay` salía sin hacer nada y el overlay se quedaba
        // mostrando información de la app anterior.
        if (overlayView != null && currentBlockedPackage != blockedAppPackage) {
            removeOverlayView()
        }

        showOverlay(blockedAppPackage, blockedAppName, dailyLimit, reason)

        return START_NOT_STICKY
    }

    private fun showOverlay(
        blockedAppPackage: String,
        blockedAppName: String,
        dailyLimit: Int,
        reason: String?
    ) {
        if (overlayView != null) return // Ya hay un overlay activo para esta app

        // Sin permiso de superposición, addView lanza excepción: mejor salir
        // limpiamente para que el llamador pueda usar otra vía.
        if (!android.provider.Settings.canDrawOverlays(this)) {
            android.util.Log.w("AppBlockOverlayService", "Sin permiso de superposición; no se puede mostrar el overlay")
            stopSelf()
            return
        }

        currentBlockedPackage = blockedAppPackage
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Configuración de la ventana overlay
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // Flags que BLOQUEAN toda interacción con la app de debajo:
        // FLAG_NOT_TOUCH_MODAL se OMITE para que el overlay capture todos los toques.
        // FLAG_NOT_FOCUSABLE se OMITE para que el overlay reciba el foco.
        //
        // BUG CORREGIDO: aquí había además FLAG_KEEP_SCREEN_ON. Como el overlay
        // sólo desaparecía cuando el usuario pulsaba stringResource(R.string.block_close), ese flag dejaba
        // la pantalla encendida de forma indefinida —toda la noche si el límite
        // se alcanzaba con el teléfono en el bolsillo—. El overlay ya no
        // necesita mantener la pantalla despierta: se retira solo (ver
        // [startForegroundWatch]).
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }

        // Crear ComposeView para el overlay
        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@AppBlockOverlayService)
            setViewTreeViewModelStoreOwner(this@AppBlockOverlayService)
            setViewTreeSavedStateRegistryOwner(this@AppBlockOverlayService)

            setContent {
                MomentumTheme {
                    AppBlockOverlayContent(
                        blockedAppName = blockedAppName,
                        dailyLimit = dailyLimit,
                        reason = reason,
                        onDismiss = { navigateToHomeAndDismiss() },
                        onOpenMomentum = { openMomentumApp() }
                    )
                }
            }
        }

        try {
            windowManager?.addView(overlayView, params)
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
            startForegroundWatch(blockedAppPackage)
        } catch (e: Exception) {
            e.printStackTrace()
            overlayView = null
            stopSelf()
        }
    }

    /**
     * Retira el overlay automáticamente cuando deja de tener sentido.
     *
     * ─── EL BUG QUE ESTO ARREGLA ──────────────────────────────────────────
     * El overlay sólo desaparecía si el usuario pulsaba stringResource(R.string.block_close), y ese botón
     * se habilita a los 10 segundos. Consecuencia: bastaba pulsar el botón de
     * inicio para salir de la app bloqueada y **el overlay se quedaba encima
     * del lanzador**, tapando el teléfono entero hasta que el usuario
     * encontraba el botón y esperaba la cuenta atrás. Con `FLAG_KEEP_SCREEN_ON`
     * además impedía que la pantalla se apagara.
     *
     * El overlay existe para tapar UNA app concreta. En cuanto esa app deja de
     * estar en primer plano —o la pantalla se apaga— su trabajo ha terminado.
     */
    private fun startForegroundWatch(blockedAppPackage: String) {
        watchJob?.cancel()
        if (blockedAppPackage.isEmpty()) return

        watchJob = serviceScope.launch {
            // Margen inicial: la app en primer plano se resuelve a partir de
            // eventos del sistema que pueden tardar unos cientos de ms en
            // aparecer. Sin este margen el overlay podría retirarse solo justo
            // después de mostrarse.
            delay(GRACE_PERIOD_MS)

            while (isActive) {
                val screenUsable = BlockingCapabilities.isScreenUsable(this@AppBlockOverlayService)
                val foreground = ForegroundAppTracker.current(this@AppBlockOverlayService)

                // La app bloqueada dejó de estar delante (inicio, otra app,
                // Momentum) o el usuario apagó la pantalla.
                val stillBlocking = screenUsable && foreground == blockedAppPackage
                if (!stillBlocking) {
                    android.util.Log.d(
                        "AppBlockOverlayService",
                        "Retirando overlay: pantalla=$screenUsable primerPlano=$foreground"
                    )
                    withContext(Dispatchers.Main) { dismissOverlay() }
                    return@launch
                }

                delay(WATCH_INTERVAL_MS)
            }
        }
    }

    private var isDestroyed = false

    /** Quita la vista del WindowManager sin detener el servicio. */
    private fun removeOverlayView() {
        watchJob?.cancel()
        watchJob = null
        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        overlayView = null
    }

    private fun dismissOverlay() {
        if (isDestroyed) return // Evitar llamadas duplicadas

        removeOverlayView()
        currentBlockedPackage = ""

        if (lifecycleRegistry.currentState != Lifecycle.State.DESTROYED) {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }
        stopSelf()
    }

    private fun openMomentumApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val launched = runCatching {
            if (intent != null) {
                startActivity(intent)
            } else {
                startActivity(homeIntent())
            }
        }.isSuccess
        if (!launched) {
            android.util.Log.w("AppBlockOverlayService", "No se pudo abrir Momentum desde el overlay")
        }
        dismissOverlay()
    }

    private fun homeIntent(): Intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_HOME)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /**
     * "Cerrar e ir al inicio": saca al usuario de la app bloqueada.
     *
     * Si el sistema rechaza el lanzamiento (las restricciones de inicio de
     * actividades en segundo plano varían por versión y fabricante) el overlay
     * se retira igualmente: el servicio de monitoreo detectará que el usuario
     * sigue dentro de la app bloqueada y volverá a taparla en un par de
     * segundos. Antes esto no se comprobaba y el usuario podía quedarse dentro
     * de la app sin overlay.
     */
    private fun navigateToHomeAndDismiss() {
        runCatching { startActivity(homeIntent()) }
            .onFailure {
                android.util.Log.w("AppBlockOverlayService", "No se pudo ir al inicio", it)
            }
        dismissOverlay()
    }

    override fun onDestroy() {
        if (isDestroyed) {
            super.onDestroy()
            return
        }
        isDestroyed = true

        removeOverlayView()
        serviceScope.cancel()

        try {
            store.clear()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (lifecycleRegistry.currentState != Lifecycle.State.DESTROYED) {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }
        super.onDestroy()
    }
}

@Composable
private fun AppBlockOverlayContent(
    blockedAppName: String,
    dailyLimit: Int,
    reason: String?,
    onDismiss: () -> Unit,
    onOpenMomentum: () -> Unit
) {
    // CRITICAL FIX: El countdown ahora solo es un timer de espera para el botón stringResource(R.string.block_close)
    // Ya NO auto-destruye el overlay después de terminar
    var countdown by remember { mutableStateOf(10) }
    var canDismiss by remember { mutableStateOf(false) }

    // Countdown timer - solo habilita el botón, NO auto-cierra
    LaunchedEffect(Unit) {
        repeat(10) {
            delay(1000)
            countdown--
        }
        canDismiss = true
        // NO llamar onDismiss() aquí - el overlay PERMANECE hasta que el usuario actúe
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Opaco a propósito. Antes el degradado era translúcido
            // (alpha 0.9/0.95) y el contenido de la app bloqueada seguía siendo
            // legible por detrás: se podía leer el feed a través de la pantalla
            // de bloqueo, que es justo lo que se intenta interrumpir.
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Neutral950,
                        Neutral900
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Scroll para que el botón stringResource(R.string.block_close) y las sugerencias sigan
                // siendo alcanzables en pantallas pequeñas o con fuente grande:
                // el contenido fijo se salía por abajo sin posibilidad de verlo.
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icono de bloqueo
            Surface(
                modifier = Modifier.size(120.dp),
                color = MaterialTheme.colorScheme.error,
                shape = RoundedCornerShape(60.dp)
            ) {
                Icon(
                    Icons.Filled.Block,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Título principal
            Text(
                text = stringResource(R.string.block_time_up),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Motivo del bloqueo. Si el bloqueo no viene del límite diario
            // (modo nuclear, horario, categoría, ventana de sueño...) se muestra
            // la razón concreta en lugar del texto genérico de límite.
            Text(
                text = reason ?: stringResource(R.string.block_daily_limit_reason, dailyLimit),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = blockedAppName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Motivación
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "\"El autocontrol es la fuerza de voluntad que nos permite alcanzar nuestras metas\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Botones de acción
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MomentumButton(
                    onClick = onOpenMomentum,
                    style = ButtonStyle.Primary,
                    size = ButtonSize.Large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.SelfImprovement,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Abrir Momentum")
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canDismiss,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White,
                        disabledContentColor = Color.White.copy(alpha = 0.4f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (canDismiss) Color.White.copy(alpha = 0.5f)
                        else Color.White.copy(alpha = 0.2f)
                    )
                ) {
                    Text(
                        if (!canDismiss) "Cerrar ($countdown)" else "Cerrar e ir al inicio"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sugerencias
            Text(
                text = stringResource(R.string.block_suggestions_header),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            val suggestions = listOf(
                stringResource(R.string.block_sugg_read),
                stringResource(R.string.block_sugg_walk),
                stringResource(R.string.block_sugg_meditate),
                stringResource(R.string.block_sugg_stretch),
                stringResource(R.string.block_sugg_water)
            )

            suggestions.take(2).forEach { suggestion ->
                Text(
                    text = suggestion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}
