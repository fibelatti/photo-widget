package com.fibelatti.photowidget.platform

import android.content.Context
import android.graphics.Bitmap
import coil3.ImageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
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
            .data(if (data.toString().contains(context.packageName)) File(data.toString()) else data)
            .apply { if (maxDimension != null && maxDimension > 0) size(maxDimension) }
            .build()

        imageLoader.execute(request)
            .also { result ->
                when (result) {
                    is ErrorResult -> Timber.d("Decoding error (data=$data, message=${result.throwable.message})")
                    is SuccessResult -> Timber.d("Decoding success (data=$data)")
                }
            }
            .image
            ?.toBitmap()
    }
}
