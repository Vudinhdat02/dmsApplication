package com.example.dmsapplication.worker

import android.content.Context
import androidx.work.*
import com.example.dmsapplication.data.repository.StatsRepository
import com.google.firebase.auth.FirebaseAuth

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = StatsRepository(applicationContext)
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.failure()

        return try {
            // 1. Upload tất cả ảnh chưa sync lên Cloudinary
            val unsynced = repository.getUnsynced()
            unsynced.forEach { repository.syncToCloud(it) }

            // 2. Xóa ảnh cũ hơn 2 ngày trên Cloudinary + xóa khỏi DB
            repository.deleteOldCloudImages(userId)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        // Chạy ngay lập tức khi có mạng (OneTimeWorkRequest)
        fun scheduleImmediate(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "sync_immediate",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        // Chạy định kỳ để dọn ảnh cũ
        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(
                6, java.util.concurrent.TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "sync_periodic",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}