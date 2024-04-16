package com.fibelatti.photowidget.platform

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import com.fibelatti.photowidget.model.PhotoWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class PhotoDecoder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageLoader: ImageLoader,
) {

    suspend fun decode(
        source: Uri,
        maxDimension: Int = PhotoWidget.MAX_DIMENSION,
    ): Bitmap? = withContext(Dispatchers.IO) {
        Timber.d("Decoding $source into a bitmap")

        val request = ImageRequest.Builder(context)
            .data(source)
            .allowHardware(enable = false)
            .size(maxDimension)
            .build()

        imageLoader.execute(request).drawable?.toBitmap()
            ?.also { Timber.d("Decoded successfully") }
    }

    suspend fun decode(localPath: String): Bitmap? = withContext(Dispatchers.IO) {
        Timber.d("Decoding $localPath into a bitmap")

        val request = ImageRequest.Builder(context)
            .data(localPath)
            .allowHardware(enable = false)
            .build()

        imageLoader.execute(request).drawable?.toBitmap()
            ?.also { Timber.d("Decoded successfully") }
    }
}
