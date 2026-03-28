package com.fibelatti.photowidget.platform

import android.annotation.SuppressLint
import android.app.WallpaperManager
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

@SuppressLint("BatteryLife")
fun disableBatteryOptimizationIntent(context: Context): Intent {
    return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        setData(Uri.fromParts("package", context.packageName, null))
    }
}

fun sharePhotoChooserIntent(context: Context, originalPhotoPath: String?, externalUri: Uri?): Intent? {
    val uri: Uri = getPhotoUri(
        context = context,
        originalPhotoPath = originalPhotoPath,
        externalUri = externalUri,
    ) ?: return null

    val shareIntent: Intent = Intent(Intent.ACTION_SEND)
        .putExtra(Intent.EXTRA_STREAM, uri)
        .setType("image/*")

    return Intent.createChooser(shareIntent, null)
}

fun setWallpaperIntent(context: Context, originalPhotoPath: String?, externalUri: Uri?): Intent? {
    val uri: Uri = getPhotoUri(
        context = context,
        originalPhotoPath = originalPhotoPath,
        externalUri = externalUri,
    ) ?: return null

    return Intent(WallpaperManager.ACTION_CROP_AND_SET_WALLPAPER)
        .setDataAndType(uri, "image/*")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}

private fun getPhotoUri(context: Context, originalPhotoPath: String?, externalUri: Uri?): Uri? {
    return when {
        originalPhotoPath != null -> {
            FileProvider.getUriForFile(
                /* context = */ context,
                /* authority = */ "${context.packageName}.fileprovider",
                /* file = */ File(originalPhotoPath),
            )
        }

        externalUri != null -> externalUri

        else -> null
    }
}
