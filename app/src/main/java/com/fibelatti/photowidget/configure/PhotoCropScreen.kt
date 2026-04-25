@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.fibelatti.photowidget.configure

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalFlexBoxApi
import androidx.compose.foundation.layout.FlexAlignContent
import androidx.compose.foundation.layout.FlexAlignItems
import androidx.compose.foundation.layout.FlexBox
import androidx.compose.foundation.layout.FlexDirection
import androidx.compose.foundation.layout.FlexJustifyContent
import androidx.compose.foundation.layout.FlexWrap
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.core.view.WindowInsetsControllerCompat
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.platform.AppTheme
import com.fibelatti.photowidget.platform.LocalAppCompatActivity
import com.fibelatti.ui.foundation.ConnectedButtonRowItem
import com.fibelatti.ui.preview.PreviewAccessibility
import com.fibelatti.ui.preview.PreviewAll
import com.fibelatti.ui.preview.PreviewThemesAndColors
import com.fibelatti.ui.theme.ExtendedTheme

@Composable
fun PhotoCropScreen(
    sourceUri: Uri,
    destinationUri: Uri,
    cropImageOptions: CropImageOptions,
    onBackClick: () -> Unit,
    onCropComplete: (CropImageView.CropResult) -> Unit,
    showAspectRatioShortcuts: Boolean,
) {
    AppTheme(
        darkTheme = true,
    ) {
        val localActivity: AppCompatActivity = LocalAppCompatActivity.current

        DisposableEffect(Unit) {
            val controller = WindowInsetsControllerCompat(localActivity.window, localActivity.window.decorView)
            val previousValue: Boolean = controller.isAppearanceLightStatusBars

            controller.isAppearanceLightStatusBars = false

            onDispose {
                controller.isAppearanceLightStatusBars = previousValue
            }
        }

        PhotoCropContent(
            sourceUri = sourceUri,
            destinationUri = destinationUri,
            cropImageOptions = cropImageOptions,
            onBackClick = onBackClick,
            onCropComplete = onCropComplete,
            showAspectRatioShortcuts = showAspectRatioShortcuts,
        )
    }
}

@Composable
private fun PhotoCropContent(
    sourceUri: Uri,
    destinationUri: Uri,
    cropImageOptions: CropImageOptions,
    onBackClick: () -> Unit,
    onCropComplete: (CropImageView.CropResult) -> Unit,
    showAspectRatioShortcuts: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        val localContext: Context = LocalContext.current
        val cropImageView: CropImageView = remember(localContext) {
            CropImageView(localContext).apply {
                setImageUriAsync(sourceUri)
                setImageCropOptions(cropImageOptions)
                setOnCropImageCompleteListener { _: CropImageView, result: CropImageView.CropResult ->
                    onCropComplete(result)
                }
            }
        }
        var isCropping: Boolean by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(all = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = onBackClick,
                shapes = IconButtonDefaults.shapes(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = null,
                    tint = Color.White,
                )
            }

            Crossfade(
                targetState = isCropping,
                modifier = Modifier.fillMaxHeight(),
            ) { value: Boolean ->
                if (value) {
                    CircularWavyProgressIndicator(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f),
                    )
                } else {
                    IconButton(
                        onClick = {
                            isCropping = true
                            cropImage(
                                context = localContext,
                                cropImageView = cropImageView,
                                sourceUri = sourceUri,
                                destinationUri = destinationUri,
                            )
                        },
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                }
            }
        }

        AndroidView(
            factory = { cropImageView },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )

        CropControls(
            onRotateLeftClick = { cropImageView.rotatedDegrees -= 90 },
            onRotateRightClick = { cropImageView.rotatedDegrees += 90 },
            onFlipHorizontalClick = cropImageView::flipImageHorizontally,
            onFlipVerticalClick = cropImageView::flipImageVertically,
            showAspectRatioShortcuts = showAspectRatioShortcuts,
            onFreeFormClick = { cropImageView.setFixedAspectRatio(false) },
            onSquareClick = {
                cropImageView.setFixedAspectRatio(true)
                cropImageView.setAspectRatio(aspectRatioX = 1, aspectRatioY = 1)
            },
            onTallClick = {
                cropImageView.setFixedAspectRatio(true)
                cropImageView.setAspectRatio(aspectRatioX = 10, aspectRatioY = 16)
            },
            onWideClick = {
                cropImageView.setFixedAspectRatio(true)
                cropImageView.setAspectRatio(aspectRatioX = 16, aspectRatioY = 10)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp),
        )
    }
}

private fun cropImage(
    context: Context,
    cropImageView: CropImageView,
    sourceUri: Uri,
    destinationUri: Uri,
) {
    val typeExtension: String? = MimeTypeMap.getSingleton()
        .getExtensionFromMimeType(context.contentResolver.getType(sourceUri))
        ?.lowercase()
    val urlExtension: String? = MimeTypeMap.getFileExtensionFromUrl(sourceUri.toString())
        ?.lowercase()
    val jpeg: Array<String> = arrayOf("jpeg", "jpg")
    val compressFormat: Bitmap.CompressFormat = if (typeExtension in jpeg || urlExtension in jpeg) {
        Bitmap.CompressFormat.JPEG
    } else {
        Bitmap.CompressFormat.PNG
    }

    cropImageView.croppedImageAsync(
        saveCompressFormat = compressFormat,
        customOutputUri = FileProvider.getUriForFile(
            /* context = */ context,
            /* authority = */ "${context.packageName}.fileprovider",
            /* file = */ destinationUri.toFile(),
        ),
    )
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

// region Previews
@Composable
@PreviewAll
private fun PhotoCropContentPreview() {
    ExtendedTheme(darkTheme = true) {
        PhotoCropContent(
            sourceUri = Uri.EMPTY,
            destinationUri = Uri.EMPTY,
            cropImageOptions = CropImageOptions(
                guidelines = CropImageView.Guidelines.ON_TOUCH,
                showProgressBar = false,
            ),
            onBackClick = {},
            onCropComplete = {},
            showAspectRatioShortcuts = true,
            modifier = Modifier.background(color = MaterialTheme.colorScheme.background),
        )
    }
}

@Composable
@PreviewThemesAndColors
@PreviewAccessibility
private fun CropControlsPreview() {
    ExtendedTheme(darkTheme = true) {
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
            modifier = Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.background),
        )
    }
}
// endregion Previews
