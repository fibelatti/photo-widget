package com.fibelatti.photowidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.DisplayMetrics
import android.widget.RemoteViews
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.configure.PhotoWidgetPinnedReceiver
import com.fibelatti.photowidget.configure.appWidgetId
import com.fibelatti.photowidget.configure.photoWidget
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import com.fibelatti.photowidget.platform.withPolygonalShape
import com.fibelatti.photowidget.platform.withRoundedCorners
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.sqrt

class PhotoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            update(context = context, appWidgetId = appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val entryPoint = entryPoint<PhotoWidgetEntryPoint>(context)
        val coroutineScope = entryPoint.coroutineScope()
        val storage = entryPoint.photoWidgetStorage()
        val alarmManager = entryPoint.photoWidgetAlarmManager()

        for (appWidgetId in appWidgetIds) {
            coroutineScope.launch {
                storage.deleteWidgetData(appWidgetId = appWidgetId)
            }
            alarmManager.cancel(appWidgetId = appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (TAP_TO_FLIP_ACTION == intent.action) {
            runCatching {
                val entryPoint = entryPoint<PhotoWidgetEntryPoint>(context)
                val scope = entryPoint.coroutineScope()
                val flipPhotoUseCase = entryPoint.flipPhotoUseCase()

                scope.launch {
                    flipPhotoUseCase(appWidgetId = intent.appWidgetId)
                }
            }
        }
    }

    companion object {

        private const val TAP_TO_FLIP_ACTION = "TAP_TO_FLIP_ACTION"

        // RemoteViews have a maximum allowed memory for bitmaps
        private const val MAX_WIDGET_BITMAP_MEMORY = 6_912_000

        fun ids(context: Context): List<Int> = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, PhotoWidgetProvider::class.java))
            .toList()

        fun update(context: Context, appWidgetId: Int) {
            val entryPoint = entryPoint<PhotoWidgetEntryPoint>(context)
            val coroutineScope = entryPoint.coroutineScope()
            val loadPhotoWidgetUseCase = entryPoint.loadPhotoWidgetUseCase()

            coroutineScope.launch {
                Timber.d("Updating widget (appWidgetId=$appWidgetId)")

                val photoWidget = loadPhotoWidgetUseCase(appWidgetId = appWidgetId, currentPhotoOnly = true)
                val tempViews = PhotoWidgetPinnedReceiver.preview?.get()
                    ?.takeIf { photoWidget.photos.isEmpty() }
                    ?.also { PhotoWidgetPinnedReceiver.preview = null }
                val tempWidget = PhotoWidgetPinnedReceiver.callbackIntent?.get()?.photoWidget
                    ?.takeIf { tempViews != null }

                val views = tempViews
                    ?: createRemoteViews(context = context, photoWidget = photoWidget)
                    ?: return@launch

                val clickPendingIntent = getClickPendingIntent(
                    context = context,
                    appWidgetId = appWidgetId,
                    tapAction = tempWidget?.tapAction ?: photoWidget.tapAction,
                    appShortcut = tempWidget?.appShortcut ?: photoWidget.appShortcut,
                )

                views.setOnClickPendingIntent(R.id.iv_widget, clickPendingIntent)

                Timber.d("Dispatching update to AppWidgetManager")
                AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views)
            }
        }

        suspend fun createRemoteViews(
            context: Context,
            photoWidget: PhotoWidget,
        ): RemoteViews? {
            Timber.d("Creating remote views")

            val entryPoint = entryPoint<PhotoWidgetEntryPoint>(context)
            val decoder = entryPoint.photoDecoder()

            Timber.d("Decoding the bitmap")
            val bitmap = try {
                val data = when {
                    !photoWidget.currentPhoto.path.isNullOrEmpty() -> photoWidget.currentPhoto.path
                    photoWidget.currentPhoto.externalUri != null -> photoWidget.currentPhoto.externalUri
                    else -> return null
                }
                val displayMetrics: DisplayMetrics = context.resources.displayMetrics
                val maxDimension = sqrt(MAX_WIDGET_BITMAP_MEMORY / 4 / displayMetrics.density).toInt()
                    .coerceAtMost(maximumValue = PhotoWidget.MAX_WIDGET_DIMENSION)

                requireNotNull(decoder.decode(data = data, maxDimension = maxDimension))
            } catch (_: Exception) {
                return null
            }

            Timber.d("Transforming the bitmap")
            val transformedBitmap = if (photoWidget.aspectRatio == PhotoWidgetAspectRatio.SQUARE) {
                bitmap.withPolygonalShape(shapeId = photoWidget.shapeId)
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

        private fun getClickPendingIntent(
            context: Context,
            appWidgetId: Int,
            tapAction: PhotoWidgetTapAction,
            appShortcut: String?,
        ): PendingIntent? = when (tapAction) {
            PhotoWidgetTapAction.NONE -> null

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

            PhotoWidgetTapAction.APP_SHORTCUT -> {
                appShortcut?.let(context.packageManager::getLaunchIntentForPackage).let { clickIntent ->
                    PendingIntent.getActivity(
                        context,
                        appWidgetId,
                        clickIntent,
                        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    )
                }
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
