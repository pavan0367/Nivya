package com.nivya.services.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nivya.NivyaApp

/**
 * Background WorkManager worker synchronizing pairing status and device telemetry.
 */
class PairingSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? NivyaApp ?: return Result.failure()
        val pairingRepo = app.container.pairingRepository

        return try {
            pairingRepo.getPairingStatus()
            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
