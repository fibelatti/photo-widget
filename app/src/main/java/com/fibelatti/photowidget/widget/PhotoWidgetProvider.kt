package com.fibelatti.photowidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.RemoteViews
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.configure.appWidgetId
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetShapeBuilder
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
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
            val cornerRadius = storage.getWidgetCornerRadius(appWidgetId = appWidgetId)
            val tapAction = storage.getWidgetTapAction(appWidgetId = appWidgetId)

            update(
                context = context,
                appWidgetId = appWidgetId,
                photoPath = photoPath,
                aspectRatio = aspectRatio,
                shapeId = shapeId,
                cornerRadius = cornerRadius,
                tapAction = tapAction,
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

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (TAP_TO_FLIP_ACTION == intent.action) {
            runCatching {
                val entryPoint = entryPoint<PhotoWidgetEntryPoint>(context)
                val flipPhotoUseCase = entryPoint.flipPhotoUseCase()

                flipPhotoUseCase(appWidgetId = intent.appWidgetId)
            }
        }
    }

    companion object {

        private const val TAP_TO_FLIP_ACTION = "TAP_TO_FLIP_ACTION"

        fun ids(context: Context): List<Int> = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, PhotoWidgetProvider::class.java))
            .toList()

        fun update(
            context: Context,
            appWidgetId: Int,
            photoPath: String,
            aspectRatio: PhotoWidgetAspectRatio,
            shapeId: String?,
            cornerRadius: Float,
            tapAction: PhotoWidgetTapAction,
            appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context),
        ) {
            val views = createRemoteViews(
                context = context,
                photoPath = photoPath,
                aspectRatio = aspectRatio,
                shapeId = shapeId,
                cornerRadius = cornerRadius,
            ) ?: return

            val clickPendingIntent = when (tapAction) {
                PhotoWidgetTapAction.VIEW_FULL_SCREEN -> {
                    val clickIntent = Intent(context, PhotoWidgetClickActivity::class.java).apply {
                        this.appWidgetId = appWidgetId
                    }
                    PendingIntent.getActivity(
                        context,
                        appWidgetId,
                        clickIntent,
                        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    )
                }

                PhotoWidgetTapAction.VIEW_NEXT_PHOTO -> {
                    val clickIntent = Intent(context, PhotoWidgetProvider::class.java).apply {
                        this.appWidgetId = appWidgetId
                        this.action = TAP_TO_FLIP_ACTION
                    }
                    PendingIntent.getBroadcast(
                        context,
                        appWidgetId,
                        clickIntent,
                        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    )
                }
            }

            views.setOnClickPendingIntent(R.id.iv_widget, clickPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun createRemoteViews(
            context: Context,
            photoPath: String,
            aspectRatio: PhotoWidgetAspectRatio,
            shapeId: String?,
            cornerRadius: Float,
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

                bitmap.withPolygonalShape(roundedPolygon = shape)
            } else {
                bitmap.withRoundedCorners(desiredAspectRatio = aspectRatio, radius = cornerRadius)
            }

            return RemoteViews(context.packageName, R.layout.photo_widget).apply {
                setImageViewBitmap(R.id.iv_widget, transformedBitmap)
            }
        }
    }
}
