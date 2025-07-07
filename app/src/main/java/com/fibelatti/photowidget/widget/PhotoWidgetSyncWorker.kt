package com.fibelatti.photowidget.widget

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltWorker
class PhotoWidgetSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val photoWidgetStorage: PhotoWidgetStorage,
    private val coroutineScope: CoroutineScope,
) : CoroutineWorker(appContext = context, params = workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("Working...")

        val ids: List<Int> = PhotoWidgetProvider.ids(applicationContext).ifEmpty {
            Timber.d("There are no widgets.")
            return Result.success()
        }

        var shouldRetry = false

        for (id in ids) {
            try {
                Timber.d("Processing widget (id=$id)")

                val source = photoWidgetStorage.getWidgetSource(appWidgetId = id)

                if (PhotoWidgetSource.DIRECTORY == source) {
                    coroutineScope.launch(NonCancellable) {
                        photoWidgetStorage.syncWidgetPhotos(appWidgetId = id)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing widget (id=$id). Will retry.")
                shouldRetry = true
            }
        }

        return if (shouldRetry) Result.retry() else Result.success()
    }

    companion object {

        private const val UNIQUE_WORK_NAME = "PhotoWidgetSyncWorker"

        fun enqueueWork(context: Context) {
            Timber.d("Enqueuing work...")

            val workRequest: PeriodicWorkRequest.Builder = PeriodicWorkRequestBuilder<PhotoWidgetSyncWorker>(
                repeatInterval = Duration.ofHours(6),
            )

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                uniqueWorkName = UNIQUE_WORK_NAME,
                existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.UPDATE,
                request = workRequest.build(),
            )
        }
    }
}
