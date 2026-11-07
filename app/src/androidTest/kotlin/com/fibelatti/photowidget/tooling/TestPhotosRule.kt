package com.fibelatti.photowidget.tooling

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.provider.MediaStore
import org.junit.rules.ExternalResource

/**
 * Creates photos in `MediaStore`, where a real photo picker would find them, and deletes them once
 * the test finishes. Inserting without a storage permission requires API 29.
 *
 * Photos served by the app's own `FileProvider` can't be used, because `PhotoDecoder` treats any URI
 * containing the package name as a file path.
 */
class TestPhotosRule : ExternalResource() {

    private val photos: MutableList<Uri> = mutableListOf()

    fun insert(): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "photo-widget-${System.nanoTime()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        }
        val uri: Uri = requireNotNull(
            targetContext.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values),
        )

        photos += uri

        val bitmap: Bitmap = Bitmap.createBitmap(1200, 800, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.MAGENTA)
        }

        requireNotNull(targetContext.contentResolver.openOutputStream(uri)).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }

        return uri
    }

    override fun after() {
        photos.forEach { uri: Uri -> targetContext.contentResolver.delete(uri, null, null) }
        photos.clear()
    }
}
