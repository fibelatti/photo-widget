package com.fibelatti.photowidget.configure

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.AppFolderResolvedEntry
import com.fibelatti.photowidget.model.AppFolderShortcut
import com.fibelatti.photowidget.model.AppShortcutInfo
import com.fibelatti.photowidget.model.InstalledApp
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import com.fibelatti.photowidget.model.PhotoWidgetTapActions
import com.fibelatti.photowidget.model.TapActionArea
import com.fibelatti.photowidget.model.contains
import com.fibelatti.photowidget.platform.getAllInstalledApps
import com.fibelatti.photowidget.platform.getAppShortcuts
import com.fibelatti.photowidget.platform.getInstalledApp
import com.fibelatti.photowidget.platform.resolveAppFolderEntries
import com.fibelatti.photowidget.platform.withRoundedCorners
import com.fibelatti.photowidget.ui.BooleanListItem
import com.fibelatti.photowidget.ui.DefaultSheetContent
import com.fibelatti.photowidget.ui.InformationalPanel
import com.fibelatti.photowidget.ui.PickerListItem
import com.fibelatti.photowidget.ui.icons.AppIcons
import com.fibelatti.photowidget.ui.icons.Back
import com.fibelatti.photowidget.ui.icons.Trash
import com.fibelatti.photowidget.ui.rememberSampleBitmap
import com.fibelatti.ui.component.AppBottomSheet
import com.fibelatti.ui.component.AppSheetState
import com.fibelatti.ui.component.AutoSizeText
import com.fibelatti.ui.component.ConnectedButtonRowItem
import com.fibelatti.ui.component.ListItem
import com.fibelatti.ui.component.RadioGroup
import com.fibelatti.ui.component.SelectionDialogBottomSheet
import com.fibelatti.ui.component.rememberAppSheetState
import com.fibelatti.ui.foundation.Shapes
import com.fibelatti.ui.foundation.dpToPx
import com.fibelatti.ui.foundation.fadingEdges
import com.fibelatti.ui.preview.PreviewAll
import com.fibelatti.ui.preview.PreviewLocales
import com.fibelatti.ui.theme.ExtendedTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sh.calvin.reorderable.ReorderableColumn

