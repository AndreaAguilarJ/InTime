package com.momentummm.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.momentummm.app.data.entity.MotivationalPreferences
import com.momentummm.app.data.manager.MotivationalNotificationManager
import com.momentummm.app.data.repository.MotivationalMessagesRepository
import com.momentummm.app.notification.MotivationalAlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Recibe las alarmas de mensajes motivacionales, muestra el mensaje y
 * **reprograma la siguiente alarma**.
 *
 * ─── LA REGLA QUE ARREGLA EL BUG ──────────────────────────────────────────
 * La reprogramación ocurre SIEMPRE y ANTES de decidir si se muestra el
 * mensaje. El sistema anterior hacía lo contrario: comprobaba las
 * preferencias, salía con `return` si estaban desactivadas o ilegibles, y sólo
 * reprogramaba después. Resultado: en cuanto una ejecución salía por ese
 * `return`, la cadena de mensajes moría y no volvía nunca.
 *
 * Aquí, aunque el mensaje no se muestre, la próxima alarma ya está puesta.
 */
@AndroidEntryPoint
class MotivationalAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MotivationalAlarmRcvr"

        const val ACTION_SHOW_MESSAGE = "com.momentummm.app.action.MOTIVATIONAL_ALARM"
        const val EXTRA_TYPE = "motivational_type"

        const val TYPE_PERIODIC = "periodic"
        const val TYPE_MORNING = "morning"
        const val TYPE_EVENING = "evening"
    }

    @Inject
    lateinit var notificationManager: MotivationalNotificationManager

    @Inject
    lateinit var messagesRepository: MotivationalMessagesRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SHOW_MESSAGE) return

        val type = intent.getStringExtra(EXTRA_TYPE) ?: TYPE_PERIODIC
        Log.d(TAG, "Alarma recibida: $type")

        // El trabajo requiere leer Room, así que se mantiene vivo el receptor.
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // Las preferencias pueden no existir todavía (primera ejecución
                // o seed fallido). Se usan los valores por defecto para no
                // perder la cadena, y se garantiza la fila para la próxima vez.
                val preferences = runCatching { messagesRepository.getPreferencesSync() }
                    .getOrNull()
                    ?: MotivationalPreferences()

                // 1. REPROGRAMAR PRIMERO. Nunca se rompe la cadena.
                rescheduleNext(context, type, preferences)

                // 2. Mostrar el mensaje sólo si procede.
                if (preferences.enabled) {
                    showMessage(type)
                } else {
                    Log.d(TAG, "Mensajes desactivados: no se muestra nada, pero la cadena sigue viva")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error procesando la alarma $type", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun rescheduleNext(context: Context, type: String, preferences: MotivationalPreferences) {
        try {
            when (type) {
                TYPE_MORNING -> MotivationalAlarmScheduler.scheduleMorning(context, preferences)
                TYPE_EVENING -> MotivationalAlarmScheduler.scheduleEvening(context, preferences)
                else -> MotivationalAlarmScheduler.scheduleNextPeriodic(context, preferences)
            }
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo reprogramar la alarma $type", e)
        }
    }

    private suspend fun showMessage(type: String) {
        if (!::notificationManager.isInitialized) {
            Log.w(TAG, "MotivationalNotificationManager no inyectado")
            return
        }

        when (type) {
            TYPE_MORNING -> notificationManager.showMorningMessage()
            TYPE_EVENING -> notificationManager.showEveningMessage()
            else -> notificationManager.showMotivationalNotification()
        }
    }
}
