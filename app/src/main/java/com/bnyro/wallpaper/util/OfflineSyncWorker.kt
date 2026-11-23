package com.bnyro.wallpaper.util

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters

/**
 * Downloads favorites and recently applied wallpapers for offline use, see [OfflineWallpaperHelper].
 */
class OfflineSyncWorker(
    applicationContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(applicationContext, workerParameters) {
    override suspend fun doWork(): Result {
        val success = OfflineWallpaperHelper.sync(applicationContext)

        // retry later if some downloads failed, but don't try forever, e.g. for deleted images
        return if (success || runAttemptCount >= MAX_ATTEMPTS) Result.success() else Result.retry()
    }

    companion object {
        private const val WORK_NAME = "OfflineWallpaperSync"
        private const val MAX_ATTEMPTS = 3

        /**
         * Download the wallpapers that should be available offline as soon as there's internet.
         *
         * @param afterRunningSync whether to sync again after a sync that's already queued or
         * running, e.g. because a new favorite was added. Otherwise, nothing is done if a sync is
         * already queued.
         */
        fun enqueue(context: Context, afterRunningSync: Boolean = true) {
            val request = OneTimeWorkRequestBuilder<OfflineSyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            // queue after a sync that is already running, so that new favorites aren't missed
            val policy = if (afterRunningSync) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.KEEP
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, policy, request)
        }
    }
}
