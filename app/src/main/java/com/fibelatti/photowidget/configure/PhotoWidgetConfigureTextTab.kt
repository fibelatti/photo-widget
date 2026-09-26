package com.fibelatti.photowidget.configure

import android.graphics.Typeface
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint
import com.fibelatti.photowidget.model.DynamicColorType
import com.fibelatti.photowidget.model.MatchPhotoColorType
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetText
import com.fibelatti.photowidget.model.PhotoWidgetTextColor
import com.fibelatti.photowidget.platform.GoogleFontsLoader
import com.fibelatti.photowidget.ui.DefaultSheetContent
import com.fibelatti.photowidget.ui.DefaultSheetFooterButtons
import com.fibelatti.photowidget.ui.InformationalPanel
import com.fibelatti.photowidget.ui.LocalSamplePhoto
import com.fibelatti.photowidget.ui.WidgetPositionViewer
import com.fibelatti.ui.component.AppBottomSheet
import com.fibelatti.ui.component.AppSheetState
import com.fibelatti.ui.component.BooleanListItem
import com.fibelatti.ui.component.ListItem
import com.fibelatti.ui.component.NumberSpinner
import com.fibelatti.ui.component.PickerListItem
import com.fibelatti.ui.component.RadioGroup
import com.fibelatti.ui.component.rememberAppSheetState
import com.fibelatti.ui.foundation.Shapes
import com.fibelatti.ui.foundation.fadingEdges
import com.fibelatti.ui.icons.AppIcons
import com.fibelatti.ui.icons.Trash
import com.fibelatti.ui.preview.PreviewAll
import com.fibelatti.ui.theme.ExtendedTheme
import kotlinx.coroutines.launch

@Composable
fun PhotoWidgetConfigureTextTab(
    viewModel: PhotoWidgetConfigureViewModel,
    modifier: Modifier = Modifier,
) {
    val state: PhotoWidgetConfigureState by viewModel.state.collectAsStateWithLifecycle()

    PhotoWidgetConfigureTextTab(
        photoWidgetText = state.photoWidget.text,
        onPhotoWidgetTextChange = viewModel::photoWidgetTextChanged,
        modifier = modifier,
    )
}

