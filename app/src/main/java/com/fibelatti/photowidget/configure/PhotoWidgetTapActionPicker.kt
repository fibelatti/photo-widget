@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.fibelatti.photowidget.configure

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import com.fibelatti.photowidget.model.PhotoWidgetTapActions
import com.fibelatti.photowidget.model.TapActionArea
import com.fibelatti.photowidget.model.toIconLabelPair
import com.fibelatti.photowidget.platform.withRoundedCorners
import com.fibelatti.photowidget.preferences.PickerDefault
import com.fibelatti.photowidget.ui.DefaultSheetContent
import com.fibelatti.photowidget.ui.RadioGroup
import com.fibelatti.photowidget.ui.Toggle
import com.fibelatti.photowidget.ui.WarningSign
import com.fibelatti.photowidget.ui.rememberSampleBitmap
import com.fibelatti.ui.foundation.AppBottomSheet
import com.fibelatti.ui.foundation.AppSheetState
import com.fibelatti.ui.foundation.ConnectedButtonRowItem
import com.fibelatti.ui.foundation.dpToPx
import com.fibelatti.ui.foundation.fadingEdges
import com.fibelatti.ui.foundation.hideBottomSheet
import com.fibelatti.ui.foundation.rememberAppSheetState
import com.fibelatti.ui.foundation.showBottomSheet
import com.fibelatti.ui.preview.AllPreviews
import com.fibelatti.ui.text.AutoSizeText
import com.fibelatti.ui.theme.ExtendedTheme
import sh.calvin.reorderable.ReorderableColumn

@Composable
fun PhotoWidgetTapActionPicker(
    onNavClick: () -> Unit,
    currentTapActions: PhotoWidgetTapActions,
    onApplyClick: (newTapActions: PhotoWidgetTapActions) -> Unit,
) {
    var tapActions: PhotoWidgetTapActions by rememberSaveable { mutableStateOf(currentTapActions) }
    var selectedArea: TapActionArea by rememberSaveable { mutableStateOf(TapActionArea.CENTER) }

    val appShortcutPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        tapActions = onAppShortcutPickerResult(result = result, tapActions = tapActions, selectedArea = selectedArea)
    }

    val appFolderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        tapActions = onAppFolderPickerResult(result = result, tapActions = tapActions, selectedArea = selectedArea)
    }

    val galleryAppPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        tapActions = onGalleryAppPickerResult(result = result, tapActions = tapActions)
    }

    TapActionPickerContent(
        onNavClick = onNavClick,
        selectedArea = selectedArea,
        onSelectedAreaChange = { newArea -> selectedArea = newArea },
        currentTapAction = when (selectedArea) {
            TapActionArea.LEFT -> tapActions.left
            TapActionArea.CENTER -> tapActions.center
            TapActionArea.RIGHT -> tapActions.right
        },
        onTapActionChange = { newAction ->
            tapActions = onTapActionChange(tapActions = tapActions, newAction = newAction, selectedArea = selectedArea)
        },
        onChooseAppShortcutClick = {
            appShortcutPickerLauncher.launch(
                Intent(Intent.ACTION_PICK_ACTIVITY).putExtra(
                    Intent.EXTRA_INTENT,
                    Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
                ),
            )
        },
        onAddAppToFolderClick = {
            appFolderPickerLauncher.launch(
                Intent(Intent.ACTION_PICK_ACTIVITY).putExtra(
                    Intent.EXTRA_INTENT,
                    Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
                ),
            )
        },
        onChooseGalleryAppClick = {
            galleryAppPickerLauncher.launch(
                Intent(Intent.ACTION_PICK_ACTIVITY).putExtra(
                    Intent.EXTRA_INTENT,
                    Intent(Intent.ACTION_VIEW).setDataAndType("content://sample".toUri(), "image/*"),
                ),
            )
        },
        onApplyClick = { onApplyClick(tapActions) },
    )
}

