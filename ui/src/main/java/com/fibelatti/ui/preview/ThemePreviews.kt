@file:Suppress("Unused")

package com.fibelatti.ui.preview

import android.content.res.Configuration
import android.os.Build
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers

@Preview(
    name = "Theme — Light",
    group = "Themes",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Preview(
    name = "Theme — Dark",
    group = "Themes",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Preview(
    name = "Colors — Red wallpaper",
    group = "Dynamic Colors",
    apiLevel = Build.VERSION_CODES.S,
    showBackground = true,
    wallpaper = Wallpapers.RED_DOMINATED_EXAMPLE,
)
@Preview(
    name = "Colors — Blue wallpaper",
    group = "Dynamic Colors",
    apiLevel = Build.VERSION_CODES.S,
    showBackground = true,
    wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE,
)
@Preview(
    name = "Colors — Green wallpaper",
    group = "Dynamic Colors",
    apiLevel = Build.VERSION_CODES.S,
    showBackground = true,
    wallpaper = Wallpapers.GREEN_DOMINATED_EXAMPLE,
)
@Preview(
    name = "Colors — Yellow wallpaper",
    group = "Dynamic Colors",
    apiLevel = Build.VERSION_CODES.S,
    showBackground = true,
    wallpaper = Wallpapers.YELLOW_DOMINATED_EXAMPLE,
)
annotation class ThemePreviews
