package com.fibelatti.photowidget.configure

import android.net.Uri
import androidx.annotation.StringRes
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

data class PhotoWidgetConfigureState(
    val photoWidget: PhotoWidget = PhotoWidget(),
    val selectedPhoto: LocalPhoto? = null,
    val isProcessing: Boolean = true,
    val cropQueue: List<LocalPhoto> = emptyList(),
    val messages: List<Message> = emptyList(),
    val hasEdits: Boolean = false,
    val isDraft: Boolean = false,
    val isImportAvailable: Boolean = false,
) {

    sealed class Message {

        data class UserPrompt(
            @StringRes val textRes: Int,
            @StringRes val buttonRes: Int = R.string.photo_widget_action_got_it,
            val textFormatArgs: Array<out Any> = emptyArray(),
        ) : Message() {

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as UserPrompt

                if (textRes != other.textRes) return false
                if (buttonRes != other.buttonRes) return false
                if (!textFormatArgs.contentEquals(other.textFormatArgs)) return false

                return true
            }

            override fun hashCode(): Int {
                var result = textRes
                result = 31 * result + buttonRes
                result = 31 * result + textFormatArgs.contentHashCode()
                return result
            }
        }

        data class LaunchCrop(
            val source: Uri,
            val destination: Uri,
            val aspectRatio: PhotoWidgetAspectRatio,
        ) : Message()

        data class RequestPin(val transparent: Boolean) : Message()

        data class AddWidget(val appWidgetId: Int, val transparent: Boolean) : Message()

        data object CancelWidget : Message()

        data object DraftSaved : Message()

        data object KeepAliveRequired : Message()

        data object AdvancedScheduleCoerced : Message()
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
