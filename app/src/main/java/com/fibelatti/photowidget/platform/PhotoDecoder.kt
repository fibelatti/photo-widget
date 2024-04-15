package com.fibelatti.photowidget.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import com.fibelatti.photowidget.model.PhotoWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

class PhotoDecoder @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val contentResolver = context.contentResolver

    suspend fun decode(
        source: Uri,
        maxDimension: Int = PhotoWidget.MAX_DIMENSION,
    ): Bitmap? = withContext(Dispatchers.IO) {
        val output = contentResolver.openInputStream(source)
            ?.use { inputStream ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }

                BitmapFactory.decodeStream(inputStream, null, options)

                options
            }
            ?: return@withContext null // Exit early if the content can't be resolved

        val originalHeight = output.outHeight
        val originalWidth = output.outWidth

        contentResolver.openInputStream(source).use { inputStream ->
            val bitmapOptions = BitmapFactory.Options().apply {
                inDensity = max(originalWidth, originalHeight)
                inTargetDensity = min(maxDimension, inDensity)
            }
            val bitmap = BitmapFactory.decodeStream(inputStream, null, bitmapOptions)
            val orientation = getOrientation(uri = source)

            if (bitmap != null && orientation != null) {
                runCatching {
                    val matrix = Matrix().apply { setRotate(orientation.toFloat()) }
                    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

                    return@withContext rotated
                }
            }

            return@withContext bitmap
        }
    }

    private fun getOrientation(uri: Uri): Int? {
        return try {
            contentResolver.query(
                /* uri = */ uri,
                /* projection = */ arrayOf(MediaStore.Images.ImageColumns.ORIENTATION),
                /* selection = */ null,
                /* selectionArgs = */ null,
                /* sortOrder = */ null,
            )?.use { cursor ->
                cursor.takeIf { it.count == 1 }?.run {
                    moveToFirst()
                    getInt(0)
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}
