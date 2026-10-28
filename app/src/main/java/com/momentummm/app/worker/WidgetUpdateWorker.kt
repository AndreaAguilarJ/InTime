package com.momentummm.app.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.momentummm.app.widget.WidgetRefresher
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class WidgetUpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    /** Quién pide programar el refresco de medianoche. */
    enum class MidnightReason {
        /** Arranque del proceso (Application.onCreate). */
        APP_START,
        /** Cambió la hora, la zona horaria o el idioma. */
        CLOCK_CHANGED,
        /** El propio refresco de medianoche acaba de ejecutarse. */
        AFTER_RUN,
    }

    companion object {
        private const val TAG = "WidgetUpdateWorker"
        private const val WIDGET_UPDATE_WORK_NAME = "widget_update_work"
        private const val WIDGET_MIDNIGHT_WORK_NAME = "widget_midnight_work"
        private const val WIDGET_IMMEDIATE_WORK_NAME = "widget_immediate_work"
        private const val KEY_MIDNIGHT = "midnight"

        /** Un minuto de margen: a las 00:00:00 en punto `LocalDate.now()` aún puede dar ayer. */
        private val AFTER_MIDNIGHT: Duration = Duration.ofMinutes(1)

        fun startPeriodicUpdate(context: Context) {
            val updateRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(1, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WIDGET_UPDATE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                updateRequest
            )
            scheduleMidnightRefresh(context, MidnightReason.APP_START)
            Log.d(TAG, "Periodic widget update scheduled")
        }

        /**
         * Al arrancar se CONSERVA el pendiente. Cuando el proceso está muerto es el
         * propio refresco de medianoche quien lo arranca, y Application.onCreate
         * corre antes que el worker: con REPLACE lo cancelaba justo antes de
         * ejecutarse y lo movía a la noche siguiente, así que con la app cerrada
         * (lo normal) el refresco de medianoche no llegaba nunca.
         */
        internal fun midnightPolicy(reason: MidnightReason): ExistingWorkPolicy = when (reason) {
            MidnightReason.APP_START -> ExistingWorkPolicy.KEEP
            MidnightReason.CLOCK_CHANGED, MidnightReason.AFTER_RUN -> ExistingWorkPolicy.REPLACE
        }

        /**
         * Progreso del año, Frase del día y Mensaje cambian a medianoche. El
         * refresco cada hora podía dejar hasta dos horas el día anterior en
         * pantalla; esta tarea sale justo después de las 00:00 y se reprograma.
         */
        fun scheduleMidnightRefresh(context: Context, reason: MidnightReason) {
            val request = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
                .setInitialDelay(delayUntilNextMidnight(ZonedDateTime.now()).toMillis(), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(KEY_MIDNIGHT to true))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WIDGET_MIDNIGHT_WORK_NAME,
                midnightPolicy(reason),
                request
            )
        }

        /**
         * Tiempo REAL hasta un minuto después del comienzo del día siguiente.
         *
         * Se cuenta en la zona horaria y no en hora local: la noche que se atrasa
         * la hora el día dura 25 h y la cuenta local salía una hora corta (el
         * refresco caía a las 23:01, todavía con el día anterior). Y
         * `atStartOfDay` cubre las zonas donde algún día no empieza a las 00:00.
         */
        internal fun delayUntilNextMidnight(now: ZonedDateTime): Duration {
            val nextDayStart = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
            return Duration.between(now, nextDayStart).plus(AFTER_MIDNIGHT)
        }

        /**
         * Fuerza una actualización inmediata de todos los widgets. Única para que
         * varios cambios seguidos (idioma, colores, fecha) no encolen repintados
         * duplicados. APPEND_OR_REPLACE y no REPLACE: reemplazar cancelaría un
         * repintado a medias, y el que se encadena detrás ya lee el dato nuevo.
         */
        fun requestImmediateUpdate(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                WIDGET_IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build()
            )
            Log.d(TAG, "Immediate widget update requested")
        }
    }

    override suspend fun doWork(): Result {
        val failures = WidgetRefresher.refreshAll(applicationContext)
        if (inputData.getBoolean(KEY_MIDNIGHT, false)) {
            // Se reprograma pase lo que pase: si no, un fallo dejaría de refrescar a
            // medianoche para siempre. Y no se reintenta: la nueva tarea REEMPLAZA
            // a esta por nombre, y el refresco de cada hora cubre un fallo suelto.
            scheduleMidnightRefresh(applicationContext, MidnightReason.AFTER_RUN)
            return Result.success()
        }
        return if (failures == 0) {
            Result.success()
        } else {
            Log.w(TAG, "$failures widget(s) failed to update")
            Result.retry()
        }
    }
}
