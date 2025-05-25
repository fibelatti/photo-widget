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
        override val label: Int = R.string.photo_widget_configure_tap_action_none

        @IgnoredOnParcel
        override val serializedName: String = "NONE"
    }

    @Parcelize
    data class ViewFullScreen(
        val increaseBrightness: Boolean = false,
        val viewOriginalPhoto: Boolean = false,
        val noShuffle: Boolean = false,
        val keepCurrentPhoto: Boolean = false,
    ) : PhotoWidgetTapAction {

        @IgnoredOnParcel
        override val label: Int = R.string.photo_widget_configure_tap_action_view_full_screen

        @IgnoredOnParcel
        override val serializedName: String = "VIEW_FULL_SCREEN"
    }

    @Parcelize
    data class ViewInGallery(
        val galleryApp: String? = null,
    ) : PhotoWidgetTapAction {

        @IgnoredOnParcel
        override val label: Int = R.string.photo_widget_configure_tap_action_view_in_gallery

        @IgnoredOnParcel
        override val serializedName: String = "VIEW_IN_GALLERY"
    }

    @Parcelize
    data object ViewNextPhoto : PhotoWidgetTapAction {

        @IgnoredOnParcel
        override val label: Int = R.string.photo_widget_configure_tap_action_view_next_photo

        @IgnoredOnParcel
        override val serializedName: String = "VIEW_NEXT_PHOTO"
    }

    @Parcelize
    data object ViewPreviousPhoto : PhotoWidgetTapAction {

        @IgnoredOnParcel
        override val label: Int = R.string.photo_widget_configure_tap_action_view_previous_photo

        @IgnoredOnParcel
        override val serializedName: String = "VIEW_PREVIOUS_PHOTO"
    }

    @Parcelize
    data object ChooseNextPhoto : PhotoWidgetTapAction {

        @IgnoredOnParcel
        override val label: Int = R.string.photo_widget_configure_tap_action_choose_next_photo

        @IgnoredOnParcel
        override val serializedName: String = "CHOOSE_NEXT_PHOTO"
    }

    @Parcelize
    data class AppShortcut(
        val appShortcut: String? = null,
    ) : PhotoWidgetTapAction {

        @IgnoredOnParcel
        override val label: Int = R.string.photo_widget_configure_tap_action_app_shortcut

        @IgnoredOnParcel
        override val serializedName: String = "APP_SHORTCUT"
    }

    @Parcelize
    data class UrlShortcut(
        val url: String? = null,
    ) : PhotoWidgetTapAction {

        @IgnoredOnParcel
        override val label: Int = R.string.photo_widget_configure_tap_action_url_shortcut

        @IgnoredOnParcel
        override val serializedName: String = "URL_SHORTCUT"
    }

    @Parcelize
    data class ToggleCycling(
        val disableTap: Boolean = false,
    ) : PhotoWidgetTapAction {

        @IgnoredOnParcel
        override val label: Int = R.string.photo_widget_configure_tap_action_toggle_cycling

        @IgnoredOnParcel
        override val serializedName: String = "TOGGLE_CYCLING"
    }

    companion object {

        val DEFAULT: PhotoWidgetTapAction = None

        val entries: List<PhotoWidgetTapAction> by lazy {
            listOf(
                None,
                ViewFullScreen(),
                ViewInGallery(),
                ViewNextPhoto,
                ViewPreviousPhoto,
                ChooseNextPhoto,
                ToggleCycling(),
                AppShortcut(),
                UrlShortcut(),
            )
        }

        fun fromSerializedName(serializedName: String): PhotoWidgetTapAction {
            return entries.firstOrNull { it.serializedName == serializedName } ?: DEFAULT
        }
    }
}
