package com.fibelatti.photowidget.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LocalPhoto(
    val name: String,
    val path: String? = null,
    val externalUri: Uri? = null,
    val cropping: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
) : Parcelable
