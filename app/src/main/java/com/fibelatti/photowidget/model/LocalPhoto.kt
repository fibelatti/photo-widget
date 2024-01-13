package com.fibelatti.photowidget.model

data class LocalPhoto(
    val name: String,
    val path: String,
    val timestamp: Long = System.currentTimeMillis(),
)