@Composable
fun PhotoWidgetTapActionPicker(
    onNavClick: () -> Unit,
    currentTapActions: PhotoWidgetTapActions,
    source: PhotoWidgetSource,
    transparent: Boolean,
    onApplyClick: (newTapActions: PhotoWidgetTapActions) -> Unit,
) {
    val context: Context = LocalContext.current

    var tapActions: PhotoWidgetTapActions by rememberSaveable { mutableStateOf(currentTapActions) }
    var selectedArea: TapActionArea by rememberSaveable { mutableStateOf(TapActionArea.CENTER) }
    var pendingShortcutPackage: String? by rememberSaveable { mutableStateOf(null) }
    var pendingFolderShortcutPackage: String? by rememberSaveable { mutableStateOf(null) }

    val appPickerSheetState: AppSheetState = rememberAppSheetState()
    val appShortcutPickerSheetState: AppSheetState = rememberAppSheetState()
    val addToFolderPickerSheetState: AppSheetState = rememberAppSheetState()
    val folderShortcutPickerSheetState: AppSheetState = rememberAppSheetState()
    val galleryAppPickerSheetState: AppSheetState = rememberAppSheetState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        tapActions = onFilePickerResult(
            context = context,
            uri = uri,
            tapActions = tapActions,
            selectedArea = selectedArea,
        )
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        tapActions = onFolderPickerResult(
            context = context,
            uri = uri,
            tapActions = tapActions,
            selectedArea = selectedArea,
        )
    }

    val launcherApps: List<InstalledApp> by produceInstalledAppsState(
        context = context,
        queryIntent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER),
        key1 = tapActions,
        shouldLoad = {
            PhotoWidgetTapAction.AppShortcut::class in tapActions || PhotoWidgetTapAction.AppFolder::class in tapActions
        },
    )
    val galleryApps: List<InstalledApp> by produceInstalledAppsState(
        context = context,
        queryIntent = Intent(Intent.ACTION_VIEW)
            .setDataAndType("content://sample".toUri(), "image/*"),
        key1 = tapActions,
        shouldLoad = { PhotoWidgetTapAction.ViewInGallery::class in tapActions },
    )

    val shortcuts: List<AppShortcutInfo> by produceState(initialValue = emptyList(), key1 = pendingShortcutPackage) {
        val packageName: String = pendingShortcutPackage ?: return@produceState
        value = withContext(Dispatchers.IO) { context.getAppShortcuts(packageName) }

        if (value.isNotEmpty()) {
            appShortcutPickerSheetState.showBottomSheet()
        } else {
            pendingShortcutPackage = null
        }
    }
    val folderShortcuts: List<AppShortcutInfo> by produceState(
        initialValue = emptyList(),
        key1 = pendingFolderShortcutPackage,
    ) {
        val packageName: String = pendingFolderShortcutPackage ?: return@produceState
        value = withContext(Dispatchers.IO) { context.getAppShortcuts(packageName) }

        if (value.isNotEmpty()) {
            folderShortcutPickerSheetState.showBottomSheet()
        } else {
            // Unlike the sibling `shortcuts` producer above (which only clears the pending state
            // and lets the caller add the app once it observes an empty shortcut list), this one
            // commits the app to the folder directly: adding to a folder is the action itself,
            // there's no separate "confirm" step waiting on `pendingFolderShortcutPackage` to
            // clear, so this is the only place left to add the app when it has no shortcuts.
            tapActions = onAppFolderPickerResult(
                packageName = packageName,
                shortcutId = null,
                tapActions = tapActions,
                selectedArea = selectedArea,
            )
            pendingFolderShortcutPackage = null
        }
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
            tapActions = onTapActionChange(
                originalActions = currentTapActions,
                tapActions = tapActions,
                newAction = newAction,
                selectedArea = selectedArea,
            )
        },
        source = source,
        transparent = transparent,
        onCopyFromClick = { sourceArea ->
            tapActions = onTapActionChange(
                originalActions = currentTapActions,
                tapActions = tapActions,
                newAction = when (sourceArea) {
                    TapActionArea.LEFT -> tapActions.left
                    TapActionArea.CENTER -> tapActions.center
                    TapActionArea.RIGHT -> tapActions.right
                },
                selectedArea = selectedArea,
            )
        },
        onChooseAppClick = { appPickerSheetState.showBottomSheet() },
        onChooseShortcutClick = { packageName -> pendingShortcutPackage = packageName },
        onAddAppToFolderClick = { addToFolderPickerSheetState.showBottomSheet() },
        onChooseGalleryAppClick = { galleryAppPickerSheetState.showBottomSheet() },
        onChooseFileClick = { filePickerLauncher.launch(arrayOf("*/*")) },
        onChooseFolderClick = { folderPickerLauncher.launch(null) },
        onApplyClick = { onApplyClick(tapActions) },
    )

    AppPickerBottomSheet(
        sheetState = appPickerSheetState,
        apps = launcherApps,
        onAppClick = { packageName: String ->
            pendingShortcutPackage = packageName
            tapActions = onAppShortcutPickerResult(
                packageName = packageName,
                shortcutId = null,
                tapActions = tapActions,
                selectedArea = selectedArea,
            )
        },
    )

    AppShortcutPickerBottomSheet(
        sheetState = appShortcutPickerSheetState,
        shortcuts = shortcuts,
        onLaunchAppClick = {
            tapActions = onAppShortcutPickerResult(
                packageName = pendingShortcutPackage ?: return@AppShortcutPickerBottomSheet,
                shortcutId = null,
                tapActions = tapActions,
                selectedArea = selectedArea,
            )
            pendingShortcutPackage = null
        },
        onShortcutClick = { shortcutId ->
            tapActions = onAppShortcutPickerResult(
                packageName = pendingShortcutPackage ?: return@AppShortcutPickerBottomSheet,
                shortcutId = shortcutId,
                tapActions = tapActions,
                selectedArea = selectedArea,
            )
            pendingShortcutPackage = null
        },
        onDismiss = {
            pendingShortcutPackage = null
        },
    )

    AppPickerBottomSheet(
        sheetState = addToFolderPickerSheetState,
        apps = launcherApps,
        onAppClick = { packageName ->
            pendingFolderShortcutPackage = packageName
        },
    )

    AppShortcutPickerBottomSheet(
        sheetState = folderShortcutPickerSheetState,
        shortcuts = folderShortcuts,
        onLaunchAppClick = {
            tapActions = onAppFolderPickerResult(
                packageName = pendingFolderShortcutPackage ?: return@AppShortcutPickerBottomSheet,
                shortcutId = null,
                tapActions = tapActions,
                selectedArea = selectedArea,
            )
            pendingFolderShortcutPackage = null
        },
        onShortcutClick = { shortcutId ->
            tapActions = onAppFolderPickerResult(
                packageName = pendingFolderShortcutPackage ?: return@AppShortcutPickerBottomSheet,
                shortcutId = shortcutId,
                tapActions = tapActions,
                selectedArea = selectedArea,
            )
            pendingFolderShortcutPackage = null
        },
        onDismiss = {
            pendingFolderShortcutPackage = null
        },
    )

    AppPickerBottomSheet(
        sheetState = galleryAppPickerSheetState,
        apps = galleryApps,
        onAppClick = { packageName ->
            tapActions = onGalleryAppPickerResult(
                packageName = packageName,
                tapActions = tapActions,
            )
        },
    )
}

