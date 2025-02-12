package com.fibelatti.photowidget.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

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

fun appSettingsIntent(context: Context): Intent {
    return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
}

fun batteryUsageSettingsIntent(): Intent {
    return Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
}
