package com.fibelatti.photowidget.model

data class LocalPhoto(
    val name: String,
    val path: String,
    val isCropped: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
)
