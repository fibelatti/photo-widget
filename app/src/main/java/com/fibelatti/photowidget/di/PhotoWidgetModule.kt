package com.fibelatti.photowidget.di

import android.app.Application
import androidx.room.Room
import com.fibelatti.photowidget.widget.PhotoWidgetDatabase
import com.fibelatti.photowidget.widget.PhotoWidgetOrderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PhotoWidgetModule {

    @Provides
    fun coroutineScope(): CoroutineScope = CoroutineScope(context = Dispatchers.Default + SupervisorJob())

    @Provides
    @Singleton
    fun photoWidgetDatabase(
        application: Application,
    ): PhotoWidgetDatabase = Room.databaseBuilder(
        context = application,
        klass = PhotoWidgetDatabase::class.java,
        name = "com.fibelatti.photowidget.db",
    ).build()

    @Provides
    fun photoWidgetOrderDao(
        photoWidgetDatabase: PhotoWidgetDatabase,
    ): PhotoWidgetOrderDao = photoWidgetDatabase.photoWidgetOrderDao()
}
