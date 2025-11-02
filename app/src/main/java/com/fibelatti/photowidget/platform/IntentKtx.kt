package com.fibelatti.photowidget.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

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

fun sharePhotoChooserIntent(
    context: Context,
    originalPhotoPath: String?,
    externalUri: Uri?,
): Intent? {
    val uri: Uri
    when {
        originalPhotoPath != null -> {
            uri = FileProvider.getUriForFile(
                /* context = */ context,
                /* authority = */ "${context.packageName}.fileprovider",
                /* file = */ File(originalPhotoPath),
            )
        }

        externalUri != null -> {
            uri = externalUri
        }

        else -> {
            return null
        }
    }

    val shareIntent: Intent = Intent(Intent.ACTION_SEND)
        .putExtra(Intent.EXTRA_STREAM, uri)
        .setType("image/*")

    return Intent.createChooser(shareIntent, null)
}
