package com.fibelatti.photowidget.platform

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.util.DisplayMetrics
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import com.fibelatti.photowidget.model.InstalledApp
import com.google.android.material.color.DynamicColors
import kotlin.math.roundToInt
import kotlin.math.sqrt
import timber.log.Timber

/**
 * Returns the first [ContextWrapper] with type [T] found in the hierarchy if any, or null if none
 * could be found.
 */
inline fun <reified T> Context.findOwner(): T? {
    var innerContext = this
    while (innerContext is ContextWrapper) {
        if (innerContext is T) {
            return innerContext
        }
        innerContext = innerContext.baseContext
    }
    return null
}

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
    return DynamicColors.wrapContextIfAvailable(applicationContext, theme).getAttributeColor(attrId, default)
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

fun Context.isBackgroundRestricted(checkUnrestrictedBattery: Boolean = false): Boolean {
    val isBackgroundRestricted: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
        getSystemService<ActivityManager>()?.isBackgroundRestricted == true

    val manufacturer: String = Build.MANUFACTURER.lowercase()
    val restrictiveManufacturers: List<String> = listOf(
        "huawei",
        "xiaomi",
        "redmi",
        "oppo",
        "vivo",
        "realme",
        "blackview",
        "tecno",
        "infinix",
    )
    val isRestrictive: Boolean = isBackgroundRestricted && manufacturer in restrictiveManufacturers

    val checkBattery: Boolean = checkUnrestrictedBattery || manufacturer in listOf("samsung", "motorola")
    val isBatteryUsageRestricted: Boolean = if (checkBattery) {
        getSystemService<PowerManager>()?.isIgnoringBatteryOptimizations(packageName) != true
    } else {
        false
    }

    return isRestrictive || isBackgroundRestricted || isBatteryUsageRestricted
}

fun widgetPinningNotAvailable(): Boolean {
    val manufacturer = Build.MANUFACTURER.lowercase()
    val notAvailable = listOf(
        "oppo",
        "realme",
    )

    return manufacturer in notAvailable
}

/**
 * Approximate memory budget (in bytes) for the bitmaps carried by a single `RemoteViews` update.
 * Mirrors the host's per-widget bitmap limit, derived from the display size.
 */
fun Context.getMaxRemoteViewsBitmapMemory(): Long {
    val displayMetrics: DisplayMetrics = resources.displayMetrics
    return (displayMetrics.heightPixels.toLong() * displayMetrics.widthPixels * 4 * 1.5).toLong()
}

fun Context.getMaxBitmapWidgetDimension(coerceMaxMemory: Boolean = false): Int {
    Timber.d("Calculating max dimension %s", mapOf("coerceMaxMemory" to coerceMaxMemory))

    val displayMetrics: DisplayMetrics = resources.displayMetrics
    val maxMemoryAllowed: Int = if (coerceMaxMemory) {
        6_912_000 // `RemoteViews` have a maximum allowed memory for bitmaps
    } else {
        getMaxRemoteViewsBitmapMemory().toInt()
    }
    val maxDimension: Int = sqrt(maxMemoryAllowed / 4 / displayMetrics.density).roundToInt()

    Timber.d("Max dimension allowed: $maxDimension %s", mapOf("maxMemoryAllowed" to maxMemoryAllowed))

    return maxDimension
}

fun Context.getAllInstalledApps(queryIntent: Intent): List<InstalledApp> {
    return packageManager.queryIntentActivities(queryIntent, 0)
        .distinctBy { it.activityInfo.packageName }
        .mapNotNull { resolveInfo ->
            runCatching {
                InstalledApp(
                    appPackage = resolveInfo.activityInfo.packageName,
                    appIcon = resolveInfo.loadIcon(packageManager),
                    appLabel = resolveInfo.loadLabel(packageManager).toString(),
                )
            }.getOrNull()
        }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.appLabel })
}

fun Context.getInstalledApp(packageName: String?): InstalledApp? {
    if (packageName == null) return null

    return runCatching {
        val appInfo: ApplicationInfo = packageManager.getApplicationInfo(
            /* packageName = */ packageName,
            /* flags = */ PackageManager.MATCH_DEFAULT_ONLY,
        )

        InstalledApp(
            appPackage = packageName,
            appIcon = packageManager.getApplicationIcon(appInfo),
            appLabel = packageManager.getApplicationLabel(appInfo).toString(),
        )
    }.getOrNull()
}

fun Context.getLaunchIntent(packageName: String?): Intent? {
    if (packageName == null) return null

    var launchIntent: Intent? = packageManager.getLaunchIntentForPackage(/* packageName = */ packageName)

    if (launchIntent == null) {
        val queryIntent: Intent = Intent(Intent.ACTION_MAIN)
            .setPackage(packageName)
        val activity: ActivityInfo? = packageManager.queryIntentActivities(queryIntent, 0).firstOrNull()?.activityInfo

        if (activity != null) {
            launchIntent = Intent(Intent.ACTION_MAIN)
                .setComponent(ComponentName(activity.applicationInfo.packageName, activity.name))
        }
    }

    return launchIntent
}