@Composable
fun PhotoWidgetConfigureTextTab(
    photoWidgetText: PhotoWidgetText,
    onPhotoWidgetTextChange: (PhotoWidgetText) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textTypeSheetState: AppSheetState = rememberAppSheetState()
    val textValueSheetState: AppSheetState = rememberAppSheetState()
    val colorSheetState: AppSheetState = rememberAppSheetState()
    val fontSheetState: AppSheetState = rememberAppSheetState()
    val textSizeSheetState: AppSheetState = rememberAppSheetState()
    val verticalOffsetSheetState: AppSheetState = rememberAppSheetState()

    val isGoogleFontsAvailable: Boolean = rememberIsGoogleFontsAvailable()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        PickerListItem(
            headlineText = stringResource(R.string.photo_widget_configure_text_type),
            currentValue = stringResource(
                when (photoWidgetText) {
                    is PhotoWidgetText.None -> R.string.photo_widget_configure_text_type_none
                    is PhotoWidgetText.Label -> R.string.photo_widget_configure_text_type_label
                },
            ),
            onClick = textTypeSheetState::showBottomSheet,
            shape = if (photoWidgetText is PhotoWidgetText.None) Shapes.StandaloneShape else Shapes.TopShape,
        )

        when (photoWidgetText) {
            is PhotoWidgetText.None -> Unit

            is PhotoWidgetText.Label -> {
                PickerListItem(
                    headlineText = stringResource(R.string.photo_widget_configure_text_value),
                    currentValue = photoWidgetText.value,
                    onClick = textValueSheetState::showBottomSheet,
                    shape = Shapes.MiddleShape,
                )

                PickerListItem(
                    headlineText = stringResource(R.string.photo_widget_configure_text_color),
                    currentValue = buildString {
                        append(stringResource(photoWidgetText.color.label))

                        when (photoWidgetText.color) {
                            is PhotoWidgetTextColor.Colored -> {
                                append(" (#${photoWidgetText.color.colorHex.toUpperCase(Locale.current)})")
                            }

                            is PhotoWidgetTextColor.Dynamic -> {
                                append(" (${stringResource(photoWidgetText.color.type.label)})")
                            }

                            is PhotoWidgetTextColor.Palette -> {
                                append(" (${stringResource(photoWidgetText.color.type.label)})")
                            }
                        }
                    },
                    onClick = colorSheetState::showBottomSheet,
                    shape = Shapes.MiddleShape,
                )

                if (isGoogleFontsAvailable) {
                    PickerListItem(
                        headlineText = stringResource(R.string.photo_widget_configure_text_font),
                        currentValue = photoWidgetText.fontFamily
                            ?: stringResource(R.string.photo_widget_configure_text_font_default),
                        onClick = fontSheetState::showBottomSheet,
                        shape = Shapes.MiddleShape,
                    )
                }

                PickerListItem(
                    headlineText = stringResource(R.string.photo_widget_configure_text_size),
                    currentValue = photoWidgetText.size.toString(),
                    onClick = textSizeSheetState::showBottomSheet,
                    shape = Shapes.MiddleShape,
                )

                PickerListItem(
                    headlineText = stringResource(R.string.photo_widget_configure_text_vertical_offset),
                    currentValue = photoWidgetText.verticalOffset.toString(),
                    onClick = verticalOffsetSheetState::showBottomSheet,
                    shape = Shapes.MiddleShape,
                )

                BooleanListItem(
                    headlineText = stringResource(R.string.photo_widget_configure_text_apply_shadow),
                    currentValue = photoWidgetText.hasShadow,
                    onValueChange = { newValue ->
                        onPhotoWidgetTextChange(photoWidgetText.copy(hasShadow = newValue))
                    },
                    shape = Shapes.BottomShape,
                )

                Spacer(modifier = Modifier.height(8.dp))

                InformationalPanel(
                    text = stringResource(R.string.photo_widget_configure_text_caveat),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }

    // region Sheets
    PhotoWidgetTextTypePicker(
        appSheetState = textTypeSheetState,
        currentValue = photoWidgetText,
        onOptionSelect = onPhotoWidgetTextChange,
    )

    PhotoWidgetTextValuePicker(
        appSheetState = textValueSheetState,
        currentValue = photoWidgetText.value,
        onApplyClick = { newValue: String ->
            when (photoWidgetText) {
                is PhotoWidgetText.None -> Unit
                is PhotoWidgetText.Label -> onPhotoWidgetTextChange(photoWidgetText.copy(value = newValue))
            }
        },
    )

    PhotoWidgetTextColorPicker(
        appSheetState = colorSheetState,
        currentValue = photoWidgetText.color,
        fontFamily = photoWidgetText.fontFamily,
        onApplyClick = { newValue: PhotoWidgetTextColor ->
            when (photoWidgetText) {
                is PhotoWidgetText.None -> Unit
                is PhotoWidgetText.Label -> onPhotoWidgetTextChange(photoWidgetText.copy(color = newValue))
            }
        },
    )

    PhotoWidgetTextFontPicker(
        appSheetState = fontSheetState,
        currentValue = photoWidgetText.fontFamily,
        text = photoWidgetText.value,
        onOptionSelect = { newValue: String? ->
            when (photoWidgetText) {
                is PhotoWidgetText.None -> Unit
                is PhotoWidgetText.Label -> onPhotoWidgetTextChange(photoWidgetText.copy(fontFamily = newValue))
            }
        },
    )

    PhotoWidgetTextSizePicker(
        appSheetState = textSizeSheetState,
        currentValue = photoWidgetText.size,
        fontFamily = photoWidgetText.fontFamily,
        color = photoWidgetText.color,
        onApplyClick = { newValue: Int ->
            when (photoWidgetText) {
                is PhotoWidgetText.None -> Unit
                is PhotoWidgetText.Label -> onPhotoWidgetTextChange(photoWidgetText.copy(size = newValue))
            }
        },
    )

    PhotoWidgetVerticalOffsetPicker(
        appSheetState = verticalOffsetSheetState,
        currentValue = photoWidgetText.verticalOffset,
        fontFamily = photoWidgetText.fontFamily,
        color = photoWidgetText.color,
        onApplyClick = { newValue: Int ->
            when (photoWidgetText) {
                is PhotoWidgetText.None -> Unit
                is PhotoWidgetText.Label -> onPhotoWidgetTextChange(photoWidgetText.copy(verticalOffset = newValue))
            }
        },
    )
    // endregion Sheets
}

// region Pickers
@Composable
private fun PhotoWidgetTextTypePicker(
    appSheetState: AppSheetState,
    currentValue: PhotoWidgetText,
    onOptionSelect: (PhotoWidgetText) -> Unit,
) {
    AppBottomSheet(
        sheetState = appSheetState,
    ) {
        DefaultSheetContent(
            title = stringResource(R.string.photo_widget_configure_text_type),
        ) {
            val localResources = LocalResources.current

            RadioGroup(
                items = PhotoWidgetText.entries,
                itemSelected = { item: PhotoWidgetText -> item::class == currentValue::class },
                onItemClick = { item: PhotoWidgetText ->
                    if (item::class != currentValue::class) {
                        onOptionSelect(item)
                    }
                    appSheetState.hideBottomSheet()
                },
                itemTitle = { item: PhotoWidgetText ->
                    localResources.getString(
                        when (item) {
                            is PhotoWidgetText.None -> R.string.photo_widget_configure_text_type_none
                            is PhotoWidgetText.Label -> R.string.photo_widget_configure_text_type_label
                        },
                    )
                },
                itemDescription = { item ->
                    when (item) {
                        is PhotoWidgetText.None -> null

                        is PhotoWidgetText.Label -> {
                            localResources.getString(R.string.photo_widget_configure_text_type_label_description)
                        }
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun PhotoWidgetTextValuePicker(
    appSheetState: AppSheetState,
    currentValue: String,
    onApplyClick: (String) -> Unit,
) {
    AppBottomSheet(
        sheetState = appSheetState,
    ) {
        val textState: TextFieldState = rememberTextFieldState(currentValue)
        val confirmAction: () -> Unit = {
            onApplyClick(textState.text.trim().toString())
            appSheetState.hideBottomSheet()
        }

        OutlinedTextField(
            state = textState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            label = {
                Text(text = stringResource(id = R.string.photo_widget_configure_text_value))
            },
            trailingIcon = {
                if (textState.text.isNotEmpty()) {
                    Icon(
                        imageVector = AppIcons.Trash,
                        contentDescription = null,
                        modifier = Modifier.clickable(onClick = textState::clearText),
                    )
                }
            },
            inputTransformation = InputTransformation.maxLength(maxLength = 50),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            onKeyboardAction = { confirmAction() },
            lineLimits = TextFieldLineLimits.SingleLine,
            shape = Shapes.StandaloneShape,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = confirmAction,
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
private fun PhotoWidgetTextColorPicker(
    appSheetState: AppSheetState,
    currentValue: PhotoWidgetTextColor,
    fontFamily: String?,
    onApplyClick: (PhotoWidgetTextColor) -> Unit,
) {
    AppBottomSheet(
        sheetState = appSheetState,
    ) {
        TextColorPickerContent(
            currentValue = currentValue,
            fontFamily = fontFamily,
        ) { newValue: PhotoWidgetTextColor ->
            onApplyClick(newValue)
            appSheetState.hideBottomSheet()
        }
    }
}

@Composable
private fun TextColorPickerContent(
    currentValue: PhotoWidgetTextColor,
    fontFamily: String?,
    onApplyClick: (PhotoWidgetTextColor) -> Unit,
) {
    var color: PhotoWidgetTextColor by rememberSaveable { mutableStateOf(currentValue) }
    val localResources = LocalResources.current

    DefaultSheetContent(
        title = stringResource(R.string.photo_widget_configure_text_color),
        modifier = Modifier.animateContentSize(),
    ) {
        RadioGroup(
            items = PhotoWidgetTextColor.entries,
            itemSelected = { item: PhotoWidgetTextColor -> item.serializedName == color.serializedName },
            onItemClick = { item: PhotoWidgetTextColor ->
                color = if (item.serializedName == currentValue.serializedName) {
                    currentValue
                } else {
                    item
                }
            },
            itemTitle = { item: PhotoWidgetTextColor -> localResources.getString(item.label) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        when (val current = color) {
            is PhotoWidgetTextColor.Colored -> {
                ColorPickerContent(
                    currentColorHex = current.colorHex,
                    onColorChange = { newValue: String -> color = current.copy(colorHex = newValue) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    TextColorPreview(
                        color = current,
                        fontFamily = fontFamily,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .widthIn(max = 200.dp)
                            .aspectRatio(.75f),
                    )
                }
            }

            is PhotoWidgetTextColor.Dynamic -> {
                TextColorTypeContent(
                    color = current,
                    fontFamily = fontFamily,
                    types = DynamicColorType.entries,
                    selectedType = current.type,
                    onTypeSelect = { newValue: DynamicColorType ->
                        color = current.copy(type = newValue)
                    },
                    typeLabel = { type: DynamicColorType -> localResources.getString(type.label) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Text(
                    text = stringResource(R.string.photo_widget_configure_color_dynamic_explanation),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp, end = 32.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            is PhotoWidgetTextColor.Palette -> {
                TextColorTypeContent(
                    color = current,
                    fontFamily = fontFamily,
                    types = MatchPhotoColorType.entries,
                    selectedType = current.type,
                    onTypeSelect = { newValue: MatchPhotoColorType ->
                        color = current.copy(type = newValue)
                    },
                    typeLabel = { type: MatchPhotoColorType -> localResources.getString(type.label) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        Button(
            onClick = { onApplyClick(color) },
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
private fun <T> TextColorTypeContent(
    color: PhotoWidgetTextColor,
    fontFamily: String?,
    types: List<T>,
    selectedType: T,
    onTypeSelect: (T) -> Unit,
    typeLabel: (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, alignment = Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextColorPreview(
            color = color,
            fontFamily = fontFamily,
            modifier = Modifier
                .widthIn(max = min(200.dp, LocalWindowInfo.current.containerDpSize.width / 2))
                .aspectRatio(.75f),
        )

        RadioGroup(
            items = types,
            itemSelected = { type: T -> type == selectedType },
            onItemClick = onTypeSelect,
            itemTitle = typeLabel,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TextColorPreview(
    color: PhotoWidgetTextColor,
    fontFamily: String?,
    modifier: Modifier = Modifier,
) {
    WidgetPositionViewer(
        photoWidget = PhotoWidget(
            currentPhoto = LocalSamplePhoto.current,
            text = PhotoWidgetText.Label(
                value = stringResource(R.string.photo_widget_configure_text_sample),
                fontFamily = fontFamily,
                color = color,
            ),
        ),
        modifier = modifier,
    )
}

@Composable
private fun PhotoWidgetTextFontPicker(
    appSheetState: AppSheetState,
    currentValue: String?,
    text: String,
    onOptionSelect: (String?) -> Unit,
) {
    AppBottomSheet(
        sheetState = appSheetState,
    ) {
        val localContext = LocalContext.current
        val localInspectionMode: Boolean = LocalInspectionMode.current
        val googleFontsLoader: GoogleFontsLoader by remember {
            lazy { entryPoint<PhotoWidgetEntryPoint>(localContext).googleFontsLoader() }
        }

        val previewText: String = text.ifBlank { stringResource(R.string.photo_widget_configure_text_sample) }
        val fontFamilies: List<String?> = remember { listOf(null) + GoogleFontsLoader.FONT_FAMILIES }

        val fontLoadStates: SnapshotStateMap<String, FontLoadState> = remember { mutableStateMapOf() }
        var loadAttempt: Int by remember { mutableIntStateOf(0) }

        LaunchedEffect(loadAttempt) {
            if (localInspectionMode) return@LaunchedEffect

            for (fontFamily in GoogleFontsLoader.FONT_FAMILIES) {
                if (fontLoadStates[fontFamily] is FontLoadState.Loaded) continue

                fontLoadStates[fontFamily] = FontLoadState.Loading
                launch {
                    fontLoadStates[fontFamily] = googleFontsLoader.loadTypeface(fontFamily = fontFamily)
                        ?.let(FontLoadState::Loaded)
                        ?: FontLoadState.Failed
                }
            }
        }

        val listState: LazyListState = rememberLazyListState()

        LaunchedEffect(Unit) {
            val selectedIndex = fontFamilies.indexOfFirst { it == currentValue }
            val visibleCount = listState.layoutInfo.visibleItemsInfo.size

            if (selectedIndex > visibleCount) {
                listState.scrollToItem(index = selectedIndex)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .nestedScroll(rememberNestedScrollInteropConnection())
                .fadingEdges(listState)
                .selectableGroup(),
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.photo_widget_configure_text_font),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            if (fontLoadStates.values.any { it is FontLoadState.Failed }) {
                item {
                    InformationalPanel(
                        text = stringResource(R.string.photo_widget_configure_text_font_load_error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                            .animateItem(),
                        showActionButton = true,
                        actionButtonText = stringResource(R.string.photo_widget_action_retry),
                        onActionButtonClick = { loadAttempt++ },
                    )
                }
            }

            itemsIndexed(
                items = fontFamilies,
                key = { _, fontFamily: String? -> fontFamily.orEmpty() },
            ) { index: Int, fontFamily: String? ->
                val loadState: FontLoadState? = fontFamily?.let(fontLoadStates::get)

                FontListItem(
                    fontFamily = fontFamily,
                    typeface = (loadState as? FontLoadState.Loaded)?.typeface ?: Typeface.DEFAULT,
                    isLoading = loadState is FontLoadState.Loading,
                    previewText = previewText,
                    selected = fontFamily == currentValue,
                    onClick = {
                        if (fontFamily != currentValue) {
                            onOptionSelect(fontFamily)
                        }
                        appSheetState.hideBottomSheet()
                    },
                    shape = when (index) {
                        0 -> Shapes.TopShape
                        fontFamilies.lastIndex -> Shapes.BottomShape
                        else -> Shapes.MiddleShape
                    },
                )
            }
        }
    }
}

private sealed interface FontLoadState {

    data object Loading : FontLoadState

    data class Loaded(val typeface: Typeface) : FontLoadState

    data object Failed : FontLoadState
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FontListItem(
    fontFamily: String?,
    typeface: Typeface,
    isLoading: Boolean,
    previewText: String,
    selected: Boolean,
    onClick: () -> Unit,
    shape: Shape,
) {
    val backgroundColor: Color by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
    )
    val contentColor: Color = contentColorFor(backgroundColor)

    androidx.compose.material3.ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ListItem.MinHeight)
            .clip(shape)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        leadingContent = {
            RadioButton(
                selected = selected,
                onClick = null,
                modifier = Modifier.padding(vertical = 8.dp),
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    unselectedColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        trailingContent = {
            if (isLoading) {
                CircularWavyProgressIndicator(modifier = Modifier.size(32.dp))
            }
        },
        supportingContent = {
            Text(
                text = fontFamily ?: stringResource(R.string.photo_widget_configure_text_font_default),
                color = contentColor,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        colors = ListItemDefaults.colors(containerColor = backgroundColor, contentColor = contentColor),
        content = {
            Text(
                text = previewText,
                color = contentColor,
                fontFamily = remember(typeface) { FontFamily(typeface) },
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = MaterialTheme.typography.titleLarge,
            )
        },
    )
}

@Composable
private fun PhotoWidgetTextSizePicker(
    appSheetState: AppSheetState,
    currentValue: Int,
    fontFamily: String?,
    color: PhotoWidgetTextColor,
    onApplyClick: (Int) -> Unit,
) {
    AppBottomSheet(
        sheetState = appSheetState,
    ) {
        DefaultSheetContent(
            title = stringResource(R.string.photo_widget_configure_text_size),
        ) {
            var updatedValue: Int by rememberSaveable(currentValue) { mutableIntStateOf(currentValue) }

            WidgetPositionViewer(
                photoWidget = PhotoWidget(
                    currentPhoto = LocalSamplePhoto.current,
                    text = PhotoWidgetText.Label(
                        value = stringResource(R.string.photo_widget_configure_text_sample),
                        size = updatedValue,
                        fontFamily = fontFamily,
                        color = color,
                    ),
                ),
                modifier = Modifier
                    .width(200.dp)
                    .aspectRatio(.75f),
            )

            NumberSpinner(
                value = updatedValue,
                onIncreaseClick = { updatedValue++ },
                onDecreaseClick = { updatedValue-- },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                lowerBound = PhotoWidgetText.SIZE_RANGE.first,
                upperBound = PhotoWidgetText.SIZE_RANGE.last,
            )

            Button(
                onClick = {
                    onApplyClick(updatedValue)
                    appSheetState.hideBottomSheet()
                },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Text(text = stringResource(id = R.string.photo_widget_action_apply))
            }
        }
    }
}

@Composable
private fun PhotoWidgetVerticalOffsetPicker(
    appSheetState: AppSheetState,
    currentValue: Int,
    fontFamily: String?,
    color: PhotoWidgetTextColor,
    onApplyClick: (Int) -> Unit,
) {
    AppBottomSheet(
        sheetState = appSheetState,
    ) {
        DefaultSheetContent(
            title = stringResource(R.string.photo_widget_configure_text_vertical_offset),
        ) {
            var updatedValue: Int by rememberSaveable(currentValue) { mutableIntStateOf(currentValue) }

            WidgetPositionViewer(
                photoWidget = PhotoWidget(
                    currentPhoto = LocalSamplePhoto.current,
                    text = PhotoWidgetText.Label(
                        value = stringResource(R.string.photo_widget_configure_text_sample),
                        verticalOffset = updatedValue,
                        fontFamily = fontFamily,
                        color = color,
                    ),
                ),
                modifier = Modifier
                    .width(200.dp)
                    .aspectRatio(.75f),
            )

            NumberSpinner(
                value = updatedValue,
                onIncreaseClick = { updatedValue++ },
                onDecreaseClick = { updatedValue-- },
                lowerBound = PhotoWidgetText.VERTICAL_OFFSET_RANGE.first,
                upperBound = PhotoWidgetText.VERTICAL_OFFSET_RANGE.last,
            )

            DefaultSheetFooterButtons(
                onApplyClick = {
                    onApplyClick(updatedValue)
                    appSheetState.hideBottomSheet()
                },
                onResetClick = { updatedValue = 0 },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }
    }
}
// endregion Pickers

@Composable
private fun rememberIsGoogleFontsAvailable(): Boolean {
    val localContext = LocalContext.current
    val localInspectionMode: Boolean = LocalInspectionMode.current

    return remember {
        localInspectionMode || entryPoint<PhotoWidgetEntryPoint>(localContext).googleFontsLoader().isAvailable()
    }
}

// region Previews
@PreviewAll
@Composable
private fun PhotoWidgetConfigureTextTabPreview() {
    ExtendedTheme {
        PhotoWidgetConfigureTextTab(
            photoWidgetText = PhotoWidgetText.Label(value = "Sample text"),
            onPhotoWidgetTextChange = {},
            modifier = Modifier.safeDrawingPadding(),
        )
    }
}
// endregion Previews
