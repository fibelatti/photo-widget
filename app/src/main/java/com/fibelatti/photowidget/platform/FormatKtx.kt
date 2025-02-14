package com.fibelatti.photowidget.platform

import java.text.NumberFormat
import java.util.Locale

fun formatPercent(
    value: Float,
    fractionDigits: Int = 2,
    locale: Locale = Locale.getDefault(),
): String {
    val numberFormat = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = fractionDigits
        maximumFractionDigits = fractionDigits
    }
    return "${numberFormat.format(value)} %"
}
