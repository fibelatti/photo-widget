package com.fibelatti.photowidget.model

import android.net.Uri
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
data class LocalPhoto(
    val photoId: String,
    val croppedPhotoPath: String? = null,
    val originalPhotoPath: String? = null,
    val externalUri: Uri? = null,
    val launcherUri: Uri? = null,
    val cropping: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
) : Parcelable

/**
 * Returns the path of the photo to be displayed to the user.
 *
 * If the photo has been cropped and [viewOriginalPhoto] is false, the cropped photo path is
 * returned. Otherwise, the original photo path is returned (`originalPhotoPath` and
 * `externalUri` are mutually exclusive and only one of them is expected to be not null).
 */
fun LocalPhoto.getPhotoPath(viewOriginalPhoto: Boolean = false): String? {
    return when {
        !croppedPhotoPath.isNullOrEmpty() && !viewOriginalPhoto -> croppedPhotoPath
        originalPhotoPath != null -> originalPhotoPath
        externalUri != null -> externalUri.toString()
        else -> null
    }
}

/**
 * Returns a value identifying the current content of the photo at [getPhotoPath], or null when the
 * path alone is enough to identify it.
 *
 * Only external photos need one as they are read from a `content://` document that another app can
 * overwrite in place, which leaves the URI and the image loader's cache key unchanged.
 * Internal files are already keyed by their last modified date by the loader itself, and their
 * [LocalPhoto.timestamp] is the moment they were listed rather than a property of the file, so it
 * must not be used as a version for them.
 */
fun LocalPhoto.getPhotoVersion(viewOriginalPhoto: Boolean = false): Long? {
    return timestamp.takeIf { getPhotoPath(viewOriginalPhoto = viewOriginalPhoto) == externalUri?.toString() }
}
