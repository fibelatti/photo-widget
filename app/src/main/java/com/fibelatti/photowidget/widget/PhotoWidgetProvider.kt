package com.fibelatti.photowidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.View
import android.widget.RemoteViews
import androidx.core.net.toUri
import androidx.core.os.postDelayed
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.configure.appWidgetId
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidget.Companion.DEFAULT_CORNER_RADIUS
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetBorder
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import com.fibelatti.photowidget.platform.WidgetSizeProvider
import com.fibelatti.photowidget.platform.setIdentifierCompat
import com.fibelatti.photowidget.viewer.PhotoWidgetViewerActivity
import java.lang.ref.WeakReference
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

class PhotoWidgetProvider : AppWidgetProvider() {

    private val handler: Handler = Handler(Looper.getMainLooper())

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        Timber.d("Broadcast received (action=${intent.action}, appWidgetId=${intent.appWidgetId})")

        val acceptedAction = ACTION_VIEW_NEXT_PHOTO == intent.action ||
            ACTION_VIEW_PREVIOUS_PHOTO == intent.action

        if (!acceptedAction) {
            return
        }

        handler.postDelayed(delayInMillis = 300) {
            runCatching {
                with(entryPoint<PhotoWidgetEntryPoint>(context)) {
                    coroutineScope().launch {
                        cyclePhotoUseCase().invoke(
                            appWidgetId = intent.appWidgetId,
                            flipBackwards = ACTION_VIEW_PREVIOUS_PHOTO == intent.action,
                        )
                        photoWidgetStorage().saveWidgetNextCycleTime(
                            appWidgetId = intent.appWidgetId,
                            nextCycleTime = null,
                        )
                        photoWidgetAlarmManager().setup(
                            appWidgetId = intent.appWidgetId,
                        )
                    }
                }
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Timber.d("Update requested by the system (appWidgetIds=${appWidgetIds.toList()})")

        handler.postDelayed(delayInMillis = 300) {
            for (appWidgetId in appWidgetIds) {
                val widgetOptions: Bundle? = appWidgetManager.getAppWidgetOptions(appWidgetId)

                update(context = context, appWidgetId = appWidgetId, widgetOptions = widgetOptions)
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?,
    ) {
        Timber.d("Options changed by the system (appWidgetId=$appWidgetId)")

        handler.postDelayed(delayInMillis = 300) {
            update(context = context, appWidgetId = appWidgetId, widgetOptions = newOptions)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        Timber.d("Delete requested by the system (appWidgetIds=${appWidgetIds.toList()})")

        handler.postDelayed(delayInMillis = 300) {
            val entryPoint = entryPoint<PhotoWidgetEntryPoint>(context)
            val storage = entryPoint.photoWidgetStorage()
            val alarmManager = entryPoint.photoWidgetAlarmManager()

            for (appWidgetId in appWidgetIds) {
                storage.saveWidgetDeletionTimestamp(appWidgetId = appWidgetId, timestamp = System.currentTimeMillis())
                alarmManager.cancel(appWidgetId = appWidgetId)
            }
        }
    }

    companion object {

        private const val ACTION_VIEW_NEXT_PHOTO = "ACTION_VIEW_NEXT_PHOTO"
        private const val ACTION_VIEW_PREVIOUS_PHOTO = "ACTION_VIEW_PREVIOUS_PHOTO"

        private val updateJobMap: MutableMap<Int, WeakReference<Job>> = mutableMapOf()

        fun ids(context: Context): List<Int> = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, PhotoWidgetProvider::class.java))
            .toList()
            .also { Timber.d("Provider widget IDs: $it") }

        fun update(
            context: Context,
            appWidgetId: Int,
            widgetOptions: Bundle? = null,
            recoveryMode: Boolean = false,
        ) {
            Timber.d("Updating widget (appWidgetId=$appWidgetId)")

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val entryPoint = entryPoint<PhotoWidgetEntryPoint>(context)
            val coroutineScope = entryPoint.coroutineScope()
            val photoWidgetStorage = entryPoint.photoWidgetStorage()
            val pinningCache = entryPoint.photoWidgetPinningCache()
            val loadPhotoWidgetUseCase = entryPoint.loadPhotoWidgetUseCase()

            val currentJob: Job? = updateJobMap[appWidgetId]?.get()
            Timber.d("Current update job (isActive=${currentJob?.isActive})")

            val newJob: Job = coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                currentJob?.join()

                val photoWidget: PhotoWidget = pinningCache.pendingWidget
                    ?.takeIf { appWidgetId !in photoWidgetStorage.getKnownWidgetIds() }
                    ?.also { Timber.d("Updating using the pending widget data") }
                    ?: loadPhotoWidgetUseCase(appWidgetId = appWidgetId).first { !it.isLoading }

                val views: RemoteViews = createRemoteViews(
                    context = context,
                    appWidgetId = appWidgetId,
                    photoWidget = photoWidget,
                    widgetOptions = widgetOptions,
                    recoveryMode = recoveryMode,
                )

                setClickPendingIntent(
                    views = views,
                    context = context,
                    appWidgetId = appWidgetId,
                    photoWidget = photoWidget,
                    isLocked = photoWidgetStorage.getWidgetLockedInApp(appWidgetId = appWidgetId),
                    isCyclePaused = photoWidgetStorage.getWidgetCyclePaused(appWidgetId = appWidgetId),
                    widgetOptions = widgetOptions,
                )

                Timber.d("Invoking AppWidgetManager#updateAppWidget")

                try {
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (ex: IllegalArgumentException) {
                    if (!recoveryMode) {
                        update(context = context, appWidgetId = appWidgetId, recoveryMode = true)
                    } else {
                        throw RuntimeException("Unable to update widget using recovery mode", ex)
                    }
                }
            }

            updateJobMap[appWidgetId] = WeakReference(newJob)
        }

        private suspend fun createRemoteViews(
            context: Context,
            appWidgetId: Int,
            photoWidget: PhotoWidget,
            widgetOptions: Bundle?,
            recoveryMode: Boolean = false,
        ): RemoteViews {
            val prepareCurrentPhotoUseCase = entryPoint<PhotoWidgetEntryPoint>(context).prepareCurrentPhotoUseCase()

            val shouldMeasure = PhotoWidgetAspectRatio.FILL_WIDGET == photoWidget.aspectRatio &&
                (photoWidget.border !is PhotoWidgetBorder.None || photoWidget.cornerRadius != DEFAULT_CORNER_RADIUS)
            val widgetSize = if (shouldMeasure && widgetOptions != null) {
                val sizeProvider = WidgetSizeProvider(context = context)
                val (width, height) = sizeProvider.getWidgetsSize(
                    widgetOptions = widgetOptions,
                    convertToPx = true,
                )
                Size(width, height)
            } else {
                null
            }

            val result = prepareCurrentPhotoUseCase(
                context = context,
                appWidgetId = appWidgetId,
                photoWidget = photoWidget,
                widgetSize = widgetSize,
                recoveryMode = recoveryMode,
            )

            val remoteViews = RemoteViews(context.packageName, R.layout.photo_widget)

            if (result == null) {
                Timber.d("Failed to prepare current photo")
                return remoteViews.apply {
                    setViewVisibility(R.id.iv_placeholder, View.VISIBLE)
                    setImageViewResource(R.id.iv_placeholder, R.drawable.ic_file_not_found)
                }
            }

            Timber.d("Current photo prepared successfully")
            return remoteViews.apply {
                val visibleImageViewId: Int
                val hiddenImageViewId: Int

                if (PhotoWidgetAspectRatio.FILL_WIDGET == photoWidget.aspectRatio) {
                    visibleImageViewId = R.id.iv_widget_fill
                    hiddenImageViewId = R.id.iv_widget
                } else {
                    visibleImageViewId = R.id.iv_widget
                    hiddenImageViewId = R.id.iv_widget_fill
                }

                setViewVisibility(visibleImageViewId, View.VISIBLE)
                setViewVisibility(hiddenImageViewId, View.GONE)
                setViewVisibility(R.id.iv_placeholder, View.GONE)

                if (result.uri != null) {
                    setImageViewUri(visibleImageViewId, result.uri)
                } else {
                    setImageViewBitmap(visibleImageViewId, result.fallback)
                }

                setViewPadding(
                    /* viewId = */
                    visibleImageViewId,
                    /* left = */
                    getDimensionValue(context, photoWidget.padding + photoWidget.horizontalOffset),
                    /* top = */
                    getDimensionValue(context, photoWidget.padding + photoWidget.verticalOffset),
                    /* right = */
                    getDimensionValue(context, photoWidget.padding),
                    /* bottom = */
                    getDimensionValue(context, photoWidget.padding),
                )
            }
        }

        private fun getDimensionValue(context: Context, value: Int): Int {
            return (value * context.resources.displayMetrics.density * PhotoWidget.POSITIONING_MULTIPLIER).roundToInt()
        }

        private fun setClickPendingIntent(
            views: RemoteViews,
            context: Context,
            appWidgetId: Int,
            photoWidget: PhotoWidget,
            isLocked: Boolean,
            isCyclePaused: Boolean,
            widgetOptions: Bundle?,
        ) {
            val multiActionSupported = if (widgetOptions != null) {
                val sizeProvider = WidgetSizeProvider(context = context)
                val (width, _) = sizeProvider.getWidgetsSize(widgetOptions = widgetOptions)
                width > 100
            } else {
                true
            }
            val mainClickPendingIntent = getClickPendingIntent(
                context = context,
                appWidgetId = appWidgetId,
                tapAction = photoWidget.tapAction,
                externalUri = photoWidget.currentPhoto?.externalUri,
            ).takeUnless {
                val photoChangingAction = photoWidget.tapAction is PhotoWidgetTapAction.ViewNextPhoto ||
                    photoWidget.tapAction is PhotoWidgetTapAction.ToggleCycling

                isLocked && photoChangingAction
            }

            views.setOnClickPendingIntent(R.id.view_tap_center, mainClickPendingIntent)

            if (!multiActionSupported) {
                // The widget is too narrow to handle 3 different click actions
                views.setOnClickPendingIntent(R.id.view_tap_left, mainClickPendingIntent)
                views.setOnClickPendingIntent(R.id.view_tap_right, mainClickPendingIntent)
                return
            }

            val shouldDisableTap = photoWidget.tapAction is PhotoWidgetTapAction.ToggleCycling &&
                photoWidget.tapAction.disableTap &&
                isCyclePaused

            views.setOnClickPendingIntent(
                R.id.view_tap_left,
                flipPhotoPendingIntent(
                    context = context,
                    appWidgetId = appWidgetId,
                    flipBackwards = true,
                ).takeUnless { isLocked || shouldDisableTap },
            )
            views.setOnClickPendingIntent(
                R.id.view_tap_right,
                flipPhotoPendingIntent(
                    context = context,
                    appWidgetId = appWidgetId,
                    flipBackwards = false,
                ).takeUnless { isLocked || shouldDisableTap },
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
                        /* context = */
                        context,
                        /* requestCode = */
                        appWidgetId,
                        /* intent = */
                        clickIntent,
                        /* flags = */
                        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    )
                }

                is PhotoWidgetTapAction.ViewInGallery -> {
                    if (externalUri == null) return null

                    val intent = Intent(Intent.ACTION_VIEW)
                        .setDataAndType(externalUri, "image/*")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .setIdentifierCompat("$appWidgetId")

                    if (tapAction.galleryApp != null) {
                        intent.setPackage(tapAction.galleryApp)
                    }

                    return PendingIntent.getActivity(
                        /* context = */
                        context,
                        /* requestCode = */
                        appWidgetId,
                        /* intent = */
                        intent,
                        /* flags = */
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    )
                }

                is PhotoWidgetTapAction.ViewNextPhoto -> {
                    return flipPhotoPendingIntent(
                        context = context,
                        appWidgetId = appWidgetId,
                        flipBackwards = false,
                    )
                }

                is PhotoWidgetTapAction.ToggleCycling -> {
                    val intent = Intent(context, ToggleCyclingFeedbackActivity::class.java).apply {
                        setIdentifierCompat("$appWidgetId")
                        this.appWidgetId = appWidgetId
                    }
                    return PendingIntent.getActivity(
                        /* context = */
                        context,
                        /* requestCode = */
                        appWidgetId,
                        /* intent = */
                        intent,
                        /* flags = */
                        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
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
                        /* context = */
                        context,
                        /* requestCode = */
                        appWidgetId,
                        /* intent = */
                        launchIntent,
                        /* flags = */
                        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    )
                }

                is PhotoWidgetTapAction.UrlShortcut -> {
                    if (tapAction.url.isNullOrBlank()) return null

                    val intent = Intent(Intent.ACTION_VIEW, tapAction.url.toUri())
                        .setIdentifierCompat("$appWidgetId")

                    return PendingIntent.getActivity(
                        /* context = */
                        context,
                        /* requestCode = */
                        appWidgetId,
                        /* intent = */
                        intent,
                        /* flags = */
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
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
                /* context = */
                context,
                /* requestCode = */
                appWidgetId,
                /* intent = */
                intent,
                /* flags = */
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
    }
}
