package com.fibelatti.photowidget.widget

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert

@Database(
    entities = [PhotoWidgetOrderDto::class],
    version = 1,
    exportSchema = false,
)
abstract class PhotoWidgetDatabase : RoomDatabase() {

    abstract fun photoWidgetOrderDao(): PhotoWidgetOrderDao
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
