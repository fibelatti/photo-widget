package com.fibelatti.photowidget.widget

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.DisplayMetrics
import android.util.Size
import androidx.core.graphics.toColorInt
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetBorder
import com.fibelatti.photowidget.platform.PhotoDecoder
import com.fibelatti.photowidget.platform.getDynamicAttributeColor
import com.fibelatti.photowidget.platform.withPolygonalShape
import com.fibelatti.photowidget.platform.withRoundedCorners
import com.fibelatti.photowidget.widget.data.PhotoWidgetInternalFileStorage
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.math.sqrt
import timber.log.Timber

class PrepareCurrentPhotoUseCase @Inject constructor(
    private val decoder: PhotoDecoder,
    private val photoWidgetInternalFileStorage: PhotoWidgetInternalFileStorage,
) {

    suspend operator fun invoke(
        context: Context,
        appWidgetId: Int,
        photoWidget: PhotoWidget,
        widgetSize: Size? = null,
        recoveryMode: Boolean = false,
    ): Result? {
        val currentPhotoPath: String = photoWidget.currentPhoto?.getPhotoPath() ?: return null

        Timber.d(
            "Preparing current photo (" +
                "appWidgetId=$appWidgetId," +
                "widgetSize=$widgetSize," +
                "recoveryMode=$recoveryMode," +
                "currentPhotoPath=$currentPhotoPath" +
                ")",
        )

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

        val borderColor = when (photoWidget.border) {
            is PhotoWidgetBorder.None -> null
            is PhotoWidgetBorder.Color -> "#${photoWidget.border.colorHex}".toColorInt()
            is PhotoWidgetBorder.Dynamic -> context.getDynamicAttributeColor(
                com.google.android.material.R.attr.colorPrimaryInverse,
            )
        }
        val borderPercent = photoWidget.border.getBorderPercent()

        Timber.d("Transforming the bitmap")
        val transformedBitmap: Bitmap = if (PhotoWidgetAspectRatio.SQUARE == photoWidget.aspectRatio) {
            bitmap.withPolygonalShape(
                shapeId = photoWidget.shapeId,
                opacity = photoWidget.opacity,
                saturation = photoWidget.saturation,
                brightness = photoWidget.brightness,
                borderColor = borderColor,
                borderPercent = borderPercent,
            )
        } else {
            bitmap.withRoundedCorners(
                aspectRatio = photoWidget.aspectRatio,
                radius = photoWidget.cornerRadius * context.resources.displayMetrics.density,
                opacity = photoWidget.opacity,
                saturation = photoWidget.saturation,
                brightness = photoWidget.brightness,
                borderColor = borderColor,
                borderPercent = borderPercent,
                widgetSize = widgetSize,
            )
        }

        val uri: Uri? = if (recoveryMode) {
            photoWidgetInternalFileStorage.prepareCurrentWidgetPhoto(
                appWidgetId = appWidgetId,
                currentPhoto = transformedBitmap,
            )
        } else {
            null
        }

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
