package com.fibelatti.photowidget.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PhotoDecoder @Inject constructor(
    @ApplicationContext context: Context,
    private val coroutineScope: CoroutineScope,
) {

    private val contentResolver = context.contentResolver

    suspend fun decode(
        source: Uri,
        maxDimension: Int = 1_000,
    ): Bitmap? = withContext(coroutineScope.coroutineContext) {
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
                if (originalWidth > maxDimension || originalHeight > maxDimension) {
                    inTargetDensity = maxDimension
                    inDensity = if (originalWidth > originalHeight) originalWidth else originalHeight
                }
            }

            return@withContext BitmapFactory.decodeStream(inputStream, null, bitmapOptions)
        }
    }
}
