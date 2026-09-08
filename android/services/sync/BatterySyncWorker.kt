package com.nivya.services.sync

import android.content.Context
import androidx.work.*
import com.nivya.NivyaApp
import java.util.concurrent.TimeUnit

/**
 * Background WorkManager worker that periodically collects legitimate battery telemetry,
 * persists readings in Room, and synchronizes with the backend or flushes the offline queue.
 */
class BatterySyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? NivyaApp ?: return Result.failure()
        val batteryRepo = app.container.batteryRepository

        return try {
            batteryRepo.collectAndRecordBattery()
            batteryRepo.flushOfflineQueue()
            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "NivyaBatterySyncWorker"

        /**
         * Enqueue a one-time immediate battery sync task.
         */
        fun enqueueOneTime(context: Context) {
            val constraints = Constraints.Builder()
                .build()

            val request = OneTimeWorkRequestBuilder<BatterySyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }

        /**
         * Schedule periodic battery sync (e.g. every 15 minutes).
         */
        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<BatterySyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }
    }
}
