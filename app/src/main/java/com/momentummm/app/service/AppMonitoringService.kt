package com.momentummm.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.net.Uri
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
import com.momentummm.app.ui.AppBlockedActivity
import com.momentummm.app.ui.overlay.AppBlockOverlayService
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

    private val MONITORING_INTERVAL = 5000L // 5 segundos - reducir frecuencia para evitar ANR
    private var lastCheckedApp: String = ""
    private var lastBlockedTime: Long = 0
    private val BLOCK_COOLDOWN = 5000L // 5 segundos entre bloqueos de la misma app

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
    private var monitoringCycleCount = 0
    private val PATTERN_ANALYSIS_INTERVAL = 60 // Cada 60 ciclos (~5min) analizar patrones

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
                try {
                    // Verificar que los repositorios estén inicializados antes de proceder
                    if (!areRepositoriesInitialized) {
                        Log.w(TAG, "Repositorios no inicializados aún, esperando...")
                        delay(MONITORING_INTERVAL)
                        continue
                    }
                    
                    // Actualizar estados de bloqueo inteligente
                    smartBlockingManager.refreshModeStates()
                    
                    // === NUEVO V2: Análisis periódico de patrones ===
                    monitoringCycleCount++
                    if (monitoringCycleCount % PATTERN_ANALYSIS_INTERVAL == 0) {
                        try {
                            patternEngine.analyzeAllPatterns()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error en análisis periódico de patrones", e)
                        }
                    }
                    
                    checkCurrentApp()
                } catch (e: Exception) {
                    Log.e(TAG, "Error en checkCurrentApp", e)
                    e.printStackTrace()
                }
                delay(MONITORING_INTERVAL)
            }
        }
    }

    private fun stopMonitoring() {
        Log.d(TAG, "Deteniendo monitoreo de aplicaciones")
        monitoringJob?.cancel()
        monitoringJob = null
    }

    private suspend fun checkCurrentApp() {
        try {
            // Timeout de 3 segundos para esta operación
            withTimeoutOrNull(3000L) {
                val currentApp = getCurrentForegroundApp()
                if (currentApp.isEmpty() || currentApp == packageName) {
                    // Si estamos en InTime, ocultar el timer flotante
                    if (floatingTimerActive) {
                        FloatingTimerService.stop(this@AppMonitoringService)
                        floatingTimerActive = false
                        currentFloatingApp = ""
                    }
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
                    
                    // La app ya alcanzó su límite hoy - bloquear si podemos mostrar pantalla
                    if (smartBlockingManager.canShowBlockScreen(currentApp)) {
                        val appLimit = withContext(Dispatchers.IO) {
                            appLimitRepository.getLimitByPackage(currentApp)
                        }
                        val dailyLimit = appLimit?.dailyLimitMinutes ?: 0
                        Log.d(TAG, "App $currentApp está bloqueada hoy - mostrando pantalla de bloqueo")
                        smartBlockingManager.registerBlockScreenShown(currentApp)
                        blockApp(currentApp, "Ya alcanzaste tu límite de $dailyLimit minutos hoy")
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
                        val usageStats = getCurrentAppUsageStats(currentApp)
                        val usageMinutes = (usageStats / 60000).toInt()
                        
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
                        
                        // INTEGRACIÓN BLOQUEO INTELIGENTE: Obtener límite efectivo
                        val originalLimit = appLimit.dailyLimitMinutes
                        val effectiveLimit = smartBlockingManager.getEffectiveDailyLimit(currentApp, originalLimit)
                        
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
                                    val intervention = smartBlockingManager.getSmartIntervention(currentApp)
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

    private fun getCurrentAppUsageStats(packageName: String): Long {
        return try {
            val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return 0L
            val time = System.currentTimeMillis()

            // Obtener estadísticas del día actual
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            val startTime = calendar.timeInMillis

            val usageStatsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                time
            )

            usageStatsList?.find { it.packageName == packageName }?.totalTimeInForeground ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error getting usage stats for $packageName", e)
            0L
        }
    }

    private fun getCurrentForegroundApp(): String {
        return try {
            val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return ""
            val time = System.currentTimeMillis()

            // Obtener estadísticas de los últimos 2 segundos
            val usageStatsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                time - 2000,
                time
            )

            usageStatsList?.maxByOrNull { it.lastTimeUsed }?.packageName ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Error getting foreground app", e)
            ""
        }
    }

    private suspend fun blockApp(blockedAppPackage: String, customReason: String? = null) {
        try {
            Log.d(TAG, "Bloqueando app: $blockedAppPackage ${customReason?.let { "- $it" } ?: ""}")

            val appLimit = withContext(Dispatchers.IO) {
                appLimitRepository.getLimitByPackage(blockedAppPackage)
            }
            val appName = appLimit?.appName ?: getAppName(blockedAppPackage)
            val dailyLimit = appLimit?.dailyLimitMinutes ?: 0

            // Abrir la pantalla de bloqueo
            withContext(Dispatchers.Main) {
                AppBlockedActivity.start(
                    this@AppMonitoringService, 
                    appName, 
                    dailyLimit, 
                    customReason,
                    blockedPackage = blockedAppPackage
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al bloquear app", e)
            e.printStackTrace()
        }
    }

    private fun openMomentumApp() {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            if (intent != null) {
                Log.d(TAG, "Abriendo Momentum App")
                startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al abrir Momentum App", e)
            e.printStackTrace()
        }
    }

    private suspend fun showAppBlockOverlay(blockedAppPackage: String) {
        try {
            // Verificar permiso de overlay antes de intentar iniciar el servicio
            if (!Settings.canDrawOverlays(this)) {
                // Mostrar notificación con acción para abrir ajustes de overlay
                val settingsIntent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

                val pending = PendingIntent.getActivity(
                    this, 0, settingsIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val nm = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return
                val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Permiso necesario: Mostrar sobre otras apps")
                    .setContentText("Permite la superposición para bloquear la app cuando se exceda el límite")
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setContentIntent(pending)
                    .setAutoCancel(true)
                    .build()

                nm.notify(2001, notification)
                return
            }

            val appLimit = withContext(Dispatchers.IO) {
                appLimitRepository.getLimitByPackage(blockedAppPackage)
            }
            val appName = appLimit?.appName ?: getAppName(blockedAppPackage)

            val intent = Intent(this, AppBlockOverlayService::class.java).apply {
                putExtra("blocked_app_package", blockedAppPackage)
                putExtra("blocked_app_name", appName)
                putExtra("daily_limit", appLimit?.dailyLimitMinutes ?: 0)
            }
            startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
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
