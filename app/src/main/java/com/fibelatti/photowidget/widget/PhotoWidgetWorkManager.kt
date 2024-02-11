package com.fibelatti.photowidget.widget

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoWidgetWorkManager @Inject constructor(@ApplicationContext context: Context) {

    private val workManager by lazy { WorkManager.getInstance(context) }

    fun enqueueLoopingPhotoWidgetWork(
        appWidgetId: Int,
        repeatInterval: Long,
        timeUnit: TimeUnit,
    ) {
        val workName = LoopingPhotoWidgetWorker.getWorkName(appWidgetId)
        val work = LoopingPhotoWidgetWorker.newWork(
            appWidgetId = appWidgetId,
            repeatInterval = repeatInterval,
            timeUnit = timeUnit,
        )

        workManager.enqueueUniquePeriodicWork(workName, ExistingPeriodicWorkPolicy.UPDATE, work)
    }

    fun cancelWidgetWork(appWidgetId: Int) {
        workManager.cancelUniqueWork(LoopingPhotoWidgetWorker.getWorkName(appWidgetId))
    }
}

class LoopingPhotoWidgetWorker(
    context: Context,
    private val workerParams: WorkerParameters,
    private val flipPhotoUseCase: FlipPhotoUseCase,
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val appWidgetId = workerParams.inputData.getInt(INPUT_DATA_WIDGET_ID, -1)
        if (appWidgetId == -1) return Result.failure()

        return try {
            flipPhotoUseCase(appWidgetId = appWidgetId)
            Result.success()
        } catch (_: Exception) {
            Result.failure()
        }
    }

    companion object {

        private const val INPUT_DATA_WIDGET_ID = "photo-widget-id"

        fun getWorkName(appWidgetId: Int): String = "LoopingPhotoWidgetWorker_$appWidgetId"

        fun newWork(
            appWidgetId: Int,
            repeatInterval: Long,
            timeUnit: TimeUnit,
        ): PeriodicWorkRequest {
            val workData = Data.Builder().putInt(INPUT_DATA_WIDGET_ID, appWidgetId).build()

            return PeriodicWorkRequestBuilder<LoopingPhotoWidgetWorker>(
                repeatInterval = repeatInterval,
                repeatIntervalTimeUnit = timeUnit,
            ).setInputData(inputData = workData)
                .setInitialDelay(duration = repeatInterval, timeUnit = timeUnit)
                .build()
        }
    }
}