private fun onAppShortcutPickerResult(
    result: ActivityResult,
    tapActions: PhotoWidgetTapActions,
    selectedArea: TapActionArea,
): PhotoWidgetTapActions {
    val newTapAction = PhotoWidgetTapAction.AppShortcut(result.data?.component?.packageName)

    return when (selectedArea) {
        TapActionArea.LEFT -> tapActions.copy(left = newTapAction)
        TapActionArea.CENTER -> tapActions.copy(center = newTapAction)
        TapActionArea.RIGHT -> tapActions.copy(right = newTapAction)
    }
}

private fun onAppFolderPickerResult(
    result: ActivityResult,
    tapActions: PhotoWidgetTapActions,
    selectedArea: TapActionArea,
): PhotoWidgetTapActions {
    val newAppShortcut: String = result.data?.component?.packageName ?: return tapActions
    val leftAction = tapActions.left as? PhotoWidgetTapAction.AppFolder
    val centerAction = tapActions.center as? PhotoWidgetTapAction.AppFolder
    val rightAction = tapActions.right as? PhotoWidgetTapAction.AppFolder

    val updatedAction: PhotoWidgetTapAction = when (selectedArea) {
        TapActionArea.LEFT if leftAction != null -> {
            leftAction.copy(shortcuts = (leftAction.shortcuts + newAppShortcut).distinct())
        }

        TapActionArea.CENTER if centerAction != null -> {
            centerAction.copy(shortcuts = (centerAction.shortcuts + newAppShortcut).distinct())
        }

        TapActionArea.RIGHT if rightAction != null -> {
            rightAction.copy(shortcuts = (rightAction.shortcuts + newAppShortcut).distinct())
        }

        else -> return tapActions
    }

    return when (selectedArea) {
        TapActionArea.LEFT -> tapActions.copy(left = updatedAction)
        TapActionArea.CENTER -> tapActions.copy(center = updatedAction)
        TapActionArea.RIGHT -> tapActions.copy(right = updatedAction)
    }
}

private fun onGalleryAppPickerResult(
    result: ActivityResult,
    tapActions: PhotoWidgetTapActions,
): PhotoWidgetTapActions {
    val newTapAction = PhotoWidgetTapAction.ViewInGallery(result.data?.component?.packageName)

    return tapActions.copy(
        left = if (tapActions.left is PhotoWidgetTapAction.ViewInGallery) {
            newTapAction
        } else {
            tapActions.left
        },
        center = if (tapActions.center is PhotoWidgetTapAction.ViewInGallery) {
            newTapAction
        } else {
            tapActions.center
        },
        right = if (tapActions.right is PhotoWidgetTapAction.ViewInGallery) {
            newTapAction
        } else {
            tapActions.right
        },
    )
}

private fun onTapActionChange(
    tapActions: PhotoWidgetTapActions,
    newAction: PhotoWidgetTapAction,
    selectedArea: TapActionArea,
): PhotoWidgetTapActions {
    val isChangingTypes: Boolean = when (selectedArea) {
        TapActionArea.LEFT -> tapActions.left.javaClass != newAction.javaClass
        TapActionArea.CENTER -> tapActions.center.javaClass != newAction.javaClass
        TapActionArea.RIGHT -> tapActions.right.javaClass != newAction.javaClass
    }

    val action: PhotoWidgetTapAction = when {
        !isChangingTypes -> newAction

        newAction is PhotoWidgetTapAction.ViewFullScreen -> {
            when {
                tapActions.left is PhotoWidgetTapAction.ViewFullScreen -> tapActions.left
                tapActions.center is PhotoWidgetTapAction.ViewFullScreen -> tapActions.center
                tapActions.right is PhotoWidgetTapAction.ViewFullScreen -> tapActions.right
                else -> newAction
            }
        }

        newAction is PhotoWidgetTapAction.ViewInGallery -> {
            when {
                tapActions.left is PhotoWidgetTapAction.ViewInGallery -> tapActions.left
                tapActions.center is PhotoWidgetTapAction.ViewInGallery -> tapActions.center
                tapActions.right is PhotoWidgetTapAction.ViewInGallery -> tapActions.right
                else -> newAction
            }
        }

        newAction is PhotoWidgetTapAction.ToggleCycling -> {
            when {
                tapActions.left is PhotoWidgetTapAction.ToggleCycling -> tapActions.left
                tapActions.center is PhotoWidgetTapAction.ToggleCycling -> tapActions.center
                tapActions.right is PhotoWidgetTapAction.ToggleCycling -> tapActions.right
                else -> newAction
            }
        }

        else -> newAction
    }

    return when (selectedArea) {
        TapActionArea.LEFT -> tapActions.copy(left = action)
        TapActionArea.CENTER -> tapActions.copy(center = action)
        TapActionArea.RIGHT -> tapActions.copy(right = action)
    }
}

