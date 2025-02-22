package com.fibelatti.photowidget.configure

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.fibelatti.photowidget.databinding.PhotoCropActivityBinding
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.platform.getAttributeColor
import com.fibelatti.photowidget.platform.intentExtras
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropFragment
import com.yalantis.ucrop.UCropFragmentCallback
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PhotoCropActivity : AppCompatActivity(), UCropFragmentCallback {

    private val binding by lazy { PhotoCropActivityBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        getDelegate().localNightMode = AppCompatDelegate.MODE_NIGHT_YES
        super.onCreate(savedInstanceState)

        setContentView(binding.root)
        setupViews()
        showCropFragment()
    }

    private fun setupViews() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )

            view.updatePadding(
                left = insets.left,
                top = insets.top,
                right = insets.right,
                bottom = insets.bottom,
            )
            windowInsets
        }

        binding.backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.cropButton.setOnClickListener {
            binding.fragmentContainerView.getFragment<UCropFragment>().cropAndSaveImage()
            binding.cropButton.isInvisible = true
            binding.progressIndicator.isVisible = true
        }
    }

    private fun showCropFragment() {
        val fragment = UCrop.of(intent.sourceUri, intent.destinationUri)
            .apply {
                if (intent.aspectRatio.isConstrained) {
                    withAspectRatio(intent.aspectRatio.x, intent.aspectRatio.y)
                }
            }
            .withOptions(
                UCrop.Options().apply {
                    setCompressionFormat(Bitmap.CompressFormat.PNG)
                    setActiveControlsWidgetColor(getAttributeColor(android.R.attr.colorPrimary))
                },
            )
            .fragment

        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainerView.id, fragment, UCropFragment.TAG)
            .commitNowAllowingStateLoss()
    }

    override fun loadingProgress(showLoader: Boolean) {
        // Intentionally empty
    }

    override fun onCropFinish(result: UCropFragment.UCropResult) {
        setResult(
            result.mResultCode,
            Intent().apply {
                this.outputPath = result.mResultData?.let(UCrop::getOutput)?.path
            },
        )
        finish()
    }

    companion object {

        private var Intent.sourceUri: Uri by intentExtras()
        private var Intent.destinationUri: Uri by intentExtras()
        private var Intent.aspectRatio: PhotoWidgetAspectRatio by intentExtras()
        var Intent.outputPath: String? by intentExtras()

        fun newIntent(
            context: Context,
            sourceUri: Uri,
            destinationUri: Uri,
            aspectRatio: PhotoWidgetAspectRatio,
        ): Intent = Intent(context, PhotoCropActivity::class.java).apply {
            this.sourceUri = sourceUri
            this.destinationUri = destinationUri
            this.aspectRatio = aspectRatio
        }
    }
}
