package com.fibelatti.photowidget.backup

import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetBorder
import com.fibelatti.photowidget.model.PhotoWidgetColors
import com.fibelatti.photowidget.model.PhotoWidgetText
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
    val text: String?,
    val textValue: String?,
    val textSize: Int?,
    val textVerticalOffset: Int?,
    val textHasShadow: Boolean?,
)

fun PhotoWidgetExport.toPhotoWidget(photos: List<LocalPhoto>): PhotoWidget {
    val photoWidgetBorder: PhotoWidgetBorder = when (
        val widgetBorder = PhotoWidgetBorder.fromSerializedName(border)
    ) {
        is PhotoWidgetBorder.None -> widgetBorder

        is PhotoWidgetBorder.Color -> {
            PhotoWidgetBorder.Color(
                colorHex = borderHex ?: "ffffff",
                width = borderWidth ?: PhotoWidgetBorder.DEFAULT_WIDTH,
            )
        }

        is PhotoWidgetBorder.Dynamic -> {
            PhotoWidgetBorder.Dynamic(
                width = borderWidth ?: PhotoWidgetBorder.DEFAULT_WIDTH,
            )
        }

        is PhotoWidgetBorder.MatchPhoto -> {
            PhotoWidgetBorder.MatchPhoto(
                width = borderWidth ?: PhotoWidgetBorder.DEFAULT_WIDTH,
                type = enumValueOfOrNull<PhotoWidgetBorder.MatchPhoto.Type>(borderType)
                    ?: PhotoWidgetBorder.MatchPhoto.Type.DOMINANT,
            )
        }
    }

    val photoWidgetText: PhotoWidgetText = when (
        val widgetText = PhotoWidgetText.fromSerializedName(text)
    ) {
        is PhotoWidgetText.None -> widgetText

        is PhotoWidgetText.Label -> {
            PhotoWidgetText.Label(
                value = textValue ?: widgetText.value,
                size = textSize ?: widgetText.size,
                verticalOffset = textVerticalOffset ?: widgetText.verticalOffset,
                hasShadow = textHasShadow ?: widgetText.hasShadow,
            )
        }
    }

    return PhotoWidget(
        photos = photos,
        aspectRatio = enumValueOfOrNull<PhotoWidgetAspectRatio>(aspectRatio) ?: PhotoWidgetAspectRatio.SQUARE,
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
        text = photoWidgetText,
    )
}

fun PhotoWidget.toPhotoWidgetExport(id: Int): PhotoWidgetExport {
    var borderHex: String? = null
    var borderWidth: Int? = null
    var borderType: String? = null

    var textValue: String? = null
    var textSize: Int? = null
    var textVerticalOffset: Int? = null
    var textHasShadow: Boolean? = null

    when (border) {
        // Nothing else to export
        is PhotoWidgetBorder.None -> Unit

        is PhotoWidgetBorder.Color -> {
            borderHex = border.colorHex
            borderWidth = border.width
        }

        is PhotoWidgetBorder.Dynamic -> {
            borderWidth = border.width
        }

        is PhotoWidgetBorder.MatchPhoto -> {
            borderWidth = border.width
            borderType = border.type.name
        }
    }

    when (text) {
        // Nothing else to export
        is PhotoWidgetText.None -> Unit

        is PhotoWidgetText.Label -> {
            textValue = text.value
            textSize = text.size
            textVerticalOffset = text.verticalOffset
            textHasShadow = text.hasShadow
        }
    }

    return PhotoWidgetExport(
        id = id,
        aspectRatio = aspectRatio.name,
        shapeId = shapeId,
        cornerRadius = cornerRadius,
        border = border.serializedName,
        borderHex = borderHex,
        borderWidth = borderWidth,
        borderType = borderType,
        opacity = colors.opacity,
        saturation = colors.saturation,
        brightness = colors.brightness,
        horizontalOffset = horizontalOffset,
        verticalOffset = verticalOffset,
        padding = padding,
        text = text.serializedName,
        textValue = textValue,
        textSize = textSize,
        textVerticalOffset = textVerticalOffset,
        textHasShadow = textHasShadow,
    )
}