@Composable
private fun TapActionPickerContent(
    onNavClick: () -> Unit,
    selectedArea: TapActionArea,
    onSelectedAreaChange: (TapActionArea) -> Unit,
    currentTapAction: PhotoWidgetTapAction,
    onTapActionChange: (PhotoWidgetTapAction) -> Unit,
    onChooseAppShortcutClick: () -> Unit,
    onAddAppToFolderClick: () -> Unit,
    onChooseGalleryAppClick: () -> Unit,
    onApplyClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(
                onClick = onNavClick,
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.align(Alignment.TopStart),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(id = R.string.photo_widget_configure_tap_action),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                )

                Text(
                    text = stringResource(id = R.string.photo_widget_configure_tap_action_description),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .fadingEdges(
                    scrollState = scrollState,
                    topEdgeHeight = 36.dp,
                    bottomEdgeHeight = 36.dp,
                )
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TapAreaSelector(
                selectedArea = selectedArea,
                onSelectedAreaChange = onSelectedAreaChange,
                modifier = Modifier.fillMaxWidth(),
            )

            TapAreaIndicator(
                tapActionArea = selectedArea,
                modifier = Modifier.fillMaxWidth(),
            )

            TapOptionsPicker(
                currentTapAction = currentTapAction,
                onTapActionClick = onTapActionChange,
                modifier = Modifier.fillMaxWidth(),
            )

            TapActionCustomizationContent(
                tapAction = currentTapAction,
                onTapActionChange = onTapActionChange,
                onChooseAppShortcutClick = onChooseAppShortcutClick,
                onAddAppToFolderClick = onAddAppToFolderClick,
                onChooseGalleryAppClick = onChooseGalleryAppClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Button(
            onClick = onApplyClick,
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Text(text = stringResource(id = R.string.photo_widget_action_apply))
        }
    }
}

@Composable
private fun TapAreaSelector(
    selectedArea: TapActionArea,
    onSelectedAreaChange: (TapActionArea) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        TapActionArea.entries.forEachIndexed { index, area ->
            val weight by animateFloatAsState(
                targetValue = if (area == selectedArea) 1.2f else 1f,
            )

            ConnectedButtonRowItem(
                checked = area == selectedArea,
                onCheckedChange = { onSelectedAreaChange(area) },
                itemIndex = index,
                itemCount = TapActionArea.entries.size,
                label = stringResource(
                    when (area) {
                        TapActionArea.LEFT -> R.string.photo_widget_configure_tap_action_area_left
                        TapActionArea.CENTER -> R.string.photo_widget_configure_tap_action_area_center
                        TapActionArea.RIGHT -> R.string.photo_widget_configure_tap_action_area_right
                    },
                ),
                modifier = Modifier.weight(weight),
            )
        }
    }
}

