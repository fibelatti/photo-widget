package com.fibelatti.photowidget.widget

import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.fibelatti.photowidget.model.PhotoWidgetCycleMode
import com.fibelatti.photowidget.platform.KeepAliveService
import com.fibelatti.photowidget.preferences.UserPreferencesStorage
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import timber.log.Timber

/**
 * This worker ensures that widgets will be updated and their alarms set, following a device
 * reboot or app update.
 *
 * It runs periodically to restore any alarms that might have been killed by the system, and
 * luckily it will work better with OEMs that prevent apps from auto-starting. Any previously
 * scheduled worker would still run, restoring expected widget functionality.
 */
@HiltWorker
class PhotoWidgetRescheduleWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val photoWidgetStorage: PhotoWidgetStorage,
    private val photoWidgetAlarmManager: PhotoWidgetAlarmManager,
    private val userPreferencesStorage: UserPreferencesStorage,
) : CoroutineWorker(appContext = context, params = workerParams) {

    override suspend fun doWork(): Result {
        Timber.i("Working...")

        if (userPreferencesStorage.keepAlive) {
            KeepAliveService.tryStart(context = applicationContext)
        }

        var shouldRetry = false

        val ids: List<Int> = PhotoWidgetProvider.ids(applicationContext)
        for (id in ids) {
            coroutineContext.ensureActive()
            try {
                val cycleMode: PhotoWidgetCycleMode = photoWidgetStorage.getWidgetCycleMode(appWidgetId = id)
                val isLocked: Boolean = photoWidgetStorage.getWidgetLockedInApp(appWidgetId = id)
                val isPaused: Boolean = photoWidgetStorage.getWidgetCyclePaused(appWidgetId = id)

                Timber.d("Processing widget (id=$id,cycleMode=$cycleMode,isLocked=$isLocked,isPaused=$isPaused)")

                if (cycleMode !is PhotoWidgetCycleMode.Disabled && !isLocked && !isPaused) {
                    photoWidgetAlarmManager.setup(appWidgetId = id)
                }

                PhotoWidgetProvider.update(context = applicationContext, appWidgetId = id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Error processing widget (id=$id). Will retry.")
                shouldRetry = true
            }
        }

        if (shouldRetry) {
            return Result.retry()
        }

        Timber.d("Enqueueing RecurringWorker...")
        // Directly calling `Companion.enqueueWork` at this point would mark this work execution as
        // canceled. The work itself would be finished by now, so that would be fine although not
        // healthy. Using another worker to reschedule ensures a successful completion of the run.
        WorkManager.getInstance(applicationContext)
            .enqueue(request = OneTimeWorkRequestBuilder<RecurringWorker>().build())

        return Result.success()
    }

    companion object {

        private const val UNIQUE_WORK_NAME = "PhotoWidgetRescheduleWorker"

        fun enqueueWork(context: Context, delay: Duration? = null) {
            Timber.i("Enqueuing work...")

            val workRequest: OneTimeWorkRequest.Builder = OneTimeWorkRequestBuilder<PhotoWidgetRescheduleWorker>()

            when {
                delay != null -> {
                    workRequest.setInitialDelay(duration = delay)
                }

                // Only expedited for Android 12+ since earlier versions require showing a notification
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    workRequest.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                }
            }

            WorkManager.getInstance(context).enqueueUniqueWork(
                uniqueWorkName = UNIQUE_WORK_NAME,
                existingWorkPolicy = ExistingWorkPolicy.REPLACE,
                request = workRequest.build(),
            )
        }
    }
}

/**
 * Periodic workers cannot be marked as expedited. The sole purpose of this worker is to enqueue
 * another run of [PhotoWidgetRescheduleWorker], delayed by one hour, while still being able to
 * mark it as expedited.
 */
class RecurringWorker(
    context: Context,
    workerParams: WorkerParameters,
) : Worker(context = context, workerParams = workerParams) {

    override fun doWork(): Result {
        Timber.i("Working...")
        PhotoWidgetRescheduleWorker.enqueueWork(context = applicationContext, delay = Duration.ofHours(1))
        return Result.success()
    }
}
