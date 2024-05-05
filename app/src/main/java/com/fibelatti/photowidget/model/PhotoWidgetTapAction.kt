package com.fibelatti.photowidget.model

import androidx.annotation.StringRes
import com.fibelatti.photowidget.R

enum class PhotoWidgetTapAction(
    @StringRes val title: Int,
) {

    NONE(title = R.string.photo_widget_configure_tap_action_none),
    VIEW_FULL_SCREEN(title = R.string.photo_widget_configure_tap_action_view_full_screen),
    VIEW_NEXT_PHOTO(title = R.string.photo_widget_configure_tap_action_next_photo),
    APP_SHORTCUT(title = R.string.photo_widget_configure_tap_action_app_shortcut),
    ;
}
