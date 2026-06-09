package com.fibelatti.photowidget.configure

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.AppShortcutInfo
import com.fibelatti.photowidget.ui.icons.AppIcons
import com.fibelatti.photowidget.ui.icons.Export
import com.fibelatti.ui.foundation.AppBottomSheet
import com.fibelatti.ui.foundation.AppSheetState
import com.fibelatti.ui.foundation.fadingEdges
import com.fibelatti.ui.foundation.hideBottomSheet

@Composable
fun AppShortcutPickerBottomSheet(
    sheetState: AppSheetState,
    shortcuts: List<AppShortcutInfo>,
    onLaunchAppClick: () -> Unit,
    onShortcutClick: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppBottomSheet(
        sheetState = sheetState,
        modifier = modifier,
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.photo_widget_configure_tap_action_choose_shortcut),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
            )

            val scrollState: LazyListState = rememberLazyListState()

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .nestedScroll(rememberNestedScrollInteropConnection())
                    .fadingEdges(scrollState = scrollState),
                state = scrollState,
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                item(key = "launch_app") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { sheetState.hideBottomSheet { onLaunchAppClick() } }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = AppIcons.Export,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .background(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = CircleShape)
                                .padding(all = 8.dp),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )

                        Text(
                            text = stringResource(R.string.photo_widget_configure_tap_action_launch_app),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }

                if (shortcuts.isNotEmpty()) {
                    items(shortcuts, key = { it.id }) { shortcut ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { sheetState.hideBottomSheet { onShortcutClick(shortcut.id) } }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val icon = shortcut.icon
                            if (icon != null) {
                                Image(
                                    bitmap = icon.toBitmap().asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                )
                            } else {
                                Box(modifier = Modifier.size(40.dp))
                            }

                            Text(
                                text = shortcut.label,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}
