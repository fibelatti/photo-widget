package com.fibelatti.photowidget.platform

import java.text.NumberFormat
import java.util.Locale

fun formatPercent(value: Float, locale: Locale = Locale.getDefault()): String {
    val numberFormat = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return "${numberFormat.format(value)} %"
}
