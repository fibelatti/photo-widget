package com.fibelatti.photowidget.platform

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.flow.MutableStateFlow

class ComposeBottomSheetDialog(
    context: Context,
    content: @Composable ComposeBottomSheetDialog.() -> Unit,
) : BottomSheetDialog(context, R.style.AppTheme_BottomSheetDialog) {

    val activityResult: MutableStateFlow<Any?> = MutableStateFlow(null)

    init {
        behavior.peekHeight = 1200.dp.value.toInt()
        behavior.skipCollapsed = true
        behavior.state = BottomSheetBehavior.STATE_EXPANDED

        setViewTreeOwners()

        setContentView(
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AppTheme {
                        content()
                    }
                }
            },
        )
    }

    fun setActivityResult(data: Any?) {
        activityResult.value = data
    }
}
