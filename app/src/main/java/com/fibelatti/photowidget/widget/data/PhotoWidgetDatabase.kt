package com.fibelatti.photowidget.widget.data

import androidx.room.AutoMigration
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert

@Database(
    entities = [
        LocalPhotoDto::class,
        DisplayedWidgetPhotoDto::class,
        PhotoWidgetOrderDto::class,
        PendingDeletionWidgetPhotoDto::class,
        ExcludedWidgetPhotoDto::class,
        WidgetDirectoryDto::class,
    ],
    version = 5,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
    ],
)
abstract class PhotoWidgetDatabase : RoomDatabase() {

    abstract fun localPhotoDao(): LocalPhotoDao

    abstract fun displayedPhotoDao(): DisplayedPhotoDao

    abstract fun photoWidgetOrderDao(): PhotoWidgetOrderDao

    abstract fun pendingDeletionWidgetPhotoDao(): PendingDeletionWidgetPhotoDao

    abstract fun excludedWidgetPhotoDao(): ExcludedWidgetPhotoDao

    abstract fun widgetDirectoryDao(): WidgetDirectoryDao
}

@Entity(
    tableName = "local_widget_photos",
    primaryKeys = ["widgetId", "photoId"],
)
data class LocalPhotoDto(
    val widgetId: Int,
    val photoId: String,
    val croppedPhotoPath: String? = null,
    val originalPhotoPath: String? = null,
    val externalUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
)

@Dao
interface LocalPhotoDao {

    @Query(
        "select lwp.* from local_widget_photos as lwp " +
            "left join photo_widget_order pwo " +
            "on lwp.widgetId = pwo.widgetId " +
            "and lwp.photoId = pwo.photoId " +
            "where lwp.widgetId = :widgetId " +
            "order by pwo.photoIndex asc",
    )
    suspend fun getLocalPhotos(widgetId: Int): List<LocalPhotoDto>

    @Query("select photoId from local_widget_photos where widgetId = :widgetId")
    suspend fun getLocalPhotoIds(widgetId: Int): List<String>

    @Upsert
    suspend fun saveLocalPhotos(photos: Collection<LocalPhotoDto>)

    @Query("delete from local_widget_photos where widgetId = :widgetId and photoId in (:photoIds)")
    suspend fun deletePhotosByPhotoIds(widgetId: Int, photoIds: Collection<String>)

    @Query("delete from local_widget_photos where widgetId = :widgetId")
    suspend fun deletePhotosByWidgetId(widgetId: Int)

    @Transaction
    suspend fun replacePhotos(
        widgetId: Int,
        photos: Collection<LocalPhotoDto>,
        idsToDelete: Collection<String>,
    ) {
        if (idsToDelete.isNotEmpty()) {
            deletePhotosByPhotoIds(widgetId = widgetId, photoIds = idsToDelete)
        }
        saveLocalPhotos(photos = photos)
    }
}

@Entity(
    tableName = "displayed_widget_photos",
    primaryKeys = ["widgetId", "photoId"],
)
data class DisplayedWidgetPhotoDto(
    val widgetId: Int,
    val photoId: String,
    val timestamp: Long,
)

@Dao
interface DisplayedPhotoDao {

    @Query("select photoId from displayed_widget_photos where widgetId = :widgetId")
    suspend fun getDisplayedPhotoIds(widgetId: Int): List<String>

    @Query("select photoId from displayed_widget_photos where widgetId = :widgetId order by timestamp desc limit 1")
    suspend fun getCurrentPhotoId(widgetId: Int): String?

    @Upsert
    suspend fun savePhoto(displayedWidgetPhotoDto: DisplayedWidgetPhotoDto)

    @Query("delete from displayed_widget_photos where widgetId = :widgetId and photoId in (:photoIds)")
    suspend fun deletePhotosByPhotoIds(widgetId: Int, photoIds: Collection<String>)

    @Query("delete from displayed_widget_photos where widgetId = :widgetId")
    suspend fun deletePhotosByWidgetId(widgetId: Int)

