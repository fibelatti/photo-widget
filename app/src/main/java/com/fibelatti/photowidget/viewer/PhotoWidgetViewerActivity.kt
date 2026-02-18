package com.fibelatti.photowidget.viewer

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibelatti.photowidget.chooser.PhotoWidgetChooserActivity
import com.fibelatti.photowidget.configure.appWidgetId
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.tapActionIncreaseBrightness
import com.fibelatti.photowidget.model.tapActionViewOriginalPhoto
import com.fibelatti.photowidget.platform.AppTheme
import com.fibelatti.photowidget.platform.RememberedEffect
import com.fibelatti.photowidget.platform.sharePhotoChooserIntent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PhotoWidgetViewerActivity : AppCompatActivity() {

    private val viewModel: PhotoWidgetViewerViewModel by viewModels()

    private var currentScreenBrightness: Float? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        WindowCompat.getInsetsController(window, window.decorView).run {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }

        setContent {
            AppTheme(
                darkTheme = true,
            ) {
                val state by viewModel.state.collectAsStateWithLifecycle()

                state.photoWidget?.let { photoWidget ->
                    RememberedEffect(Unit) {
                        if (photoWidget.tapActionIncreaseBrightness) {
                            setScreenBrightness(value = 0.9f)
                        }
                    }

                    PhotoWidgetViewerScreen(
                        photo = photoWidget.currentPhoto,
                        isLoading = photoWidget.isLoading,
                        viewOriginalPhoto = photoWidget.tapActionViewOriginalPhoto,
                        aspectRatio = if (photoWidget.tapActionViewOriginalPhoto) {
                            PhotoWidgetAspectRatio.ORIGINAL
                        } else {
                            photoWidget.aspectRatio
                        },
                        onDismissClick = ::finishAndRemoveTask,
                        showFlipControls = state.showMoveControls,
                        onPreviousClick = viewModel::viewPreviousPhoto,
                        onNextClick = viewModel::viewNextPhoto,
                        onAllPhotosClick = ::showPhotoChooser,
                        onShareClick = ::sharePhoto,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadData()
    }

    override fun onDestroy() {
        currentScreenBrightness?.let(::setScreenBrightness)
        super.onDestroy()
    }

    private fun setScreenBrightness(value: Float) {
        window?.attributes = window?.attributes?.apply {
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
