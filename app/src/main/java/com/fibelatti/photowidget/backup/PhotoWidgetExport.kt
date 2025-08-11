package com.fibelatti.photowidget.backup

import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetBorder
import com.fibelatti.photowidget.model.PhotoWidgetColors
import com.fibelatti.photowidget.platform.enumValueOfOrNull
import kotlinx.serialization.Serializable

@Serializable
data class PhotoWidgetExport(
    val id: Int,
    val aspectRatio: String,
    val shapeId: String,
    val cornerRadius: Int,
    val border: String,
    val borderHex: String?,
    val borderWidth: Int?,
    val borderType: String?,
    val opacity: Float,
    val saturation: Float,
    val brightness: Float,
    val horizontalOffset: Int,
    val verticalOffset: Int,
    val padding: Int,
)

fun PhotoWidgetExport.toPhotoWidget(photos: List<LocalPhoto>): PhotoWidget {
    val photoWidgetBorder: PhotoWidgetBorder = when (border) {
        "COLOR" -> {
            PhotoWidgetBorder.Color(
                colorHex = borderHex ?: "ffffff",
                width = borderWidth ?: PhotoWidgetBorder.DEFAULT_WIDTH,
            )
        }

        "DYNAMIC" -> {
            PhotoWidgetBorder.Dynamic(
                width = borderWidth ?: PhotoWidgetBorder.DEFAULT_WIDTH,
            )
        }

        "MATCH_PHOTO" -> {
            PhotoWidgetBorder.MatchPhoto(
                width = borderWidth ?: PhotoWidgetBorder.DEFAULT_WIDTH,
                type = enumValueOfOrNull<PhotoWidgetBorder.MatchPhoto.Type>(borderType)
                    ?: PhotoWidgetBorder.MatchPhoto.Type.DOMINANT,
            )
        }

        else -> PhotoWidgetBorder.None
    }

    return PhotoWidget(
        photos = photos,
        aspectRatio = enumValueOfOrNull<PhotoWidgetAspectRatio>(aspectRatio)
            ?: PhotoWidgetAspectRatio.SQUARE,
        shapeId = shapeId,
        cornerRadius = cornerRadius,
        border = photoWidgetBorder,
        colors = PhotoWidgetColors(
            opacity = opacity,
            saturation = saturation,
            brightness = brightness,
        ),
        horizontalOffset = horizontalOffset,
        verticalOffset = verticalOffset,
        padding = padding,
    )
}

fun PhotoWidget.toPhotoWidgetExport(id: Int): PhotoWidgetExport {
    val borderName: String
    var borderHex: String? = null
    var borderWidth: Int? = null
    var borderType: String? = null

    when (border) {
        is PhotoWidgetBorder.None -> {
            borderName = "NONE"
        }

        is PhotoWidgetBorder.Color -> {
            borderName = "COLOR"
            borderHex = border.colorHex
            borderWidth = border.width
        }

        is PhotoWidgetBorder.Dynamic -> {
            borderName = "DYNAMIC"
            borderWidth = border.width
        }

        is PhotoWidgetBorder.MatchPhoto -> {
            borderName = "MATCH_PHOTO"
            borderWidth = border.width
            borderType = border.type.name
        }
    }

    return PhotoWidgetExport(
        id = id,
        aspectRatio = aspectRatio.name,
        shapeId = shapeId,
        cornerRadius = cornerRadius,
        border = borderName,
        borderHex = borderHex,
        borderWidth = borderWidth,
        borderType = borderType,
        opacity = colors.opacity,
        saturation = colors.saturation,
        brightness = colors.brightness,
        horizontalOffset = horizontalOffset,
        verticalOffset = verticalOffset,
        padding = padding,
    )
}
