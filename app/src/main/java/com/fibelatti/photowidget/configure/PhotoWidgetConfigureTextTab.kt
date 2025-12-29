@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.fibelatti.photowidget.configure

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetText
import com.fibelatti.photowidget.preferences.BooleanDefault
import com.fibelatti.photowidget.preferences.PickerDefault
import com.fibelatti.photowidget.ui.DefaultSheetContent
import com.fibelatti.photowidget.ui.DefaultSheetFooterButtons
import com.fibelatti.photowidget.ui.LocalSamplePhoto
import com.fibelatti.photowidget.ui.NumberSpinner
import com.fibelatti.photowidget.ui.RadioGroup
import com.fibelatti.photowidget.ui.WarningSign
import com.fibelatti.photowidget.ui.WidgetPositionViewer
import com.fibelatti.ui.foundation.AppBottomSheet
import com.fibelatti.ui.foundation.AppSheetState
import com.fibelatti.ui.foundation.hideBottomSheet
import com.fibelatti.ui.foundation.rememberAppSheetState
import com.fibelatti.ui.foundation.showBottomSheet
import com.fibelatti.ui.preview.AllPreviews
import com.fibelatti.ui.theme.ExtendedTheme

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
    val textSizeSheetState: AppSheetState = rememberAppSheetState()
    val verticalOffsetSheetState: AppSheetState = rememberAppSheetState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PickerDefault(
            title = stringResource(R.string.photo_widget_configure_text_type),
            currentValue = stringResource(
                when (photoWidgetText) {
                    is PhotoWidgetText.None -> R.string.photo_widget_configure_text_type_none
                    is PhotoWidgetText.Label -> R.string.photo_widget_configure_text_type_label
                },
            ),
            onClick = textTypeSheetState::showBottomSheet,
        )

        when (photoWidgetText) {
            is PhotoWidgetText.None -> Unit

            is PhotoWidgetText.Label -> {
                PickerDefault(
                    title = stringResource(R.string.photo_widget_configure_text_value),
                    currentValue = photoWidgetText.value,
                    onClick = textValueSheetState::showBottomSheet,
                )

                PickerDefault(
                    title = stringResource(R.string.photo_widget_configure_text_size),
                    currentValue = photoWidgetText.size.toString(),
                    onClick = textSizeSheetState::showBottomSheet,
                )

                PickerDefault(
                    title = stringResource(R.string.photo_widget_configure_text_vertical_offset),
                    currentValue = photoWidgetText.verticalOffset.toString(),
                    onClick = verticalOffsetSheetState::showBottomSheet,
                )

                BooleanDefault(
                    title = stringResource(R.string.photo_widget_configure_text_apply_shadow),
                    currentValue = photoWidgetText.hasShadow,
                    onCheckedChange = { newValue ->
                        onPhotoWidgetTextChange(photoWidgetText.copy(hasShadow = newValue))
                    },
                )

                WarningSign(
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
        onOptionSelected = onPhotoWidgetTextChange,
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

    PhotoWidgetTextSizePicker(
        appSheetState = textSizeSheetState,
        currentValue = photoWidgetText.size,
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
    onOptionSelected: (PhotoWidgetText) -> Unit,
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
                        onOptionSelected(item)
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
        val confirmAction: () -> Unit by rememberUpdatedState {
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
                        painter = painterResource(R.drawable.ic_trash),
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
private fun PhotoWidgetTextSizePicker(
    appSheetState: AppSheetState,
    currentValue: Int,
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
                lowerBound = 10,
                upperBound = 20,
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
                lowerBound = -20,
                upperBound = 0,
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

// region Previews
@AllPreviews
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
