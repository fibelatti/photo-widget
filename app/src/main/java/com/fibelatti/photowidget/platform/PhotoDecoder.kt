package com.fibelatti.photowidget.platform

import android.content.Context
import android.graphics.Bitmap
import coil3.ImageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.transformations
import coil3.toBitmap
import coil3.transform.Transformation
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import timber.log.Timber

class PhotoDecoder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageLoader: ImageLoader,
) {

    /**
     * Decodes [data] into a [Bitmap].
     *
     * [version] identifies the content behind [data] and must be provided whenever [data] can be
     * modified in place (e.g. a directory photo overwritten by another app). The image loader
     * enables `addLastModifiedToFileCacheKey`, but that only covers `file://` URIs: `content://`
     * ones fall back to Coil's default _keyer_, which is the URI string alone, so a changed file
     * would keep hitting the cached bitmap of its previous content.
     */
    suspend fun decode(
        data: Any?,
        maxDimension: Int? = null,
        transformations: List<Transformation> = emptyList(),
        version: Long? = null,
    ): Bitmap? {
        Timber.d(
            "Decoding %s into a bitmap (maxDimension=%s, transformations=%d, version=%s)",
            data,
            maxDimension,
            transformations.size,
            version,
        )

        val cacheKey: String = buildCacheKey(
            data = data,
            maxDimension = maxDimension,
            transformations = transformations,
            version = version,
        )
        val request: ImageRequest = ImageRequest.Builder(context)
            .data(if (data.toString().contains(context.packageName)) File(data.toString()) else data)
            .apply {
                if (maxDimension != null && maxDimension > 0) size(maxDimension)
                if (transformations.isNotEmpty()) transformations(transformations)
                if (version != null) memoryCacheKeyExtras(mapOf(EXTRA_VERSION to version.toString()))
            }
            .diskCacheKey(cacheKey)
            .build()

        return imageLoader.execute(request)
            .also { result ->
                when (result) {
                    is ErrorResult -> Timber.e(result.throwable, "Decoding error (data=%s)", data)
                    is SuccessResult -> Timber.d("Decoding success (data=%s)", data)
                }
            }
            .image
            ?.toBitmap()
    }

    private fun buildCacheKey(
        data: Any?,
        maxDimension: Int?,
        transformations: List<Transformation>,
        version: Long?,
    ): String = buildString {
        append(data)
        if (maxDimension != null && maxDimension > 0) append("|size=$maxDimension")
        transformations.forEach { append("|t=${it.cacheKey}") }
        if (version != null) append("|v=$version")
    }

    private companion object {

        private const val EXTRA_VERSION: String = "version"
    }
}
