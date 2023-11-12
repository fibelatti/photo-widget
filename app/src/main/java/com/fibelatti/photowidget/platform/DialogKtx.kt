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
        decorView.setViewTreeLifecycleOwner(activity as? LifecycleOwner)
        decorView.setViewTreeViewModelStoreOwner(activity as? ViewModelStoreOwner)
        decorView.setViewTreeSavedStateRegistryOwner(activity as? SavedStateRegistryOwner)
    }
}
