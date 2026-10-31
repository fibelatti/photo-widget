package com.fibelatti.photowidget.di

import com.fibelatti.photowidget.widget.DefaultWidgetProviders
import com.fibelatti.photowidget.widget.WidgetProviders
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface BindingsModule {

    @Binds
    fun widgetProviders(impl: DefaultWidgetProviders): WidgetProviders
}
