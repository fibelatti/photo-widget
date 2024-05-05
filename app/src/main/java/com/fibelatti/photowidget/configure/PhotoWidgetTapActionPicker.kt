package com.fibelatti.photowidget.configure

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
        onChooseApp: () -> Unit,
        onApplyClick: (newTapAction: PhotoWidgetTapAction, newAppShortcut: String?) -> Unit,
    ): ComposeBottomSheetDialog = ComposeBottomSheetDialog(context) {
        val result by activityResult.collectAsStateWithLifecycle()

        TapActionPickerContent(
            currentTapAction = currentTapAction,
            currentAppShortcut = currentAppShortcut,
            onChooseApp = onChooseApp,
            selectedApp = result as? String,
            onApplyClick = { newTapAction, newAppShortcut ->
                onApplyClick(newTapAction, newAppShortcut)
                dismiss()
            },
        )
    }.also { it.show() }
}

@Composable
private fun TapActionPickerContent(
    currentTapAction: PhotoWidgetTapAction,
    currentAppShortcut: String?,
    onChooseApp: () -> Unit,
    selectedApp: String?,
    onApplyClick: (newTapAction: PhotoWidgetTapAction, newAppShortcut: String?) -> Unit,
) {
    var tapAction by remember { mutableStateOf(currentTapAction) }
    val appShortcutPackageName = selectedApp ?: currentAppShortcut

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
                    text = stringResource(id = it.title),
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

        AnimatedVisibility(
            visible = PhotoWidgetTapAction.APP_SHORTCUT == tapAction,
        ) {
            Row(
                modifier = Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onChooseApp,
                ) {
                    Text(text = stringResource(id = R.string.photo_widget_configure_tap_action_choose_app))
                }

                appShortcutPackageName?.runCatching {
                    val packageManager = LocalContext.current.packageManager
                    val appInfo = packageManager.getApplicationInfo(
                        appShortcutPackageName,
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

        FilledTonalButton(
            onClick = { onApplyClick(tapAction, appShortcutPackageName) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        ) {
            Text(text = stringResource(id = R.string.photo_widget_action_apply))
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
            onChooseApp = {},
            selectedApp = null,
            onApplyClick = { _, _ -> },
        )
    }
}
