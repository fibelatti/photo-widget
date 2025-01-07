package com.fibelatti.photowidget.widget

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.DisplayMetrics
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.platform.PhotoDecoder
import com.fibelatti.photowidget.platform.withPolygonalShape
import com.fibelatti.photowidget.platform.withRoundedCorners
import com.fibelatti.photowidget.widget.data.PhotoWidgetInternalFileStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.math.sqrt
import timber.log.Timber

class PrepareCurrentPhotoUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val decoder: PhotoDecoder,
    private val photoWidgetInternalFileStorage: PhotoWidgetInternalFileStorage,
) {

    suspend operator fun invoke(
        appWidgetId: Int,
        photoWidget: PhotoWidget,
        recoveryMode: Boolean = false,
    ): Result? {
        val currentPhotoPath: String = photoWidget.currentPhoto?.getPhotoPath() ?: return null

        val bitmap: Bitmap = try {
            val displayMetrics: DisplayMetrics = context.resources.displayMetrics
            val maxMemoryAllowed: Int = if (!recoveryMode) {
                (displayMetrics.heightPixels * displayMetrics.widthPixels * 4 * 1.5).roundToInt()
            } else {
                MAX_WIDGET_BITMAP_MEMORY
            }
            val maxMemoryDimension: Int = sqrt(maxMemoryAllowed / 4 / displayMetrics.density).roundToInt()
            val maxDimension: Int = if (PhotoWidgetAspectRatio.SQUARE != photoWidget.aspectRatio) {
                maxMemoryDimension
            } else {
                maxMemoryDimension.coerceAtMost(maximumValue = PhotoWidget.MAX_WIDGET_DIMENSION)
            }

            Timber.d(
                "Creating widget bitmap (" +
                    "maxMemoryAllowed=$maxMemoryAllowed," +
                    "maxDimension=$maxDimension," +
                    "recoveryMode=$recoveryMode" +
                    ")",
            )

            requireNotNull(decoder.decode(data = currentPhotoPath, maxDimension = maxDimension))
        } catch (_: Exception) {
            return null
        }

        Timber.d("Transforming the bitmap")
        val transformedBitmap: Bitmap = if (PhotoWidgetAspectRatio.SQUARE == photoWidget.aspectRatio) {
            bitmap.withPolygonalShape(
                shapeId = photoWidget.shapeId,
                opacity = photoWidget.opacity,
                borderColorHex = photoWidget.borderColor,
                borderWidth = photoWidget.borderWidth,
            )
        } else {
            bitmap.withRoundedCorners(
                aspectRatio = photoWidget.aspectRatio,
                radius = if (PhotoWidgetAspectRatio.FILL_WIDGET == photoWidget.aspectRatio) {
                    0F
                } else {
                    photoWidget.cornerRadius
                },
                opacity = photoWidget.opacity,
                borderColorHex = photoWidget.borderColor,
                borderWidth = photoWidget.borderWidth,
            )
        }

        val uri: Uri? = photoWidgetInternalFileStorage.prepareCurrentWidgetPhoto(
            appWidgetId = appWidgetId,
            currentPhoto = transformedBitmap,
        )

        return Result(uri = uri, fallback = transformedBitmap)
    }

    data class Result(
        val uri: Uri?,
        val fallback: Bitmap,
    )

    private companion object {

        // RemoteViews have a maximum allowed memory for bitmaps
        private const val MAX_WIDGET_BITMAP_MEMORY = 6_912_000
    }
}
