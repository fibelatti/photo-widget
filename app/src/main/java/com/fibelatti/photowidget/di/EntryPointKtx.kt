package com.fibelatti.photowidget.di

import android.content.Context
import dagger.hilt.android.EntryPointAccessors

inline fun <reified T> entryPoint(context: Context): T {
    return EntryPointAccessors.fromApplication<T>(context = context.applicationContext)
}
