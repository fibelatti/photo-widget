@file:Suppress("Unused")

package com.fibelatti.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

internal val LightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0XFF3A693B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0XFFBBF0B6),
    onPrimaryContainer = Color(0xFF225025),
    inversePrimary = Color(0xFF9FD49B),

    secondary = Color(0XFF52634F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0XFFD5E8CF),
    onSecondaryContainer = Color(0XFF3B4B39),

    tertiary = Color(0xFF39656B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBCEBF1),
    onTertiaryContainer = Color(0xFF1F4D53),

    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),

    background = Color(0xFFF7FBF1),
    onBackground = Color(0xFF181D17),

    surface = Color(0xFFF7FBF1),
    surfaceDim = Color(0xFFD7DBD2),
    surfaceBright = Color(0xFFF7FBF1),
    surfaceVariant = Color(0xFFDEE5D9),
    surfaceTint = Color(0xFF3A693B),

    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F5EC),
    surfaceContainer = Color(0xFFEBEFE6),
    surfaceContainerHigh = Color(0xFFE6E9E0),
    surfaceContainerHighest = Color(0xFFE0E4DB),

    onSurface = Color(0xFF181D17),
    onSurfaceVariant = Color(0xFF424940),

    inverseSurface = Color(0xFF2D322C),
    inverseOnSurface = Color(0xFFEEF2E9),

    outline = Color(0xFF72796F),
    outlineVariant = Color(0xFFC2C9BD),

    scrim = Color(0xFF000000),

    primaryFixed = Color(0xFFBBF0B6),
    onPrimaryFixed = Color(0xFF002105),
    primaryFixedDim = Color(0xFF9FD49B),
    onPrimaryFixedVariant = Color(0xFF225025),

    secondaryFixed = Color(0xFFD5E8CF),
    onSecondaryFixed = Color(0xFF101F10),
    secondaryFixedDim = Color(0xFFB9CCB4),
    onSecondaryFixedVariant = Color(0xFF3B4B39),

    tertiaryFixed = Color(0xFFBCEBF1),
    onTertiaryFixed = Color(0xFF001F23),
    tertiaryFixedDim = Color(0xFFA1CED5),
    onTertiaryFixedVariant = Color(0xFF1F4D53),
)

internal val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF9FD49B),
    onPrimary = Color(0xFF073910),
    primaryContainer = Color(0xFF225025),
    onPrimaryContainer = Color(0xFFBBF0B6),
    inversePrimary = Color(0xFF3A693B),

    secondary = Color(0xFFB9CCB4),
    onSecondary = Color(0xFF253424),
    secondaryContainer = Color(0xFF3B4B39),
    onSecondaryContainer = Color(0xFFD5E8CF),

    tertiary = Color(0xFFA1CED5),
    onTertiary = Color(0xFF00363C),
    tertiaryContainer = Color(0xFF1F4D53),
    onTertiaryContainer = Color(0xFFBCEBF1),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF10140F),
    onBackground = Color(0xFFE0E4DB),

    surface = Color(0xFF10140F),
    surfaceDim = Color(0xFF10140F),
    surfaceBright = Color(0xFF363A34),
    surfaceVariant = Color(0xFF424940),
    surfaceTint = Color(0xFF9FD49B),

    surfaceContainerLowest = Color(0xFF0B0F0A),
    surfaceContainerLow = Color(0xFF181D17),
    surfaceContainer = Color(0xFF1C211B),
    surfaceContainerHigh = Color(0xFF272B25),
    surfaceContainerHighest = Color(0xFF323630),

    onSurface = Color(0xFFE0E4DB),
    onSurfaceVariant = Color(0xFFC2C9BD),

    inverseSurface = Color(0xFFE0E4DB),
    inverseOnSurface = Color(0xFF2D322C),

    outline = Color(0xFF8C9388),
    outlineVariant = Color(0xFF424940),

    scrim = Color(0xFF000000),

    primaryFixed = Color(0xFFBBF0B6),
    onPrimaryFixed = Color(0xFF002105),
    primaryFixedDim = Color(0xFF9FD49B),
    onPrimaryFixedVariant = Color(0xFF225025),

    secondaryFixed = Color(0xFFD5E8CF),
    onSecondaryFixed = Color(0xFF101F10),
    secondaryFixedDim = Color(0xFFB9CCB4),
    onSecondaryFixedVariant = Color(0xFF3B4B39),

    tertiaryFixed = Color(0xFFBCEBF1),
    onTertiaryFixed = Color(0xFF001F23),
    tertiaryFixedDim = Color(0xFFA1CED5),
    onTertiaryFixedVariant = Color(0xFF1F4D53),
)
