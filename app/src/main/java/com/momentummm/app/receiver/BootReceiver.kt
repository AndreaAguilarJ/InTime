package com.momentummm.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.momentummm.app.service.AppMonitoringService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completado - iniciando AppMonitoringService")
            try {
                // Iniciar el servicio de monitoreo después de que el dispositivo se reinicia
                AppMonitoringService.startService(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error al iniciar AppMonitoringService después del boot", e)
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}

