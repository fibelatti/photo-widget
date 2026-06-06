package com.fibelatti.photowidget.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.toColorInt
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetBorder
import com.fibelatti.photowidget.model.borderPercent
import com.fibelatti.photowidget.platform.PhotoDecoder
import com.fibelatti.photowidget.platform.colorForType
import com.fibelatti.photowidget.platform.getColorPalette
import com.fibelatti.photowidget.platform.getDynamicAttributeColor
import com.fibelatti.photowidget.platform.getMaxBitmapWidgetDimension
import com.fibelatti.photowidget.platform.runWithFileOutputStream
import com.fibelatti.photowidget.platform.withPolygonalShape
import com.fibelatti.photowidget.platform.withRoundedCorners
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.measureTime
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

class PrepareGifPhotosUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val decoder: PhotoDecoder,
) {

    suspend operator fun invoke(
        appWidgetId: Int,
        photoWidget: PhotoWidget,
    ) {
        Timber.i("Preparing gif photos %s", mapOf("appWidgetId" to appWidgetId))

        val maxDimension: Int = context.getMaxBitmapWidgetDimension()
        val borderPercent: Float = photoWidget.border.borderPercent()

        coroutineScope {
            val totalTime: Duration = measureTime {
                val deferred = photoWidget.photos.map { photo ->
                    async {
                        if (photo.originalPhotoPath == null || photo.croppedPhotoPath == null) return@async

                        val bitmap: Bitmap = decoder.decode(
                            data = photo.originalPhotoPath,
                            maxDimension = maxDimension,
                        ) ?: return@async

                        val borderColor: Int? = when (photoWidget.border) {
                            is PhotoWidgetBorder.None -> null

                            is PhotoWidgetBorder.Color -> "#${photoWidget.border.colorHex}".toColorInt()

                            is PhotoWidgetBorder.Dynamic -> context.getDynamicAttributeColor(
                                photoWidget.border.type.colorAttr,
                            )

                            is PhotoWidgetBorder.MatchPhoto -> getColorPalette(bitmap).colorForType(
                                photoWidget.border.type,
                            )
                        }

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

                        File(/* pathname = */ photo.croppedPhotoPath).runWithFileOutputStream { fos ->
                            transformedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                        }
                    }
                }

                deferred.awaitAll()
            }

            Timber.d("Prepared gif photos in $totalTime")
        }
    }
}
