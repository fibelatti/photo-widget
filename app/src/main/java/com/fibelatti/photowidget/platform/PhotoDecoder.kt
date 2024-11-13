package com.fibelatti.photowidget.platform

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class PhotoDecoder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageLoader: ImageLoader,
) {

    suspend fun decode(
        data: Any?,
        maxDimension: Int? = null,
    ): Bitmap? = withContext(Dispatchers.IO) {
        Timber.d("Decoding $data into a bitmap (maxDimension=$maxDimension)")

        val request = ImageRequest.Builder(context)
            .data(data)
            .allowHardware(enable = false)
            .apply { if (maxDimension != null) size(maxDimension) }
            .build()

        imageLoader.execute(request)
            .also { result ->
                when (result) {
                    is ErrorResult -> Timber.d("Decoding error ${result.throwable.message}")
                    is SuccessResult -> Timber.d("Decoding success")
                }
            }
            .drawable?.toBitmap()
    }
}
