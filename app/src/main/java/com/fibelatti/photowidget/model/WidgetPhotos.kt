package com.fibelatti.photowidget.model

data class WidgetPhotos(
    val current: List<LocalPhoto>,
    val excluded: List<LocalPhoto>,
)