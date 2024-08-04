package com.fibelatti.photowidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
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
        val storage = entryPoint.photoWidgetStorage()
        val alarmManager = entryPoint.photoWidgetAlarmManager()

        for (appWidgetId in appWidgetIds) {
            storage.saveWidgetDeletionTimestamp(
                appWidgetId = appWidgetId,
                timestamp = System.currentTimeMillis(),
            )
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

                val views = tempViews ?: createRemoteViews(context = context, photoWidget = photoWidget)

                views.setOnClickPendingIntent(
                    R.id.view_tap_previous,
                    getClickPendingIntent(
                        context = context,
                        appWidgetId = appWidgetId,
                        tapAction = (tempWidget ?: photoWidget).tapAction,
                        flipBackwards = true,
                        externalUri = (tempWidget ?: photoWidget).currentPhoto?.externalUri,
                        appShortcut = (tempWidget ?: photoWidget).appShortcut,
                    ),
                )
                views.setOnClickPendingIntent(
                    R.id.view_tap_next,
                    getClickPendingIntent(
                        context = context,
                        appWidgetId = appWidgetId,
                        tapAction = (tempWidget ?: photoWidget).tapAction,
                        flipBackwards = false,
                        externalUri = (tempWidget ?: photoWidget).currentPhoto?.externalUri,
                        appShortcut = (tempWidget ?: photoWidget).appShortcut,
                    ),
                )

                Timber.d("Dispatching update to AppWidgetManager")
                AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views)
            }
        }

        suspend fun createRemoteViews(
            context: Context,
            photoWidget: PhotoWidget,
        ): RemoteViews {
            Timber.d("Creating remote views")
            val currentPhoto = photoWidget.currentPhoto ?: return createErrorView(context = context)

            val entryPoint = entryPoint<PhotoWidgetEntryPoint>(context)
            val decoder = entryPoint.photoDecoder()

            Timber.d("Decoding the bitmap")
            val bitmap = try {
                val data = when {
                    !currentPhoto.path.isNullOrEmpty() -> currentPhoto.path
                    currentPhoto.externalUri != null -> currentPhoto.externalUri
                    else -> return createErrorView(context = context)
                }
                val displayMetrics: DisplayMetrics = context.resources.displayMetrics
                val maxDimension = sqrt(MAX_WIDGET_BITMAP_MEMORY / 4 / displayMetrics.density).toInt()
                    .coerceAtMost(maximumValue = PhotoWidget.MAX_WIDGET_DIMENSION)

                requireNotNull(decoder.decode(data = data, maxDimension = maxDimension))
            } catch (_: Exception) {
                return createErrorView(context = context)
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

            val layoutId = if (PhotoWidgetAspectRatio.FILL_WIDGET == photoWidget.aspectRatio) {
                R.layout.photo_widget_fill
            } else {
                R.layout.photo_widget
            }

            return RemoteViews(context.packageName, layoutId).apply {
                setImageViewBitmap(R.id.iv_widget, transformedBitmap)
                setViewPadding(
                    /* viewId = */ R.id.iv_widget,
                    /* left = */ getDimensionValue(context, photoWidget.padding + photoWidget.horizontalOffset),
                    /* top = */ getDimensionValue(context, photoWidget.padding + photoWidget.verticalOffset),
                    /* right = */ getDimensionValue(context, photoWidget.padding),
                    /* bottom = */ getDimensionValue(context, photoWidget.padding),
                )
            }
        }

        private fun createErrorView(context: Context): RemoteViews {
            return RemoteViews(context.packageName, R.layout.photo_widget_file_not_found)
        }

        private fun getDimensionValue(context: Context, value: Int): Int {
            return TypedValue.applyDimension(
                /* unit = */ TypedValue.COMPLEX_UNIT_DIP,
                /* value = */ value * 10f,
                /* metrics = */ context.resources.displayMetrics,
            ).toInt()
        }

        private fun getClickPendingIntent(
            context: Context,
            appWidgetId: Int,
            tapAction: PhotoWidgetTapAction,
            flipBackwards: Boolean,
            externalUri: Uri?,
            appShortcut: String?,
        ): PendingIntent? {
            return when (tapAction) {
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

                PhotoWidgetTapAction.VIEW_IN_GALLERY -> {
                    if (externalUri == null) return null

                    val intent = Intent(Intent.ACTION_VIEW, externalUri).apply {
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }

                    PendingIntent.getActivity(
                        context,
                        appWidgetId,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
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
