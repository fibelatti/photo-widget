package com.fibelatti.photowidget.configure

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface PhotoWidgetConfigureNav : NavKey {

    @Serializable
    data object Home : PhotoWidgetConfigureNav

    @Serializable
    data object TapActionPicker : PhotoWidgetConfigureNav
}
