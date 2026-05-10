package com.fibelatti.photowidget.viewer

import android.content.Intent
import android.os.Bundle
import android.view.Window
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibelatti.photowidget.chooser.PhotoWidgetChooserActivity
import com.fibelatti.photowidget.configure.appWidgetId
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.tapActionIncreaseBrightness
import com.fibelatti.photowidget.model.tapActionViewOriginalPhoto
import com.fibelatti.photowidget.model.tapActionViewerBackgroundColorHex
import com.fibelatti.photowidget.platform.AppTheme
import com.fibelatti.photowidget.platform.sharePhotoChooserIntent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PhotoWidgetViewerActivity : AppCompatActivity() {

    private val viewModel: PhotoWidgetViewerViewModel by viewModels()

    private var currentScreenBrightness: Float? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setFullScreen()
        setContent()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadData()
    }

    override fun onDestroy() {
        currentScreenBrightness?.let(::setScreenBrightness)
        super.onDestroy()
    }

    private fun setFullScreen() {
        WindowCompat.getInsetsController(window, window.decorView).run {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun setContent() {
        setContent {
            AppTheme(
                darkTheme = true,
            ) {
                val state: PhotoWidgetViewerViewModel.State by viewModel.state.collectAsStateWithLifecycle()
                val photoWidget: PhotoWidget = state.photoWidget

                val backgroundColor: Color = photoWidget.tapActionViewerBackgroundColorHex
                    ?.let { Color("#$it".toColorInt()) }
                    ?: MaterialTheme.colorScheme.background

                window.setBackgroundDrawable(backgroundColor.toArgb().toDrawable())

                SideEffect(photoWidget.tapActionIncreaseBrightness) {
                    if (photoWidget.tapActionIncreaseBrightness) {
                        setScreenBrightness(value = 0.9f)
                    }
                }

                PhotoWidgetViewerScreen(
                    photo = photoWidget.currentPhoto,
                    isLoading = photoWidget.isLoading,
                    viewOriginalPhoto = photoWidget.tapActionViewOriginalPhoto,
                    onDismissClick = ::finishAndRemoveTask,
                    backgroundColor = backgroundColor,
                    showNextButton = state.showNextButton,
                    showPreviousButton = state.showPreviousButton,
                    onNextClick = viewModel::viewNextPhoto,
                    onPreviousClick = viewModel::viewPreviousPhoto,
                    onAllPhotosClick = ::showPhotoChooser,
                    onShareClick = ::sharePhoto,
                )
            }
        }
    }

    private fun setScreenBrightness(value: Float) {
        val window: Window = window ?: return
        window.attributes = window.attributes.apply {
            currentScreenBrightness = screenBrightness
            screenBrightness = value
        }
    }

    private fun showPhotoChooser() {
        val clickIntent = Intent(this, PhotoWidgetChooserActivity::class.java).apply {
            this.appWidgetId = intent.appWidgetId
        }
        startActivity(clickIntent)
    }

    private fun sharePhoto(photo: LocalPhoto) {
        val intent: Intent? = sharePhotoChooserIntent(
            context = this,
            originalPhotoPath = photo.originalPhotoPath,
            externalUri = photo.externalUri,
        )

        if (intent != null) {
            startActivity(intent)
        }
    }
}