@Composable
private fun TapAreaIndicator(
    tapActionArea: TapActionArea,
    modifier: Modifier = Modifier,
) {
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
            bitmap = rememberSampleBitmap()
                .withRoundedCorners(radius = PhotoWidget.DEFAULT_CORNER_RADIUS.dpToPx())
                .asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .size(200.dp)
                .alpha(.6F),
        )

        val areaRadius: Float = 28.dp.dpToPx()
        val areaColor: Color = MaterialTheme.colorScheme.onSurface

        Row(
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight()
                .drawWithContent {
                    drawRoundRect(
                        color = areaColor,
                        cornerRadius = CornerRadius(areaRadius),
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                        ),
                    )
                    drawContent()
                }
                .clip(shape = RoundedCornerShape(size = areaRadius)),
        ) {
            Box(
                modifier = Modifier
                    .weight(3f)
                    .fillMaxHeight()
                    .background(color = if (TapActionArea.LEFT == tapActionArea) color else Color.Transparent),
            )

            Box(
                modifier = Modifier
                    .weight(4f)
                    .fillMaxHeight()
                    .background(color = if (TapActionArea.CENTER == tapActionArea) color else Color.Transparent),
            )

            Box(
                modifier = Modifier
                    .weight(3f)
                    .fillMaxHeight()
                    .background(color = if (TapActionArea.RIGHT == tapActionArea) color else Color.Transparent),
            )
        }
    }
}

