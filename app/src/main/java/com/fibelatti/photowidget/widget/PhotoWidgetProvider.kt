package com.fibelatti.photowidget.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.graphics.BitmapFactory
import android.widget.RemoteViews
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetShapeBuilder
import com.fibelatti.photowidget.platform.withPolygonalShape
import com.fibelatti.photowidget.platform.withRoundedCorners

class PhotoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val entryPoint = entryPoint<PhotoWidgetEntryPoint>(context)
        val storage = entryPoint.photoWidgetStorage()

        for (appWidgetId in appWidgetIds) {
            val photos = storage.getWidgetPhotos(appWidgetId = appWidgetId)
            val currentIndex = storage.getWidgetIndex(appWidgetId = appWidgetId)
            val photoPath = photos.getOrNull(currentIndex)?.path ?: continue
            val aspectRatio = storage.getWidgetAspectRatio(appWidgetId = appWidgetId)
            val shapeId = storage.getWidgetShapeId(appWidgetId = appWidgetId)

            update(
                context = context,
                appWidgetId = appWidgetId,
                photoPath = photoPath,
                aspectRatio = aspectRatio,
                shapeId = shapeId,
                appWidgetManager = appWidgetManager,
            )
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val entryPoint = entryPoint<PhotoWidgetEntryPoint>(context)
        val storage = entryPoint.photoWidgetStorage()
        val workManager = entryPoint.photoWidgetWorkManager()

        for (appWidgetId in appWidgetIds) {
            storage.deleteWidgetData(appWidgetId = appWidgetId)
            workManager.cancelWidgetWork(appWidgetId = appWidgetId)
        }
    }

    companion object {

        fun ids(context: Context): List<Int> = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, PhotoWidgetProvider::class.java))
            .toList()

        fun update(
            context: Context,
            appWidgetId: Int,
            photoPath: String,
            aspectRatio: PhotoWidgetAspectRatio,
            shapeId: String?,
            appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context),
        ) {
            val views = createRemoteViews(
                context = context,
                photoPath = photoPath,
                aspectRatio = aspectRatio,
                shapeId = shapeId,
            )

            if (views != null) {
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        fun createRemoteViews(
            context: Context,
            photoPath: String,
            aspectRatio: PhotoWidgetAspectRatio,
            shapeId: String?,
        ): RemoteViews? {
            val bitmap = try {
                requireNotNull(BitmapFactory.decodeFile(photoPath))
            } catch (_: Exception) {
                return null
            }
            val transformedBitmap = if (aspectRatio == PhotoWidgetAspectRatio.SQUARE) {
                val shape = PhotoWidgetShapeBuilder.buildShape(
                    shapeId = shapeId,
                    width = bitmap.width,
                    height = bitmap.height,
                )

                bitmap.withPolygonalShape(shape)
            } else {
                bitmap.withRoundedCorners()
            }

            return RemoteViews(context.packageName, R.layout.photo_widget).apply {
                setImageViewBitmap(R.id.iv_widget, transformedBitmap)
            }
        }
    }
}