    @Query(
        "delete from displayed_widget_photos " +
            "where widgetId = :widgetId " +
            "and photoId = (select photoId from displayed_widget_photos " +
            "where widgetId = :widgetId order by timestamp desc limit 1)",
    )
    suspend fun deleteMostRecentPhoto(widgetId: Int)
}

@Entity(
    tableName = "photo_widget_order",
    primaryKeys = ["widgetId", "photoId"],
)
data class PhotoWidgetOrderDto(
    val widgetId: Int,
    val photoIndex: Int,
    val photoId: String,
)

@Dao
interface PhotoWidgetOrderDao {

    @Query("select photoId from photo_widget_order where widgetId = :widgetId order by photoIndex asc")
    suspend fun getWidgetOrder(widgetId: Int): List<String>

    @Query("select * from photo_widget_order where widgetId = :widgetId order by photoIndex asc")
    suspend fun getWidgetOrderObject(widgetId: Int): List<PhotoWidgetOrderDto>

    @Upsert
    suspend fun saveWidgetOrder(order: Collection<PhotoWidgetOrderDto>)

    @Query("delete from photo_widget_order where widgetId = :widgetId and photoId in (:photoIds)")
    suspend fun deletePhotosByPhotoIds(widgetId: Int, photoIds: Collection<String>)

    @Query("delete from photo_widget_order where widgetId = :widgetId")
    suspend fun deletePhotosByWidgetId(widgetId: Int)

    @Transaction
    suspend fun replaceWidgetOrder(
        widgetId: Int,
        order: Collection<PhotoWidgetOrderDto>,
        idsToDelete: Collection<String>,
    ) {
        if (idsToDelete.isNotEmpty()) {
            deletePhotosByPhotoIds(widgetId = widgetId, photoIds = idsToDelete)
        }
        saveWidgetOrder(order)
    }

    @Transaction
    suspend fun copyWidgetOrder(sourceWidgetId: Int, targetWidgetId: Int) {
        val source: List<PhotoWidgetOrderDto> = getWidgetOrderObject(sourceWidgetId)
        saveWidgetOrder(order = source.map { it.copy(widgetId = targetWidgetId) })
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
    suspend fun savePendingDeletionPhotos(photos: Collection<PendingDeletionWidgetPhotoDto>)

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
    suspend fun saveExcludedPhotos(photos: Collection<ExcludedWidgetPhotoDto>)

    @Query("select * from excluded_widget_photos where widgetId = :widgetId")
    suspend fun getExcludedPhotos(widgetId: Int): List<ExcludedWidgetPhotoDto>

    @Query("delete from excluded_widget_photos where widgetId = :widgetId")
    suspend fun deletePhotosByWidgetId(widgetId: Int)
}

@Entity(tableName = "widget_directories")
data class WidgetDirectoryDto(
    @PrimaryKey val directoryName: String,
    val widgetId: Int,
)

@Dao
interface WidgetDirectoryDao {

    @Query("select directoryName from widget_directories where widgetId = :widgetId")
    suspend fun getDirectoryName(widgetId: Int): String?

    @Query("select * from widget_directories")
    suspend fun getAll(): List<WidgetDirectoryDto>

    @Query("select * from widget_directories where widgetId = 0")
    suspend fun getDraftDirectories(): List<WidgetDirectoryDto>

    @Query("update widget_directories set widgetId = :newWidgetId where widgetId = :oldWidgetId")
    suspend fun updateWidgetId(oldWidgetId: Int, newWidgetId: Int)

    @Upsert
    suspend fun insert(directory: WidgetDirectoryDto)

    @Upsert
    suspend fun insert(directories: List<WidgetDirectoryDto>)

    @Query("delete from widget_directories where widgetId = :widgetId")
    suspend fun deleteByWidgetId(widgetId: Int)

    @Query("delete from widget_directories where directoryName = :directoryName")
    suspend fun deleteByDirectoryName(directoryName: String)
}
