package com.fibelatti.photowidget.widget

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@HiltWorker
class PhotoWidgetSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val photoWidgetStorage: PhotoWidgetStorage,
    private val coroutineScope: CoroutineScope,
) : CoroutineWorker(appContext = context, params = workerParams) {

    override suspend fun doWork(): Result {
        Timber.i("Working...")

        val ids: List<Int> = PhotoWidgetProvider.ids(applicationContext).ifEmpty {
            Timber.d("There are no widgets.")
            return Result.success()
        }

        ids.map { id ->
            coroutineScope.launch {
                withContext(NonCancellable) {
                    try {
                        Timber.d("Processing widget %s", mapOf("id" to id))
                        if (photoWidgetStorage.getWidgetSource(appWidgetId = id) == PhotoWidgetSource.DIRECTORY) {
                            if (photoWidgetStorage.syncWidgetPhotos(appWidgetId = id)) {
                                Timber.d("Photos changed, updating widget %s", mapOf("id" to id))
                                // Joined so the worker stays alive until the render completes: it runs on the
                                // application scope, and returning from `doWork` while it is ongoing lets the process
                                // be killed mid-render.
                                PhotoWidgetProvider.update(context = applicationContext, appWidgetId = id).join()
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Timber.e(e, "Error processing widget %s.", mapOf("id" to id))
                    }
                }
            }
        }.joinAll()

        return Result.success()
    }

    companion object {

        private const val UNIQUE_WORK_NAME = "PhotoWidgetSyncWorker"

        const val MIN_INTERVAL_HOURS: Int = 1
        const val MAX_INTERVAL_HOURS: Int = 12
        const val DEFAULT_INTERVAL_HOURS: Int = 6

        /**
         * (Re)schedules the folder sync using the interval currently set in the user preferences.
         *
         * Safe to call at any time: [ExistingPeriodicWorkPolicy.UPDATE] keeps the existing work and
         * only applies the new period, so calling it on every app start does not reset the schedule.
         */
        fun enqueueWork(context: Context) {
            val intervalHours: Int = entryPoint<PhotoWidgetEntryPoint>(context)
                .userPreferencesStorage()
                .folderSyncInterval

            Timber.i("Enqueuing work %s", mapOf("intervalHours" to intervalHours))

            val workRequest: PeriodicWorkRequest.Builder = PeriodicWorkRequestBuilder<PhotoWidgetSyncWorker>(
                repeatInterval = Duration.ofHours(intervalHours.toLong()),
            )

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                uniqueWorkName = UNIQUE_WORK_NAME,
                existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.UPDATE,
                request = workRequest.build(),
            )
        }
    }
}
