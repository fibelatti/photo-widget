package com.fibelatti.photowidget.configure

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import com.fibelatti.photowidget.platform.ComposeBottomSheetDialog
import com.fibelatti.photowidget.platform.withRoundedCorners
import com.fibelatti.photowidget.ui.Toggle
import com.fibelatti.ui.foundation.ColumnToggleButtonGroup
import com.fibelatti.ui.foundation.ToggleButtonGroup
import com.fibelatti.ui.preview.AllPreviews
import com.fibelatti.ui.text.AutoSizeText
import com.fibelatti.ui.theme.ExtendedTheme

object PhotoWidgetTapActionPicker {

    fun show(
        context: Context,
        currentTapAction: PhotoWidgetTapAction,
        onApplyClick: (newTapAction: PhotoWidgetTapAction) -> Unit,
    ) {
        ComposeBottomSheetDialog(context) {
            var selectedAppShortcut: String? by remember {
                mutableStateOf((currentTapAction as? PhotoWidgetTapAction.AppShortcut)?.appShortcut)
            }
            val appShortcutPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult(),
            ) { result ->
                selectedAppShortcut = result.data?.component?.packageName
            }

            var selectedGalleryApp: String? by remember {
                mutableStateOf((currentTapAction as? PhotoWidgetTapAction.ViewInGallery)?.galleryApp)
            }
            val galleryAppPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult(),
            ) { result ->
                selectedGalleryApp = result.data?.component?.packageName
            }

            TapActionPickerContent(
                currentTapAction = currentTapAction,
                currentAppShortcut = selectedAppShortcut,
                onChooseAppShortcutClick = {
                    appShortcutPickerLauncher.launch(
                        Intent(Intent.ACTION_PICK_ACTIVITY).putExtra(
                            Intent.EXTRA_INTENT,
                            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
                        ),
                    )
                },
                currentGalleryApp = selectedGalleryApp,
                onChooseGalleryAppClick = {
                    galleryAppPickerLauncher.launch(
                        Intent(Intent.ACTION_PICK_ACTIVITY).putExtra(
                            Intent.EXTRA_INTENT,
                            Intent(Intent.ACTION_VIEW).setDataAndType("content://sample".toUri(), "image/*"),
                        ),
                    )
                },
                onApplyClick = { newTapAction ->
                    onApplyClick(newTapAction)
                    dismiss()
                },
            )
        }.show()
    }
}

@Composable
private fun TapActionPickerContent(
    currentTapAction: PhotoWidgetTapAction,
    currentAppShortcut: String?,
    onChooseAppShortcutClick: () -> Unit,
    currentGalleryApp: String?,
    onChooseGalleryAppClick: () -> Unit,
    onApplyClick: (newTapAction: PhotoWidgetTapAction) -> Unit,
) {
    var tapAction by remember {
        mutableStateOf(currentTapAction)
    }
    var urlShortcut by remember {
        mutableStateOf((currentTapAction as? PhotoWidgetTapAction.UrlShortcut)?.url.orEmpty())
    }

    LaunchedEffect(currentAppShortcut) {
        if (currentAppShortcut != null && tapAction is PhotoWidgetTapAction.AppShortcut) {
            tapAction = PhotoWidgetTapAction.AppShortcut(currentAppShortcut)
        } else if (currentGalleryApp != null && tapAction is PhotoWidgetTapAction.ViewInGallery) {
            tapAction = PhotoWidgetTapAction.ViewInGallery(currentGalleryApp)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .nestedScroll(rememberNestedScrollInteropConnection())
            .verticalScroll(rememberScrollState())
            .padding(all = 16.dp),
    ) {
        Text(
            text = stringResource(id = R.string.photo_widget_configure_tap_action),
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
        )

        Text(
            text = stringResource(R.string.photo_widget_configure_tap_action_description),
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
        )

        BoxWithConstraints(
            modifier = Modifier.padding(top = 16.dp),
        ) {
            if (maxWidth < 600.dp) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    TapAreaIndicator(
                        modifier = Modifier.fillMaxWidth(),
                    )

                    TapOptionsPicker(
                        currentTapAction = tapAction,
                        onTapActionClick = { tapAction = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TapAreaIndicator(
                        modifier = Modifier.weight(0.4f),
                    )

                    TapOptionsPicker(
                        currentTapAction = tapAction,
                        onTapActionClick = { tapAction = it },
                        modifier = Modifier.weight(0.6f),
                    )
                }
            }
        }

        AnimatedContent(
            targetState = tapAction,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "TapAction_CustomOptions",
        ) { value ->
            val customOptionModifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)

            when (value) {
                is PhotoWidgetTapAction.None -> Unit

                is PhotoWidgetTapAction.ViewFullScreen -> {
                    Column(
                        modifier = customOptionModifier,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Toggle(
                            title = stringResource(id = R.string.photo_widget_configure_tap_action_increase_brightness),
                            checked = value.increaseBrightness,
                            onCheckedChange = { tapAction = value.copy(increaseBrightness = it) },
                        )

                        Toggle(
                            title = stringResource(R.string.photo_widget_configure_tap_action_view_original_photo),
                            checked = value.viewOriginalPhoto,
                            onCheckedChange = { tapAction = value.copy(viewOriginalPhoto = it) },
                        )
                    }
                }

                is PhotoWidgetTapAction.ViewInGallery -> {
                    Column(
                        modifier = customOptionModifier,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.photo_widget_configure_tap_action_gallery_description),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )

                        AppPicker(
                            onChooseApp = onChooseGalleryAppClick,
                            currentAppShortcut = currentGalleryApp,
                        )
                    }
                }

                is PhotoWidgetTapAction.ViewNextPhoto -> Unit

                is PhotoWidgetTapAction.ToggleCycling -> {
                    Column(
                        modifier = customOptionModifier,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(
                                id = R.string.photo_widget_configure_tap_action_toggle_cycling_description,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )

                        Toggle(
                            title = stringResource(id = R.string.photo_widget_configure_tap_action_disable_tap),
                            checked = value.disableTap,
                            onCheckedChange = { tapAction = value.copy(disableTap = it) },
                        )
                    }
                }

                is PhotoWidgetTapAction.AppShortcut -> {
                    AppPicker(
                        onChooseApp = onChooseAppShortcutClick,
                        currentAppShortcut = currentAppShortcut,
                        modifier = customOptionModifier,
                    )
                }

                is PhotoWidgetTapAction.UrlShortcut -> {
                    TextField(
                        value = urlShortcut,
                        onValueChange = { newValue -> urlShortcut = newValue },
                        modifier = customOptionModifier,
                        placeholder = { Text(text = "https://...") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        singleLine = true,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    )
                }
            }
        }

        FilledTonalButton(
            onClick = {
                if (tapAction is PhotoWidgetTapAction.UrlShortcut) {
                    tapAction = PhotoWidgetTapAction.UrlShortcut(url = urlShortcut)
                }
                onApplyClick(tapAction)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        ) {
            Text(text = stringResource(id = R.string.photo_widget_action_apply))
        }
    }
}

