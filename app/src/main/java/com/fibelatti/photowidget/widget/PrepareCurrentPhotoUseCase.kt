package com.fibelatti.photowidget.widget

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Size
import androidx.core.graphics.toColorInt
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetBorder
import com.fibelatti.photowidget.model.borderPercent
import com.fibelatti.photowidget.model.getPhotoPath
import com.fibelatti.photowidget.platform.PhotoDecoder
import com.fibelatti.photowidget.platform.colorForType
import com.fibelatti.photowidget.platform.getColorPalette
import com.fibelatti.photowidget.platform.getDynamicAttributeColor
import com.fibelatti.photowidget.platform.getMaxBitmapWidgetDimension
import com.fibelatti.photowidget.platform.withPolygonalShape
import com.fibelatti.photowidget.platform.withRoundedCorners
import com.fibelatti.photowidget.widget.data.PhotoWidgetInternalFileStorage
import javax.inject.Inject
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
            val maxDimension = context.getMaxBitmapWidgetDimension(
                coerceMaxMemory = recoveryMode,
                coerceDimension = PhotoWidgetAspectRatio.SQUARE == photoWidget.aspectRatio,
            )

            Timber.d("Creating widget bitmap (maxDimension=$maxDimension, recoveryMode=$recoveryMode)")

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

            is PhotoWidgetBorder.MatchPhoto -> getColorPalette(bitmap).colorForType(photoWidget.border.type)
        }
        val borderPercent = photoWidget.border.borderPercent()

        Timber.d("Transforming the bitmap")
        val transformedBitmap: Bitmap = if (PhotoWidgetAspectRatio.SQUARE == photoWidget.aspectRatio) {
            bitmap.withPolygonalShape(
                shapeId = photoWidget.shapeId,
                colors = photoWidget.colors,
                borderColor = borderColor,
                borderPercent = borderPercent,
            )
        } else {
            bitmap.withRoundedCorners(
                radius = photoWidget.cornerRadius * context.resources.displayMetrics.density,
                aspectRatio = photoWidget.aspectRatio,
                colors = photoWidget.colors,
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
}
