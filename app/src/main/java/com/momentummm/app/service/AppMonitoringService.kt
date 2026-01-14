package com.momentummm.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Handler
import android.os.Looper
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
import com.momentummm.app.data.repository.AppLimitRepository
import com.momentummm.app.data.repository.AppWhitelistRepository
import com.momentummm.app.data.manager.SmartNotificationManager
import com.momentummm.app.ui.AppBlockedActivity
import com.momentummm.app.ui.overlay.AppBlockOverlayService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AppMonitoringService : Service() {

    @Inject lateinit var appLimitRepository: AppLimitRepository
    @Inject lateinit var appWhitelistRepository: AppWhitelistRepository
    @Inject lateinit var smartNotificationManager: SmartNotificationManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    private var monitoringRunnable: Runnable? = null

    private val MONITORING_INTERVAL = 2000L // 2 segundos - más frecuente para mejor detección
    private var lastCheckedApp: String = ""
    private var lastBlockedTime: Long = 0
    private val BLOCK_COOLDOWN = 3000L // 3 segundos entre bloqueos de la misma app

    // Para tracking de notificaciones de advertencia
    private val warningNotifiedApps = mutableSetOf<String>()
    private val lastWarningTime = mutableMapOf<String, Long>()
    private val WARNING_COOLDOWN = 300000L // 5 minutos entre advertencias de la misma app

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
        monitoringRunnable = object : Runnable {
            override fun run() {
                serviceScope.launch {
                    try {
                        checkCurrentApp()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error en checkCurrentApp", e)
                        e.printStackTrace()
                    } finally {
                        handler.postDelayed(this@AppMonitoringService.monitoringRunnable!!, MONITORING_INTERVAL)
                    }
                }
            }
        }
        handler.post(monitoringRunnable!!)
    }

    private fun stopMonitoring() {
        Log.d(TAG, "Deteniendo monitoreo de aplicaciones")
        monitoringRunnable?.let { handler.removeCallbacks(it) }
    }

    private suspend fun checkCurrentApp() {
        try {
            val currentApp = getCurrentForegroundApp()
            if (currentApp.isNotEmpty() && currentApp != packageName) {

                // CAMBIO IMPORTANTE: Primero verificar si la app tiene un límite configurado
                // Solo monitoreamos apps que están explícitamente en la lista de límites
                val appLimit = appLimitRepository.getLimitByPackage(currentApp)

                // Si la app NO tiene límite configurado, simplemente la ignoramos
                if (appLimit == null || !appLimit.isEnabled) {
                    // No está en la lista de límites o está deshabilitada - ignorar completamente
                    return
                }

                Log.d(TAG, "App monitoreada detectada: $currentApp")

                // Verificar si la app está en la whitelist (apps de emergencia)
                val isWhitelisted = appWhitelistRepository.isAppWhitelisted(currentApp)
                if (isWhitelisted) {
                    Log.d(TAG, "App $currentApp está en whitelist - no se bloqueará")
                    return
                }

                // Obtener uso actual de la app
                val usageStats = getCurrentAppUsageStats(currentApp)
                val usageMinutes = (usageStats / 60000).toInt()
                val limitMinutes = appLimit.dailyLimitMinutes
                val usagePercent = if (limitMinutes > 0) (usageMinutes * 100) / limitMinutes else 0

                Log.d(TAG, "App $currentApp - Uso: ${usageMinutes}m / ${limitMinutes}m (${usagePercent}%)")

                when {
                    // Límite alcanzado - bloquear
                    usageMinutes >= limitMinutes -> {
                        val currentTime = System.currentTimeMillis()
                        if (currentApp != lastCheckedApp || (currentTime - lastBlockedTime) > BLOCK_COOLDOWN) {
                            Log.d(TAG, "App $currentApp ha excedido su límite - bloqueando")
                            lastCheckedApp = currentApp
                            lastBlockedTime = currentTime
                            warningNotifiedApps.remove(currentApp) // Reset para el próximo día
                            blockApp(currentApp)
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
                        }
                    }
                }

                lastCheckedApp = currentApp
            }
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

    private suspend fun blockApp(blockedAppPackage: String) {
        try {
            Log.d(TAG, "Bloqueando app: $blockedAppPackage")

            val appLimit = appLimitRepository.getLimitByPackage(blockedAppPackage)
            val appName = appLimit?.appName ?: getAppName(blockedAppPackage)
            val dailyLimit = appLimit?.dailyLimitMinutes ?: 0

            // Abrir la pantalla de bloqueo
            AppBlockedActivity.start(this, appName, dailyLimit)

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

            val appLimit = appLimitRepository.getLimitByPackage(blockedAppPackage)
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