@Composable
private fun produceInstalledAppsState(
    context: Context,
    queryIntent: Intent,
    key1: Any?,
    shouldLoad: () -> Boolean,
): State<List<InstalledApp>> {
    val shouldLoadState by rememberUpdatedState(shouldLoad)

    return produceState(initialValue = emptyList(), key1 = key1) {
        if (shouldLoadState() && value.isEmpty()) {
            value = withContext(Dispatchers.IO) { context.getAllInstalledApps(queryIntent) }
        }
    }
}

// region Activity Result handlers
private fun onAppShortcutPickerResult(
    packageName: String,
    shortcutId: String?,
    tapActions: PhotoWidgetTapActions,
    selectedArea: TapActionArea,
): PhotoWidgetTapActions {
    val newTapAction = PhotoWidgetTapAction.AppShortcut(
        appShortcut = packageName,
        shortcutId = shortcutId,
    )

    return when (selectedArea) {
        TapActionArea.LEFT -> tapActions.copy(left = newTapAction)
        TapActionArea.CENTER -> tapActions.copy(center = newTapAction)
        TapActionArea.RIGHT -> tapActions.copy(right = newTapAction)
    }
}

private fun onAppFolderPickerResult(
    packageName: String,
    shortcutId: String?,
    tapActions: PhotoWidgetTapActions,
    selectedArea: TapActionArea,
): PhotoWidgetTapActions {
    val current: PhotoWidgetTapAction.AppFolder = when (selectedArea) {
        TapActionArea.LEFT -> tapActions.left
        TapActionArea.CENTER -> tapActions.center
        TapActionArea.RIGHT -> tapActions.right
    } as? PhotoWidgetTapAction.AppFolder ?: return tapActions

    val entry: String = AppFolderShortcut(packageName = packageName, shortcutId = shortcutId).encoded()
    val newTapAction = current.copy(shortcuts = (current.shortcuts + entry).distinct())

    return when (selectedArea) {
        TapActionArea.LEFT -> tapActions.copy(left = newTapAction)
        TapActionArea.CENTER -> tapActions.copy(center = newTapAction)
        TapActionArea.RIGHT -> tapActions.copy(right = newTapAction)
    }
}

private fun onFilePickerResult(
    context: Context,
    uri: Uri?,
    tapActions: PhotoWidgetTapActions,
    selectedArea: TapActionArea,
): PhotoWidgetTapActions {
    if (uri == null) return tapActions

    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

    val newTapAction = PhotoWidgetTapAction.FileShortcut(fileUri = uri.toString())

    return when (selectedArea) {
        TapActionArea.LEFT -> tapActions.copy(left = newTapAction)
        TapActionArea.CENTER -> tapActions.copy(center = newTapAction)
        TapActionArea.RIGHT -> tapActions.copy(right = newTapAction)
    }
}

private fun onGalleryAppPickerResult(
    packageName: String,
    tapActions: PhotoWidgetTapActions,
): PhotoWidgetTapActions {
    val newTapAction = PhotoWidgetTapAction.ViewInGallery(packageName)

    val apply: (PhotoWidgetTapAction) -> PhotoWidgetTapAction = { action ->
        if (action is PhotoWidgetTapAction.ViewInGallery) newTapAction else action
    }

    return tapActions.copy(
        left = apply(tapActions.left),
        center = apply(tapActions.center),
        right = apply(tapActions.right),
    )
}

private fun onFolderPickerResult(
    context: Context,
    uri: Uri?,
    tapActions: PhotoWidgetTapActions,
    selectedArea: TapActionArea,
): PhotoWidgetTapActions {
    if (uri == null) return tapActions

    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

    val newTapAction = PhotoWidgetTapAction.FolderShortcut(folderUri = uri.toString())

    return when (selectedArea) {
        TapActionArea.LEFT -> tapActions.copy(left = newTapAction)
        TapActionArea.CENTER -> tapActions.copy(center = newTapAction)
        TapActionArea.RIGHT -> tapActions.copy(right = newTapAction)
    }
}
// endregion Activity Result handlers