@Composable
private fun TapAreaIndicator(
    modifier: Modifier = Modifier,
) {
    val localContext = LocalContext.current
    val baseBitmap = remember {
        BitmapFactory.decodeResource(localContext.resources, R.drawable.image_sample)
    }
    val infiniteTransition = rememberInfiniteTransition(label = "ClickAreaIndicator_InfiniteTransition")
    val color by infiniteTransition.animateColor(
        initialValue = Color(0x664CAF50),
        targetValue = Color.Transparent,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ClickAreaIndicator_ColorAnimation",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            bitmap = baseBitmap
                .withRoundedCorners(aspectRatio = PhotoWidgetAspectRatio.SQUARE)
                .asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .size(200.dp)
                .alpha(.6F),
        )

        Row(
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight()
                .clip(MaterialTheme.shapes.large)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.large,
                ),
        ) {
            Box(
                modifier = Modifier
                    .weight(3f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceDim.copy(alpha = .7F),
                            shape = MaterialTheme.shapes.medium,
                        )
                        .padding(all = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_tap_arrow_left),
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = null,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(4f)
                    .fillMaxHeight()
                    .background(color = color),
            )

            Box(
                modifier = Modifier
                    .weight(3f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceDim.copy(alpha = .7F),
                            shape = MaterialTheme.shapes.medium,
                        )
                        .padding(all = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_tap_arrow_right),
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

@Composable
private fun TapOptionsPicker(
    currentTapAction: PhotoWidgetTapAction,
    onTapActionClick: (PhotoWidgetTapAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    ColumnToggleButtonGroup(
        items = PhotoWidgetTapAction.entries.map {
            ToggleButtonGroup.Item(
                id = it.serializedName,
                text = stringResource(id = it.label),
            )
        },
        onButtonClick = { item ->
            onTapActionClick(
                PhotoWidgetTapAction.fromSerializedName(item.id).let { selection ->
                    if (selection.javaClass == currentTapAction.javaClass) {
                        currentTapAction
                    } else {
                        selection
                    }
                },
            )
        },
        modifier = modifier.fillMaxWidth(),
        selectedIndex = PhotoWidgetTapAction.entries.indexOfFirst {
            it.serializedName == currentTapAction.serializedName
        },
        colors = ToggleButtonGroup.colors(unselectedButtonColor = MaterialTheme.colorScheme.surfaceContainerLow),
    )
}

@Composable
private fun AppPicker(
    onChooseApp: () -> Unit,
    currentAppShortcut: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = onChooseApp,
        ) {
            Text(text = stringResource(id = R.string.photo_widget_configure_tap_action_choose_app))
        }

        currentAppShortcut?.runCatching {
            val packageManager = LocalContext.current.packageManager
            val appInfo = packageManager.getApplicationInfo(
                currentAppShortcut,
                PackageManager.MATCH_DEFAULT_ONLY,
            )
            val appIcon = packageManager.getApplicationIcon(appInfo).toBitmap().asImageBitmap()
            val appLabel = packageManager.getApplicationLabel(appInfo).toString()

            Image(
                bitmap = appIcon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
            )

            AutoSizeText(
                text = appLabel,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
@AllPreviews
private fun PhotoWidgetTapActionPickerPreview() {
    ExtendedTheme {
        TapActionPickerContent(
            currentTapAction = PhotoWidgetTapAction.ToggleCycling(),
            currentAppShortcut = null,
            onChooseAppShortcutClick = {},
            currentGalleryApp = null,
            onChooseGalleryAppClick = {},
            onApplyClick = {},
        )
    }
}
