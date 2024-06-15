package com.fibelatti.photowidget.configure

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import com.fibelatti.photowidget.platform.ComposeBottomSheetDialog
import com.fibelatti.ui.foundation.ColumnToggleButtonGroup
import com.fibelatti.ui.foundation.ToggleButtonGroup
import com.fibelatti.ui.preview.DevicePreviews
import com.fibelatti.ui.preview.LocalePreviews
import com.fibelatti.ui.preview.ThemePreviews
import com.fibelatti.ui.text.AutoSizeText
import com.fibelatti.ui.theme.ExtendedTheme

object PhotoWidgetTapActionPicker {

    fun show(
        context: Context,
        currentTapAction: PhotoWidgetTapAction,
        currentAppShortcut: String?,
        currentIncreaseBrightness: Boolean,
        onApplyClick: (newTapAction: PhotoWidgetTapAction, newAppShortcut: String?, newIncreaseBrightness: Boolean) -> Unit,
    ) {
        ComposeBottomSheetDialog(context) {
            var selectedApp: String? by remember(currentAppShortcut) {
                mutableStateOf(currentAppShortcut)
            }
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult(),
            ) { result ->
                selectedApp = result.data?.component?.packageName
            }

            TapActionPickerContent(
                currentTapAction = currentTapAction,
                currentAppShortcut = selectedApp,
                currentIncreaseBrightness = currentIncreaseBrightness,
                onChooseApp = {
                    launcher.launch(
                        Intent(Intent.ACTION_PICK_ACTIVITY).putExtra(
                            Intent.EXTRA_INTENT,
                            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
                        ),
                    )
                },
                onApplyClick = { newTapAction, newAppShortcut, newIncreaseBrightness ->
                    onApplyClick(newTapAction, newAppShortcut, newIncreaseBrightness)
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
    currentIncreaseBrightness: Boolean,
    onChooseApp: () -> Unit,
    onApplyClick: (newTapAction: PhotoWidgetTapAction, newAppShortcut: String?, newIncreaseBrightness: Boolean) -> Unit,
) {
    var tapAction by remember { mutableStateOf(currentTapAction) }
    var increaseBrightness by remember { mutableStateOf(currentIncreaseBrightness) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = 16.dp),
    ) {
        Text(
            text = stringResource(id = R.string.photo_widget_configure_tap_action),
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
        )

        ColumnToggleButtonGroup(
            items = PhotoWidgetTapAction.entries.map {
                ToggleButtonGroup.Item(
                    id = it.name,
                    text = stringResource(id = it.label),
                )
            },
            onButtonClick = { item ->
                tapAction = PhotoWidgetTapAction.valueOf(item.id)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            selectedIndex = PhotoWidgetTapAction.entries.indexOf(tapAction),
            colors = ToggleButtonGroup.colors(
                unselectedButtonColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        )

        AnimatedContent(
            targetState = tapAction,
            label = "TapAction_CustomOptions",
        ) { value ->
            val customOptionModifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)

            when (value) {
                PhotoWidgetTapAction.VIEW_FULL_SCREEN -> {
                    BrightnessToggle(
                        enabled = increaseBrightness,
                        onChange = { increaseBrightness = it },
                        modifier = customOptionModifier,
                    )
                }

                PhotoWidgetTapAction.VIEW_IN_GALLERY -> {
                    Text(
                        text = stringResource(id = R.string.photo_widget_configure_tap_action_gallery_description),
                        modifier = customOptionModifier,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }

                PhotoWidgetTapAction.VIEW_NEXT_PHOTO -> {
                    Text(
                        text = stringResource(id = R.string.photo_widget_configure_tap_action_flip_description),
                        modifier = customOptionModifier,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }

                PhotoWidgetTapAction.APP_SHORTCUT -> {
                    AppPicker(
                        onChooseApp = onChooseApp,
                        currentAppShortcut = currentAppShortcut,
                        modifier = customOptionModifier,
                    )
                }

                PhotoWidgetTapAction.NONE -> Unit
            }
        }

        FilledTonalButton(
            onClick = { onApplyClick(tapAction, currentAppShortcut, increaseBrightness) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        ) {
            Text(text = stringResource(id = R.string.photo_widget_action_apply))
        }
    }
}

@Composable
private fun BrightnessToggle(
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Switch(
            checked = enabled,
            onCheckedChange = onChange,
        )

        Text(
            text = stringResource(id = R.string.photo_widget_configure_tap_action_increase_brightness),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge,
        )
    }
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
@ThemePreviews
@LocalePreviews
@DevicePreviews
private fun PhotoWidgetTapActionPickerPreview() {
    ExtendedTheme {
        TapActionPickerContent(
            currentTapAction = PhotoWidgetTapAction.APP_SHORTCUT,
            currentAppShortcut = null,
            currentIncreaseBrightness = false,
            onChooseApp = {},
            onApplyClick = { _, _, _ -> },
        )
    }
}