@Composable
private fun TapOptionsPicker(
    currentTapAction: PhotoWidgetTapAction,
    onTapActionClick: (PhotoWidgetTapAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState: AppSheetState = rememberAppSheetState()
    val localResources: Resources = LocalResources.current

    PickerDefault(
        title = stringResource(R.string.photo_widget_configure_tap_action),
        currentValue = stringResource(currentTapAction.label),
        onClick = sheetState::showBottomSheet,
        modifier = modifier.fillMaxWidth(),
    )

    AppBottomSheet(
        sheetState = sheetState,
    ) {
        DefaultSheetContent(
            title = stringResource(R.string.photo_widget_configure_tap_action),
        ) {
            RadioGroup(
                items = PhotoWidgetTapAction.entries,
                itemSelected = { action -> action::class == currentTapAction::class },
                onItemClick = { selection ->
                    onTapActionClick(
                        if (selection.javaClass == currentTapAction.javaClass) {
                            currentTapAction
                        } else {
                            selection
                        },
                    )
                    sheetState.hideBottomSheet()
                },
                itemTitle = { action -> localResources.getString(action.label) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                itemDescription = itemDescription@{ action ->
                    val descriptionRes: Int = when (action) {
                        is PhotoWidgetTapAction.ViewInGallery -> {
                            R.string.photo_widget_configure_tap_action_gallery_description_compatibility
                        }

                        is PhotoWidgetTapAction.AppFolder -> {
                            R.string.photo_widget_configure_tap_action_app_folder_description
                        }

                        else -> return@itemDescription null
                    }

                    localResources.getString(descriptionRes)
                },
            )
        }
    }
}

// region Customization Content
@Composable
private fun TapActionCustomizationContent(
    tapAction: PhotoWidgetTapAction,
    onTapActionChange: (PhotoWidgetTapAction) -> Unit,
    onChooseAppShortcutClick: () -> Unit,
    onAddAppToFolderClick: () -> Unit,
    onChooseGalleryAppClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (tapAction) {
        is PhotoWidgetTapAction.None -> Unit

        is PhotoWidgetTapAction.ViewFullScreen -> {
            ViewFullScreenCustomizationContent(
                value = tapAction,
                onValueChange = onTapActionChange,
                modifier = modifier,
            )
        }

        is PhotoWidgetTapAction.ViewInGallery -> {
            ViewInGalleryCustomizationContent(
                value = tapAction,
                onChooseGalleryAppClick = onChooseGalleryAppClick,
                modifier = modifier,
            )
        }

        is PhotoWidgetTapAction.ViewNextPhoto -> Unit

        is PhotoWidgetTapAction.ViewPreviousPhoto -> Unit

        is PhotoWidgetTapAction.ChooseNextPhoto -> Unit

        is PhotoWidgetTapAction.ToggleCycling -> {
            ToggleCyclingCustomizationContent(
                value = tapAction,
                onValueChange = onTapActionChange,
                modifier = modifier,
            )
        }

        is PhotoWidgetTapAction.AppShortcut -> {
            AppPicker(
                value = tapAction.appShortcut,
                onChooseAppClick = onChooseAppShortcutClick,
                modifier = modifier,
            )
        }

        is PhotoWidgetTapAction.AppFolder -> {
            AppFolderCustomizationContent(
                value = tapAction,
                onValueChange = onTapActionChange,
                onAddAppClick = onAddAppToFolderClick,
                modifier = modifier,
            )
        }

        is PhotoWidgetTapAction.UrlShortcut -> {
            TextField(
                value = tapAction.url.orEmpty(),
                onValueChange = { newValue -> onTapActionChange(tapAction.copy(url = newValue)) },
                modifier = modifier,
                placeholder = { Text(text = "https://...") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                singleLine = true,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            )
        }

        is PhotoWidgetTapAction.SharePhoto -> Unit
    }
}

@Composable
private fun AppPicker(
    value: String?,
    onChooseAppClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = onChooseAppClick,
            shapes = ButtonDefaults.shapes(),
        ) {
            Text(text = stringResource(id = R.string.photo_widget_configure_tap_action_choose_app))
        }

        val packageManager = LocalContext.current.packageManager
        val (appIcon: ImageBitmap, appLabel: String) = value
            ?.runCatching {
                val appInfo = packageManager.getApplicationInfo(
                    value,
                    PackageManager.MATCH_DEFAULT_ONLY,
                )
                Pair(
                    first = packageManager.getApplicationIcon(appInfo).toBitmap().asImageBitmap(),
                    second = packageManager.getApplicationLabel(appInfo).toString(),
                )
            }
            ?.getOrNull()
            ?: return

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

@Composable
private fun ViewFullScreenCustomizationContent(
    value: PhotoWidgetTapAction.ViewFullScreen,
    onValueChange: (PhotoWidgetTapAction.ViewFullScreen) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Toggle(
            title = stringResource(id = R.string.photo_widget_configure_tap_action_increase_brightness),
            checked = value.increaseBrightness,
            onCheckedChange = { onValueChange(value.copy(increaseBrightness = it)) },
        )

        Toggle(
            title = stringResource(R.string.photo_widget_configure_tap_action_view_original_photo),
            checked = value.viewOriginalPhoto,
            onCheckedChange = { onValueChange(value.copy(viewOriginalPhoto = it)) },
        )

        Toggle(
            title = stringResource(R.string.photo_widget_configure_tap_action_do_not_shuffle),
            checked = value.noShuffle,
            onCheckedChange = { onValueChange(value.copy(noShuffle = it)) },
        )

        Toggle(
            title = stringResource(R.string.photo_widget_configure_tap_action_keep_current_photo),
            checked = value.keepCurrentPhoto,
            onCheckedChange = { onValueChange(value.copy(keepCurrentPhoto = it)) },
        )

        WarningSign(
            text = stringResource(id = R.string.photo_widget_configure_tap_action_shared_preferences),
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun ViewInGalleryCustomizationContent(
    value: PhotoWidgetTapAction.ViewInGallery,
    onChooseGalleryAppClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(
                        stringResource(R.string.photo_widget_configure_tap_action_gallery_description_warning),
                    )
                }
                append(" ")
                append(
                    stringResource(R.string.photo_widget_configure_tap_action_gallery_description_app_selection),
                )
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )

        AppPicker(
            value = value.galleryApp,
            onChooseAppClick = onChooseGalleryAppClick,
        )

        WarningSign(
            text = stringResource(id = R.string.photo_widget_configure_tap_action_shared_selected_app),
        )
    }
}

@Composable
private fun ToggleCyclingCustomizationContent(
    value: PhotoWidgetTapAction.ToggleCycling,
    onValueChange: (PhotoWidgetTapAction.ToggleCycling) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(id = R.string.photo_widget_configure_tap_action_toggle_cycling_description),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )

        Toggle(
            title = stringResource(id = R.string.photo_widget_configure_tap_action_disable_tap),
            checked = value.disableTap,
            onCheckedChange = { onValueChange(value.copy(disableTap = it)) },
        )

        WarningSign(
            text = stringResource(id = R.string.photo_widget_configure_tap_action_shared_preferences),
        )
    }
}

