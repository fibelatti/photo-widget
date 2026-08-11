package com.fibelatti.photowidget.model

import android.net.Uri
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

/**
 * A directory synced by a [PhotoWidgetSource.DIRECTORY] widget.
 */
@Parcelize
@Immutable
data class SyncDir(
    val dir: Uri,
    val subdirectories: Boolean = true,
) : Parcelable
