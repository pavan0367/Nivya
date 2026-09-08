package com.nivya.services.sync

import android.content.Context
import androidx.work.*
import com.nivya.NivyaApp
import java.util.concurrent.TimeUnit

/**
 * Background WorkManager worker that periodically aggregates legitimate application usage statistics,
 * records daily totals in Room, and synchronizes with the backend or flushes the offline queue.
 */
class UsageSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? NivyaApp ?: return Result.failure()
        val usageRepo = app.container.usageRepository

        return try {
            usageRepo.collectAndRecordUsage()
            usageRepo.flushOfflineQueue()
            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "NivyaUsageSyncWorker"

        fun enqueueOneTime(context: Context) {
            val request = OneTimeWorkRequestBuilder<UsageSyncWorker>()
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }

        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<UsageSyncWorker>(15, TimeUnit.MINUTES)
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
