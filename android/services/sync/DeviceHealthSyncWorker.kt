package com.nivya.services.sync

import android.content.Context
import androidx.work.*
import com.nivya.NivyaApp
import java.util.concurrent.TimeUnit

/**
 * Background WorkManager worker that periodically audits device health,
 * records local reading in Room, and synchronizes with the backend or flushes offline queued health metrics.
 */
class DeviceHealthSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? NivyaApp ?: return Result.failure()
        val healthRepo = app.container.deviceHealthRepository

        return try {
            healthRepo.collectAndRecordHealth()
            healthRepo.syncPendingHealth()
            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "NivyaDeviceHealthSyncWorker"

        fun enqueueOneTime(context: Context) {
            val request = OneTimeWorkRequestBuilder<DeviceHealthSyncWorker>()
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }

        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<DeviceHealthSyncWorker>(15, TimeUnit.MINUTES)
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
