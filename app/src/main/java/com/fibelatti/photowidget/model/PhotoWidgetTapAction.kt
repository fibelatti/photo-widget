package com.fibelatti.photowidget.model

import androidx.annotation.StringRes
import com.fibelatti.photowidget.R

enum class PhotoWidgetTapAction(
    @StringRes val title: Int,
) {

    VIEW_FULL_SCREEN(title = R.string.photo_widget_configure_tap_action_view_full_screen),
    VIEW_NEXT_PHOTO(title = R.string.photo_widget_configure_tap_action_next_photo),
    ;
}
