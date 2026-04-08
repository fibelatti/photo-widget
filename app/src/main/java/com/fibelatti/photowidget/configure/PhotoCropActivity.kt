@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.fibelatti.photowidget.configure

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.webkit.MimeTypeMap
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalFlexBoxApi
import androidx.compose.foundation.layout.FlexAlignContent
import androidx.compose.foundation.layout.FlexAlignItems
import androidx.compose.foundation.layout.FlexBox
import androidx.compose.foundation.layout.FlexDirection
import androidx.compose.foundation.layout.FlexJustifyContent
import androidx.compose.foundation.layout.FlexWrap
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.databinding.PhotoCropActivityBinding
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.platform.AppTheme
import com.fibelatti.photowidget.platform.intentExtras
import com.fibelatti.ui.foundation.ConnectedButtonRowItem
import com.fibelatti.ui.preview.ThemePreviews
import com.fibelatti.ui.theme.ExtendedTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.roundToInt

@AndroidEntryPoint
class PhotoCropActivity : AppCompatActivity() {

    private val binding: PhotoCropActivityBinding by lazy { PhotoCropActivityBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        getDelegate().localNightMode = AppCompatDelegate.MODE_NIGHT_YES
        super.onCreate(savedInstanceState)

        setContentView(binding.root)
        setupViews()
        setupSourceAndOptions()
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
            cropImage()
        }
        binding.cropImageView.setOnCropImageCompleteListener { _: CropImageView, result: CropImageView.CropResult ->
            finishWithResult(result)
        }

