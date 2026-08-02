package com.momentummm.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.momentummm.app.data.entity.MotivationalPreferences
import com.momentummm.app.receiver.MotivationalAlarmReceiver
import java.util.Calendar

/**
 * Programa los mensajes motivacionales con [AlarmManager].
 *
 * ─── POR QUÉ EXISTE ESTA CLASE ────────────────────────────────────────────
 * El sistema anterior tenía cuatro fallos que, juntos, dejaban al usuario sin
 * mensajes:
 *
 * 1. **La cadena diaria se auto-destruía.** Los mensajes de mañana y noche
 *    eran `OneTimeWorkRequest` que se reprogramaban a sí mismos *dentro* de
 *    `doWork()`. Pero `doWork()` hacía `return` antes de reprogramar si las
 *    preferencias estaban desactivadas o no se podían leer. Un solo día con
 *    el interruptor apagado mataba la cadena para siempre.
 *
 * 2. **Las preferencias no se aplicaban.** `dailyFrequency`, `startHour` y
 *    `endHour` se ignoraban por completo: el trabajo periódico estaba fijado a
 *    2 horas y las horas de mañana/noche estaban escritas a fuego (8:00 y
 *    21:00) en tres sitios distintos.
 *
 * 3. **Un flag de un solo uso.** `MOTIVATIONAL_NOTIFICATIONS_SCHEDULED` se
 *    guardaba tras el primer arranque, así que si el trabajo se perdía nunca
 *    se volvía a programar.
 *
 * 4. **WorkManager no sirve para horas concretas.** Es "best effort" y con
 *    Doze puede retrasarse horas. Para "un mensaje a las 8:00" hace falta una
 *    alarma exacta.
 *
 * ─── GARANTÍAS DEL NUEVO DISEÑO ───────────────────────────────────────────
 * - [scheduleAll] es **idempotente**: se puede llamar en cada arranque, tras
 *   cambiar cualquier ajuste y al reiniciar el dispositivo.
 * - El receptor reprograma la siguiente alarma **antes** de decidir si
 *   muestra el mensaje, así que la cadena no se puede romper.
 * - Si no hay permiso de alarmas exactas se cae a alarmas inexactas en lugar
 *   de fallar.
 */
object MotivationalAlarmScheduler {

    private const val TAG = "MotivationalAlarmSched"

    private const val REQUEST_CODE_PERIODIC = 7101
    private const val REQUEST_CODE_MORNING = 7102
    private const val REQUEST_CODE_EVENING = 7103

    /**
     * Reprograma las tres cadenas de mensajes según [preferences].
     *
     * Si las notificaciones están desactivadas se cancelan todas las alarmas.
     */
    fun scheduleAll(context: Context, preferences: MotivationalPreferences) {
        if (!preferences.enabled) {
            cancelAll(context)
            Log.d(TAG, "Mensajes motivacionales desactivados: alarmas canceladas")
            return
        }

        scheduleNextPeriodic(context, preferences)
        scheduleMorning(context, preferences)
        scheduleEvening(context, preferences)
    }

    /** Cancela todas las alarmas de mensajes motivacionales. */
    fun cancelAll(context: Context) {
        val manager = alarmManager(context) ?: return
        listOf(
            MotivationalAlarmReceiver.TYPE_PERIODIC to REQUEST_CODE_PERIODIC,
            MotivationalAlarmReceiver.TYPE_MORNING to REQUEST_CODE_MORNING,
            MotivationalAlarmReceiver.TYPE_EVENING to REQUEST_CODE_EVENING
        ).forEach { (type, requestCode) ->
            runCatching {
                manager.cancel(pendingIntent(context, type, requestCode))
            }.onFailure { Log.e(TAG, "No se pudo cancelar la alarma $type", it) }
        }
    }

    /**
     * Programa el siguiente mensaje periódico del día.
     *
     * Los mensajes se reparten uniformemente dentro de la ventana activa: con
     * frecuencia N la ventana se divide en N bloques y el mensaje cae en el
     * centro de cada bloque, de modo que ninguno coincide con los límites
     * (ahí van los mensajes de mañana y noche).
     */
    fun scheduleNextPeriodic(context: Context, preferences: MotivationalPreferences) {
        val slots = periodicSlotsInMinutes(preferences)
        if (slots.isEmpty()) {
            Log.w(TAG, "Sin franjas periódicas válidas; no se programa nada")
            return
        }

        val now = Calendar.getInstance()
        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        val nextToday = slots.firstOrNull { it > nowMinutes }
        val target = Calendar.getInstance().apply {
            if (nextToday != null) {
                setMinuteOfDay(nextToday)
            } else {
                // Ya pasaron todas las franjas de hoy: la primera de mañana.
                add(Calendar.DAY_OF_YEAR, 1)
                // La frecuencia del fin de semana puede diferir, así que se
                // recalculan las franjas para el día de destino.
                val tomorrowSlots = periodicSlotsInMinutes(preferences, this)
                setMinuteOfDay(tomorrowSlots.firstOrNull() ?: (preferences.startHour * 60))
            }
        }

        schedule(context, MotivationalAlarmReceiver.TYPE_PERIODIC, REQUEST_CODE_PERIODIC, target.timeInMillis)
    }

