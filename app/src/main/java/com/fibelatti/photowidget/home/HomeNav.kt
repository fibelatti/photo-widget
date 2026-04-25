package com.fibelatti.photowidget.home

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface HomeNav : NavKey {

    @Serializable
    data object Home : HomeNav

    @Serializable
    data object WidgetDefaults : HomeNav

    @Serializable
    data object OssLicenses : HomeNav
}
