package com.fibelatti.photowidget.di

import com.fibelatti.photowidget.configure.SavePhotoWidgetUseCase
import com.fibelatti.photowidget.platform.PhotoDecoder
import com.fibelatti.photowidget.preferences.UserPreferencesStorage
import com.fibelatti.photowidget.widget.CyclePhotoUseCase
import com.fibelatti.photowidget.widget.LoadPhotoWidgetUseCase
import com.fibelatti.photowidget.widget.PhotoWidgetAlarmManager
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PhotoWidgetEntryPoint {

    fun userPreferencesStorage(): UserPreferencesStorage

    fun photoWidgetStorage(): PhotoWidgetStorage

    fun photoWidgetAlarmManager(): PhotoWidgetAlarmManager

    fun loadPhotoWidgetUseCase(): LoadPhotoWidgetUseCase

    fun savePhotoWidgetUseCase(): SavePhotoWidgetUseCase

    fun flipPhotoUseCase(): CyclePhotoUseCase

    fun photoDecoder(): PhotoDecoder

    fun coroutineScope(): CoroutineScope
}
