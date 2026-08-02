package com.momentummm.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.momentummm.app.data.AppDatabase
import com.momentummm.app.data.entity.MotivationalPreferences
import com.momentummm.app.notification.MotivationalAlarmScheduler
import com.momentummm.app.service.AppMonitoringService
import com.momentummm.app.service.NuclearModeService
import com.momentummm.app.service.ContextBlockingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * CRITICAL FIX: Ahora reinicia TODOS los servicios necesarios al reiniciar:
 * - AppMonitoringService (límites de apps)
 * - NuclearModeService (si estaba activo)
 * - ContextBlockingService (si había reglas activas)
 * 
 * Antes solo reiniciaba AppMonitoringService, lo que significaba que
 * el Modo Nuclear se podía bypasear simplemente reiniciando el teléfono.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            Log.d(TAG, "Boot completado - reiniciando servicios de monitoreo")
            
            // 1. SIEMPRE reiniciar el servicio de monitoreo principal
            try {
                AppMonitoringService.startService(context)
                Log.d(TAG, "AppMonitoringService reiniciado")
            } catch (e: Exception) {
                Log.e(TAG, "Error al iniciar AppMonitoringService después del boot", e)
            }
            
            // 2. Verificar si Nuclear Mode estaba activo y reiniciarlo
            // 3. Verificar si Context Blocking tenía reglas activas
            val pendingResult = goAsync() // Permite trabajo asíncrono en BroadcastReceiver
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val database = AppDatabase.getDatabase(context)
                    val configDao = database.smartBlockingConfigDao()
                    val config = configDao.getConfigSync()
                    
                    // Reiniciar Nuclear Mode si estaba activo
                    if (config?.isNuclearModeActive() == true) {
                        try {
                            val nuclearIntent = Intent(context, NuclearModeService::class.java).apply {
                                action = NuclearModeService.ACTION_START
                            }
                            context.startForegroundService(nuclearIntent)
                            Log.d(TAG, "NuclearModeService reiniciado - modo nuclear activo")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error reiniciando NuclearModeService", e)
                        }
                    }
                    
                    // Reiniciar Context Blocking si hay reglas habilitadas
                    val contextRuleDao = database.contextBlockRuleDao()
                    val enabledRules = contextRuleDao.getEnabledRulesSync()
                    if (enabledRules.isNotEmpty()) {
                        // startIfPossible comprueba el permiso de ubicación: el
                        // servicio es un foreground service de tipo "location" y
                        // sin el permiso concedido Android 14+ lo mata al
                        // arrancar, dejando la función muerta en silencio.
                        val started = ContextBlockingService.startIfPossible(context)
                        if (started) {
                            Log.d(TAG, "ContextBlockingService reiniciado - ${enabledRules.size} reglas activas")
                        } else {
                            Log.w(TAG, "Bloqueo por contexto no reiniciado: falta el permiso de ubicación")
                        }
                    }

                    // 4. Reprogramar los mensajes motivacionales.
                    // El reinicio del dispositivo borra TODAS las alarmas, así
                    // que sin esto el usuario dejaba de recibir mensajes hasta
                    // volver a abrir la app.
                    try {
                        val preferences = database.motivationalPreferencesDao().getPreferencesSync()
                            ?: MotivationalPreferences()
                        MotivationalAlarmScheduler.scheduleAll(context, preferences)
                        Log.d(TAG, "Alarmas de mensajes motivacionales reprogramadas tras el boot")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reprogramando mensajes motivacionales", e)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error verificando servicios post-boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}

