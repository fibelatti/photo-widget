package com.fibelatti.photowidget.model

import androidx.compose.runtime.Stable

/**
 * A decoded [AppFolder] entry resolved against the device's installed apps and shortcuts, ready
 * for display and launching.
 *
 * [encoded] preserves the original [AppFolder.shortcuts] string so callers can reorder/remove
 * entries by identity instead of by position (entries can be dropped during resolution, e.g. when
 * their app was uninstalled, which would otherwise desync display indices from storage indices).
 */
@Stable
data class AppFolderResolvedEntry(
    val encoded: String,
    val app: InstalledApp,
    val shortcut: AppShortcutInfo?,
)
