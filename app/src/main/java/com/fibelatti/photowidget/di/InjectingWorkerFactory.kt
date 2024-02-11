package com.fibelatti.photowidget.di

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.fibelatti.photowidget.widget.FlipPhotoUseCase
import com.fibelatti.photowidget.widget.LoopingPhotoWidgetWorker
import javax.inject.Inject
import javax.inject.Provider

class InjectingWorkerFactory @Inject constructor(
    private val flipPhotoUseCase: Provider<FlipPhotoUseCase>,
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? = when (workerClassName) {
        LoopingPhotoWidgetWorker::class.java.name -> LoopingPhotoWidgetWorker(
            context = appContext,
            workerParams = workerParameters,
            flipPhotoUseCase = flipPhotoUseCase.get(),
        )

        else -> null
    }
}
