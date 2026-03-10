package com.fibelatti.photowidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.RemoteViews
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.configure.PhotoWidgetConfigureActivity
import com.fibelatti.photowidget.configure.PhotoWidgetPinningCache
import com.fibelatti.photowidget.configure.appWidgetId
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import com.fibelatti.photowidget.model.PhotoWidgetText
import com.fibelatti.photowidget.model.tapActionDisableTap
import com.fibelatti.photowidget.model.textToBitmap
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
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

        val action: Action = Action.fromValue(intent.action) ?: return

        handler.post {
            entryPoint<PhotoWidgetEntryPoint>(context).photoWidgetProviderActionHandler().invoke(
                action = action,
                appWidgetId = intent.appWidgetId,
                onRemoveActionHandled = { didRemove: Boolean ->
                    context.startActivity(
                        HeadlessFeedbackActivity.newIntent(
                            context = context,
                            message = context.getString(
                                if (didRemove) {
                                    R.string.photo_widget_remove_action_feedback_positive
                                } else {
                                    R.string.photo_widget_remove_action_feedback_negative
                                },
                            ),
                        ),
                    )
                },
            )
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Timber.d("Update requested by the system (appWidgetIds=${appWidgetIds.toList()})")

        handler.post {
            for (appWidgetId in appWidgetIds) {
                update(context = context, appWidgetId = appWidgetId)
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

        handler.post {
            update(context = context, appWidgetId = appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        Timber.d("Delete requested by the system (appWidgetIds=${appWidgetIds.toList()})")

        handler.post {
            val entryPoint: PhotoWidgetEntryPoint = entryPoint(context)
            val storage: PhotoWidgetStorage = entryPoint.photoWidgetStorage()
            val alarmManager: PhotoWidgetAlarmManager = entryPoint.photoWidgetAlarmManager()

            for (appWidgetId in appWidgetIds) {
                storage.saveWidgetDeletionTimestamp(appWidgetId = appWidgetId, timestamp = System.currentTimeMillis())
                alarmManager.cancel(appWidgetId = appWidgetId)
            }
        }
    }

    enum class Action(val value: String) {

        VIEW_NEXT_PHOTO(value = "ACTION_VIEW_NEXT_PHOTO"),
        VIEW_PREVIOUS_PHOTO(value = "ACTION_VIEW_PREVIOUS_PHOTO"),
        REMOVE_PHOTO(value = "ACTION_REMOVE_PHOTO"),
        ;

        companion object {

            fun fromValue(value: String?): Action? = entries.firstOrNull { it.value == value }
        }
    }

    companion object {

        private val updateJobMap: MutableMap<Int, WeakReference<Job>> = mutableMapOf()

        fun ids(context: Context): List<Int> = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, PhotoWidgetProvider::class.java))
            .toList()
            .also { Timber.d("Provider widget IDs: $it") }

        fun update(
            context: Context,
            appWidgetId: Int,
            recoveryMode: Boolean = false,
        ) {
            Timber.d("Updating widget (appWidgetId=$appWidgetId)")

            val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
            val entryPoint: PhotoWidgetEntryPoint = entryPoint(context)
            val coroutineScope: CoroutineScope = entryPoint.coroutineScope()
            val photoWidgetStorage: PhotoWidgetStorage = entryPoint.photoWidgetStorage()
            val pinningCache: PhotoWidgetPinningCache = entryPoint.photoWidgetPinningCache()
            val loadPhotoWidgetUseCase: LoadPhotoWidgetUseCase = entryPoint.loadPhotoWidgetUseCase()

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
                    recoveryMode = recoveryMode,
                )

                setTapActions(
                    remoteViews = views,
                    context = context,
                    appWidgetId = appWidgetId,
                    photoWidget = photoWidget,
                    isLocked = photoWidgetStorage.getWidgetLockedInApp(appWidgetId = appWidgetId),
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

        // region RemoteViews
        // region Appearance
        private suspend fun createRemoteViews(
            context: Context,
            appWidgetId: Int,
            photoWidget: PhotoWidget,
            recoveryMode: Boolean = false,
        ): RemoteViews {
            val entryPoint: PhotoWidgetEntryPoint = entryPoint(context)
            val prepareCurrentPhotoUseCase: PrepareCurrentPhotoUseCase = entryPoint.prepareCurrentPhotoUseCase()
            val result: PrepareCurrentPhotoUseCase.Result? = prepareCurrentPhotoUseCase(
                context = context,
                appWidgetId = appWidgetId,
                photoWidget = photoWidget,
                recoveryMode = recoveryMode,
            )

            val remoteViews = RemoteViews(context.packageName, R.layout.photo_widget)

            if (result == null) {
                Timber.d("Failed to prepare current photo")
                return setErrorState(
                    remoteViews = remoteViews,
                    context = context,
                    appWidgetId = appWidgetId,
                )
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

                setViewVisibility(R.id.placeholder_layout, View.GONE)
                setViewVisibility(visibleImageViewId, View.VISIBLE)
                setViewVisibility(hiddenImageViewId, View.GONE)

                if (result.uri != null) {
                    setImageViewUri(visibleImageViewId, result.uri)
                } else {
                    setImageViewBitmap(visibleImageViewId, result.fallback)
                }

                setText(
                    remoteViews = this,
                    context = context,
                    photoWidgetText = photoWidget.text,
                )

                setPadding(
                    remoteViews = this,
                    viewId = visibleImageViewId,
                    context = context,
                    padding = photoWidget.padding,
                    verticalOffset = photoWidget.verticalOffset,
                    horizontalOffset = photoWidget.horizontalOffset,
                )
            }
        }

        private fun setText(
            remoteViews: RemoteViews,
            context: Context,
            photoWidgetText: PhotoWidgetText,
        ) {
            when (photoWidgetText) {
                is PhotoWidgetText.None -> {
                    remoteViews.setViewVisibility(R.id.iv_widget_label, View.GONE)
                }

                is PhotoWidgetText.Label -> {
                    val bitmap: Bitmap = photoWidgetText.textToBitmap(context = context)
                    val bottomPadding: Int = abs(photoWidgetText.verticalOffset)
                        .times(context.resources.displayMetrics.density)
                        .roundToInt()

                    remoteViews.setViewVisibility(R.id.iv_widget_label, View.VISIBLE)
                    remoteViews.setImageViewBitmap(R.id.iv_widget_label, bitmap)
                    remoteViews.setViewPadding(
                        /* viewId = */ R.id.iv_widget_label,
                        /* left = */ 0,
                        /* top = */ 0,
                        /* right = */ 0,
                        /* bottom = */ bottomPadding,
                    )
                }
            }
        }

        private fun setPadding(
            remoteViews: RemoteViews,
            viewId: Int,
            context: Context,
            padding: Int,
            verticalOffset: Int,
            horizontalOffset: Int,
        ) {
            var paddingLeft: Int = padding
            var paddingTop: Int = padding
            var paddingRight: Int = padding
            var paddingBottom: Int = padding

            when {
                horizontalOffset > 0 -> paddingLeft = padding + horizontalOffset
                horizontalOffset < 0 -> paddingRight = padding + abs(horizontalOffset)
            }

            when {
                verticalOffset > 0 -> paddingTop = padding + verticalOffset
                verticalOffset < 0 -> paddingBottom = padding + abs(verticalOffset)
            }

            val applyDimension: (Int) -> Int = { value: Int ->
                (value * context.resources.displayMetrics.density * PhotoWidget.POSITIONING_MULTIPLIER).roundToInt()
            }

            remoteViews.setViewPadding(
                /* viewId = */ viewId,
                /* left = */ applyDimension(paddingLeft),
                /* top = */ applyDimension(paddingTop),
                /* right = */ applyDimension(paddingRight),
                /* bottom = */ applyDimension(paddingBottom),
            )
        }
        // endregion Appearance

        // region Tap Actions
        private fun setTapActions(
            remoteViews: RemoteViews,
            context: Context,
            appWidgetId: Int,
            photoWidget: PhotoWidget,
            isLocked: Boolean,
            isCyclePaused: Boolean,
        ) {
            remoteViews.setViewVisibility(R.id.tap_actions_layout, View.VISIBLE)

            val shouldDisableTap: Boolean = photoWidget.tapActionDisableTap && isCyclePaused

            val pendingIntentArgs: (PhotoWidgetTapAction) -> PendingIntent? = { tapAction ->
                TapActionPendingIntentFactory.create(
                    context = context,
                    appWidgetId = appWidgetId,
                    tapAction = tapAction,
                    isLocked = isLocked,
                    shouldDisableTap = shouldDisableTap,
                    originalPhotoPath = photoWidget.currentPhoto?.originalPhotoPath,
                    externalUri = photoWidget.currentPhoto?.externalUri,
                )
            }

            remoteViews.setOnClickPendingIntent(R.id.view_tap_center, pendingIntentArgs(photoWidget.tapActions.center))

            val tapLeftPendingIntent: PendingIntent? = pendingIntentArgs(photoWidget.tapActions.left)
            if (tapLeftPendingIntent != null) {
                remoteViews.setViewVisibility(R.id.view_tap_left, View.VISIBLE)
                remoteViews.setOnClickPendingIntent(R.id.view_tap_left, tapLeftPendingIntent)
            } else {
                remoteViews.setViewVisibility(R.id.view_tap_left, View.INVISIBLE)
            }

            val tapRightPendingIntent: PendingIntent? = pendingIntentArgs(photoWidget.tapActions.right)
            if (tapRightPendingIntent != null) {
                remoteViews.setViewVisibility(R.id.view_tap_right, View.VISIBLE)
                remoteViews.setOnClickPendingIntent(R.id.view_tap_right, tapRightPendingIntent)
            } else {
                remoteViews.setViewVisibility(R.id.view_tap_right, View.INVISIBLE)
            }
        }

        // endregion Tap Actions

        private fun setErrorState(
            remoteViews: RemoteViews,
            context: Context,
            appWidgetId: Int,
        ): RemoteViews {
            val clickIntent = PhotoWidgetConfigureActivity.editWidgetIntent(
                context = context,
                appWidgetId = appWidgetId,
            )

            val pendingIntent = PendingIntent.getActivity(
                /* context = */ context,
                /* requestCode = */ appWidgetId,
                /* intent = */ clickIntent,
                /* flags = */ PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

            return remoteViews.apply {
                setViewVisibility(R.id.placeholder_layout, View.VISIBLE)
                setViewVisibility(R.id.iv_widget, View.GONE)
                setViewVisibility(R.id.iv_widget_fill, View.GONE)
                setViewVisibility(R.id.tap_actions_layout, View.GONE)

                setImageViewResource(R.id.iv_placeholder, R.drawable.ic_file_not_found)
                setTextViewText(R.id.tv_placeholder, context.getString(R.string.photo_widget_host_failed))

                setOnClickPendingIntent(R.id.placeholder_layout, pendingIntent)
            }
        }
        // endregion RemoteViews
    }
}
