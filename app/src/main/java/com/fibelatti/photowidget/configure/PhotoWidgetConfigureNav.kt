package com.fibelatti.photowidget.configure

import androidx.navigation3.runtime.NavKey
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import kotlinx.serialization.Serializable

sealed interface PhotoWidgetConfigureNav : NavKey {

    @Serializable
    data object Home : PhotoWidgetConfigureNav

    @Serializable
    data object TapActionPicker : PhotoWidgetConfigureNav

    @Serializable
    data class PhotoCrop(
        val sourceUri: String,
        val destinationUri: String,
        val aspectRatio: PhotoWidgetAspectRatio,
    ) : PhotoWidgetConfigureNav
}
