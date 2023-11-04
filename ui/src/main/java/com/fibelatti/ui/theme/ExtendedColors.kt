@file:Suppress("Unused")

package com.fibelatti.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// region Extended Colors
@Immutable
data class ExtendedColors(
    val backgroundNoOverlay: Color,
    val statusBar: Color,
    val navigationBar: Color,
    val icon: Color,
)

internal val ExtendedLightColorScheme = ExtendedColors(
    backgroundNoOverlay = Color(0xFFFFFFFF),
    statusBar = Color(0xFFC2C2C2),
    navigationBar = Color(0xFF9E9E9E),
    icon = Color(0xFF424242),
)

internal val ExtendedDarkColorScheme = ExtendedColors(
    backgroundNoOverlay = Color(0xFF000000),
    statusBar = Color(0x00000000),
    navigationBar = Color(0xFF000000),
    icon = Color(0xFFF5F5F5),
)

internal val LocalExtendedColors = staticCompositionLocalOf { ExtendedLightColorScheme }
// endregion Extended Colors

// region Material Colors
internal val LightColorScheme = lightColorScheme(
    primary = Color(0xFF186D28),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA2F6A0),
    onPrimaryContainer = Color(0xFF002105),

    secondary = Color(0xFF52634F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD5E8CF),
    onSecondaryContainer = Color(0xFF101F10),

    tertiary = Color(0xFF39656B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBCEBF1),
    onTertiaryContainer = Color(0xFF001F23),

    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFF410002),

    background = Color(0xFFFCFDF6),
    onBackground = Color(0xFF1A1C19),

    surface = Color(0xFFFCFDF6),
    onSurface = Color(0xFF1A1C19),
    surfaceVariant = Color(0xFFDEE5D9),
    onSurfaceVariant = Color(0xFF424940),

    outline = Color(0xFF72796F),
    outlineVariant = Color(0xFFC2C9BD),

    inverseSurface = Color(0xFF2F312D),
    inverseOnSurface = Color(0xFFF0F1EB),
    inversePrimary = Color(0xFF86D986),
)

internal val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF86D986),
    onPrimary = Color(0xFF00390D),
    primaryContainer = Color(0xFF005317),
    onPrimaryContainer = Color(0xFFA2F6A0),

    secondary = Color(0xFFB9CCB4),
    onSecondary = Color(0xFF253423),
    secondaryContainer = Color(0xFF3B4B39),
    onSecondaryContainer = Color(0xFFD5E8CF),

    tertiary = Color(0xFFA1CED5),
    onTertiary = Color(0xFF00363C),
    tertiaryContainer = Color(0xFF1F4D53),
    onTertiaryContainer = Color(0xFFBCEBF1),

    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF1A1C19),
    onBackground = Color(0xFFE2E3DD),

    surface = Color(0xFF1A1C19),
    onSurface = Color(0xFFE2E3DD),
    surfaceVariant = Color(0xFF424940),
    onSurfaceVariant = Color(0xFFC2C9BD),

    outline = Color(0xFF8C9388),
    outlineVariant = Color(0xFF424940),

    inverseSurface = Color(0xFFE2E3DD),
    inverseOnSurface = Color(0xFF1A1C19),
    inversePrimary = Color(0xFF186D28),
)
// endregion Material Colors
