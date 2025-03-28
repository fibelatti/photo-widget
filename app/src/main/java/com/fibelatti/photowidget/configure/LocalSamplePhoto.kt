package com.fibelatti.photowidget.configure

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.platform.PhotoDecoder

@SuppressLint("ComposeCompositionLocalUsage")
val LocalSamplePhoto = staticCompositionLocalOf<LocalPhoto?> { null }

@Composable
fun rememberSampleBitmap(): Bitmap {
    val localContext: Context = LocalContext.current
    val localPhoto: LocalPhoto? = LocalSamplePhoto.current
    val decoder: PhotoDecoder by remember {
        lazy { entryPoint<PhotoWidgetEntryPoint>(localContext).photoDecoder() }
    }

    var bitmap: Bitmap by remember {
        mutableStateOf(BitmapFactory.decodeResource(localContext.resources, R.drawable.image_sample))
    }

    LaunchedEffect(localPhoto) {
        localPhoto?.getPhotoPath()?.let { path ->
            decoder.decode(data = path, maxDimension = 400)?.let { result ->
                bitmap = result
            }
        }
    }

    return bitmap
}
