package com.fibelatti.photowidget.platform

import android.content.Context
import android.content.ContextWrapper
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity

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

fun Context?.findActivity(): AppCompatActivity? {
    var currentContext = this
    while (currentContext != null) {
        if (currentContext is AppCompatActivity) return currentContext
        if (currentContext !is ContextWrapper) break
        currentContext = currentContext.baseContext
    }
    return null
}
