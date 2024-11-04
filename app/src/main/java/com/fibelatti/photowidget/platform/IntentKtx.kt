package com.fibelatti.photowidget.platform

import android.content.Intent
import android.os.Build

fun Intent.setIdentifierCompat(value: String?): Intent = apply {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        setIdentifier(value)
    }
}
