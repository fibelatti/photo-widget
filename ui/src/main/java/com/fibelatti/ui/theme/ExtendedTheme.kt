@file:Suppress("Unused")

package com.fibelatti.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun ExtendedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    useTrueBlack: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) {
                val dynamicDarkColorScheme = dynamicDarkColorScheme(context)
                if (useTrueBlack) {
                    dynamicDarkColorScheme.copy(background = Color.Black)
                } else {
                    dynamicDarkColorScheme
                }
            } else {
                dynamicLightColorScheme(context)
            }
        }

        darkTheme -> {
            if (useTrueBlack) {
                DarkColorScheme.copy(background = Color.Black)
            } else {
                DarkColorScheme
            }
        }

        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
