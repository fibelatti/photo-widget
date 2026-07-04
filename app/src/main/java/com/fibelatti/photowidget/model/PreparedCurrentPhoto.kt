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
 * @property fadeBitmap in-memory, downscaled version of [bitmap] used for crossfade.
 * @property pendingWrite a write operation that has NOT run yet, present only on the crossfade
 * path. The fade renders from the in-memory bitmaps as an optimization to avoid reading files.
 * The caller may run this concurrently with the fade instead of having to wait for it beforehand.
 */
data class PreparedCurrentPhoto(
    val bitmap: Bitmap,
    val uri: Uri? = null,
    val previousBitmap: Bitmap? = null,
    val fadeBitmap: Bitmap? = null,
    val pendingWrite: (suspend () -> Unit)? = null,
)
