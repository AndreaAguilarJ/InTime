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
import com.momentummm.app.data.repository.GoalsRepository
import com.momentummm.app.data.UserPreferencesRepository
import com.momentummm.app.data.manager.SmartBlockingManager
import com.momentummm.app.data.manager.SmartNotificationManager
import com.momentummm.app.ui.AppBlockedActivity
import com.momentummm.app.ui.overlay.AppBlockOverlayService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AppMonitoringService : Service() {

    @Inject lateinit var appLimitRepository: AppLimitRepository
    @Inject lateinit var appWhitelistRepository: AppWhitelistRepository
    @Inject lateinit var goalsRepository: GoalsRepository
    @Inject lateinit var smartNotificationManager: SmartNotificationManager
    @Inject lateinit var smartBlockingManager: SmartBlockingManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
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
            "Monitoreo de Aplicaciones",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Monitorea el tiempo de uso de aplicaciones"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
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
            .setContentTitle("Control de Aplicaciones Activo")
            .setContentText("Monitoreando el tiempo de uso de aplicaciones")
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
                    // Actualizar estados de bloqueo inteligente
                    smartBlockingManager.refreshModeStates()
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

                    // Si la app tiene límite configurado y está habilitada, procesarla
                    if (appLimit != null && appLimit.isEnabled) {
                        Log.d(TAG, "App monitoreada detectada: $currentApp")

                        // Obtener uso actual de la app
                        val usageStats = getCurrentAppUsageStats(currentApp)
                        val usageMinutes = (usageStats / 60000).toInt()
                        
                        // INTEGRACIÓN BLOQUEO INTELIGENTE: Obtener límite efectivo
                        val originalLimit = appLimit.dailyLimitMinutes
                        val effectiveLimit = smartBlockingManager.getEffectiveDailyLimit(currentApp, originalLimit)
                        
                        val usagePercent = if (effectiveLimit > 0) (usageMinutes * 100) / effectiveLimit else 0

                        Log.d(TAG, "App $currentApp - Uso: ${usageMinutes}m / ${effectiveLimit}m (original: ${originalLimit}m) (${usagePercent}%)")

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
                                    
                                    Log.d(TAG, "App $currentApp ha excedido su límite - bloqueando")
                                    lastCheckedApp = currentApp
                                    lastBlockedTime = currentTime
                                    warningNotifiedApps.remove(currentApp) // Reset para el próximo día
                                    
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
        val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as? UsageStatsManager
        val time = System.currentTimeMillis()

        // Obtener estadísticas del día actual
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis

        val usageStatsList = usageStatsManager?.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            time
        )

        return usageStatsList?.find { it.packageName == packageName }?.totalTimeInForeground ?: 0L
    }

    private fun getCurrentForegroundApp(): String {
        val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as? UsageStatsManager
        val time = System.currentTimeMillis()

        // Obtener estadísticas de los últimos 2 segundos
        val usageStatsList = usageStatsManager?.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            time - 2000,
            time
        )

        return usageStatsList?.maxByOrNull { it.lastTimeUsed }?.packageName ?: ""
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
                AppBlockedActivity.start(this@AppMonitoringService, appName, dailyLimit, customReason)
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

                val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
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
