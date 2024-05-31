package com.fibelatti.photowidget.platform

import androidx.appcompat.app.AppCompatDialog
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

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
