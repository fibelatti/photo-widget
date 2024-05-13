package com.fibelatti.photowidget.home

import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fibelatti.photowidget.BuildConfig
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.configure.ColoredShape
import com.fibelatti.photowidget.configure.ShapedPhoto
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetLoopingInterval
import com.fibelatti.photowidget.model.PhotoWidgetShapeBuilder
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import com.fibelatti.ui.foundation.dpToPx
import com.fibelatti.ui.preview.DevicePreviews
import com.fibelatti.ui.preview.LocalePreviews
import com.fibelatti.ui.preview.ThemePreviews
import com.fibelatti.ui.text.AutoSizeText
import com.fibelatti.ui.theme.ExtendedTheme

@Composable
fun HomeScreen(
    onCreateNewWidgetClick: (PhotoWidgetAspectRatio) -> Unit,
    currentWidgets: List<Pair<Int, PhotoWidget>>,
    onCurrentWidgetClick: (appWidgetId: Int) -> Unit,
    onDefaultsClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onColorsClick: () -> Unit,
    onRateClick: () -> Unit,
    onShareClick: () -> Unit,
    onHelpClick: () -> Unit,
    onViewLicensesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentDestination: HomeNavigationDestination by rememberSaveable {
        mutableStateOf(HomeNavigationDestination.NEW_WIDGET)
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            HomeNavigation(
                currentDestination = currentDestination,
                onDestinationClick = { currentDestination = it },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.background)
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            ShapesBanner(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            )

            AnimatedContent(
                targetState = currentDestination,
                label = "Home_Navigation",
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) { destination ->
                when (destination) {
                    HomeNavigationDestination.NEW_WIDGET -> {
                        NewWidgetScreen(
                            onCreateNewWidgetClick = onCreateNewWidgetClick,
                        )
                    }

                    HomeNavigationDestination.MY_WIDGETS -> {
                        MyWidgetsScreen(
                            widgets = currentWidgets,
                            onClick = onCurrentWidgetClick,
                        )
                    }

                    HomeNavigationDestination.SETTINGS -> {
                        SettingsScreen(
                            onDefaultsClick = onDefaultsClick,
                            onAppearanceClick = onAppearanceClick,
                            onColorsClick = onColorsClick,
                            onRateClick = onRateClick,
                            onShareClick = onShareClick,
                            onHelpClick = onHelpClick,
                            onViewLicensesClick = onViewLicensesClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeNavigation(
    currentDestination: HomeNavigationDestination,
    onDestinationClick: (HomeNavigationDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
    ) {
        HomeNavigationDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = destination == currentDestination,
                onClick = { onDestinationClick(destination) },
                icon = {
                    Icon(
                        painter = painterResource(id = destination.icon),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                },
                label = {
                    Text(text = stringResource(id = destination.label))
                },
            )
        }
    }
}

private enum class HomeNavigationDestination(
    @DrawableRes val icon: Int,
    @StringRes val label: Int,
) {
    NEW_WIDGET(icon = R.drawable.ic_new_widget, label = R.string.photo_widget_home_new),
    MY_WIDGETS(icon = R.drawable.ic_my_widgets, label = R.string.photo_widget_home_current),
    SETTINGS(icon = R.drawable.ic_settings, label = R.string.photo_widget_home_settings),
}

@Composable
private fun NewWidgetScreen(
    onCreateNewWidgetClick: (PhotoWidgetAspectRatio) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val maxHeight = maxHeight

        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            AutoSizeText(
                text = stringResource(id = R.string.photo_widget_home_title),
                modifier = Modifier.padding(horizontal = 32.dp),
                maxLines = 2,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
            )

            var selectedAspectRatio by remember {
                mutableStateOf(PhotoWidgetAspectRatio.SQUARE)
            }

            AspectRatioPicker(
                selectedAspectRatio = selectedAspectRatio,
                onAspectRatioSelected = { selectedAspectRatio = it },
                modifier = Modifier.fillMaxWidth(),
                useGridLayout = maxHeight > 600.dp,
            )

            Text(
                text = stringResource(id = R.string.photo_widget_home_aspect_ratio),
                modifier = Modifier.padding(horizontal = 32.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )

            FilledTonalButton(
                onClick = { onCreateNewWidgetClick(selectedAspectRatio) },
            ) {
                Text(text = stringResource(id = R.string.photo_widget_home_new_widget))
            }
        }
    }
}

@Composable
private fun ShapesBanner(
    modifier: Modifier = Modifier,
    polygonSize: Dp = 48.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(polygonSize),
    ) {
        val polygons = remember {
            PhotoWidgetShapeBuilder.buildAllShapes().values.drop(4)
        }

        val screenWidth = LocalConfiguration.current.screenWidthDp.dp.dpToPx()
        val transition = rememberInfiniteTransition(label = "ShapesBannerTransition")
        val animationDuration = 2_000
        val animationSpec = tween<Float>(
            durationMillis = polygons.size * animationDuration,
            easing = LinearEasing,
        )

        polygons.forEachIndexed { index, roundedPolygon ->
            val translationValue by transition.animateFloat(
                initialValue = screenWidth,
                targetValue = -polygonSize.dpToPx(),
                animationSpec = infiniteRepeatable(
                    animation = animationSpec,
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(offsetMillis = index * animationDuration),
                ),
                label = "ShapesBannerAnimation_Translation",
            )
            val rotationValue by transition.animateFloat(
                initialValue = 0f,
                targetValue = -360f,
                animationSpec = infiniteRepeatable(
                    animation = animationSpec,
                    repeatMode = RepeatMode.Restart,
                ),
                label = "ShapesBannerAnimation_Rotation",
            )

            ColoredShape(
                polygon = roundedPolygon,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(ratio = 1f)
                    .graphicsLayer {
                        translationX = translationValue
                        rotationZ = rotationValue
                    },
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun AspectRatioPicker(
    selectedAspectRatio: PhotoWidgetAspectRatio,
    onAspectRatioSelected: (PhotoWidgetAspectRatio) -> Unit,
    modifier: Modifier = Modifier,
    useGridLayout: Boolean = true,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        if (useGridLayout) {
            FlowRow(
                modifier = Modifier.widthIn(max = 240.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 2,
            ) {
                PhotoWidgetAspectRatio.entries.forEach {
                    AspectRatioItem(
                        ratio = it.aspectRatio,
                        label = stringResource(id = it.label),
                        isSelected = it == selectedAspectRatio,
                        onClick = { onAspectRatioSelected(it) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                items(PhotoWidgetAspectRatio.entries) {
                    AspectRatioItem(
                        ratio = it.aspectRatio,
                        label = stringResource(id = it.label),
                        isSelected = it == selectedAspectRatio,
                        onClick = { onAspectRatioSelected(it) },
                        modifier = Modifier.height(120.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AspectRatioItem(
    ratio: Float,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val transition = updateTransition(
        targetState = isSelected,
        label = "AspectRatioItem_Transition",
    )
    val containerColor by transition.animateColor(label = "ContainerColor") { selected ->
        if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
    }

    ElevatedCard(
        modifier = modifier
            .aspectRatio(ratio = 1f)
            .clip(shape = CardDefaults.elevatedShape)
            .clickable(onClick = onClick),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier
                .padding(all = 12.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val aspectRatioColor by transition.animateColor(label = "AspectRatioColor") { selected ->
                if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.secondary
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(ratio = ratio)
                    .background(
                        color = aspectRatioColor,
                        shape = RoundedCornerShape(8.dp),
                    ),
            )

            val textColor by transition.animateColor(label = "LabelColor") { selected ->
                if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            }

            Text(
                text = label,
                color = textColor,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MyWidgetsScreen(
    widgets: List<Pair<Int, PhotoWidget>>,
    onClick: (appWidgetId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val maxWidth = maxWidth

        var selectedSource: PhotoWidgetSource? by remember { mutableStateOf(null) }
        val filteredWidgets: List<Pair<Int, PhotoWidget>> by remember(widgets) {
            derivedStateOf {
                widgets.filter { selectedSource == null || it.second.source == selectedSource }
            }
        }

        if (filteredWidgets.isNotEmpty()) {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(count = if (maxWidth < 600.dp) 2 else 4),
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 80.dp, end = 16.dp, bottom = 16.dp),
                verticalItemSpacing = 16.dp,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(filteredWidgets) { (id, widget) ->
                    ShapedPhoto(
                        photo = widget.currentPhoto,
                        aspectRatio = widget.aspectRatio,
                        shapeId = widget.shapeId,
                        cornerRadius = widget.cornerRadius,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onClick(id) },
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, top = 120.dp, end = 32.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ColoredShape(
                    polygon = remember {
                        PhotoWidgetShapeBuilder.buildAllShapes().values.random()
                    },
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(120.dp),
                )

                Text(
                    text = stringResource(id = R.string.photo_widget_home_empty_widgets),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp),
        ) {
            val borderColor = SegmentedButtonDefaults.borderStroke(SegmentedButtonDefaults.colors().activeBorderColor)

            SegmentedButton(
                selected = selectedSource == null,
                onClick = { selectedSource = null },
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                border = borderColor,
                label = {
                    Text(
                        text = stringResource(id = R.string.photo_widget_home_filter_all),
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
            )

            SegmentedButton(
                selected = PhotoWidgetSource.PHOTOS == selectedSource,
                onClick = { selectedSource = PhotoWidgetSource.PHOTOS },
                shape = RectangleShape,
                border = borderColor,
                label = {
                    Text(
                        text = stringResource(id = R.string.photo_widget_home_filter_photos),
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
            )

            SegmentedButton(
                selected = PhotoWidgetSource.DIRECTORY == selectedSource,
                onClick = { selectedSource = PhotoWidgetSource.DIRECTORY },
                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                border = borderColor,
                label = {
                    Text(
                        text = stringResource(id = R.string.photo_widget_home_filter_folder),
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
            )
        }
    }
}

@Composable
private fun SettingsScreen(
    onDefaultsClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onColorsClick: () -> Unit,
    onRateClick: () -> Unit,
    onShareClick: () -> Unit,
    onHelpClick: () -> Unit,
    onViewLicensesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(all = 16.dp),
    ) {
        SettingsAction(
            icon = R.drawable.ic_default,
            label = R.string.widget_defaults_title,
            onClick = onDefaultsClick,
        )

        SettingsAction(
            icon = R.drawable.ic_appearance,
            label = R.string.photo_widget_home_appearance,
            onClick = onAppearanceClick,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SettingsAction(
                icon = R.drawable.ic_dynamic_color,
                label = R.string.photo_widget_home_dynamic_colors,
                onClick = onColorsClick,
            )
        }

        SettingsAction(
            icon = R.drawable.ic_rate,
            label = R.string.photo_widget_home_rate,
            onClick = onRateClick,
        )

        SettingsAction(
            icon = R.drawable.ic_share,
            label = R.string.photo_widget_home_share,
            onClick = onShareClick,
        )

        SettingsAction(
            icon = R.drawable.ic_help,
            label = R.string.photo_widget_home_help,
            onClick = onHelpClick,
        )

        Spacer(modifier = Modifier.weight(1f))

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        )

        Text(
            text = stringResource(id = R.string.photo_widget_home_developer),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 2.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
        )

        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable(
                    onClick = onViewLicensesClick,
                    role = Role.Button,
                ),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.photo_widget_home_version, BuildConfig.VERSION_NAME),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )

            Text(
                text = "—",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )

            Text(
                text = stringResource(id = R.string.photo_widget_home_view_licenses),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun SettingsAction(
    @DrawableRes icon: Int,
    @StringRes label: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                role = Role.Button,
            ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        AutoSizeText(
            text = stringResource(id = label),
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            minTextSize = 8.sp,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

// region Previews
@Composable
@ThemePreviews
@LocalePreviews
@DevicePreviews
private fun HomeScreenPreview() {
    ExtendedTheme {
        HomeScreen(
            onCreateNewWidgetClick = {},
            currentWidgets = emptyList(),
            onCurrentWidgetClick = {},
            onDefaultsClick = {},
            onAppearanceClick = {},
            onColorsClick = {},
            onRateClick = {},
            onShareClick = {},
            onHelpClick = {},
            onViewLicensesClick = {},
        )
    }
}

@Composable
@ThemePreviews
@LocalePreviews
@DevicePreviews
private fun MyWidgetsScreenPreview() {
    ExtendedTheme {
        val allShapeIds = PhotoWidgetShapeBuilder.buildAllShapes().map { it.key.id }

        MyWidgetsScreen(
            widgets = List(size = 10) { index ->
                index to PhotoWidget(
                    source = PhotoWidgetSource.PHOTOS,
                    photos = listOf(LocalPhoto(name = "photo-1")),
                    shuffle = false,
                    loopingInterval = PhotoWidgetLoopingInterval.ONE_DAY,
                    tapAction = PhotoWidgetTapAction.VIEW_FULL_SCREEN,
                    aspectRatio = when {
                        index % 3 == 0 -> PhotoWidgetAspectRatio.WIDE
                        index % 2 == 0 -> PhotoWidgetAspectRatio.TALL
                        else -> PhotoWidgetAspectRatio.SQUARE
                    },
                    shapeId = allShapeIds.random(),
                    cornerRadius = PhotoWidgetAspectRatio.DEFAULT_CORNER_RADIUS,
                )
            },
            onClick = {},
        )
    }
}

@Composable
@ThemePreviews
@LocalePreviews
@DevicePreviews
private fun MyWidgetsScreenEmptyPreview() {
    ExtendedTheme {
        MyWidgetsScreen(
            widgets = emptyList(),
            onClick = {},
        )
    }
}

@Composable
@ThemePreviews
@LocalePreviews
@DevicePreviews
private fun SettingsScreenPreview() {
    ExtendedTheme {
        SettingsScreen(
            onDefaultsClick = {},
            onAppearanceClick = {},
            onColorsClick = {},
            onRateClick = {},
            onShareClick = {},
            onHelpClick = {},
            onViewLicensesClick = {},
        )
    }
}
// endregion Previews
