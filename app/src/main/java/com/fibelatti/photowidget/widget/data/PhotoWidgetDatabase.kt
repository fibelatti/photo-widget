package com.fibelatti.photowidget.widget.data

import androidx.room.AutoMigration
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert

@Database(
    entities = [
        PhotoWidgetOrderDto::class,
        PendingDeletionWidgetPhotoDto::class,
        ExcludedWidgetPhotoDto::class,
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
    ],
)
abstract class PhotoWidgetDatabase : RoomDatabase() {

    abstract fun photoWidgetOrderDao(): PhotoWidgetOrderDao

    abstract fun pendingDeletionWidgetPhotoDao(): PendingDeletionWidgetPhotoDao

    abstract fun excludedWidgetPhotoDao(): ExcludedWidgetPhotoDao
}

@Entity(
    tableName = "photo_widget_order",
    primaryKeys = ["widgetId", "photoIndex"],
)
data class PhotoWidgetOrderDto(
    val widgetId: Int,
    val photoIndex: Int,
    val photoId: String,
)

@Dao
interface PhotoWidgetOrderDao {

    @Query("select photoId from photo_widget_order where widgetId = :appWidgetId")
    suspend fun getWidgetOrder(appWidgetId: Int): List<String>

    @Upsert
    suspend fun saveWidgetOrder(order: List<PhotoWidgetOrderDto>)

    @Query("delete from photo_widget_order where widgetId = :appWidgetId")
    suspend fun deleteWidgetOrder(appWidgetId: Int)

    @Transaction
    suspend fun replaceWidgetOrder(widgetId: Int, order: List<PhotoWidgetOrderDto>) {
        deleteWidgetOrder(widgetId)
        saveWidgetOrder(order)
    }
}

@Entity(
    tableName = "pending_deletion_widget_photos",
    primaryKeys = ["widgetId", "photoId"],
)
data class PendingDeletionWidgetPhotoDto(
    val widgetId: Int,
    val photoId: String,
    val deletionTimestamp: Long,
)

@Dao
interface PendingDeletionWidgetPhotoDao {

    @Upsert
    suspend fun savePendingDeletionPhotos(photos: List<PendingDeletionWidgetPhotoDto>)

    @Query("select * from pending_deletion_widget_photos where widgetId = :widgetId")
    suspend fun getPendingDeletionPhotos(widgetId: Int): List<PendingDeletionWidgetPhotoDto>

    @Query("delete from pending_deletion_widget_photos where widgetId = :widgetId")
    suspend fun deletePhotosByWidgetId(widgetId: Int)

    @Query("select * from pending_deletion_widget_photos where deletionTimestamp <= :timestamp")
    suspend fun getPhotosToDelete(timestamp: Long): List<PendingDeletionWidgetPhotoDto>

    @Query("delete from pending_deletion_widget_photos where deletionTimestamp <= :timestamp")
    suspend fun deletePhotosBeforeTimestamp(timestamp: Long)
}

@Entity(
    tableName = "excluded_widget_photos",
    primaryKeys = ["widgetId", "photoId"],
)
data class ExcludedWidgetPhotoDto(
    val widgetId: Int,
    val photoId: String,
)

@Dao
interface ExcludedWidgetPhotoDao {

    @Upsert
    suspend fun saveExcludedPhotos(photos: List<ExcludedWidgetPhotoDto>)

    @Query("select * from excluded_widget_photos where widgetId = :widgetId")
    suspend fun getExcludedPhotos(widgetId: Int): List<ExcludedWidgetPhotoDto>

    @Query("delete from excluded_widget_photos where widgetId = :widgetId")
    suspend fun deletePhotosByWidgetId(widgetId: Int)
}
