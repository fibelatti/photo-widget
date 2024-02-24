package com.fibelatti.photowidget.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LocalPhoto(
    val name: String,
    val path: String,
    val timestamp: Long = System.currentTimeMillis(),
) : Parcelable
