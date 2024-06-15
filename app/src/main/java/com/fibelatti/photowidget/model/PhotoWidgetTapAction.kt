package com.fibelatti.photowidget.model

import androidx.annotation.StringRes
import com.fibelatti.photowidget.R

enum class PhotoWidgetTapAction(
    @StringRes val label: Int,
) {

    NONE(label = R.string.photo_widget_configure_tap_action_none),
    VIEW_FULL_SCREEN(label = R.string.photo_widget_configure_tap_action_view_full_screen),
    VIEW_IN_GALLERY(label = R.string.photo_widget_configure_tap_action_view_in_gallery),
    VIEW_NEXT_PHOTO(label = R.string.photo_widget_configure_tap_action_flip_photo),
    APP_SHORTCUT(label = R.string.photo_widget_configure_tap_action_app_shortcut),
    ;
}