// region Change handler
private fun onTapActionChange(
    originalActions: PhotoWidgetTapActions,
    tapActions: PhotoWidgetTapActions,
    newAction: PhotoWidgetTapAction,
    selectedArea: TapActionArea,
): PhotoWidgetTapActions {
    val currentAction: PhotoWidgetTapAction = when (selectedArea) {
        TapActionArea.LEFT -> tapActions.left
        TapActionArea.CENTER -> tapActions.center
        TapActionArea.RIGHT -> tapActions.right
    }

    val resultingAction: PhotoWidgetTapAction = when {
        currentAction::class == newAction::class -> newAction

        newAction.sharesPreferences -> when {
            tapActions.left::class == newAction::class -> tapActions.left
            tapActions.center::class == newAction::class -> tapActions.center
            tapActions.right::class == newAction::class -> tapActions.right
            else -> restoreOrNew(originalActions = originalActions, newAction = newAction, selectedArea = selectedArea)
        }

        else -> restoreOrNew(originalActions = originalActions, newAction = newAction, selectedArea = selectedArea)
    }

    val apply: (TapActionArea, PhotoWidgetTapAction) -> PhotoWidgetTapAction = { expectedArea, tapAction ->
        if (selectedArea == expectedArea) {
            resultingAction
        } else {
            keepActionsSynced(newAction = resultingAction, otherAction = tapAction)
        }
    }

    return tapActions.copy(
        left = apply(TapActionArea.LEFT, tapActions.left),
        center = apply(TapActionArea.CENTER, tapActions.center),
        right = apply(TapActionArea.RIGHT, tapActions.right),
    )
}

private fun restoreOrNew(
    originalActions: PhotoWidgetTapActions,
    newAction: PhotoWidgetTapAction,
    selectedArea: TapActionArea,
): PhotoWidgetTapAction {
    return when (selectedArea) {
        TapActionArea.LEFT if newAction::class == originalActions.left::class -> originalActions.left
        TapActionArea.CENTER if newAction::class == originalActions.center::class -> originalActions.center
        TapActionArea.RIGHT if newAction::class == originalActions.right::class -> originalActions.right
        else -> newAction
    }
}

private fun keepActionsSynced(
    newAction: PhotoWidgetTapAction,
    otherAction: PhotoWidgetTapAction,
): PhotoWidgetTapAction {
    return if (newAction::class == otherAction::class && newAction.sharesPreferences) newAction else otherAction
}
// endregion Change handler

@Composable
private fun TapActionPickerContent(
    onNavClick: () -> Unit,
    selectedArea: TapActionArea,
    onSelectedAreaChange: (TapActionArea) -> Unit,
    currentTapAction: PhotoWidgetTapAction,
    onTapActionChange: (PhotoWidgetTapAction) -> Unit,
    source: PhotoWidgetSource,
    onCopyFromClick: (TapActionArea) -> Unit,
    onChooseAppClick: () -> Unit,
    onChooseShortcutClick: (packageName: String) -> Unit,
    onAddAppToFolderClick: () -> Unit,
    onChooseGalleryAppClick: () -> Unit,
    onChooseFileClick: () -> Unit,
    onChooseFolderClick: () -> Unit,
    onApplyClick: () -> Unit,
    transparent: Boolean = false,
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
                    imageVector = AppIcons.Back,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = 48.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (!transparent) {
                        stringResource(id = R.string.photo_widget_configure_tap_action)
                    } else {
                        stringResource(id = R.string.transparent_widget_label)
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                )

                Text(
                    text = if (!transparent) {
                        stringResource(id = R.string.photo_widget_configure_tap_action_description)
                    } else {
                        stringResource(id = R.string.photo_widget_configure_tap_action_description_transparent)
                    },
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
                .fadingEdges(scrollState = scrollState)
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
                selectedArea = selectedArea,
                source = source,
                transparent = transparent,
                onCopyFromClick = onCopyFromClick,
                modifier = Modifier.fillMaxWidth(),
            )

            TapActionCustomizationContent(
                tapAction = currentTapAction,
                onTapActionChange = onTapActionChange,
                onChooseAppClick = onChooseAppClick,
                onChooseShortcutClick = onChooseShortcutClick,
                onAddAppToFolderClick = onAddAppToFolderClick,
                onChooseGalleryAppClick = onChooseGalleryAppClick,
                onChooseFileClick = onChooseFileClick,
                onChooseFolderClick = onChooseFolderClick,
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

// region Components
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
                label = stringResource(area.label),
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
                .drawWithCache {
                    val cornerRadius = CornerRadius(areaRadius)
                    val stroke = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                    )
                    onDrawWithContent {
                        drawRoundRect(
                            color = areaColor,
                            cornerRadius = cornerRadius,
                            style = stroke,
                        )
                        drawContent()
                    }
                }
                .clip(shape = RoundedCornerShape(size = areaRadius)),
        ) {
            Box(
                modifier = Modifier
                    .weight(3f)
                    .fillMaxHeight()
                    .background(color = if (tapActionArea == TapActionArea.LEFT) color else Color.Transparent),
            )

            Box(
                modifier = Modifier
                    .weight(4f)
                    .fillMaxHeight()
                    .background(color = if (tapActionArea == TapActionArea.CENTER) color else Color.Transparent),
            )

            Box(
                modifier = Modifier
                    .weight(3f)
                    .fillMaxHeight()
                    .background(color = if (tapActionArea == TapActionArea.RIGHT) color else Color.Transparent),
            )
        }
    }
}

