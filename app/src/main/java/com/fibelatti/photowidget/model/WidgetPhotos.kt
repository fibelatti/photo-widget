package com.fibelatti.photowidget.model

data class WidgetPhotos(
    val current: List<LocalPhoto>,
    val excluded: List<LocalPhoto>,
    val fromCache: Boolean,
)

fun WidgetPhotos.allWidgetPhotos(): List<LocalPhoto> = current + excluded
