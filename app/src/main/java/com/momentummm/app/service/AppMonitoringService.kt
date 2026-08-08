package com.momentummm.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.app.usage.UsageStatsManager
import android.app.usage.UsageEvents
import android.content.Context
import android.content.pm.PackageManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import com.momentummm.app.data.repository.AppLimitRepository
import com.momentummm.app.data.repository.AppWhitelistRepository
import com.momentummm.app.data.repository.AppCategoryRepository
import com.momentummm.app.data.repository.GoalsRepository
import com.momentummm.app.data.UserPreferencesRepository
import com.momentummm.app.data.manager.SmartBlockingManager
import com.momentummm.app.data.manager.SmartNotificationManager
import com.momentummm.app.data.engine.AdaptiveBlockingManager
import com.momentummm.app.data.engine.BlockingEventType
import com.momentummm.app.data.engine.UsagePatternEngine
import com.momentummm.app.data.usage.DailyUsageCalculator
import com.momentummm.app.data.usage.ForegroundAppTracker
import com.momentummm.app.ui.AppBlockedActivity
import com.momentummm.app.ui.overlay.AppBlockOverlayService
import com.momentummm.app.util.BlockingCapabilities
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * APP MONITORING SERVICE V2 - Servicio de Monitoreo con IA Predictiva
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * Mejoras V2:
 * - Registro de uso en UsagePatternEngine para ML-like predictions
 * - Intervenciones inteligentes del AdaptiveBlockingManager
 * - Tracking de eventos para analytics
 * - Predicciones proactivas (advertir ANTES de que el usuario exceda)
 * - Auto-activación de perfiles de enfoque
 */
@AndroidEntryPoint
class AppMonitoringService : Service() {

    @Inject lateinit var appLimitRepository: AppLimitRepository
    @Inject lateinit var appWhitelistRepository: AppWhitelistRepository
    @Inject lateinit var appCategoryRepository: AppCategoryRepository
    @Inject lateinit var goalsRepository: GoalsRepository
    @Inject lateinit var smartNotificationManager: SmartNotificationManager
    @Inject lateinit var smartBlockingManager: SmartBlockingManager
    @Inject lateinit var patternEngine: UsagePatternEngine
    @Inject lateinit var adaptiveBlockingManager: AdaptiveBlockingManager

