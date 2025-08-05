package com.momentum.app.worker

import android.content.Context
import androidx.work.*
import androidx.glance.appwidget.updateAll
import com.momentum.app.MomentumApplication
import com.momentum.app.widget.LifeWeeksWidget
import com.momentum.app.widget.QuoteWidget
import java.util.concurrent.TimeUnit

class WidgetUpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Update life weeks widget
            LifeWeeksWidget().updateAll(applicationContext)
            
            // Update quote widget
            QuoteWidget().updateAll(applicationContext)
            
            Result.success()
        } catch (exception: Exception) {
            Result.retry()
        }
    }

    companion object {
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
        }
    }
}