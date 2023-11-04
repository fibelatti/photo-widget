@file:Suppress("Unused")

package com.fibelatti.ui.foundation

import android.os.Build
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

val WindowInsets.Companion.imeCompat: WindowInsets
    @Composable
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ime
    } else {
        var insetValue by remember { mutableIntStateOf(0) }

        ViewCompat.setOnApplyWindowInsetsListener(LocalView.current.rootView) { _, insets ->
            insetValue = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            insets
        }

        WindowInsets(bottom = insetValue)
    }

val WindowInsets.Companion.navigationBarsCompat: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        navigationBars
    } else {
        val inset = ViewCompat.getRootWindowInsets(LocalView.current)
            ?.getInsets(WindowInsetsCompat.Type.navigationBars())

        WindowInsets(
            left = (inset?.left ?: 0).pxToDp(),
            top = (inset?.top ?: 0).pxToDp(),
            right = (inset?.right ?: 0).pxToDp(),
            bottom = (inset?.bottom ?: 0).pxToDp(),
        )
    }

@Composable
@ReadOnlyComposable
fun WindowInsets.asHorizontalPaddingDp(
    addStart: Dp = 0.dp,
    addEnd: Dp = 0.dp,
): Pair<Dp, Dp> {
    val base = add(WindowInsets(left = addStart, right = addEnd)).asPaddingValues()

    return base.calculateLeftPadding(LayoutDirection.Ltr) to base.calculateRightPadding(LayoutDirection.Ltr)
}

fun Modifier.imePaddingCompat(): Modifier = composed {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        imePadding()
    } else {
        var insetValue by remember { mutableIntStateOf(0) }

        ViewCompat.setOnApplyWindowInsetsListener(LocalView.current.rootView) { _, insets ->
            insetValue = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            insets
        }

        padding(bottom = insetValue.pxToDp())
    }
}

fun Modifier.navigationBarsPaddingCompat(): Modifier = composed {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        navigationBarsPadding()
    } else {
        val inset = ViewCompat.getRootWindowInsets(LocalView.current)
            ?.getInsets(WindowInsetsCompat.Type.navigationBars())

        padding(
            start = (inset?.left ?: 0).pxToDp(),
            top = (inset?.top ?: 0).pxToDp(),
            end = (inset?.right ?: 0).pxToDp(),
            bottom = (inset?.bottom ?: 0).pxToDp(),
        )
    }
}

fun Modifier.topSystemBarsPaddingCompat(): Modifier = composed {
    val inset = ViewCompat.getRootWindowInsets(LocalView.current)
        ?.getInsets(WindowInsetsCompat.Type.systemBars())

    padding(
        start = (inset?.left ?: 0).pxToDp(),
        top = (inset?.top ?: 0).pxToDp(),
        end = (inset?.right ?: 0).pxToDp(),
    )
}
