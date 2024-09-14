package com.fibelatti.photowidget.model

import android.os.Parcelable
import androidx.annotation.StringRes
import com.fibelatti.photowidget.R
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

sealed interface PhotoWidgetTapAction : Parcelable {

    @get:StringRes
    val label: Int

    val serializedName: String

    @Parcelize
    data object None : PhotoWidgetTapAction {

        @IgnoredOnParcel
        override val label = R.string.photo_widget_configure_tap_action_none

        @IgnoredOnParcel
        override val serializedName: String = "NONE"
    }

    @Parcelize
    data class ViewFullScreen(
        val increaseBrightness: Boolean = false,
        val viewOriginalPhoto: Boolean = false,
    ) : PhotoWidgetTapAction {

        @IgnoredOnParcel
        override val label = R.string.photo_widget_configure_tap_action_view_full_screen

        @IgnoredOnParcel
        override val serializedName: String = "VIEW_FULL_SCREEN"
    }

    @Parcelize
    data object ViewInGallery : PhotoWidgetTapAction {

        @IgnoredOnParcel
        override val label = R.string.photo_widget_configure_tap_action_view_in_gallery

        @IgnoredOnParcel
        override val serializedName: String = "VIEW_IN_GALLERY"
    }

    @Parcelize
    data object ViewNextPhoto : PhotoWidgetTapAction {

        @IgnoredOnParcel
        override val label = R.string.photo_widget_configure_tap_action_flip_photo

        @IgnoredOnParcel
        override val serializedName: String = "VIEW_NEXT_PHOTO"
    }

    @Parcelize
    data class AppShortcut(
        val appShortcut: String? = null,
    ) : PhotoWidgetTapAction {

        @IgnoredOnParcel
        override val label = R.string.photo_widget_configure_tap_action_app_shortcut

        @IgnoredOnParcel
        override val serializedName: String = "APP_SHORTCUT"
    }

    companion object {

        val DEFAULT: PhotoWidgetTapAction = None

        val entries: List<PhotoWidgetTapAction> by lazy {
            listOf(
                None,
                ViewFullScreen(),
                ViewInGallery,
                ViewNextPhoto,
                AppShortcut(),
            )
        }

        fun fromSerializedName(serializedName: String): PhotoWidgetTapAction {
            return entries.firstOrNull { it.serializedName == serializedName } ?: DEFAULT
        }
    }
}
