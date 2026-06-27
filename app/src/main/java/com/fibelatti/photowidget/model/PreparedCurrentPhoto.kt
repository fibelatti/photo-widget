package com.fibelatti.photowidget.model

import android.graphics.Bitmap
import android.net.Uri

data class PreparedCurrentPhoto(
    val uri: Uri?,
    val fallback: Bitmap,
    val previousBitmap: Bitmap?,
)
