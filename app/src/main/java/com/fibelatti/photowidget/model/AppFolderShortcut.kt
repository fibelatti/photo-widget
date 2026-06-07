package com.fibelatti.photowidget.model

import com.fibelatti.photowidget.model.PhotoWidgetTapAction.AppFolder

/**
 * Encodes an [AppFolder] entry as either `"packageName"` or `"packageName:shortcutId"`.
 *
 * Android package names cannot contain `:`, making it a safe, unambiguous separator that
 * lets [AppFolder.shortcuts] keep persisting as a flat, comma-joined list of strings.
 */
data class AppFolderShortcut(
    val packageName: String,
    val shortcutId: String? = null,
) {

    /**
     * [AppFolder.shortcuts] is persisted as a comma-joined string (see
     * [com.fibelatti.photowidget.widget.data.PhotoWidgetSharedPreferences]), so a [shortcutId]
     * containing a comma would corrupt that list by splitting a single entry into two on the next
     * load. Shortcut ids come from the target app's manifest and aren't expected to contain one,
     * but if they do, falling back to a plain app launch is preferable to corrupting the list.
     */
    fun encoded(): String {
        val safeShortcutId: String? = shortcutId?.takeUnless { ',' in it }
        return if (safeShortcutId != null) "$packageName:$safeShortcutId" else packageName
    }

    companion object {

        fun from(encoded: String): AppFolderShortcut {
            val separatorIndex = encoded.indexOf(':')
            return if (separatorIndex >= 0) {
                AppFolderShortcut(
                    packageName = encoded.substring(0, separatorIndex),
                    shortcutId = encoded.substring(separatorIndex + 1),
                )
            } else {
                AppFolderShortcut(packageName = encoded)
            }
        }
    }
}
