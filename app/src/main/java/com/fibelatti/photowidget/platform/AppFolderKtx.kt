package com.fibelatti.photowidget.platform

import android.content.Context
import com.fibelatti.photowidget.model.AppFolderResolvedEntry
import com.fibelatti.photowidget.model.AppFolderShortcut
import com.fibelatti.photowidget.model.AppShortcutInfo
import com.fibelatti.photowidget.model.InstalledApp

/**
 * Decodes [AppFolder][com.fibelatti.photowidget.model.PhotoWidgetTapAction.AppFolder] entries and
 * resolves them against installed apps and shortcuts, dropping entries whose app is no longer
 * installed.
 *
 * Shortcut metadata is loaded once per distinct package (rather than once per entry), since
 * [getAppShortcuts] parses the whole app manifest in a single pass regardless of how many of its
 * shortcuts end up being referenced.
 */
fun Context.resolveAppFolderEntries(shortcuts: List<String>): List<AppFolderResolvedEntry> {
    val decoded: List<AppFolderShortcut> = shortcuts.map(AppFolderShortcut::from)

    val packagesWithShortcuts: Set<String> = decoded.mapNotNullTo(mutableSetOf()) { entry ->
        entry.packageName.takeIf { entry.shortcutId != null }
    }
    val shortcutsByPackage: Map<String, List<AppShortcutInfo>> = if (packagesWithShortcuts.isEmpty()) {
        emptyMap()
    } else {
        getAppShortcutsByPackage(packagesWithShortcuts)
    }

    return decoded.mapNotNull { entry ->
        val app: InstalledApp = getInstalledApp(entry.packageName) ?: return@mapNotNull null
        val shortcut: AppShortcutInfo? = entry.shortcutId?.let { shortcutId ->
            shortcutsByPackage[entry.packageName]?.find { it.id == shortcutId }
        }

        AppFolderResolvedEntry(
            encoded = entry.encoded(),
            app = app,
            shortcut = shortcut,
        )
    }
}
