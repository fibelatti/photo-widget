package com.fibelatti.photowidget.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.toColorInt
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetBorder
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PreparedCurrentPhoto
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
import com.fibelatti.photowidget.widget.data.WidgetDirectoryDao
import javax.inject.Inject
import timber.log.Timber

class PrepareCurrentPhotoUseCase @Inject constructor(
    private val decoder: PhotoDecoder,
    private val photoWidgetInternalFileStorage: PhotoWidgetInternalFileStorage,
    private val widgetDirectoryDao: WidgetDirectoryDao,
) {

    suspend operator fun invoke(
        context: Context,
        appWidgetId: Int,
        photoWidget: PhotoWidget,
        recoveryMode: Boolean = false,
    ): PreparedCurrentPhoto? {
        val currentPhotoPath: String = photoWidget.currentPhoto?.getPhotoPath() ?: return null

        Timber.i(
            "Preparing current photo %s",
            mapOf(
                "appWidgetId" to appWidgetId,
                "recoveryMode" to recoveryMode,
                "currentPhotoPath" to currentPhotoPath,
            ),
        )

        val bitmap: Bitmap = try {
            val maxDimension = context.getMaxBitmapWidgetDimension(coerceMaxMemory = recoveryMode)

            Timber.d("Creating widget bitmap %s", mapOf("maxDimension" to maxDimension, "recoveryMode" to recoveryMode))

            requireNotNull(decoder.decode(data = currentPhotoPath, maxDimension = maxDimension))
        } catch (_: Exception) {
            return null
        }

        val borderColor = when (photoWidget.border) {
            is PhotoWidgetBorder.None -> null

            is PhotoWidgetBorder.Color -> "#${photoWidget.border.colorHex}".toColorInt()

            is PhotoWidgetBorder.Dynamic -> context.getDynamicAttributeColor(
                photoWidget.border.type.colorAttr,
            )

            is PhotoWidgetBorder.MatchPhoto -> getColorPalette(bitmap).colorForType(photoWidget.border.type)
        }
        val borderPercent = photoWidget.border.borderPercent()

        Timber.d("Transforming the bitmap")
        val transformedBitmap: Bitmap = if (photoWidget.aspectRatio == PhotoWidgetAspectRatio.SQUARE) {
            bitmap.withPolygonalShape(
                context = context,
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
            )
        }

        // Persist the photo to a file (and keep the previous one) for every static-photo widget: the
        // content URI lets the provider render large photos without hitting the RemoteViews Binder
        // transaction size limit, and the retained previous file is decoded for the crossfade. GIF
        // widgets keep their existing behavior and only persist in recovery mode.
        val shouldPersist: Boolean = photoWidget.source != PhotoWidgetSource.GIF || recoveryMode
        val directoryName: String? = widgetDirectoryDao.getDirectoryName(appWidgetId)

        return if (shouldPersist && directoryName != null) {
            photoWidgetInternalFileStorage.prepareCurrentWidgetPhoto(
                directoryName = directoryName,
                currentPhoto = transformedBitmap,
            )
        } else {
            if (shouldPersist) Timber.w("Unable to find the directory of widget with ID = $appWidgetId.")
            PreparedCurrentPhoto(uri = null, fallback = transformedBitmap, previousBitmap = null)
        }
    }
}
