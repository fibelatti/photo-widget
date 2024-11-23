package com.fibelatti.photowidget.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

fun Intent.setIdentifierCompat(value: String?): Intent = apply {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        setIdentifier(value)
    }
}

fun requestScheduleExactAlarmIntent(context: Context): Intent {
    return Intent("android.settings.REQUEST_SCHEDULE_EXACT_ALARM").apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
}
