package com.fibelatti.photowidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
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
import com.fibelatti.photowidget.platform.WidgetSizeProvider
import com.fibelatti.photowidget.platform.setIdentifierCompat
import com.fibelatti.photowidget.platform.withPolygonalShape
import com.fibelatti.photowidget.platform.withRoundedCorners
import com.fibelatti.photowidget.viewer.PhotoWidgetViewerActivity
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.flow.last
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

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        update(context = context, appWidgetId = appWidgetId)
    }

    companion object {

        private const val ACTION_VIEW_NEXT_PHOTO = "ACTION_VIEW_NEXT_PHOTO"
        private const val ACTION_VIEW_PREVIOUS_PHOTO = "ACTION_VIEW_PREVIOUS_PHOTO"

        // RemoteViews have a maximum allowed memory for bitmaps
        private const val MAX_WIDGET_BITMAP_MEMORY = 6_912_000

        fun ids(context: Context): List<Int> = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, PhotoWidgetProvider::class.java))
            .toList()
            .also { Timber.d("Provider widget IDs: $it") }

        fun update(context: Context, appWidgetId: Int, recoveryMode: Boolean = false) {
            Timber.d("Updating widget (appWidgetId=$appWidgetId)")

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val entryPoint = entryPoint<PhotoWidgetEntryPoint>(context)
            val coroutineScope = entryPoint.coroutineScope()
            val loadPhotoWidgetUseCase = entryPoint.loadPhotoWidgetUseCase()

            coroutineScope.launch {
                Timber.d("Loading widget data")
                val photoWidget = loadPhotoWidgetUseCase(appWidgetId = appWidgetId).last()
                val tempViews = PhotoWidgetPinnedReceiver.preview?.get()
                    ?.takeIf { photoWidget.photos.isEmpty() }
                    ?.also { PhotoWidgetPinnedReceiver.preview = null }
                val tempWidget = PhotoWidgetPinnedReceiver.callbackIntent?.get()?.photoWidget
                    ?.takeIf { tempViews != null }

                val views = tempViews ?: createRemoteViews(
                    context = context,
                    photoWidget = photoWidget,
                    recoveryMode = recoveryMode,
                )

                setClickPendingIntent(
                    views = views,
                    context = context,
                    appWidgetId = appWidgetId,
                    photoWidget = tempWidget ?: photoWidget,
                )

                Timber.d("Dispatching post-load remote views to AppWidgetManager")

                try {
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (ex: IllegalArgumentException) {
                    if (!recoveryMode) {
                        update(context = context, appWidgetId = appWidgetId, recoveryMode = true)
                    } else {
                        throw ex
                    }
                }
            }
        }

        suspend fun createRemoteViews(
            context: Context,
            photoWidget: PhotoWidget,
            recoveryMode: Boolean = false,
        ): RemoteViews {
            Timber.d("Creating remote views")
            val errorView = createBaseView(context = context, aspectRatio = photoWidget.aspectRatio).apply {
                setViewVisibility(R.id.iv_placeholder, View.VISIBLE)
                setImageViewResource(R.id.iv_placeholder, R.drawable.ic_file_not_found)
            }

            val currentPhoto = photoWidget.currentPhoto ?: return errorView

            val entryPoint = entryPoint<PhotoWidgetEntryPoint>(context)
            val decoder = entryPoint.photoDecoder()

            Timber.d("Decoding the bitmap")
            val bitmap = try {
                val data = currentPhoto.getPhotoPath() ?: return errorView
                val displayMetrics: DisplayMetrics = context.resources.displayMetrics
                val maxMemoryAllowed: Int = if (!recoveryMode) {
                    (displayMetrics.heightPixels * displayMetrics.widthPixels * 4 * 1.5).roundToInt()
                } else {
                    MAX_WIDGET_BITMAP_MEMORY
                }
                val maxMemoryDimension: Int = sqrt(maxMemoryAllowed / 4 / displayMetrics.density).roundToInt()
                val maxDimension: Int = if (PhotoWidgetAspectRatio.SQUARE != photoWidget.aspectRatio) {
                    maxMemoryDimension
                } else {
                    maxMemoryDimension.coerceAtMost(maximumValue = PhotoWidget.MAX_WIDGET_DIMENSION)
                }

                Timber.d(
                    "Creating widget bitmap (" +
                        "maxMemoryAllowed=$maxMemoryAllowed," +
                        "maxDimension=$maxDimension," +
                        "recoveryMode=$recoveryMode" +
                        ")",
                )

                requireNotNull(decoder.decode(data = data, maxDimension = maxDimension))
            } catch (_: Exception) {
                return errorView
            }

            Timber.d("Transforming the bitmap")
            val transformedBitmap = if (PhotoWidgetAspectRatio.SQUARE == photoWidget.aspectRatio) {
                bitmap.withPolygonalShape(
                    shapeId = photoWidget.shapeId,
                    opacity = photoWidget.opacity,
                    borderColorHex = photoWidget.borderColor,
                    borderWidth = photoWidget.borderWidth,
                )
            } else {
                bitmap.withRoundedCorners(
                    aspectRatio = photoWidget.aspectRatio,
                    radius = if (PhotoWidgetAspectRatio.FILL_WIDGET == photoWidget.aspectRatio) {
                        0F
                    } else {
                        photoWidget.cornerRadius
                    },
                    opacity = photoWidget.opacity,
                    borderColorHex = photoWidget.borderColor,
                    borderWidth = photoWidget.borderWidth,
                )
            }

            return createBaseView(context = context, aspectRatio = photoWidget.aspectRatio).apply {
                setViewVisibility(R.id.iv_placeholder, View.GONE)
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

        private fun createBaseView(context: Context, aspectRatio: PhotoWidgetAspectRatio): RemoteViews {
            val layoutId = if (PhotoWidgetAspectRatio.FILL_WIDGET == aspectRatio) {
                R.layout.photo_widget_fill
            } else {
                R.layout.photo_widget
            }

            return RemoteViews(context.packageName, layoutId)
        }

        private fun getDimensionValue(context: Context, value: Int): Int {
            return TypedValue.applyDimension(
                /* unit = */ TypedValue.COMPLEX_UNIT_DIP,
                /* value = */ value * 10f,
                /* metrics = */ context.resources.displayMetrics,
            ).toInt()
        }

        private fun setClickPendingIntent(
            views: RemoteViews,
            context: Context,
            appWidgetId: Int,
            photoWidget: PhotoWidget,
        ) {
            val sizeProvider = WidgetSizeProvider(context = context.applicationContext)
            val (width, _) = sizeProvider.getWidgetsSize(appWidgetId = appWidgetId)

            if (width < 100) {
                // The widget is too narrow to handle 3 different click areas
                views.setViewVisibility(R.id.tap_actions_layout, View.GONE)
                views.setOnClickPendingIntent(
                    R.id.iv_widget,
                    getClickPendingIntent(
                        context = context,
                        appWidgetId = appWidgetId,
                        tapAction = photoWidget.tapAction,
                        externalUri = photoWidget.currentPhoto?.externalUri,
                    ),
                )

                return
            }

            views.setViewVisibility(R.id.tap_actions_layout, View.VISIBLE)
            views.setOnClickPendingIntent(
                R.id.view_tap_left,
                flipPhotoPendingIntent(
                    context = context,
                    appWidgetId = appWidgetId,
                    flipBackwards = true,
                ),
            )
            views.setOnClickPendingIntent(
                R.id.view_tap_center,
                getClickPendingIntent(
                    context = context,
                    appWidgetId = appWidgetId,
                    tapAction = photoWidget.tapAction,
                    externalUri = photoWidget.currentPhoto?.externalUri,
                ),
            )
            views.setOnClickPendingIntent(
                R.id.view_tap_right,
                flipPhotoPendingIntent(
                    context = context,
                    appWidgetId = appWidgetId,
                    flipBackwards = false,
                ),
            )
        }

        private fun getClickPendingIntent(
            context: Context,
            appWidgetId: Int,
            tapAction: PhotoWidgetTapAction,
            externalUri: Uri?,
        ): PendingIntent? {
            when (tapAction) {
                is PhotoWidgetTapAction.None -> return null

                is PhotoWidgetTapAction.ViewFullScreen -> {
                    val clickIntent = Intent(context, PhotoWidgetViewerActivity::class.java).apply {
                        setIdentifierCompat("$appWidgetId")
                        this.appWidgetId = appWidgetId
                    }
                    return PendingIntent.getActivity(
                        /* context = */ context,
                        /* requestCode = */ appWidgetId,
                        /* intent = */ clickIntent,
                        /* flags = */ PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    )
                }

                is PhotoWidgetTapAction.ViewInGallery -> {
                    if (externalUri == null) return null

                    val intent = Intent(Intent.ACTION_VIEW, externalUri).apply {
                        setIdentifierCompat("$appWidgetId")
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }

                    return PendingIntent.getActivity(
                        /* context = */ context,
                        /* requestCode = */ appWidgetId,
                        /* intent = */ intent,
                        /* flags = */ PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    )
                }

                is PhotoWidgetTapAction.AppShortcut -> {
                    if (tapAction.appShortcut == null) return null

                    val pm = context.packageManager
                    var launchIntent = pm.getLaunchIntentForPackage(tapAction.appShortcut)

                    if (launchIntent == null) {
                        val queryIntent = Intent(Intent.ACTION_MAIN).setPackage(tapAction.appShortcut)
                        val activity = pm.queryIntentActivities(queryIntent, 0).firstOrNull()?.activityInfo
                        if (activity != null) {
                            launchIntent = Intent(Intent.ACTION_MAIN)
                                .setComponent(ComponentName(activity.applicationInfo.packageName, activity.name))
                        }
                    }

                    if (launchIntent == null) return null

                    launchIntent.setIdentifierCompat("$appWidgetId")

                    return PendingIntent.getActivity(
                        /* context = */ context,
                        /* requestCode = */ appWidgetId,
                        /* intent = */ launchIntent,
                        /* flags = */ PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    )
                }

                is PhotoWidgetTapAction.UrlShortcut -> {
                    if (tapAction.url.isNullOrBlank()) return null

                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tapAction.url))
                        .setIdentifierCompat("$appWidgetId")

                    return PendingIntent.getActivity(
                        /* context = */ context,
                        /* requestCode = */ appWidgetId,
                        /* intent = */ intent,
                        /* flags = */ PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    )
                }

                is PhotoWidgetTapAction.ToggleCycling -> {
                    val intent = Intent(context, ToggleCyclingFeedbackActivity::class.java).apply {
                        setIdentifierCompat("$appWidgetId")
                        this.appWidgetId = appWidgetId
                    }
                    return PendingIntent.getActivity(
                        /* context = */ context,
                        /* requestCode = */ appWidgetId,
                        /* intent = */ intent,
                        /* flags = */ PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
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
                setIdentifierCompat("$appWidgetId")
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