@Composable
private fun TapOptionsPicker(
    currentTapAction: PhotoWidgetTapAction,
    onTapActionClick: (PhotoWidgetTapAction) -> Unit,
    selectedArea: TapActionArea,
    source: PhotoWidgetSource,
    onCopyFromClick: (TapActionArea) -> Unit,
    modifier: Modifier = Modifier,
    transparent: Boolean = false,
) {
    val tapActionSheetState: AppSheetState = rememberAppSheetState()
    val copyFromSheetState: AppSheetState = rememberAppSheetState()
    val localResources: Resources = LocalResources.current

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        PickerListItem(
            headlineText = stringResource(R.string.photo_widget_configure_tap_action),
            currentValue = stringResource(currentTapAction.label),
            onClick = tapActionSheetState::showBottomSheet,
            modifier = Modifier.fillMaxWidth(),
            trailingContent = {
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    VerticalDivider(modifier = Modifier.fillMaxHeight())

                    TextButton(
                        onClick = { copyFromSheetState.showBottomSheet() },
                    ) {
                        AutoSizeText(
                            text = stringResource(R.string.photo_widget_configure_tap_action_copy_from),
                            modifier = Modifier.heightIn(max = 80.dp),
                            maxLines = 1,
                        )
                    }
                }
            },
            shape = Shapes.StandaloneShape,
        )
    }

    AppBottomSheet(
        sheetState = tapActionSheetState,
    ) {
        DefaultSheetContent(
            title = stringResource(R.string.photo_widget_configure_tap_action),
        ) {
            RadioGroup(
                items = if (transparent) {
                    PhotoWidgetTapAction.entriesForTransparent()
                } else {
                    PhotoWidgetTapAction.entriesForSource(source = source)
                },
                itemSelected = { action -> action::class == currentTapAction::class },
                onItemClick = { selection ->
                    onTapActionClick(selection)
                    tapActionSheetState.hideBottomSheet()
                },
                itemTitle = { action -> localResources.getString(action.label) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                itemDescription = itemDescription@{ action ->
                    val descriptionRes: Int = when (action) {
                        is PhotoWidgetTapAction.AppFolder -> {
                            R.string.photo_widget_configure_tap_action_app_folder_description
                        }

                        is PhotoWidgetTapAction.RemovePhoto -> {
                            R.string.photo_widget_configure_tap_action_remove_photo_description
                        }

                        else -> return@itemDescription null
                    }

                    localResources.getString(descriptionRes)
                },
            )
        }
    }

    SelectionDialogBottomSheet(
        sheetState = copyFromSheetState,
        title = stringResource(R.string.photo_widget_configure_tap_action_copy_from),
        options = TapActionArea.entries - selectedArea,
        optionName = { option -> localResources.getString(option.label) },
        onOptionSelect = onCopyFromClick,
    )
}
// endregion Components

