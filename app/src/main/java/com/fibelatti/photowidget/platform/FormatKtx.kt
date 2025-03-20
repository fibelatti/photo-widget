package com.fibelatti.photowidget.platform

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

fun formatPercent(
    value: Float,
    fractionDigits: Int = 2,
    locale: Locale = Locale.getDefault(),
): String {
    val numberFormat = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = fractionDigits
        maximumFractionDigits = fractionDigits
    }

    return "${numberFormat.format(value)}%"
}

fun formatRangeValue(value: Float): String {
    val rounded = value.roundToInt()

    return when {
        rounded > 0 -> "+$rounded"
        rounded < 0 -> "$rounded"
        else -> "—"
    }
}
