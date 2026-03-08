package com.fibelatti.photowidget.platform

import android.content.Context
import androidx.appcompat.app.AppCompatDialog
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder

fun AppCompatDialog.setViewTreeOwners() {
    val activity = context.findActivity()
    val decorView = window?.decorView
    if (activity != null && decorView != null) {
        // Even though `androidx.appcompat:appcompat:1.7.0` started setting `LifecycleOwner`
        // and `SavedStateRegistryOwner` it still doesn't set `ViewModelStoreOwner` so keep
        // setting all 3 to ensure they all use the same owner
        decorView.setViewTreeLifecycleOwner(activity as? LifecycleOwner)
        decorView.setViewTreeViewModelStoreOwner(activity as? ViewModelStoreOwner)
        decorView.setViewTreeSavedStateRegistryOwner(activity as? SavedStateRegistryOwner)
    }
}

fun Context.showMaterialAlertDialog(body: MaterialAlertDialogBuilder.() -> Unit) {
    val entryPoint: PhotoWidgetEntryPoint = entryPoint(this)

    val builder: MaterialAlertDialogBuilder = if (entryPoint.userPreferencesStorage().dynamicColors) {
        MaterialAlertDialogBuilder(
            /* context = */ DynamicColors.wrapContextIfAvailable(this),
            /* overrideThemeResId = */ R.style.AppTheme_MaterialDialog,
        )
    } else {
        MaterialAlertDialogBuilder(
            /* context = */ this,
            /* overrideThemeResId = */ R.style.AppTheme_MaterialDialog_DefaultColors,
        )
    }

    builder.apply { body() }.show()
}