// region Customization Content
@Composable
private fun TapActionCustomizationContent(
    tapAction: PhotoWidgetTapAction,
    onTapActionChange: (PhotoWidgetTapAction) -> Unit,
    onChooseAppClick: () -> Unit,
    onChooseShortcutClick: (packageName: String) -> Unit,
    onAddAppToFolderClick: () -> Unit,
    onChooseGalleryAppClick: () -> Unit,
    onChooseFileClick: () -> Unit,
    onChooseFolderClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (tapAction) {
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

        is PhotoWidgetTapAction.ToggleCycling -> {
            ToggleCyclingCustomizationContent(
                value = tapAction,
                onValueChange = onTapActionChange,
                modifier = modifier,
            )
        }

        is PhotoWidgetTapAction.AppShortcut -> {
            AppPicker(
                packageName = tapAction.appShortcut,
                shortcutId = tapAction.shortcutId,
                onChooseAppClick = onChooseAppClick,
                onChooseShortcutClick = onChooseShortcutClick,
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
                modifier = modifier.heightIn(min = ListItem.MinHeight),
                placeholder = { Text(text = "https://...") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                singleLine = true,
                shape = Shapes.StandaloneShape,
            )
        }

        is PhotoWidgetTapAction.FileShortcut -> {
            FileShortcutCustomizationContent(
                value = tapAction,
                onChooseFileClick = onChooseFileClick,
                modifier = modifier,
            )
        }

        is PhotoWidgetTapAction.FolderShortcut -> {
            FolderShortcutCustomizationContent(
                value = tapAction,
                onChooseFolderClick = onChooseFolderClick,
                modifier = modifier,
            )
        }

        else -> return // No content
    }
}

@Composable
private fun AppPicker(
    packageName: String?,
    onChooseAppClick: () -> Unit,
    modifier: Modifier = Modifier,
    shortcutId: String? = null,
    onChooseShortcutClick: ((packageName: String) -> Unit)? = null,
) {
    val localContext: Context = LocalContext.current

    val installedApp: InstalledApp? = remember(packageName) { localContext.getInstalledApp(packageName) }
    val shortcut: AppShortcutInfo? by produceState(initialValue = null, key1 = packageName, key2 = shortcutId) {
        value = if (packageName != null && shortcutId != null) {
            withContext(Dispatchers.IO) { localContext.getAppShortcuts(packageName) }
                .find { it.id == shortcutId }
        } else {
            null
        }
    }

    ListItem(
        headlineText = installedApp?.appLabel
            ?: stringResource(id = R.string.photo_widget_configure_tap_action_choose_app),
        supportingText = shortcut?.label ?: shortcutId,
        modifier = modifier
            .fillMaxWidth()
            .clip(ListItem.DefaultShape)
            .clickable(onClick = onChooseAppClick, role = Role.Button),
        trailingContent = trailingContent@{
            val icon: Drawable = shortcut?.icon ?: installedApp?.appIcon ?: return@trailingContent

            Image(
                bitmap = remember(icon) { icon.toBitmap().asImageBitmap() },
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(
                        enabled = packageName != null && onChooseShortcutClick != null,
                        role = Role.Button,
                    ) {
                        if (packageName != null && onChooseShortcutClick != null) {
                            onChooseShortcutClick(packageName)
                        }
                    },
            )
        },
    )
}

@Composable
private fun ViewFullScreenCustomizationContent(
    value: PhotoWidgetTapAction.ViewFullScreen,
    onValueChange: (PhotoWidgetTapAction.ViewFullScreen) -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColorSheetState: AppSheetState = rememberAppSheetState()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        BooleanListItem(
            headlineText = stringResource(id = R.string.photo_widget_configure_tap_action_increase_brightness),
            currentValue = value.increaseBrightness,
            onValueChange = { onValueChange(value.copy(increaseBrightness = it)) },
            shape = Shapes.TopShape,
        )

        BooleanListItem(
            headlineText = stringResource(R.string.photo_widget_configure_tap_action_view_original_photo),
            currentValue = value.viewOriginalPhoto,
            onValueChange = { onValueChange(value.copy(viewOriginalPhoto = it)) },
            shape = Shapes.MiddleShape,
        )

        BooleanListItem(
            headlineText = stringResource(R.string.photo_widget_configure_tap_action_do_not_shuffle),
            currentValue = value.noShuffle,
            onValueChange = { onValueChange(value.copy(noShuffle = it)) },
            shape = Shapes.MiddleShape,
        )

        BooleanListItem(
            headlineText = stringResource(R.string.photo_widget_configure_tap_action_keep_current_photo),
            currentValue = value.keepCurrentPhoto,
            onValueChange = { onValueChange(value.copy(keepCurrentPhoto = it)) },
            shape = Shapes.MiddleShape,
        )

        PickerListItem(
            headlineText = stringResource(R.string.photo_widget_configure_tap_action_viewer_background_color),
            currentValue = value.backgroundColorHex?.let { "#$it" }
                ?: stringResource(R.string.photo_widget_configure_tap_action_viewer_background_color_default),
            onClick = backgroundColorSheetState::showBottomSheet,
            modifier = Modifier.fillMaxWidth(),
            shape = Shapes.BottomShape,
        )

        Spacer(modifier = Modifier.height(6.dp))

        InformationalPanel(
            text = stringResource(id = R.string.photo_widget_configure_tap_action_shared_preferences),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        )
    }

    PhotoWidgetViewerBackgroundColorBottomSheet(
        sheetState = backgroundColorSheetState,
        currentColorHex = value.backgroundColorHex,
        onApplyClick = { hex -> onValueChange(value.copy(backgroundColorHex = hex)) },
        onResetClick = { onValueChange(value.copy(backgroundColorHex = null)) },
    )
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )

        OpenWithPicker(
            packageName = value.galleryApp,
            onChooseAppClick = onChooseGalleryAppClick,
        )

        InformationalPanel(
            text = stringResource(id = R.string.photo_widget_configure_tap_action_shared_preferences),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        )
    }
}

