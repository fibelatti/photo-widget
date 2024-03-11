package com.fibelatti.photowidget.di

import com.fibelatti.photowidget.configure.SavePhotoWidgetUseCase
import com.fibelatti.photowidget.widget.FlipPhotoUseCase
import com.fibelatti.photowidget.widget.LoadPhotoWidgetUseCase
import com.fibelatti.photowidget.widget.PhotoWidgetAlarmManager
import com.fibelatti.photowidget.widget.PhotoWidgetStorage
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PhotoWidgetEntryPoint {

    fun photoWidgetStorage(): PhotoWidgetStorage

    fun photoWidgetAlarmManager(): PhotoWidgetAlarmManager

    fun loadPhotoWidgetUseCase(): LoadPhotoWidgetUseCase

    fun savePhotoWidgetUseCase(): SavePhotoWidgetUseCase

    fun flipPhotoUseCase(): FlipPhotoUseCase
}
