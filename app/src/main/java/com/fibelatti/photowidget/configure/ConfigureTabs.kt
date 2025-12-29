package com.fibelatti.photowidget.configure

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.R
import com.fibelatti.ui.preview.LocalePreviews
import com.fibelatti.ui.preview.ThemePreviews
import com.fibelatti.ui.text.AutoSizeText
import com.fibelatti.ui.theme.ExtendedTheme

enum class ConfigureTab(
    @StringRes val title: Int,
) {

    CONTENT(title = R.string.photo_widget_configure_tab_content),
    APPEARANCE(title = R.string.photo_widget_configure_tab_appearance),
    TEXT(title = R.string.photo_widget_configure_tab_text),
    BEHAVIOR(title = R.string.photo_widget_configure_tab_behavior),
}

@Composable
inline fun ConfigureTabs(
    modifier: Modifier = Modifier,
    tabHeight: Dp = 48.dp,
    tabContent: @Composable (ConfigureTab) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(ConfigureTab.CONTENT) }
    val selectedTabIndex by remember { derivedStateOf { ConfigureTab.entries.indexOf(selectedTab) } }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.background,
            edgePadding = 26.dp,
            indicator = {
                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(selectedTabIndex, matchContentSize = true),
                    width = 50.dp,
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 3.dp, bottomEnd = 3.dp),
                )
            },
            divider = {},
            minTabWidth = 105.dp,
        ) {
            ConfigureTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    modifier = Modifier.height(tabHeight),
                ) {
                    AutoSizeText(
                        text = stringResource(tab.title),
                        modifier = Modifier.padding(horizontal = 8.dp),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            tabContent(selectedTab)
        }
    }
}

// region Previews
@Composable
@ThemePreviews
@LocalePreviews
private fun ConfigureTabsPreview() {
    ExtendedTheme {
        ConfigureTabs(
            modifier = Modifier.systemBarsPadding(),
            tabContent = {},
        )
    }
}
// endregion Previews
