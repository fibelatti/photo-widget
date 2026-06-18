package com.fibelatti.photowidget.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.configure.PhotoWidgetPinningCache
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.platform.ExceptionReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Renders a "transparent widget": a widget with no photos, only tap behavior. It reuses the shared
 * [PhotoWidget] model, storage, and configure activity, but has its own (photo-less) layout so the
 * rendering path skips all the photo decode / crop / cycle / alarm machinery of [PhotoWidgetProvider].
 */
class TransparentWidgetProvider : AppWidgetProvider() {

    private val handler: Handler = Handler(Looper.getMainLooper())

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Timber.i("Update requested by the system %s", mapOf("appWidgetIds" to appWidgetIds.toList()))

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
        Timber.i("Options changed by the system %s", mapOf("appWidgetId" to appWidgetId))

        handler.post {
            update(context = context, appWidgetId = appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        Timber.i("Delete requested by the system %s", mapOf("appWidgetIds" to appWidgetIds.toList()))

        handler.post {
            val storage = entryPoint<PhotoWidgetEntryPoint>(context).photoWidgetStorage()
            for (appWidgetId in appWidgetIds) {
                storage.saveWidgetDeletionTimestamp(appWidgetId = appWidgetId, timestamp = System.currentTimeMillis())
            }
        }
    }

    companion object {

        /**
         * Semi-transparent neutral scrim applied to transparent widgets while the user has enabled
         * the "highlight transparent widgets" preference, so they can be spotted and rearranged on
         * the home screen.
         */
        private const val HIGHLIGHT_COLOR: Int = 0x66808080

        fun ids(context: Context): List<Int> = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, TransparentWidgetProvider::class.java))
            .toList()
            .also { Timber.d("Provider widget IDs: $it") }

        fun update(context: Context, appWidgetId: Int) {
            Timber.i("Updating widget %s", mapOf("appWidgetId" to appWidgetId))

            val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
            val entryPoint: PhotoWidgetEntryPoint = entryPoint(context)
            val coroutineScope: CoroutineScope = entryPoint.coroutineScope()
            val pinningCache: PhotoWidgetPinningCache = entryPoint.photoWidgetPinningCache()
            val loadPhotoWidgetUseCase: LoadPhotoWidgetUseCase = entryPoint.loadPhotoWidgetUseCase()
            val exceptionReporter: ExceptionReporter = entryPoint.exceptionReporter()

            coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                val storage = entryPoint.photoWidgetStorage()

                val photoWidget: PhotoWidget = pinningCache.pendingWidget
                    ?.takeIf { appWidgetId !in storage.getKnownWidgetIds().first() }
                    ?.also { Timber.d("Updating using the pending widget data") }
                    ?: loadPhotoWidgetUseCase(appWidgetId = appWidgetId).first { !it.isLoading }

                val views = RemoteViews(context.packageName, R.layout.transparent_widget)

                views.setWidgetTapActions(
                    context = context,
                    appWidgetId = appWidgetId,
                    photoWidget = photoWidget,
                    isLocked = false,
                    isCyclePaused = false,
                    fillSidesWithCenter = true,
                )

                // Set the background unconditionally on every update: an already-hosted widget
                // reapplies RemoteViews actions onto its existing view tree, so the XML transparent
                // default is not restored. Without an explicit reset the highlight would stick once
                // enabled and never clear when the preference is turned off.
                val highlightColor: Int = if (entryPoint.userPreferencesStorage().highlightTransparentWidgets) {
                    HIGHLIGHT_COLOR
                } else {
                    Color.TRANSPARENT
                }
                views.setInt(R.id.tap_actions_layout, "setBackgroundColor", highlightColor)

                try {
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (ex: IllegalArgumentException) {
                    exceptionReporter.collectReport(ex)
                }
            }
        }
    }
}
