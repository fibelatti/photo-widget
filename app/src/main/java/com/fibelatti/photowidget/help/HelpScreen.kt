package com.fibelatti.photowidget.help

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.platform.isBackgroundRestricted
import com.fibelatti.photowidget.ui.WarningSign
import com.fibelatti.ui.foundation.AppBottomSheet
import com.fibelatti.ui.foundation.AppSheetState
import com.fibelatti.ui.preview.AllPreviews
import com.fibelatti.ui.theme.ExtendedTheme

@Composable
fun HelpBottomSheet(
    sheetState: AppSheetState,
    onBackgroundRestrictionClick: () -> Unit,
) {
    AppBottomSheet(
        sheetState = sheetState,
    ) {
        HelpScreen(
            onBackgroundRestrictionClick = onBackgroundRestrictionClick,
        )
    }
}

@Composable
private fun HelpScreen(
    onBackgroundRestrictionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded: HelpArticle? by remember { mutableStateOf(null) }
    val localContext = LocalContext.current
    val localInspectionMode = LocalInspectionMode.current

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .nestedScroll(rememberNestedScrollInteropConnection()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(start = 16.dp, bottom = 24.dp, end = 16.dp),
    ) {
        item {
            Text(
                text = stringResource(id = R.string.photo_widget_home_common_issues),
                modifier = Modifier.padding(bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge,
            )
        }

        items(HelpArticle.allArticles()) { article ->
            HelpCard(
                cardTitle = stringResource(id = article.title),
                cardText = stringResource(id = article.body),
                expanded = article == expanded,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { expanded = article.takeUnless { expanded == article } },
                ),
            )
        }

        if (localInspectionMode || localContext.isBackgroundRestricted()) {
            item {
                WarningSign(
                    text = stringResource(R.string.restriction_warning_hint),
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .clickable(
                            onClick = onBackgroundRestrictionClick,
                            role = Role.Button,
                        ),
                )
            }
        }
    }
}

@Composable
private fun HelpCard(
    cardTitle: String,
    cardText: String,
    expanded: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
            )
            .padding(all = 16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = cardTitle,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
            )

            val rotation by animateFloatAsState(
                targetValue = if (expanded) -180f else 0f,
                animationSpec = tween(durationMillis = 300),
                label = "HelpCard_ExpandChevron",
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_chevron_down),
                contentDescription = "",
                modifier = Modifier
                    .rotate(rotation)
                    .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
                    .padding(all = 2.dp),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }

        AnimatedVisibility(
            visible = expanded,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text(
                    text = cardText,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
@AllPreviews
private fun HelpScreenPreview() {
    ExtendedTheme {
        HelpScreen(
            onBackgroundRestrictionClick = {},
        )
    }
}
