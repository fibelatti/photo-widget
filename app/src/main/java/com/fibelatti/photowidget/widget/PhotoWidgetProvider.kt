package com.fibelatti.photowidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.DisplayMetrics
import android.util.TypedValue
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
import kotlin.math.sqrt
import kotlinx.coroutines.launch
import timber.log.Timber

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

        if (ACTION_VIEW_NEXT_PHOTO == intent.action || ACTION_VIEW_PREVIOUS_PHOTO == intent.action) {
            runCatching {
                val entryPoint = entryPoint<PhotoWidgetEntryPoint>(context)

                entryPoint.coroutineScope().launch {
                    entryPoint.flipPhotoUseCase().invoke(
                        appWidgetId = intent.appWidgetId,
                        flipBackwards = ACTION_VIEW_PREVIOUS_PHOTO == intent.action,
                    )
                }
            }
        }
    }

    companion object {

        private const val ACTION_VIEW_NEXT_PHOTO = "ACTION_VIEW_NEXT_PHOTO"
        private const val ACTION_VIEW_PREVIOUS_PHOTO = "ACTION_VIEW_PREVIOUS_PHOTO"

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

                val photoWidget = loadPhotoWidgetUseCase(appWidgetId = appWidgetId)
                val tempViews = PhotoWidgetPinnedReceiver.preview?.get()
                    ?.takeIf { photoWidget.photos.isEmpty() }
                    ?.also { PhotoWidgetPinnedReceiver.preview = null }
                val tempWidget = PhotoWidgetPinnedReceiver.callbackIntent?.get()?.photoWidget
                    ?.takeIf { tempViews != null }

                val views = tempViews
                    ?: createRemoteViews(context = context, photoWidget = photoWidget)
                    ?: return@launch

                views.setOnClickPendingIntent(
                    R.id.view_tap_previous,
                    getClickPendingIntent(
                        context = context,
                        appWidgetId = appWidgetId,
                        tapAction = tempWidget?.tapAction ?: photoWidget.tapAction,
                        flipBackwards = true,
                        appShortcut = tempWidget?.appShortcut ?: photoWidget.appShortcut,
                    ),
                )
                views.setOnClickPendingIntent(
                    R.id.view_tap_next,
                    getClickPendingIntent(
                        context = context,
                        appWidgetId = appWidgetId,
                        tapAction = tempWidget?.tapAction ?: photoWidget.tapAction,
                        flipBackwards = false,
                        appShortcut = tempWidget?.appShortcut ?: photoWidget.appShortcut,
                    ),
                )

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
                bitmap.withPolygonalShape(
                    shapeId = photoWidget.shapeId,
                    opacity = photoWidget.opacity,
                )
            } else {
                bitmap.withRoundedCorners(
                    aspectRatio = photoWidget.aspectRatio,
                    radius = photoWidget.cornerRadius,
                    opacity = photoWidget.opacity,
                )
            }

            return RemoteViews(context.packageName, R.layout.photo_widget).apply {
                setImageViewBitmap(R.id.iv_widget, transformedBitmap)
                setViewPadding(
                    /* viewId = */ R.id.iv_widget,
                    /* left = */ TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        photoWidget.horizontalOffset * 10f,
                        context.resources.displayMetrics,
                    ).toInt(),
                    /* top = */ TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        photoWidget.verticalOffset * 10f,
                        context.resources.displayMetrics,
                    ).toInt(),
                    /* right = */ 0,
                    /* bottom = */ 0,
                )
            }
        }

        private fun getClickPendingIntent(
            context: Context,
            appWidgetId: Int,
            tapAction: PhotoWidgetTapAction,
            flipBackwards: Boolean,
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

            PhotoWidgetTapAction.VIEW_NEXT_PHOTO -> flipPhotoPendingIntent(
                context = context,
                appWidgetId = appWidgetId,
                flipBackwards = flipBackwards,
            )

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
            flipBackwards: Boolean = false,
        ): PendingIntent {
            val intent = Intent(context, PhotoWidgetProvider::class.java).apply {
                this.appWidgetId = appWidgetId
                this.action = if (flipBackwards) ACTION_VIEW_PREVIOUS_PHOTO else ACTION_VIEW_NEXT_PHOTO
            }
            return PendingIntent.getBroadcast(
                /* context = */ context,
                /* requestCode = */ appWidgetId,
                /* intent = */ intent,
                /* flags = */ PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
    }
}
