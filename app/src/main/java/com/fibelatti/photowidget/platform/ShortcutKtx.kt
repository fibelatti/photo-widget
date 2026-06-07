package com.fibelatti.photowidget.platform

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.XmlResourceParser
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import com.fibelatti.photowidget.model.AppShortcutInfo
import org.xmlpull.v1.XmlPullParser
import timber.log.Timber

private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
private const val SHORTCUTS_METADATA = "android.app.shortcuts"

private object ShortcutAttributes {

    const val TAG = "shortcut"
    const val ID = "shortcutId"
    const val SHORT_LABEL = "shortcutShortLabel"
    const val LONG_LABEL = "shortcutLongLabel"
    const val ICON = "icon"
}

private object IntentAttributes {

    const val TAG = "intent"
    const val ACTION = "action"
    const val TARGET_PACKAGE = "targetPackage"
    const val TARGET_CLASS = "targetClass"
}

/**
 * Parses [packageName]'s manifest-declared shortcuts (`android.app.shortcuts` metadata) into
 * [AppShortcutInfo], resolving each shortcut's label and icon against the target app's resources.
 *
 * Returns an empty list if the package has no shortcuts metadata or parsing fails.
 */
fun Context.getAppShortcuts(packageName: String): List<AppShortcutInfo> {
    return runCatching {
        val appResources = packageManager.getResourcesForApplication(packageName)
        val shortcuts = mutableListOf<AppShortcutInfo>()

        withShortcutsMetadata(packageName = packageName) { parser, eventType ->
            if (eventType == XmlPullParser.START_TAG && parser.name == ShortcutAttributes.TAG) {
                val id: String = parser.getAttrString(ShortcutAttributes.ID) ?: return@withShortcutsMetadata
                val labelResId: Int? = parser.getAttrInt(ShortcutAttributes.SHORT_LABEL)
                    ?: parser.getAttrInt(ShortcutAttributes.LONG_LABEL)
                val label: String = labelResId?.let {
                    runCatching { appResources.getString(it) }.getOrNull()
                } ?: id
                val icon: Drawable? = parser.getAttrInt(ShortcutAttributes.ICON)?.let {
                    runCatching { ResourcesCompat.getDrawable(appResources, it, null) }.getOrNull()
                }

                shortcuts += AppShortcutInfo(id = id, label = label, icon = icon)
            }
        }

        shortcuts
    }.getOrElse { e ->
        Timber.e(e, "Failed to load shortcuts for $packageName")
        emptyList()
    }
}

/**
 * Loads shortcuts for several packages at once, parsing each package's manifest metadata only
 * once regardless of how many shortcuts from it end up being referenced.
 */
fun Context.getAppShortcutsByPackage(packageNames: Set<String>): Map<String, List<AppShortcutInfo>> {
    return packageNames.associateWith(::getAppShortcuts)
}

/**
 * Parses [packageName]'s manifest-declared shortcuts metadata for the `<shortcut>` matching
 * [shortcutId] and builds the [Intent] declared by its first `<intent>` entry.
 *
 * Returns `null` if the package has no shortcuts metadata, the shortcut isn't found, it declares
 * no intent, or parsing fails.
 */
fun Context.getAppShortcutIntent(packageName: String, shortcutId: String): Intent? {
    return runCatching {
        var inTargetShortcut = false
        var lastIntent: Intent? = null
        var depth = 0

        withShortcutsMetadata(packageName = packageName) { parser, eventType ->
            when {
                eventType == XmlPullParser.START_TAG && parser.name == ShortcutAttributes.TAG -> {
                    val id: String? = parser.getAttrString(ShortcutAttributes.ID)
                    inTargetShortcut = id == shortcutId
                    if (inTargetShortcut) depth = parser.depth
                }

                inTargetShortcut && eventType == XmlPullParser.START_TAG && parser.name == IntentAttributes.TAG -> {
                    val action: String = parser.getAttrString(IntentAttributes.ACTION) ?: Intent.ACTION_MAIN
                    val targetPackage: String = parser.getAttrString(IntentAttributes.TARGET_PACKAGE) ?: packageName
                    val targetClass: String? = parser.getAttrString(IntentAttributes.TARGET_CLASS)

                    val intent: Intent = Intent(action).apply {
                        setPackage(targetPackage)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        if (targetClass != null) {
                            setClassName(targetPackage, targetClass)
                        }
                    }
                    lastIntent = intent
                }

                inTargetShortcut && eventType == XmlPullParser.END_TAG &&
                    parser.name == ShortcutAttributes.TAG && parser.depth == depth -> {
                    if (lastIntent != null) return lastIntent
                    inTargetShortcut = false
                }
            }
        }

        null
    }.getOrElse { e ->
        Timber.e(e, "Failed to load shortcut intent for $packageName/$shortcutId")
        null
    }
}

private inline fun Context.withShortcutsMetadata(
    packageName: String,
    body: (parser: XmlResourceParser, eventType: Int) -> Unit,
) {
    val activities: List<ResolveInfo> = packageManager.queryIntentActivities(
        Intent(Intent.ACTION_MAIN).setPackage(packageName),
        PackageManager.GET_META_DATA,
    )

    for (resolveInfo: ResolveInfo in activities) {
        val parser: XmlResourceParser = resolveInfo.activityInfo
            .loadXmlMetaData(packageManager, SHORTCUTS_METADATA) ?: continue

        parser.use {
            var eventType: Int = parser.eventType

            while (eventType != XmlPullParser.END_DOCUMENT) {
                body(parser, eventType)
                eventType = parser.next()
            }
        }
    }
}

private fun XmlPullParser.getAttrString(name: String): String? {
    return getAttributeValue(ANDROID_NS, name)?.takeIf { it.isNotEmpty() }
        ?: getAttributeValue(null, name)?.takeIf { it.isNotEmpty() }
}

private fun XmlPullParser.getAttrInt(name: String): Int? {
    val raw: String = getAttrString(name) ?: return null
    return if (raw.startsWith("@")) raw.substring(startIndex = 1).toIntOrNull() else raw.toIntOrNull()
}
