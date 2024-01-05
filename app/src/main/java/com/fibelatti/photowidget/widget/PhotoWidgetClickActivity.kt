package com.fibelatti.photowidget.widget

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.configure.ShapedPhoto
import com.fibelatti.photowidget.configure.appWidgetId
import com.fibelatti.photowidget.model.PhotoWidgetShapeBuilder
import com.fibelatti.photowidget.platform.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PhotoWidgetClickActivity : AppCompatActivity() {

    @Inject
    lateinit var photoWidgetStorage: PhotoWidgetStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val appWidgetId = try {
            intent.appWidgetId
        } catch (_: Exception) {
            finishAffinity()
            return
        }

        val photos = photoWidgetStorage.getWidgetPhotos(appWidgetId)
        val index = photoWidgetStorage.getWidgetIndex(appWidgetId)
        val aspectRatio = photoWidgetStorage.getWidgetAspectRatio(appWidgetId)
        val shapeId = photoWidgetStorage.getWidgetShapeId(appWidgetId)

        setContent {
            AppTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = Color.Black.copy(alpha = 0.6f))
                        .clickable { finishAffinity() },
                    contentAlignment = Alignment.Center,
                ) {
                    ShapedPhoto(
                        photo = photos[index],
                        aspectRatio = aspectRatio,
                        shapeId = shapeId ?: PhotoWidgetShapeBuilder.defaultShapeId(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = 16.dp),
                    )
                }
            }
        }
    }
}
