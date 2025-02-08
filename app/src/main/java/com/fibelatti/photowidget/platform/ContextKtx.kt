package com.fibelatti.photowidget.platform

import android.content.Context
import android.content.ContextWrapper
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.color.DynamicColors

@ColorInt
fun Context.getAttributeColor(
    @AttrRes attrId: Int,
    @ColorInt default: Int = -1,
): Int {
    val resolved = obtainStyledAttributes(intArrayOf(attrId))
    val color = resolved.getColor(0, default)
    resolved.recycle()
    return color
}

@ColorInt
fun Context.getDynamicAttributeColor(
    @AttrRes attrId: Int,
    @ColorInt default: Int = -1,
    @StyleRes theme: Int = com.google.android.material.R.style.ThemeOverlay_Material3_DynamicColors_DayNight,
): Int {
    return DynamicColors.wrapContextIfAvailable(this, theme).getAttributeColor(attrId, default)
}

fun Context?.findActivity(): AppCompatActivity? {
    var currentContext = this
    while (currentContext != null) {
        if (currentContext is AppCompatActivity) return currentContext
        if (currentContext !is ContextWrapper) break
        currentContext = currentContext.baseContext
    }
    return null
}