@Composable
private fun AppFolderCustomizationContent(
    value: PhotoWidgetTapAction.AppFolder,
    onValueChange: (PhotoWidgetTapAction.AppFolder) -> Unit,
    onAddAppClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val localContext: Context = LocalContext.current
    val localHaptics: HapticFeedback = LocalHapticFeedback.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        ReorderableColumn(
            list = remember(value, localContext) {
                value.toIconLabelPair(context = localContext)
            },
            onSettle = { fromIndex: Int, toIndex: Int ->
                onValueChange(
                    value.copy(
                        shortcuts = value.shortcuts.toMutableList().apply {
                            add(index = toIndex, element = removeAt(fromIndex))
                        },
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) ReorderableColumnItem@{ index: Int, item: Pair<Drawable, String>, isDragging: Boolean ->
            ReorderableItem {
                val (appIcon: Drawable, appLabel: String) = item
                val topCorner: Dp by animateDpAsState(
                    targetValue = when {
                        index == 0 || isDragging -> 12.dp
                        else -> 2.dp
                    },
                )
                val bottomCorner: Dp by animateDpAsState(
                    targetValue = when {
                        (index == value.shortcuts.lastIndex && value.shortcuts.size == 12) || isDragging -> 12.dp
                        else -> 2.dp
                    },
                )

                AppFolderCustomizationItem(
                    label = appLabel,
                    modifier = Modifier.longPressDraggableHandle(
                        onDragStarted = {
                            localHaptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                        },
                        onDragStopped = {
                            localHaptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
                        },
                    ),
                    leadingIcon = {
                        Image(
                            bitmap = appIcon.toBitmap().asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                onValueChange(value.copy(shortcuts = value.shortcuts - value.shortcuts[index]))
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_trash),
                                contentDescription = null,
                            )
                        }
                    },
                    backgroundShape = RoundedCornerShape(
                        topStart = topCorner,
                        topEnd = topCorner,
                        bottomStart = bottomCorner,
                        bottomEnd = bottomCorner,
                    ),
                )
            }
        }

        if (value.shortcuts.size < 12) {
            AppFolderCustomizationItem(
                label = stringResource(R.string.photo_widget_app_folder_add_app),
                modifier = Modifier.clickable(onClick = onAddAppClick),
                backgroundShape = if (value.shortcuts.isEmpty()) {
                    MaterialTheme.shapes.medium
                } else {
                    MaterialTheme.shapes.medium.copy(
                        topStart = CornerSize(2.dp),
                        topEnd = CornerSize(2.dp),
                    )
                },
                labelTextAlign = TextAlign.Center,
                labelTextStyle = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun AppFolderCustomizationItem(
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (RowScope.() -> Unit)? = null,
    trailingIcon: @Composable (RowScope.() -> Unit)? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    backgroundShape: Shape = MaterialTheme.shapes.medium,
    labelTextAlign: TextAlign = TextAlign.Left,
    labelTextStyle: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    Row(
        modifier = modifier
            .background(color = backgroundColor, shape = backgroundShape)
            .minimumInteractiveComponentSize()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColorFor(backgroundColor)) {
            if (leadingIcon != null) {
                leadingIcon()
            }

            AutoSizeText(
                text = label,
                modifier = Modifier.weight(1f),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                textAlign = labelTextAlign,
                style = labelTextStyle,
            )

            if (trailingIcon != null) {
                trailingIcon()
            }
        }
    }
}
// endregion Customization Content

// region Previews
@Composable
@AllPreviews
private fun PhotoWidgetTapActionPickerPreview() {
    ExtendedTheme {
        TapActionPickerContent(
            onNavClick = {},
            selectedArea = TapActionArea.CENTER,
            onSelectedAreaChange = {},
            currentTapAction = PhotoWidgetTapAction.DEFAULT,
            onTapActionChange = {},
            onChooseAppShortcutClick = {},
            onAddAppToFolderClick = {},
            onChooseGalleryAppClick = {},
            onApplyClick = {},
        )
    }
}
// endregion Previews
