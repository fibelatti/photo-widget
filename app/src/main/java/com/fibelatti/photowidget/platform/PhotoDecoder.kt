package com.fibelatti.photowidget.platform

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
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
        data: Any?,
        maxDimension: Int,
    ): Bitmap? = withContext(Dispatchers.IO) {
        Timber.d("Decoding $data into a bitmap (maxDimension=$maxDimension)")

        val request = ImageRequest.Builder(context)
            .data(data)
            .allowHardware(enable = false)
            .size(maxDimension)
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