    private val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Coroutine exception in AppMonitoringService", throwable)
    }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)
    private var monitoringJob: Job? = null

    // El bloqueo INSTANTÁNEO al abrir una app ya lo hace MomentumAccessibilityService
    // (reacciona al cambio de ventana). Este poll solo cubre el cruce de límite
    // mientras la app sigue abierta y los modos/timers, así que su ritmo base se
    // relajó de 2s a 5s (menos wakeups/CPU con la pantalla encendida). El ritmo
    // agresivo se mantiene a 1s para volver a tapar al instante una app ya bloqueada.
    private val MONITORING_INTERVAL = 5000L // 5 segundos - detección rápida la cubre el a11y service
    private val MONITORING_INTERVAL_AGGRESSIVE = 1000L // 1 segundo para apps ya bloqueadas

    /** Con la pantalla apagada no hay nada que vigilar: se baja el ritmo. */
    private val SCREEN_OFF_INTERVAL = 15_000L

    /** Sin permiso de acceso al uso no se puede hacer nada; se reintenta despacio. */
    private val PERMISSION_RECHECK_INTERVAL = 30_000L

    /** Validez del resultado de la comprobación de AppOps. */
    private val USAGE_ACCESS_CHECK_TTL = 60_000L

    private var lastUsageAccessCheckAt = 0L
    private var cachedUsageAccess = false

    private var lastCheckedApp: String = ""
    private var lastBlockedTime: Long = 0
    private val BLOCK_COOLDOWN = 2000L // 2 segundos entre bloqueos de la misma app

    // Para tracking de notificaciones de advertencia
    private val warningNotifiedApps = mutableSetOf<String>()
    private val lastWarningTime = mutableMapOf<String, Long>()
    private val WARNING_COOLDOWN = 300000L // 5 minutos entre advertencias de la misma app

    // Para el Timer Flotante
    private var floatingTimerActive = false
    private var currentFloatingApp: String = ""

    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "app_monitoring_channel"
    
    // Flag para verificar si los repositorios están inicializados
    private val areRepositoriesInitialized: Boolean
        get() = ::appLimitRepository.isInitialized && 
                ::appWhitelistRepository.isInitialized && 
                ::appCategoryRepository.isInitialized &&
                ::goalsRepository.isInitialized &&
                ::smartNotificationManager.isInitialized &&
                ::smartBlockingManager.isInitialized &&
                ::patternEngine.isInitialized &&
                ::adaptiveBlockingManager.isInitialized
    
    // === NUEVO V2: Tracking de uso por sesión ===
    private var lastUsageRecordTime = 0L
    private val USAGE_RECORD_INTERVAL = 60_000L // Registrar cada 60s
    private var lastPatternAnalysisAt = 0L
    private val PATTERN_ANALYSIS_INTERVAL_MS = 3_600_000L // Máx. 1 análisis de patrones por hora (antes ~cada 2 min)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        try {
            createNotificationChannel()
            startForeground(NOTIFICATION_ID, createNotification())
        } catch (e: Exception) {
            android.util.Log.e("AppMonitoringService", "Error in onCreate", e)
            stopSelf()
            return
        }
        startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY // Reiniciar si el servicio es terminado
    }

    /**
     * Android 14+ (API 34) llama este método cuando un foreground service
     * excede su tiempo límite. Con specialUse esto no debería pasar,
     * pero lo manejamos por seguridad para evitar crashes.
     */
    override fun onTimeout(startId: Int) {
        Log.w(TAG, "onTimeout called - service exceeded time limit, restarting...")
        // Re-crear la notificación para mantener el servicio vivo
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
            startForeground(NOTIFICATION_ID, createNotification())
        } catch (e: Exception) {
            Log.e(TAG, "Error restarting foreground after timeout", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(com.momentummm.app.R.string.notification_channel_monitoring_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(com.momentummm.app.R.string.notification_channel_monitoring_desc)
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): android.app.Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
            ?: Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(com.momentummm.app.R.string.notification_monitoring_title))
            .setContentText(getString(com.momentummm.app.R.string.notification_monitoring_message))
            .setSmallIcon(android.R.drawable.ic_menu_view) // Usar icono del sistema
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun startMonitoring() {
        Log.d(TAG, "Iniciando monitoreo de aplicaciones")
        monitoringJob?.cancel()
        monitoringJob = serviceScope.launch {
            while (isActive) {
                var nextInterval = MONITORING_INTERVAL
                try {
                    // Verificar que los repositorios estén inicializados antes de proceder
                    if (!areRepositoriesInitialized) {
                        Log.w(TAG, "Repositorios no inicializados aún, esperando...")
                        delay(MONITORING_INTERVAL)
                        continue
                    }

                    // Actualizar estados de bloqueo inteligente
                    smartBlockingManager.refreshModeStates()

                    // ─── GUARDA 1: permiso de acceso al uso ───────────────
                    // Sin él `queryEvents` devuelve una lista vacía, el uso
                    // diario siempre es 0 y NUNCA se bloquea nada. Antes no se
                    // comprobaba: el fallo era total y completamente silencioso.
                    if (!hasUsageAccess()) {
                        Log.w(TAG, "Sin permiso de acceso al uso: el bloqueo no puede funcionar")
                        BlockEnforcer.notifyBlockingUnavailable(
                            this@AppMonitoringService,
                            BlockingCapabilities.MissingRequirement.USAGE_STATS
                        )
                        delay(PERMISSION_RECHECK_INTERVAL)
                        continue
                    }

                    // ─── GUARDA 2: pantalla apagada o bloqueada ───────────
                    // No hay ninguna app que tapar, y consultar UsageStats cada
                    // 2 s con la pantalla apagada sólo gasta batería.
                    if (!BlockingCapabilities.isScreenUsable(this@AppMonitoringService)) {
                        delay(SCREEN_OFF_INTERVAL)
                        continue
                    }

                    // === NUEVO V2: Análisis periódico de patrones ===
                    // Antes corría cada ~60 ciclos (~2 min): un análisis histórico
                    // completo (30-90 días, varias queries agregadas por app) cada
                    // par de minutos con la pantalla encendida. Los patrones no
                    // cambian tan rápido, así que ahora se limita a 1 vez por hora.
                    val nowForPatterns = System.currentTimeMillis()
                    if (nowForPatterns - lastPatternAnalysisAt > PATTERN_ANALYSIS_INTERVAL_MS) {
                        lastPatternAnalysisAt = nowForPatterns
                        try {
                            patternEngine.analyzeAllPatterns()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error en análisis periódico de patrones", e)
                        }
                    }

                    // Una sola resolución de la app en primer plano por ciclo.
                    // Antes se llamaba dos veces —una dentro de checkCurrentApp
                    // y otra para elegir el intervalo—, duplicando una consulta
                    // IPC que recorre los eventos de uso del sistema.
                    val currentApp = getCurrentForegroundApp()

                    // Intervalo agresivo mientras el usuario sigue dentro de una
                    // app ya bloqueada: hay que volver a taparla cuanto antes.
                    nextInterval = if (
                        currentApp.isNotEmpty() && smartBlockingManager.isAppBlockedToday(currentApp)
                    ) {
                        MONITORING_INTERVAL_AGGRESSIVE
                    } else {
                        MONITORING_INTERVAL
                    }

                    checkCurrentApp(currentApp)
                } catch (e: Exception) {
                    Log.e(TAG, "Error en el ciclo de monitoreo", e)
                }
                delay(nextInterval)
            }
        }
    }

    /**
     * Permiso de acceso al uso, con cache: consultar AppOps es una llamada IPC
     * y este bucle gira cada 1-2 segundos.
     */
    private fun hasUsageAccess(): Boolean {
        val now = System.currentTimeMillis()
        if (lastUsageAccessCheckAt != 0L && now - lastUsageAccessCheckAt < USAGE_ACCESS_CHECK_TTL) {
            return cachedUsageAccess
        }
        lastUsageAccessCheckAt = now
        cachedUsageAccess = BlockingCapabilities.hasUsageStatsPermission(this)
        return cachedUsageAccess
    }

    private fun stopMonitoring() {
        Log.d(TAG, "Deteniendo monitoreo de aplicaciones")
        monitoringJob?.cancel()
        monitoringJob = null
    }

    private suspend fun checkCurrentApp(currentApp: String) {
        try {
            // Timeout de 3 segundos para esta operación
            withTimeoutOrNull(3000L) {
                if (currentApp.isEmpty() || currentApp == packageName) {
                    // Si estamos en InTime, ocultar el timer flotante
                    if (floatingTimerActive) {
                        FloatingTimerService.stop(this@AppMonitoringService)
                        floatingTimerActive = false
                        currentFloatingApp = ""
                    }
                    return@withTimeoutOrNull
                }

                // Nada de infraestructura del sistema ni vías de emergencia.
                // Ver isUserBlockableApp: sin esta guarda, la Ventana de Sueño
                // llegaba a lanzar la pantalla de bloqueo sobre el diálogo de
                // permisos de Android.
                if (!isUserBlockableApp(currentApp)) {
                    return@withTimeoutOrNull
                }

                // VERIFICACIÓN 0: Ventana de Sueño - ignorar tracking si está configurado
                if (smartBlockingManager.shouldIgnoreUsageTracking()) {
                    Log.d(TAG, "Ventana de sueño activa - ignorando tracking para $currentApp")
                    return@withTimeoutOrNull
                }

                // VERIFICACIÓN 1: Modo Solo Comunicación
                val isCommunicationOnlyMode = smartBlockingManager.isCommunicationOnlyModeActive()
                if (isCommunicationOnlyMode) {
                    val allowedApps = smartBlockingManager.getCommunicationOnlyAllowedApps()
                    if (!allowedApps.contains(currentApp)) {
                        Log.d(TAG, "Modo Solo Comunicación activo - bloqueando $currentApp")
                        blockApp(currentApp, "Solo están permitidas apps de comunicación")
                        return@withTimeoutOrNull
                    }
                }

                // VERIFICACIÓN 2: Modo Nuclear
                if (smartBlockingManager.isAppInNuclearMode(currentApp)) {
                    val remainingDays = smartBlockingManager.getNuclearModeRemainingDays()
                    Log.d(TAG, "Modo Nuclear activo - bloqueando $currentApp (faltan $remainingDays días)")
                    blockApp(currentApp, "Modo Nuclear: Bloqueado por $remainingDays días más")
                    return@withTimeoutOrNull
                }

                // VERIFICACIÓN 3: Ventana de Sueño (bloqueo, no solo ignorar tracking)
                if (smartBlockingManager.isInSleepMode.value) {
                    // En modo sueño, bloquear todas las apps que no sean esenciales
                    val isWhitelisted = withContext(Dispatchers.IO) {
                        appWhitelistRepository.isAppWhitelisted(currentApp)
                    }
                    if (!isWhitelisted) {
                        Log.d(TAG, "Ventana de Sueño activa - bloqueando $currentApp")
                        blockApp(currentApp, "Es hora de descansar. Las apps estarán disponibles mañana.")
                        return@withTimeoutOrNull
                    }
                }

                // VERIFICACIÓN 4: Bloqueo por Contexto (ubicación, horario, WiFi)
                if (smartBlockingManager.isAppBlockedByContext(currentApp)) {
                    val activeRule = smartBlockingManager.activeContextRules.value.firstOrNull()
                    Log.d(TAG, "Bloqueo por contexto activo - bloqueando $currentApp")
                    blockApp(currentApp, "Bloqueado por regla: ${activeRule?.ruleName ?: "Contexto"}")
                    return@withTimeoutOrNull
                }

                // Verificar si la app está en la whitelist (apps de emergencia)
                val isWhitelisted = withContext(Dispatchers.IO) {
                    appWhitelistRepository.isAppWhitelisted(currentApp)
                }
                if (isWhitelisted) {
                    Log.d(TAG, "App $currentApp está en whitelist - no se bloqueará")
                    return@withTimeoutOrNull
                }
                
                // VERIFICACIÓN: Whitelist temporal para apps de compartir (durante flujo de shame share)
                val isInShareWhitelist = withContext(Dispatchers.IO) {
                    UserPreferencesRepository.isAppInShareWhitelist(this@AppMonitoringService, currentApp)
                }
                if (isInShareWhitelist) {
                    Log.d(TAG, "App $currentApp está en share whitelist temporal - permitiendo para compartir")
                    return@withTimeoutOrNull
                }

                // VERIFICACIÓN 5: Si la app ya está bloqueada hoy, bloquearla inmediatamente
                if (smartBlockingManager.isAppBlockedToday(currentApp)) {
                    // Primero verificar si tiene desbloqueo temporal activo
                    val isTemporarilyUnlocked = withContext(Dispatchers.IO) {
                        UserPreferencesRepository.isAppTemporarilyUnlocked(this@AppMonitoringService, currentApp)
                    }
                    
                    if (isTemporarilyUnlocked) {
                        Log.d(TAG, "App $currentApp tiene desbloqueo temporal activo - permitiendo")
                        return@withTimeoutOrNull
                    }

                    val activeLimit = withContext(Dispatchers.IO) {
                        appLimitRepository.getLimitByPackage(currentApp)
                    }

                    // BUG CORREGIDO: la marca "bloqueada hoy" se respetaba aunque
                    // el usuario hubiera borrado o desactivado el límite. Como
                    // esta comprobación va antes de leer el límite, la app
                    // quedaba bloqueada el resto del día sin ningún límite
                    // configurado y sin forma de recuperarla salvo esperar al
                    // día siguiente.
                    if (activeLimit == null || !activeLimit.isEnabled) {
                        Log.d(TAG, "App $currentApp ya no tiene límite activo - liberando bloqueo del día")
                        smartBlockingManager.temporarilyUnblockApp(currentApp)
                        return@withTimeoutOrNull
                    }

                    // CRITICAL FIX: La app ya alcanzó su límite hoy - bloquear SIEMPRE
                    // Solo respetar el cooldown mínimo para evitar crash por múltiples activities
                    if (smartBlockingManager.canShowBlockScreen(currentApp)) {
                        val dailyLimit = activeLimit.dailyLimitMinutes
                        Log.d(TAG, "App $currentApp está bloqueada hoy - FORZANDO pantalla de bloqueo")
                        smartBlockingManager.registerBlockScreenShown(currentApp)
                        blockApp(currentApp, "Ya alcanzaste tu límite de $dailyLimit minutos hoy")
                    } else {
                        // Incluso si no podemos mostrar pantalla (cooldown),
                        // volver a tapar la app para sacar al usuario de ella
                        Log.d(TAG, "App $currentApp bloqueada - reforzando bloqueo (cooldown activo)")
                        forceNavigateToHome(currentApp)
                    }
                    return@withTimeoutOrNull
                }

                // Focus Mode: bloqueo agresivo para apps en la lista negra
                val focusModeEnabled = withContext(Dispatchers.IO) {
                    UserPreferencesRepository.getFocusModeEnabled(this@AppMonitoringService)
                }
                
                var shouldContinue = true
                
                if (focusModeEnabled) {
                    val blockedApps = withContext(Dispatchers.IO) {
                        UserPreferencesRepository.getFocusModeBlockedApps(this@AppMonitoringService)
                    }
                    if (blockedApps.contains(currentApp)) {
                        Log.d(TAG, "Focus Mode activo - bloqueando app $currentApp")
                        blockApp(currentApp)
                        shouldContinue = false
                    }
                }

                if (shouldContinue) {
                    // CAMBIO IMPORTANTE: Primero verificar si la app tiene un límite configurado
                    // Solo monitoreamos apps que están explícitamente en la lista de límites
                    val appLimit = withContext(Dispatchers.IO) {
                        appLimitRepository.getLimitByPackage(currentApp)
                    }

                    // VERIFICACIÓN 6: Bloqueo por Horario (Schedule Limit)
                    // Feature solicitada: "block certain apps... at a certain time"
                    if (appLimit != null && appLimit.isEnabled && appLimit.isWithinScheduleBlock()) {
                        Log.d(TAG, "App $currentApp bloqueada por horario: ${appLimit.getScheduleFormatted()}")
                        blockApp(currentApp, "Bloqueada por horario: ${appLimit.getScheduleFormatted()}")
                        return@withTimeoutOrNull
                    }
                    
                    // VERIFICACIÓN 7: Bloqueo por Categoría
                    // Feature solicitada: "block apps via category"
                    val categoryBlockReason = withContext(Dispatchers.IO) {
                        appCategoryRepository.getCategoryBlockReason(currentApp)
                    }
                    if (categoryBlockReason != null) {
                        val message = when (categoryBlockReason) {
                            is com.momentummm.app.data.repository.CategoryBlockReason.LimitExceeded -> 
                                "Límite de categoría '${categoryBlockReason.categoryName}' excedido (${categoryBlockReason.limitMinutes}m)"
                            is com.momentummm.app.data.repository.CategoryBlockReason.ScheduleBlock ->
                                "Categoría '${categoryBlockReason.categoryName}' bloqueada: ${categoryBlockReason.startTime} - ${categoryBlockReason.endTime}"
                        }
                        Log.d(TAG, "App $currentApp bloqueada por categoría: $message")
                        blockApp(currentApp, message)
                        return@withTimeoutOrNull
                    }

                    // Si la app tiene límite configurado y está habilitada, procesarla
                    if (appLimit != null && appLimit.isEnabled) {
                        Log.d(TAG, "App monitoreada detectada: $currentApp")

                        // Obtener uso actual de la app
                        val cachedUsageMinutes =
                            (getCurrentAppUsageStats(currentApp) / 60000).toInt()

                        // INTEGRACIÓN BLOQUEO INTELIGENTE: Obtener límite efectivo
                        val originalLimit = appLimit.dailyLimitMinutes
                        val effectiveLimit = smartBlockingManager.getEffectiveDailyLimit(currentApp, originalLimit)

                        // A un minuto o menos del límite el valor cacheado ya no
                        // sirve: se fuerza el recálculo para que el bloqueo caiga
                        // lo más cerca posible del minuto exacto.
                        val usageMinutes = if (
                            effectiveLimit > 0 && cachedUsageMinutes >= effectiveLimit - 1
                        ) {
                            DailyUsageCalculator.foregroundMinutesToday(
                                this@AppMonitoringService,
                                currentApp,
                                maxAgeMs = 0L
                            )
                        } else {
                            cachedUsageMinutes
                        }

                        // === NUEVO V2: Registrar uso en el motor de patrones ===
                        val now = System.currentTimeMillis()
                        if (now - lastUsageRecordTime > USAGE_RECORD_INTERVAL) {
                            lastUsageRecordTime = now
                            serviceScope.launch(Dispatchers.IO) {
                                try {
                                    patternEngine.recordUsage(currentApp, usageMinutes)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error registrando uso en PatternEngine", e)
                                }
                            }
                        }

                        val usagePercent = if (effectiveLimit > 0) (usageMinutes * 100) / effectiveLimit else 0

                        Log.d(TAG, "App $currentApp - Uso: ${usageMinutes}m / ${effectiveLimit}m (original: ${originalLimit}m) (${usagePercent}%)")

                        // === NUEVO V2: Predicción proactiva ===
                        if (usagePercent in 50..70 && smartBlockingManager.willExceedLimit(currentApp, usageMinutes, effectiveLimit)) {
                            Log.d(TAG, "🔮 Predicción: $currentApp probablemente excederá el límite hoy")
                            serviceScope.launch(Dispatchers.IO) {
                                smartBlockingManager.trackBlockingEvent(
                                    currentApp, BlockingEventType.PREDICTION_GENERATED,
                                    "Predicted to exceed ${effectiveLimit}m limit"
                                )
                            }
                        }
                        
                        // === NUEVO V2: Intervenciones inteligentes ===
                        if (usagePercent in 60..90) {
                            serviceScope.launch(Dispatchers.IO) {
                                try {
                                    val intervention = smartBlockingManager.getSmartIntervention(currentApp, usageMinutes)
                                    if (intervention != null) {
                                        Log.d(TAG, "💡 Intervención: ${intervention.type.name} - ${intervention.message}")
                                        // La intervención se muestra a través de notificaciones
                                        smartBlockingManager.trackBlockingEvent(
                                            currentApp, BlockingEventType.INTERVENTION_SHOWN,
                                            intervention.type.name
                                        )
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error obteniendo intervención", e)
                                }
                            }
                        }

                        // === INTEGRACIÓN TIMER FLOTANTE ===
                        val floatingTimerEnabled = smartBlockingManager.isFloatingTimerEnabled()
                        val remainingMinutes = (effectiveLimit - usageMinutes).coerceAtLeast(0)
                        
                        if (floatingTimerEnabled && remainingMinutes > 0 && Settings.canDrawOverlays(this@AppMonitoringService)) {
                            // Mostrar o actualizar el timer flotante
                            if (!floatingTimerActive || currentFloatingApp != currentApp) {
                                FloatingTimerService.start(
                                    this@AppMonitoringService,
                                    appName = getAppName(currentApp),
                                    packageName = currentApp,
                                    remainingMinutes = remainingMinutes,
                                    totalMinutes = effectiveLimit
                                )
                                floatingTimerActive = true
                                currentFloatingApp = currentApp
                            } else {
                                FloatingTimerService.update(this@AppMonitoringService, remainingMinutes)
                            }
                        }

                        when {
                            // Límite alcanzado - bloquear
                            usageMinutes >= effectiveLimit -> {
                                val currentTime = System.currentTimeMillis()
                                if (currentApp != lastCheckedApp || (currentTime - lastBlockedTime) > BLOCK_COOLDOWN) {
                                    val blockReason = if (effectiveLimit < originalLimit) {
                                        when {
                                            smartBlockingManager.isInFastingMode.value -> "Ayuno Digital: Límite reducido a ${effectiveLimit}m"
                                            smartBlockingManager.activeContextRules.value.isNotEmpty() -> "Bloqueo por Contexto activo"
                                            else -> null
                                        }
                                    } else null
                                    
                                    Log.d(TAG, "App $currentApp ha excedido su límite - bloqueando y marcando")
                                    lastCheckedApp = currentApp
                                    lastBlockedTime = currentTime
                                    warningNotifiedApps.remove(currentApp) // Reset para el próximo día
                                    
                                    // Marcar la app como bloqueada hoy para que no se pueda volver a abrir
                                    smartBlockingManager.markAppAsBlocked(currentApp)
                                    smartBlockingManager.registerBlockScreenShown(currentApp)
                                    
                                    // === NUEVO V2: Registrar evento de bloqueo ===
                                    serviceScope.launch(Dispatchers.IO) {
                                        smartBlockingManager.trackBlockingEvent(
                                            currentApp, BlockingEventType.APP_BLOCKED,
                                            "Limit exceeded: ${usageMinutes}m / ${effectiveLimit}m"
                                        )
                                    }
                                    
                                    // FEATURE: Marcar límite como excedido para bloquear edición
                                    // Los usuarios se quejan de poder cambiar límites después de excederlos
                                    withContext(Dispatchers.IO) {
                                        appLimitRepository.markAsExceeded(currentApp)
                                    }
                                    
                                    // Ocultar timer flotante al bloquear
                                    if (floatingTimerActive) {
                                        FloatingTimerService.stop(this@AppMonitoringService)
                                        floatingTimerActive = false
                                        currentFloatingApp = ""
                                    }
                                    
                                    blockApp(currentApp, blockReason)
                                }
                            }
                            // Advertencia al 80% - notificar
                            usagePercent >= 80 && usagePercent < 100 -> {
                                val currentTime = System.currentTimeMillis()
                                val lastWarning = lastWarningTime[currentApp] ?: 0L

                                if (!warningNotifiedApps.contains(currentApp) ||
                                    (currentTime - lastWarning) > WARNING_COOLDOWN) {
                                    Log.d(TAG, "Enviando advertencia para $currentApp")
                                    warningNotifiedApps.add(currentApp)
                                    lastWarningTime[currentApp] = currentTime
                                    // Usar el sistema de notificaciones inteligentes
                                    smartNotificationManager.checkAppLimitsAndNotify()
                                    
                                    // === ADVERTENCIA DE RACHA ===
                                    // Si la protección de racha está habilitada, advertir antes del límite
                                    if (smartBlockingManager.shouldWarnAboutStreakBreak(usageMinutes, effectiveLimit)) {
                                        // Obtener racha actual de goals
                                        serviceScope.launch(Dispatchers.IO) {
                                            try {
                                                val goals = goalsRepository.getAllGoals().first()
                                                val bestStreak = goals.maxOfOrNull { it.currentStreak } ?: 0
                                                if (bestStreak > 0) {
                                                    smartNotificationManager.showStreakWarningNotification(
                                                        packageName = currentApp,
                                                        appName = getAppName(currentApp),
                                                        remainingMinutes = effectiveLimit - usageMinutes,
                                                        currentStreak = bestStreak
                                                    )
                                                }
                                            } catch (e: Exception) {
                                                Log.e(TAG, "Error obteniendo racha para advertencia", e)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        lastCheckedApp = currentApp
                    } // Cierre del if (appLimit != null && appLimit.isEnabled)
                } // Cierre del if (shouldContinue)
            } ?: Log.w(TAG, "checkCurrentApp timeout - operación cancelada para prevenir ANR")
        } catch (e: Exception) {
            Log.e(TAG, "Error en checkCurrentApp", e)
            e.printStackTrace()
        }
    }

    /**
     * Uso en primer plano de la app durante el día actual, en milisegundos.
     *
     * BUG CORREGIDO: antes esta función hacía
     *     queryUsageStats(INTERVAL_DAILY, ...).find { it.packageName == pkg }
     * y `queryUsageStats` devuelve VARIOS buckets por paquete, así que `find`
     * se quedaba con el primero y el uso quedaba gravemente subestimado
     * (a menudo casi cero). Como consecuencia `uso >= límite` casi nunca era
     * cierto y el bloqueo por límite de tiempo no se disparaba nunca.
     *
     * Ahora se delega en [DailyUsageCalculator], que suma los intervalos
     * reales de primer plano a partir de queryEvents.
     */
    private fun getCurrentAppUsageStats(packageName: String): Long =
        DailyUsageCalculator.foregroundMillisToday(this, packageName)

    private fun getCurrentForegroundApp(): String = ForegroundAppTracker.current(this)

    /**
     * Aplica el bloqueo de una app.
     *
     * BUG CORREGIDO: antes esta función llamaba directamente a
     * `AppBlockedActivity.start(...)`, es decir `startActivity()` desde un
     * Service. Desde Android 10 el sistema prohíbe lanzar actividades desde
     * segundo plano y la llamada **fallaba en silencio**: se superaba el
     * límite y no ocurría nada. Además `showAppBlockOverlay()` —la vía que sí
     * funciona— existía en este archivo pero nunca se invocaba.
     *
     * Ahora se delega en [BlockEnforcer], que usa el overlay como vía
     * principal y avisa al usuario si falta el permiso necesario.
     */
    private suspend fun blockApp(blockedAppPackage: String, customReason: String? = null) {
        try {
            Log.d(TAG, "Bloqueando app: $blockedAppPackage ${customReason?.let { "- $it" } ?: ""}")

            val appLimit = withContext(Dispatchers.IO) {
                appLimitRepository.getLimitByPackage(blockedAppPackage)
            }
            val appName = appLimit?.appName ?: getAppName(blockedAppPackage)
            val dailyLimit = appLimit?.dailyLimitMinutes ?: 0

            BlockEnforcer.enforce(
                context = this,
                packageName = blockedAppPackage,
                appName = appName,
                dailyLimitMinutes = dailyLimit,
                reason = customReason
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error al bloquear app", e)
        }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val packageManager = applicationContext.packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    /**
     * Vuelve a tapar la app bloqueada cuando el usuario sigue dentro de ella.
     *
     * Antes se hacía `startActivity(HOME)` desde el servicio, sujeto a las
     * mismas restricciones de lanzamiento en segundo plano que impedían el
     * bloqueo. [BlockEnforcer] usa el overlay, que sí funciona.
     */
    private suspend fun forceNavigateToHome(blockedAppPackage: String) {
        if (blockedAppPackage.isEmpty()) return
        val appLimit = withContext(Dispatchers.IO) {
            appLimitRepository.getLimitByPackage(blockedAppPackage)
        }
        BlockEnforcer.enforce(
            context = this,
            packageName = blockedAppPackage,
            appName = appLimit?.appName ?: getAppName(blockedAppPackage),
            dailyLimitMinutes = appLimit?.dailyLimitMinutes ?: 0,
            reason = null
        )
    }

    // ─── Qué se puede bloquear ────────────────────────────────────────────
    //
    // El monitor trataba cualquier paquete en primer plano como una app del
    // usuario. Con la Ventana de Sueño activa eso lanzaba la pantalla de
    // bloqueo sobre `com.google.android.permissioncontroller`, el diálogo de
    // permisos de Android — comprobado en logcat, una vez cada dos segundos.
    // El mismo camino alcanzaba al lanzador, a SystemUI y al teléfono: durante
    // la franja de sueño el usuario podía quedarse sin poder marcar una
    // llamada.
    //
    // Regla: sólo se bloquea lo que el usuario puede abrir por sí mismo (tiene
    // actividad de lanzador) y no es infraestructura ni una vía de emergencia.
    // El resultado se memoriza porque esta comprobación corre cada dos
    // segundos.

    private val blockabilityCache = mutableMapOf<String, Boolean>()

    /** Paquetes que actúan como pantalla de inicio. */
    private val launcherPackages: Set<String> by lazy {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        runCatching {
            packageManager.queryIntentActivities(home, 0)
                .mapNotNull { it.activityInfo?.packageName }
                .toSet()
        }.getOrDefault(emptySet())
    }

    /**
     * Infraestructura y vías de emergencia.
     *
     * Los marcadores de teléfono se resuelven en el dispositivo en vez de
     * escribirse a mano, porque el paquete cambia entre fabricantes y una lista
     * fija dejaría a algunos usuarios sin poder llamar.
     */
    private val criticalPackages: Set<String> by lazy {
        val dialers = runCatching {
            packageManager
                .queryIntentActivities(Intent(Intent.ACTION_DIAL), 0)
                .mapNotNull { it.activityInfo?.packageName }
        }.getOrDefault(emptyList())

        dialers.toSet() + setOf(
            "com.android.systemui",
            "com.google.android.permissioncontroller",
            "com.android.permissioncontroller",
            // Ajustes queda fuera a propósito: es donde se conceden los
            // permisos que necesita esta app y donde se desactivan los
            // bloqueos. Bloquearlo dejaría al usuario sin salida.
            "com.android.settings",
            "com.android.emergency",
            "com.android.dialer",
            "com.google.android.dialer"
        )
    }

    private fun isUserBlockableApp(pkg: String): Boolean = blockabilityCache.getOrPut(pkg) {
        when {
            pkg == packageName -> false
            pkg in criticalPackages -> false
            pkg in launcherPackages -> false
            // Sin actividad de lanzador el usuario no ha abierto esto: es un
            // diálogo del sistema, un teclado o un proveedor en segundo plano.
            runCatching { packageManager.getLaunchIntentForPackage(pkg) }.getOrNull() == null -> false
            else -> true
        }
    }

    companion object {
        private const val TAG = "AppMonitoringService"

        fun startService(context: Context) {
            try {
                Log.d(TAG, "Iniciando AppMonitoringService")
                val intent = Intent(context, AppMonitoringService::class.java)
                context.startForegroundService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error al iniciar servicio", e)
            }
        }

        fun stopService(context: Context) {
            Log.d(TAG, "Deteniendo AppMonitoringService")
            val intent = Intent(context, AppMonitoringService::class.java)
            context.stopService(intent)
        }
    }
}
