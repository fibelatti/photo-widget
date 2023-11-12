package com.fibelatti.photowidget.di

import com.fibelatti.photowidget.configure.SavePhotoWidgetUseCase
import com.fibelatti.photowidget.widget.PhotoWidgetStorage
import com.fibelatti.photowidget.widget.PhotoWidgetWorkManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PhotoWidgetEntryPoint {

    fun photoWidgetStorage(): PhotoWidgetStorage

    fun photoWidgetWorkManager(): PhotoWidgetWorkManager

    fun savePhotoWidgetUseCase(): SavePhotoWidgetUseCase
}
