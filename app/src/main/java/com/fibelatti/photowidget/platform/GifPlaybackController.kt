package com.fibelatti.photowidget.platform

import android.appwidget.AppWidgetManager
import android.content.Context
import android.net.Uri
import android.widget.RemoteViews
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.GifFrames
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.widget.LoadPhotoWidgetUseCase
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class GifPlaybackController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoWidgetStorage: PhotoWidgetStorage,
    private val loadPhotoWidgetUseCase: LoadPhotoWidgetUseCase,
    private val coroutineScope: CoroutineScope,
) {

    private val widgetManager: AppWidgetManager by lazy { AppWidgetManager.getInstance(context) }

    private val playbackAllowed: MutableStateFlow<Map<Int, Boolean>> = MutableStateFlow(emptyMap())
    private val gifPlaybackJobs: ConcurrentHashMap<Int, Job> = ConcurrentHashMap()

    fun setupWidgetGif(appWidgetId: Int) {
        logger.i("Setting up gif playback (appWidgetId=$appWidgetId)")

        coroutineScope.launch {
            val source: PhotoWidgetSource = photoWidgetStorage.getWidgetSource(appWidgetId = appWidgetId)
            if (source != PhotoWidgetSource.GIF) return@launch

            val shouldPlay: Boolean = !photoWidgetStorage.getWidgetCyclePaused(appWidgetId = appWidgetId)
            playbackAllowed.update { currentMap ->
                currentMap.toMutableMap().apply { put(appWidgetId, shouldPlay) }.toMap()
            }

            val existingJob: Job? = gifPlaybackJobs.remove(appWidgetId)
            existingJob?.cancel()

            gifPlaybackJobs[appWidgetId] = getGifPlaybackJob(
                appWidgetId = appWidgetId,
                packageName = context.packageName,
            )
        }
    }

    fun tearDownWidgetGif(appWidgetId: Int) {
        logger.i("Tearing down gif (appWidgetId=$appWidgetId)")
        gifPlaybackJobs.remove(appWidgetId)?.cancel()
    }

    fun tearDown() {
        logger.i("Tearing down gifs...")

        gifPlaybackJobs.values.forEach { it.cancel() }
        gifPlaybackJobs.clear()
    }

    fun setPlaybackAllowed(value: Boolean, appWidgetId: Int? = null) {
        logger.d("Updating playback allowed (value=$value,appWidgetId=$appWidgetId)")
        if (appWidgetId == null) {
            coroutineScope.launch {
                playbackAllowed.update { currentMap ->
                    currentMap.mapValues { (key, _) ->
                        value && !photoWidgetStorage.getWidgetCyclePaused(appWidgetId = key)
                    }
                }
            }
        } else {
            playbackAllowed.update { currentMap ->
                currentMap.toMutableMap().apply { put(appWidgetId, value) }.toMap()
            }
        }
    }

    private fun getGifPlaybackJob(appWidgetId: Int, packageName: String): Job {
        return coroutineScope.launch {
            val gifFlow: Flow<Pair<List<Uri>, Long>> = loadPhotoWidgetUseCase(appWidgetId = appWidgetId)
                .filterNot { it.isLoading }
                .take(1)
                .map { photoWidget ->
                    photoWidget.photos.mapNotNull { it.launcherUri } to photoWidget.gifInterval
                }
            val playbackFlow: Flow<Boolean> = playbackAllowed.map { dict ->
                dict.getOrDefault(appWidgetId, false)
            }

            combine(flow = gifFlow, flow2 = playbackFlow) { gif: Pair<List<Uri>, Long>, shouldPlay: Boolean ->
                Triple(gif.first, gif.second, shouldPlay)
            }.collectLatest { (frames: List<Uri>, interval: Long, shouldPlay: Boolean) ->
                while (shouldPlay && frames.isNotEmpty()) {
                    frames.forEach { frame: Uri ->
                        val remoteViews = RemoteViews(packageName, R.layout.photo_widget)
                        remoteViews.setImageViewUri(R.id.iv_widget, frame)

                        widgetManager.partiallyUpdateAppWidget(appWidgetId, remoteViews)

                        delay(timeMillis = interval.coerceAtLeast(GifFrames.MIN_INTERVAL_MS))
                    }
                }
            }
        }
    }

    private companion object {

        private val logger: Timber.Tree = Timber.tag("GifPlaybackController")
    }
}
