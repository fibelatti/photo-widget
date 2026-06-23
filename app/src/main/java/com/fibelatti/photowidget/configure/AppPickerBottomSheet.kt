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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.InstalledApp
import com.fibelatti.ui.component.AppBottomSheet
import com.fibelatti.ui.component.AppSheetState
import com.fibelatti.ui.foundation.fadingEdges
import com.fibelatti.ui.foundation.pxToDp

@Composable
fun AppPickerBottomSheet(
    sheetState: AppSheetState,
    apps: List<InstalledApp>,
    onAppClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchQuery: TextFieldState = rememberTextFieldState("")

    val filteredApps: List<InstalledApp> by remember(apps) {
        derivedStateOf {
            if (searchQuery.text.isBlank()) {
                apps
            } else {
                apps.filter { it.appLabel.contains(searchQuery.text, ignoreCase = true) }
            }
        }
    }

    AppBottomSheet(
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.photo_widget_configure_tap_action_choose_app),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
            )

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                val scrollState: LazyListState = rememberLazyListState()
                var searchBarHeight: Int by remember { mutableIntStateOf(0) }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .nestedScroll(rememberNestedScrollInteropConnection())
                        .fadingEdges(scrollState = scrollState, endEdgeSize = 16.dp + searchBarHeight.pxToDp()),
                    state = scrollState,
                    contentPadding = PaddingValues(bottom = 72.dp),
                ) {
                    when {
                        apps.isEmpty() -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularWavyProgressIndicator()
                                }
                            }
                        }

                        filteredApps.isEmpty() -> {
                            item {
                                Text(
                                    text = stringResource(R.string.photo_widget_configure_tap_action_search_no_apps),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 56.dp)
                                        .padding(horizontal = 32.dp),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }

                        else -> {
                            items(filteredApps, key = { it.appPackage }) { app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { sheetState.hideBottomSheet { onAppClick(app.appPackage) } }
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    val bitmap: ImageBitmap = remember(app.appPackage) {
                                        app.appIcon.toBitmap().asImageBitmap()
                                    }

                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                    )

                                    Text(
                                        text = app.appLabel,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    state = searchQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onPlaced { coordinates -> searchBarHeight = coordinates.size.height }
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 16.dp),
                    label = { Text(text = stringResource(R.string.photo_widget_configure_tap_action_search_apps)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    lineLimits = TextFieldLineLimits.SingleLine,
                )
            }
        }
    }
}
