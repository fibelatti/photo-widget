package com.fibelatti.photowidget.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LocalPhoto(
    val photoId: String,
    val croppedPhotoPath: String? = null,
    val originalPhotoPath: String? = null,
    val externalUri: Uri? = null,
    val cropping: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
) : Parcelable {

    /**
     * Returns the path of the photo to be displayed to the user.
     *
     * If the photo has been cropped and [viewOriginalPhoto] is false, the cropped photo path is
     * returned. Otherwise, the original photo path is returned (`originalPhotoPath` and
     * `externalUri` are mutually exclusive and only one of them is expected to be not null).
     */
    fun getPhotoPath(viewOriginalPhoto: Boolean = false): String? {
        return when {
            !croppedPhotoPath.isNullOrEmpty() && !viewOriginalPhoto -> croppedPhotoPath
            originalPhotoPath != null -> originalPhotoPath
            externalUri != null -> externalUri.toString()
            else -> null
        }
    }
}