@Composable
private fun OpenWithPicker(
    packageName: String?,
    onChooseAppClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val localContext: Context = LocalContext.current
    val installedApp: InstalledApp? = remember(packageName) { localContext.getInstalledApp(packageName) }

    ListItem(
        headlineText = stringResource(R.string.photo_widget_configure_tap_action_open_with),
        modifier = modifier
            .fillMaxWidth()
            .clip(ListItem.DefaultShape)
            .clickable(onClick = onChooseAppClick, role = Role.Button),
        supportingText = installedApp?.appLabel,
        trailingContent = trailingContent@{
            val icon: Drawable = installedApp?.appIcon ?: return@trailingContent

            Image(
                bitmap = remember(icon) { icon.toBitmap().asImageBitmap() },
                contentDescription = null,
                modifier = Modifier.size(40.dp),
            )
        },
    )
}

@Composable
private fun ToggleCyclingCustomizationContent(
    value: PhotoWidgetTapAction.ToggleCycling,
    onValueChange: (PhotoWidgetTapAction.ToggleCycling) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(id = R.string.photo_widget_configure_tap_action_toggle_cycling_description),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )

        BooleanListItem(
            headlineText = stringResource(id = R.string.photo_widget_configure_tap_action_disable_tap),
            currentValue = value.disableTap,
            onValueChange = { onValueChange(value.copy(disableTap = it)) },
        )

        InformationalPanel(
            text = stringResource(id = R.string.photo_widget_configure_tap_action_shared_preferences),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        )
    }
}

@Composable
private fun FileShortcutCustomizationContent(
    value: PhotoWidgetTapAction.FileShortcut,
    onChooseFileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineText = stringResource(id = R.string.photo_widget_configure_tap_action_choose_file),
        modifier = modifier
            .fillMaxWidth()
            .clip(ListItem.DefaultShape)
            .clickable(onClick = onChooseFileClick, role = Role.Button),
        // lastPathSegment only takes care of the URI structure, the file can be in a nested directory
        supportingText = value.fileUri?.toUri()?.lastPathSegment.orEmpty().substringAfterLast("/"),
    )
}