    fun scheduleMorning(context: Context, preferences: MotivationalPreferences) {
        val target = nextOccurrenceOf(preferences.startHour, preferences.startMinute)
        schedule(context, MotivationalAlarmReceiver.TYPE_MORNING, REQUEST_CODE_MORNING, target)
    }

    fun scheduleEvening(context: Context, preferences: MotivationalPreferences) {
        val target = nextOccurrenceOf(preferences.endHour, preferences.endMinute)
        schedule(context, MotivationalAlarmReceiver.TYPE_EVENING, REQUEST_CODE_EVENING, target)
    }

    /**
     * Franjas horarias del día (en minutos desde medianoche) para los mensajes
     * periódicos, respetando la frecuencia y la ventana configuradas.
     */
    internal fun periodicSlotsInMinutes(
        preferences: MotivationalPreferences,
        day: Calendar = Calendar.getInstance()
    ): List<Int> {
        val isWeekend = day.get(Calendar.DAY_OF_WEEK).let {
            it == Calendar.SATURDAY || it == Calendar.SUNDAY
        }
        val useWeekend = isWeekend && preferences.differentWeekendSchedule

        val frequency = (if (useWeekend) preferences.weekendFrequency else preferences.dailyFrequency)
            .coerceIn(1, 12)

        val startMinutes = if (useWeekend) {
            preferences.weekendStartHour * 60
        } else {
            preferences.startHour * 60 + preferences.startMinute
        }
        val endMinutes = if (useWeekend) {
            preferences.weekendEndHour * 60
        } else {
            preferences.endHour * 60 + preferences.endMinute
        }

        // Ventana inválida (o que cruza medianoche): se usa un día completo
        // desde la hora de inicio para no dejar al usuario sin mensajes.
        val windowMinutes = if (endMinutes > startMinutes) {
            endMinutes - startMinutes
        } else {
            (24 * 60) - startMinutes
        }
        if (windowMinutes <= 0) return emptyList()

        val block = windowMinutes.toDouble() / frequency
        return (0 until frequency).map { index ->
            val offset = block * (index + 0.5)
            ((startMinutes + offset).toInt()).coerceIn(0, 24 * 60 - 1)
        }.distinct().sorted()
    }

    private fun nextOccurrenceOf(hour: Int, minute: Int): Long {
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, safeHour)
            set(Calendar.MINUTE, safeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= System.currentTimeMillis()) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }

    private fun Calendar.setMinuteOfDay(minuteOfDay: Int) {
        set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
        set(Calendar.MINUTE, minuteOfDay % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun schedule(context: Context, type: String, requestCode: Int, triggerAtMillis: Long) {
        val manager = alarmManager(context) ?: return
        val intent = pendingIntent(context, type, requestCode)

        try {
            if (canScheduleExact(manager)) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, intent)
            } else {
                // Sin permiso de alarmas exactas: mejor una alarma aproximada
                // que ningún mensaje.
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, intent)
                Log.w(TAG, "Sin permiso de alarma exacta; usando alarma aproximada para $type")
            }
            Log.d(TAG, "Alarma $type programada para ${java.util.Date(triggerAtMillis)}")
        } catch (e: SecurityException) {
            // Algunos fabricantes revocan el permiso en caliente.
            runCatching {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, intent)
            }.onFailure { Log.e(TAG, "No se pudo programar la alarma $type", it) }
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo programar la alarma $type", e)
        }
    }

    private fun canScheduleExact(manager: AlarmManager): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { manager.canScheduleExactAlarms() }.getOrDefault(false)
        } else {
            true
        }

    /**
     * true si el sistema nos deja programar alarmas exactas.
     *
     * En Android 14+ (API 34) `SCHEDULE_EXACT_ALARM` **no** se concede por
     * defecto a las apps recién instaladas. Sin él los mensajes se programan
     * con `setAndAllowWhileIdle`, que el sistema puede retrasar. Conviene
     * poder decírselo al usuario y ofrecerle activarlo.
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        val manager = alarmManager(context) ?: return false
        return canScheduleExact(manager)
    }

    /**
     * Intent que abre la pantalla del sistema para conceder alarmas exactas,
     * o null si la versión de Android no lo necesita.
     */
    fun exactAlarmSettingsIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = android.net.Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun alarmManager(context: Context): AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    private fun pendingIntent(context: Context, type: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MotivationalAlarmReceiver::class.java).apply {
            action = MotivationalAlarmReceiver.ACTION_SHOW_MESSAGE
            putExtra(MotivationalAlarmReceiver.EXTRA_TYPE, type)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
