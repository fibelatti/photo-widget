package com.fibelatti.photowidget.di

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.fibelatti.photowidget.widget.LoopingPhotoWidgetWorker
import com.fibelatti.photowidget.widget.PhotoWidgetStorage
import javax.inject.Inject

class InjectingWorkerFactory @Inject constructor(
    private val photoWidgetStorage: PhotoWidgetStorage,
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? = when (workerClassName) {
        LoopingPhotoWidgetWorker::class.java.name -> LoopingPhotoWidgetWorker(
            context = appContext,
            workerParams = workerParameters,
            photoWidgetStorage = photoWidgetStorage,
        )

        else -> null
    }
}