@Composable
private fun FolderShortcutCustomizationContent(
    value: PhotoWidgetTapAction.FolderShortcut,
    onChooseFolderClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ListItem(
            headlineText = stringResource(id = R.string.photo_widget_configure_tap_action_choose_folder),
            modifier = Modifier
                .fillMaxWidth()
                .clip(Shapes.TopShape)
                .clickable(onClick = onChooseFolderClick, role = Role.Button),
            // lastPathSegment of a tree URI holds the document id (e.g. "primary:Pictures").
            supportingText = value.folderUri?.toUri()?.lastPathSegment.orEmpty(),
        )

        InformationalPanel(
            text = stringResource(R.string.photo_widget_configure_tap_action_choose_folder_description),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
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

    val items: List<AppFolderResolvedEntry> by produceState(initialValue = emptyList(), key1 = value.shortcuts) {
        this.value = withContext(Dispatchers.IO) { localContext.resolveAppFolderEntries(value.shortcuts) }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ReorderableColumn(
            list = items,
            onSettle = { fromIndex: Int, toIndex: Int ->
                // Resolved entries can be a subset of value.shortcuts (e.g. an uninstalled app's
                // entry is dropped during resolution), so fromIndex/toIndex from the displayed
                // list don't necessarily match positions in the persisted list. Reorder by entry
                // identity instead of by index to keep both lists consistent.
                val movedEntry: String = items[fromIndex].encoded
                val targetEntry: String = items[toIndex].encoded
                val mutableShortcuts: MutableList<String> = value.shortcuts.toMutableList()
                val actualFromIndex: Int = mutableShortcuts.indexOf(movedEntry)
                val actualToIndex: Int = mutableShortcuts.indexOf(targetEntry)

                if (actualFromIndex != -1 && actualToIndex != -1) {
                    onValueChange(
                        value.copy(
                            shortcuts = mutableShortcuts.apply {
                                add(index = actualToIndex, element = removeAt(actualFromIndex))
                            },
                        ),
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) ReorderableColumnItem@{ index: Int, item: AppFolderResolvedEntry, isDragging: Boolean ->
            ReorderableItem {
                val topCorner: Dp by animateDpAsState(
                    targetValue = when {
                        index == 0 || isDragging -> 12.dp
                        else -> 2.dp
                    },
                )
                val bottomCorner: Dp by animateDpAsState(
                    targetValue = when {
                        (index == items.lastIndex && value.shortcuts.size == 12) || isDragging -> 12.dp
                        else -> 2.dp
                    },
                )

                val icon: Drawable = item.shortcut?.icon ?: item.app.appIcon

                AppFolderCustomizationItem(
                    title = item.shortcut?.label ?: item.app.appLabel,
                    subtitle = item.shortcut?.let { item.app.appLabel },
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
                            bitmap = remember(icon) { icon.toBitmap().asImageBitmap() },
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                onValueChange(value.copy(shortcuts = value.shortcuts - item.encoded))
                            },
                        ) {
                            Icon(
                                imageVector = AppIcons.Trash,
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
                title = stringResource(R.string.photo_widget_app_folder_add_app),
                modifier = Modifier.clickable(onClick = onAddAppClick),
                backgroundShape = if (value.shortcuts.isEmpty()) Shapes.StandaloneShape else Shapes.BottomShape,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AppFolderCustomizationItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    backgroundShape: Shape = MaterialTheme.shapes.medium,
    textAlign: TextAlign = TextAlign.Left,
) {
    val colors: ListItemColors = ListItemDefaults.colors()

    CompositionLocalProvider(LocalContentColor provides colors.contentColor) {
        ListItem(
            headlineContent = {
                AutoSizeText(
                    text = title,
                    modifier = Modifier.fillMaxWidth(),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    textAlign = textAlign,
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            modifier = modifier
                .clip(backgroundShape)
                .heightIn(min = ListItem.MinHeight),
            supportingContent = {
                if (subtitle != null) {
                    AutoSizeText(
                        text = subtitle,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        textAlign = textAlign,
                    )
                }
            },
            leadingContent = leadingIcon,
            trailingContent = trailingIcon,
            colors = colors,
            tonalElevation = 4.dp,
        )
    }
}
// endregion Customization Content

// region Previews
@Composable
@PreviewAll
private fun PhotoWidgetTapActionPickerPreview() {
    ExtendedTheme {
        TapActionPickerContent(
            onNavClick = {},
            selectedArea = TapActionArea.CENTER,
            onSelectedAreaChange = {},
            currentTapAction = PhotoWidgetTapAction.DEFAULT,
            onTapActionChange = {},
            source = PhotoWidgetSource.PHOTOS,
            onCopyFromClick = {},
            onChooseAppClick = {},
            onChooseShortcutClick = {},
            onAddAppToFolderClick = {},
            onChooseGalleryAppClick = {},
            onChooseFileClick = {},
            onChooseFolderClick = {},
            onApplyClick = {},
        )
    }
}

@Composable
@PreviewLocales
private fun TapOptionsPickerPreview(
    @PreviewParameter(TapActionPreviewParameterProvider::class) tapAction: PhotoWidgetTapAction,
) {
    ExtendedTheme {
        TapOptionsPicker(
            currentTapAction = tapAction,
            onTapActionClick = {},
            selectedArea = TapActionArea.CENTER,
            source = PhotoWidgetSource.PHOTOS,
            onCopyFromClick = {},
        )
    }
}

private class TapActionPreviewParameterProvider : PreviewParameterProvider<PhotoWidgetTapAction> {

    override val values: Sequence<PhotoWidgetTapAction> = PhotoWidgetTapAction.entries.asSequence()

    override fun getDisplayName(index: Int): String {
        return PhotoWidgetTapAction.entries[index].serializedName
    }
}

// endregion Previews
