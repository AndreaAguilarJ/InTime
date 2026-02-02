package com.momentummm.app.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import androidx.glance.appwidget.updateAll
import com.momentummm.app.widget.QuoteWidget
import com.momentummm.app.widget.LifeWeeksWidget
import com.momentummm.app.widget.YearProgressWidget
import com.momentummm.app.widget.MotivationalMessageWidget
import java.util.concurrent.TimeUnit

class WidgetUpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "WidgetUpdateWorker"
        private const val WIDGET_UPDATE_WORK_NAME = "widget_update_work"

        fun startPeriodicUpdate(context: Context) {
            val updateRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                1, TimeUnit.HOURS
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WIDGET_UPDATE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                updateRequest
            )
            Log.d(TAG, "Periodic widget update scheduled")
        }
        
        /**
         * Fuerza una actualización inmediata de todos los widgets
         */
        fun requestImmediateUpdate(context: Context) {
            val updateRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
                .build()
            
            WorkManager.getInstance(context).enqueue(updateRequest)
            Log.d(TAG, "Immediate widget update requested")
        }
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting widget update...")
            
            // Update life weeks widget (usa el nuevo método con colores personalizados)
            LifeWeeksWidget.updateAllWidgets(applicationContext)

            // Update quote widget
            QuoteWidget().updateAll(applicationContext)

            // Update year progress widget
            YearProgressWidget().updateAll(applicationContext)

            // Update motivational message widget
            MotivationalMessageWidget.updateAllWidgets(applicationContext)
            
            Log.d(TAG, "All widgets updated successfully")
            Result.success()
        } catch (exception: Exception) {
            Log.e(TAG, "Error updating widgets", exception)
            Result.retry()
        }
    }
}