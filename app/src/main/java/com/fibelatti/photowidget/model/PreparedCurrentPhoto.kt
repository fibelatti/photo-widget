package com.fibelatti.photowidget.model

import android.graphics.Bitmap
import android.net.Uri

/**
 * Resources ready to be displayed in a widget.
 *
 * @property bitmap in-memory [Bitmap] of the current widget photo.
 * @property uri content [Uri] for the file that [bitmap] was persisted to, if available. The [uri]
 * already has granted permissions for all required packages.
 * @property previousBitmap in-memory [Bitmap] of the previous widget photo, when available.
 */
data class PreparedCurrentPhoto(
    val bitmap: Bitmap,
    val uri: Uri? = null,
    val previousBitmap: Bitmap? = null,
)
