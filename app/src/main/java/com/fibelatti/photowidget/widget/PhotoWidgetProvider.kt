package com.fibelatti.photowidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.configure.appWidgetId
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetBorder
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import com.fibelatti.photowidget.platform.WidgetSizeProvider
import com.fibelatti.photowidget.platform.setIdentifierCompat
import com.fibelatti.photowidget.viewer.PhotoWidgetViewerActivity
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

class PhotoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Timber.d("Update requested by the system (appWidgetIds=${appWidgetIds.toList()})")
        for (appWidgetId in appWidgetIds) {
            update(context = context, appWidgetId = appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        Timber.d("Deletion requested by the system (appWidgetIds=${appWidgetIds.toList()})")

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
                    entryPoint.cyclePhotoUseCase().invoke(
                        appWidgetId = intent.appWidgetId,
                        flipBackwards = ACTION_VIEW_PREVIOUS_PHOTO == intent.action,
                    )
                    entryPoint.photoWidgetStorage().saveWidgetNextCycleTime(
                        appWidgetId = intent.appWidgetId,
                        nextCycleTime = null,
                    )
                    entryPoint.photoWidgetAlarmManager().setup(appWidgetId = intent.appWidgetId)
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

        Timber.d("Options changed by the system (appWidgetId=$appWidgetId)")
        update(context = context, appWidgetId = appWidgetId)
    }

    companion object {

        private const val ACTION_VIEW_NEXT_PHOTO = "ACTION_VIEW_NEXT_PHOTO"
        private const val ACTION_VIEW_PREVIOUS_PHOTO = "ACTION_VIEW_PREVIOUS_PHOTO"

        private val updateJobMap: MutableMap<Int, WeakReference<Job>> = mutableMapOf()

        fun ids(context: Context): List<Int> = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, PhotoWidgetProvider::class.java))
            .toList()
            .also { Timber.d("Provider widget IDs: $it") }

        fun update(context: Context, appWidgetId: Int, recoveryMode: Boolean = false) {
            Timber.d("Updating widget (appWidgetId=$appWidgetId)")

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val entryPoint = entryPoint<PhotoWidgetEntryPoint>(context)
            val coroutineScope = entryPoint.coroutineScope()
            val photoWidgetStorage = entryPoint.photoWidgetStorage()
            val pinningCache = entryPoint.photoWidgetPinningCache()
            val loadPhotoWidgetUseCase = entryPoint.loadPhotoWidgetUseCase()

            val currentJob = updateJobMap[appWidgetId]?.get()
            Timber.d("Current update job (isActive=${currentJob?.isActive})")

            val newJob = coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                currentJob?.join()

                val photoWidget = pinningCache.pendingWidget
                    ?.takeIf { appWidgetId !in photoWidgetStorage.getKnownWidgetIds() }
                    ?.also { Timber.d("Updating using the pending widget data") }
                    ?: loadPhotoWidgetUseCase(appWidgetId = appWidgetId).first { !it.isLoading }

                val views = createRemoteViews(
                    context = context,
                    appWidgetId = appWidgetId,
                    photoWidget = photoWidget,
                    recoveryMode = recoveryMode,
                )

                setClickPendingIntent(
                    views = views,
                    context = context,
                    appWidgetId = appWidgetId,
                    photoWidget = photoWidget,
                    isCyclePaused = photoWidgetStorage.getWidgetCyclePaused(appWidgetId = appWidgetId),
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
            recoveryMode: Boolean = false,
        ): RemoteViews {
            val prepareCurrentPhotoUseCase = entryPoint<PhotoWidgetEntryPoint>(context).prepareCurrentPhotoUseCase()
            val widgetSize = if (PhotoWidgetAspectRatio.FILL_WIDGET == photoWidget.aspectRatio &&
                photoWidget.border !is PhotoWidgetBorder.None
            ) {
                val sizeProvider = WidgetSizeProvider(context = context)
                val (width, height) = sizeProvider.getWidgetsSize(appWidgetId = appWidgetId, convertToPx = true)
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
            return TypedValue.applyDimension(
                /* unit = */
                TypedValue.COMPLEX_UNIT_DIP,
                /* value = */
                value * 10f,
                /* metrics = */
                context.resources.displayMetrics,
            ).toInt()
        }

        private fun setClickPendingIntent(
            views: RemoteViews,
            context: Context,
            appWidgetId: Int,
            photoWidget: PhotoWidget,
            isCyclePaused: Boolean,
        ) {
            val sizeProvider = WidgetSizeProvider(context = context)
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

            val shouldDisableTap = photoWidget.tapAction is PhotoWidgetTapAction.ToggleCycling &&
                photoWidget.tapAction.disableTap &&
                isCyclePaused

            views.setViewVisibility(R.id.tap_actions_layout, View.VISIBLE)
            views.setOnClickPendingIntent(
                R.id.view_tap_left,
                flipPhotoPendingIntent(
                    context = context,
                    appWidgetId = appWidgetId,
                    flipBackwards = true,
                ).takeUnless { shouldDisableTap },
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
                ).takeUnless { shouldDisableTap },
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
                        .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
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

                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tapAction.url))
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
