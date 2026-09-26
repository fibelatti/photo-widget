package com.fibelatti.photowidget.model

import androidx.annotation.AttrRes
import androidx.annotation.StringRes
import com.fibelatti.photowidget.R

enum class DynamicColorType(@AttrRes val colorAttr: Int, @StringRes val label: Int) {
    PRIMARY_INVERSE(
        colorAttr = com.google.android.material.R.attr.colorPrimaryInverse,
        label = R.string.photo_widget_configure_color_dynamic_inverse,
    ),
    PRIMARY(
        colorAttr = androidx.appcompat.R.attr.colorPrimary,
        label = R.string.photo_widget_configure_color_dynamic_primary,
    ),
    PRIMARY_FIXED(
        colorAttr = com.google.android.material.R.attr.colorPrimaryFixed,
        label = R.string.photo_widget_configure_color_dynamic_primary_fixed,
    ),
    SECONDARY(
        colorAttr = com.google.android.material.R.attr.colorSecondary,
        label = R.string.photo_widget_configure_color_dynamic_secondary,
    ),
    SECONDARY_FIXED(
        colorAttr = com.google.android.material.R.attr.colorSecondaryFixed,
        label = R.string.photo_widget_configure_color_dynamic_secondary_fixed,
    ),
}
