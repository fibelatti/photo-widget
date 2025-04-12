package com.fibelatti.photowidget.chooser

import android.appwidget.AppWidgetManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.platform.savedState
import com.fibelatti.photowidget.widget.LoadPhotoWidgetUseCase
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber

@HiltViewModel
class PhotoWidgetChooserViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    loadPhotoWidgetUseCase: LoadPhotoWidgetUseCase,
    private val photoWidgetStorage: PhotoWidgetStorage,
) : ViewModel() {

    private val appWidgetId: Int by savedStateHandle.savedState(
        key = AppWidgetManager.EXTRA_APPWIDGET_ID,
        default = AppWidgetManager.INVALID_APPWIDGET_ID,
    )

    val state: StateFlow<PhotoWidget?> = loadPhotoWidgetUseCase(appWidgetId = appWidgetId)
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(), initialValue = null)

    suspend fun setPhoto(photo: LocalPhoto) {
        Timber.d("Updating current photo to ${photo.photoId}")
        photoWidgetStorage.clearDisplayedPhotos(appWidgetId = appWidgetId)
        photoWidgetStorage.saveDisplayedPhoto(appWidgetId = appWidgetId, photoId = photo.photoId)
    }
}
