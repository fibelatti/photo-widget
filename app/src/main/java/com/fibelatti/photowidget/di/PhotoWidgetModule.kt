package com.fibelatti.photowidget.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.memoryCacheMaxSizePercentWhileInBackground
import coil3.request.addLastModifiedToFileCacheKey
import coil3.request.allowHardware
import com.fibelatti.photowidget.widget.data.DisplayedPhotoDao
import com.fibelatti.photowidget.widget.data.ExcludedWidgetPhotoDao
import com.fibelatti.photowidget.widget.data.LocalPhotoDao
import com.fibelatti.photowidget.widget.data.PendingDeletionWidgetPhotoDao
import com.fibelatti.photowidget.widget.data.PhotoWidgetDatabase
import com.fibelatti.photowidget.widget.data.PhotoWidgetOrderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

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
    fun localPhotoDao(
        photoWidgetDatabase: PhotoWidgetDatabase,
    ): LocalPhotoDao = photoWidgetDatabase.localPhotoDao()

    @Provides
    fun displayedPhotoDao(
        photoWidgetDatabase: PhotoWidgetDatabase,
    ): DisplayedPhotoDao = photoWidgetDatabase.displayedPhotoDao()

    @Provides
    fun photoWidgetOrderDao(
        photoWidgetDatabase: PhotoWidgetDatabase,
    ): PhotoWidgetOrderDao = photoWidgetDatabase.photoWidgetOrderDao()

    @Provides
    fun photoPendingDeletionPhotoDao(
        photoWidgetDatabase: PhotoWidgetDatabase,
    ): PendingDeletionWidgetPhotoDao = photoWidgetDatabase.pendingDeletionWidgetPhotoDao()

    @Provides
    fun excludedPhotoDao(
        photoWidgetDatabase: PhotoWidgetDatabase,
    ): ExcludedWidgetPhotoDao = photoWidgetDatabase.excludedWidgetPhotoDao()

    @Provides
    @Singleton
    @OptIn(ExperimentalCoilApi::class)
    fun imageLoader(@ApplicationContext context: Context): ImageLoader = ImageLoader.Builder(context)
        .memoryCache {
            MemoryCache.Builder()
                .maxSizePercent(context = context, percent = 0.25)
                .build()
        }
        .memoryCacheMaxSizePercentWhileInBackground(percent = 0.25)
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve(relative = "image_cache"))
                .maxSizePercent(0.02)
                .build()
        }
        .interceptorCoroutineContext(Dispatchers.IO)
        .addLastModifiedToFileCacheKey(enable = true)
        .allowHardware(enable = false)
        .build()
}