        setupCropControls()
    }

    private fun setupSourceAndOptions() {
        val cropOptions = CropImageOptions(
            guidelines = CropImageView.Guidelines.ON_TOUCH,
            showProgressBar = false,
            maxZoom = 8,
            fixAspectRatio = intent.aspectRatio.isConstrained,
            aspectRatioX = intent.aspectRatio.x.roundToInt(),
            aspectRatioY = intent.aspectRatio.y.roundToInt(),
        )

        binding.cropImageView.setImageUriAsync(intent.sourceUri)
        binding.cropImageView.setImageCropOptions(cropOptions)
    }

    private fun cropImage() {
        val typeExtension: String? = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(contentResolver.getType(intent.sourceUri))
            ?.lowercase()
        val urlExtension: String? = MimeTypeMap.getFileExtensionFromUrl(intent.sourceUri.toString())
            ?.lowercase()
        val jpeg: Array<String> = arrayOf("jpeg", "jpg")
        val compressFormat: Bitmap.CompressFormat = if (typeExtension in jpeg || urlExtension in jpeg) {
            Bitmap.CompressFormat.JPEG
        } else {
            Bitmap.CompressFormat.PNG
        }

        binding.cropButton.isInvisible = true
        binding.progressIndicator.isVisible = true
        binding.cropImageView.croppedImageAsync(
            saveCompressFormat = compressFormat,
            customOutputUri = FileProvider.getUriForFile(
                /* context = */ this,
                /* authority = */ "$packageName.fileprovider",
                /* file = */ intent.destinationUri.toFile(),
            ),
        )
    }

    private fun finishWithResult(result: CropImageView.CropResult) {
        setResult(
            /* resultCode = */ if (result.isSuccessful && result.uriContent != null) RESULT_OK else RESULT_CANCELED,
            /* data = */ result.uriContent?.let { Intent().apply { this.outputPath = intent.destinationUri.path } },
        )
        finish()
    }

    private fun setupCropControls() {
        binding.composeViewRatioShortcuts.setContent {
            AppTheme {
                CropControls(
                    onRotateLeftClick = { binding.cropImageView.rotatedDegrees -= 90 },
                    onRotateRightClick = { binding.cropImageView.rotatedDegrees += 90 },
                    onFlipHorizontalClick = binding.cropImageView::flipImageHorizontally,
                    onFlipVerticalClick = binding.cropImageView::flipImageVertically,
                    showAspectRatioShortcuts = !intent.aspectRatio.isConstrained,
                    onFreeFormClick = { binding.cropImageView.setFixedAspectRatio(false) },
                    onSquareClick = {
                        binding.cropImageView.setFixedAspectRatio(true)
                        binding.cropImageView.setAspectRatio(aspectRatioX = 1, aspectRatioY = 1)
                    },
                    onTallClick = {
                        binding.cropImageView.setFixedAspectRatio(true)
                        binding.cropImageView.setAspectRatio(aspectRatioX = 10, aspectRatioY = 16)
                    },
                    onWideClick = {
                        binding.cropImageView.setFixedAspectRatio(true)
                        binding.cropImageView.setAspectRatio(aspectRatioX = 16, aspectRatioY = 10)
                    },
                    modifier = Modifier.padding(all = 16.dp),
                )
            }
        }
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

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun CropControls(
    onRotateLeftClick: () -> Unit,
    onRotateRightClick: () -> Unit,
    onFlipHorizontalClick: () -> Unit,
    onFlipVerticalClick: () -> Unit,
    showAspectRatioShortcuts: Boolean,
    onFreeFormClick: () -> Unit,
    onSquareClick: () -> Unit,
    onTallClick: () -> Unit,
    onWideClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlexBox(
        modifier = modifier.fillMaxWidth(),
        config = {
            direction(FlexDirection.Row)
            wrap(FlexWrap.Wrap)
            justifyContent(FlexJustifyContent.Center)
            alignItems(FlexAlignItems.Center)
            alignContent(FlexAlignContent.Center)
            rowGap(8.dp)
            columnGap(12.dp)
        },
    ) {
        if (showAspectRatioShortcuts) {
            RatioShortcuts(
                onFreeFormClick = onFreeFormClick,
                onSquareClick = onSquareClick,
                onTallClick = onTallClick,
                onWideClick = onWideClick,
                modifier = Modifier.height(48.dp),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 1.dp, alignment = Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(
                onClick = onRotateLeftClick,
                shapes = IconButtonDefaults.shapes(
                    shape = ButtonGroupDefaults.connectedLeadingButtonShape,
                    pressedShape = ButtonGroupDefaults.connectedLeadingButtonPressShape,
                ),
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    painter = painterResource(com.canhub.cropper.R.drawable.ic_rotate_left_24),
                    contentDescription = null,
                )
            }

            FilledTonalIconButton(
                onClick = onRotateRightClick,
                shapes = IconButtonDefaults.shapes(
                    shape = ButtonGroupDefaults.connectedTrailingButtonShape,
                    pressedShape = ButtonGroupDefaults.connectedTrailingButtonPressShape,
                ),
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    painter = painterResource(com.canhub.cropper.R.drawable.ic_rotate_right_24),
                    contentDescription = null,
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            FilledTonalIconButton(
                onClick = onFlipHorizontalClick,
                shapes = IconButtonDefaults.shapes(
                    shape = ButtonGroupDefaults.connectedLeadingButtonShape,
                    pressedShape = ButtonGroupDefaults.connectedLeadingButtonPressShape,
                ),
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    painter = painterResource(com.canhub.cropper.R.drawable.ic_flip_24),
                    contentDescription = null,
                )
            }

            FilledTonalIconButton(
                onClick = onFlipVerticalClick,
                shapes = IconButtonDefaults.shapes(
                    shape = ButtonGroupDefaults.connectedTrailingButtonShape,
                    pressedShape = ButtonGroupDefaults.connectedTrailingButtonPressShape,
                ),
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    painter = painterResource(com.canhub.cropper.R.drawable.ic_flip_24),
                    contentDescription = null,
                    modifier = Modifier.rotate(90f),
                )
            }
        }
    }
}

@Composable
private fun RatioShortcuts(
    onFreeFormClick: () -> Unit,
    onSquareClick: () -> Unit,
    onTallClick: () -> Unit,
    onWideClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val freeFormLabel = stringResource(R.string.photo_widget_crop_free_form)
    val items: Map<String, () -> Unit> by rememberUpdatedState(
        mapOf(
            freeFormLabel to onFreeFormClick,
            "1:1" to onSquareClick,
            "16:10" to onTallClick,
            "10:16" to onWideClick,
        ),
    )
    var selectionIndex by remember { mutableIntStateOf(0) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        items.onEachIndexed { index: Int, (label: String, action: () -> Unit) ->
            val weight by animateFloatAsState(
                targetValue = if (index == selectionIndex) 1.2f else 1f,
            )

            ConnectedButtonRowItem(
                checked = index == selectionIndex,
                onCheckedChange = {
                    selectionIndex = index
                    action()
                },
                itemIndex = index,
                itemCount = items.size,
                label = label,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(weight),
            )
        }
    }
}

@Composable
@ThemePreviews
private fun RatioShortcutsPreview() {
    ExtendedTheme {
        CropControls(
            onRotateLeftClick = {},
            onRotateRightClick = {},
            onFlipHorizontalClick = {},
            onFlipVerticalClick = {},
            showAspectRatioShortcuts = true,
            onFreeFormClick = {},
            onSquareClick = {},
            onTallClick = {},
            onWideClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
