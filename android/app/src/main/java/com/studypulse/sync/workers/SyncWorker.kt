package com.studypulse.sync.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.studypulse.sync.StudyPulseApplication

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val syncRepository =
        (applicationContext as StudyPulseApplication).container.syncRepository

    override suspend fun doWork(): Result {
        return try {
            val result = syncRepository.syncPendingEvents()
            if (result.isSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}