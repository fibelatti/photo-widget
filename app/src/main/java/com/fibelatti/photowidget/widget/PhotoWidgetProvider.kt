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
import com.fibelatti.photowidget.model.PhotoWidget
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
        val loadPhotoWidgetUseCase = entryPoint.loadPhotoWidgetUseCase()

        for (appWidgetId in appWidgetIds) {
            update(
                context = context,
                appWidgetId = appWidgetId,
                appWidgetManager = appWidgetManager,
                loadPhotoWidgetUseCase = loadPhotoWidgetUseCase,
            )
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val entryPoint = entryPoint<PhotoWidgetEntryPoint>(context)
        val storage = entryPoint.photoWidgetStorage()
        val alarmManager = entryPoint.photoWidgetAlarmManager()

        for (appWidgetId in appWidgetIds) {
            storage.deleteWidgetData(appWidgetId = appWidgetId)
            alarmManager.cancel(appWidgetId = appWidgetId)
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
            appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context),
            loadPhotoWidgetUseCase: LoadPhotoWidgetUseCase = entryPoint<PhotoWidgetEntryPoint>(context)
                .loadPhotoWidgetUseCase(),
        ) {
            val photoWidget = loadPhotoWidgetUseCase(appWidgetId = appWidgetId)

            val views = createRemoteViews(
                context = context,
                photoWidget = photoWidget,
            ) ?: return

            val clickPendingIntent = when (photoWidget.tapAction) {
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

                PhotoWidgetTapAction.VIEW_NEXT_PHOTO -> flipPhotoPendingIntent(context, appWidgetId)
            }

            views.setOnClickPendingIntent(R.id.iv_widget, clickPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun createRemoteViews(
            context: Context,
            photoWidget: PhotoWidget,
        ): RemoteViews? {
            val bitmap = try {
                requireNotNull(BitmapFactory.decodeFile(photoWidget.currentPhoto.path))
            } catch (_: Exception) {
                return null
            }
            val transformedBitmap = if (photoWidget.aspectRatio == PhotoWidgetAspectRatio.SQUARE) {
                val shape = PhotoWidgetShapeBuilder.buildShape(
                    shapeId = photoWidget.shapeId,
                    width = bitmap.width.toFloat(),
                    height = bitmap.height.toFloat(),
                )

                bitmap.withPolygonalShape(
                    roundedPolygon = shape,
                )
            } else {
                bitmap.withRoundedCorners(
                    desiredAspectRatio = photoWidget.aspectRatio,
                    radius = photoWidget.cornerRadius,
                )
            }

            return RemoteViews(context.packageName, R.layout.photo_widget).apply {
                setImageViewBitmap(R.id.iv_widget, transformedBitmap)
            }
        }

        fun flipPhotoPendingIntent(
            context: Context,
            appWidgetId: Int,
        ): PendingIntent {
            val clickIntent = Intent(context, PhotoWidgetProvider::class.java).apply {
                this.appWidgetId = appWidgetId
                this.action = TAP_TO_FLIP_ACTION
            }
            return PendingIntent.getBroadcast(
                /* context = */ context,
                /* requestCode = */ appWidgetId,
                /* intent = */ clickIntent,
                /* flags = */ PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
    }
}
