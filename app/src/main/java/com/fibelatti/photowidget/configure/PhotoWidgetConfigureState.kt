package com.fibelatti.photowidget.configure

import android.net.Uri
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

data class PhotoWidgetConfigureState(
    val photoWidget: PhotoWidget = PhotoWidget(),
    val selectedPhoto: LocalPhoto? = null,
    val isProcessing: Boolean = true,
    val isProcessingPin: Boolean = true,
    val cropQueue: List<LocalPhoto> = emptyList(),
    val messages: List<Message> = emptyList(),
    val hasEdits: Boolean = false,
    val isDraft: Boolean = false,
    val isImportAvailable: Boolean = false,
) {

    sealed class Message {

        data object ImportFailed : Message()

        data object TooManyPhotos : Message()

        data class LaunchCrop(
            val source: Uri,
            val destination: Uri,
            val aspectRatio: PhotoWidgetAspectRatio,
        ) : Message()

        data object RequestPin : Message()

        data class AddWidget(val appWidgetId: Int) : Message()

        data object MissingPhotos : Message()

        data object MissingBackupData : Message()

        data object CancelWidget : Message()

        data object DraftSaved : Message()
    }
}

// region Ktx

operator fun PhotoWidgetConfigureState.plus(photo: LocalPhoto): PhotoWidgetConfigureState {
    return this + listOf(photo)
}

operator fun PhotoWidgetConfigureState.plus(photos: Collection<LocalPhoto>): PhotoWidgetConfigureState {
    val updatedPhotos: List<LocalPhoto> = photoWidget.photos + photos
    val newIds: Set<String> = photos.map { it.photoId }.toSet()

    return copy(
        photoWidget = photoWidget.copy(
            photos = updatedPhotos,
            currentPhoto = if (photoWidget.currentPhoto == null || updatedPhotos.size == 1) {
                updatedPhotos.firstOrNull()
            } else {
                photoWidget.currentPhoto
            },
            removedPhotos = photoWidget.removedPhotos.filterNot { it.photoId in newIds },
        ),
        selectedPhoto = if (selectedPhoto == null || updatedPhotos.size == 1) {
            updatedPhotos.firstOrNull()
        } else {
            selectedPhoto
        },
    )
}

operator fun PhotoWidgetConfigureState.plus(dir: Uri): PhotoWidgetConfigureState {
    return copy(
        photoWidget = photoWidget.copy(
            syncedDir = photoWidget.syncedDir + dir,
        ),
    )
}

operator fun PhotoWidgetConfigureState.plus(message: PhotoWidgetConfigureState.Message?): PhotoWidgetConfigureState {
    return copy(messages = (messages + message).filterNotNull())
}

operator fun PhotoWidgetConfigureState.minus(message: PhotoWidgetConfigureState.Message): PhotoWidgetConfigureState {
    return copy(messages = messages - message)
}

operator fun MutableStateFlow<PhotoWidgetConfigureState>.plusAssign(message: PhotoWidgetConfigureState.Message?) {
    update { current -> current + message }
}

operator fun MutableStateFlow<PhotoWidgetConfigureState>.minusAssign(message: PhotoWidgetConfigureState.Message) {
    update { current -> current - message }
}

// endregion Ktx
