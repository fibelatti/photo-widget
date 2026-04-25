package com.fibelatti.photowidget.configure

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

sealed interface PhotoWidgetConfigureNav : NavKey {

    @Serializable
    data object Home : PhotoWidgetConfigureNav

    @Serializable
    data object TapActionPicker : PhotoWidgetConfigureNav

    @Serializable
    data class PhotoCrop(
        @Contextual val sourceUri: Uri,
        @Contextual val destinationUri: Uri,
        val aspectRatio: PhotoWidgetAspectRatio,
    ) : PhotoWidgetConfigureNav
}
