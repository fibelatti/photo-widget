package com.fibelatti.photowidget.backup

import android.content.Context
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.widget.LoadPhotoWidgetUseCase
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber

class CreateBackupUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoWidgetStorage: PhotoWidgetStorage,
    private val loadPhotoWidgetUseCase: LoadPhotoWidgetUseCase,
    private val json: Json,
    private val zipUtils: ZipUtils,
) {

    suspend operator fun invoke(): File = withContext(Dispatchers.IO) {
        Timber.d("Creating backup. Loading widgets...")

        val widgetsToExport: Map<Int, PhotoWidget> = photoWidgetStorage.getKnownWidgetIds()
            .associateWith { id: Int -> loadPhotoWidgetUseCase(appWidgetId = id).first() }
            .filterValues { widget: PhotoWidget -> widget.source == PhotoWidgetSource.PHOTOS }

        val backupDir: File = createBackupFiles()
            .apply {
                exportData(backupDir = this, widgets = widgetsToExport)
                exportPhotos(backupDir = this, widgets = widgetsToExport)
            }

        val zipFile: File = try {
            zipUtils.writeToZip(sourceDir = backupDir, destinationDir = context.cacheDir)
        } finally {
            backupDir.deleteRecursively()
        }

        Timber.d("Backup file created successfully!")

        return@withContext zipFile
    }

    private fun createBackupFiles(): File {
        Timber.d("Creating backup files...")

        val timestamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val dirName = "mpw_backup_$timestamp"
        val backupDir: File = File("${context.cacheDir}/$dirName")
            .apply {
                if (exists()) deleteRecursively()
                mkdirs()
            }

        return backupDir
    }

    private fun exportData(backupDir: File, widgets: Map<Int, PhotoWidget>) {
        Timber.d("Populating backup json...")

        val backupJson: File = File("$backupDir/widgets.json")
            .apply { createNewFile() }
        val export: List<PhotoWidgetExport> = widgets.map { (id, photoWidget) ->
            photoWidget.toPhotoWidgetExport(id = id)
        }

        backupJson.writeText(text = json.encodeToString(export))
    }

    private suspend fun exportPhotos(backupDir: File, widgets: Map<Int, PhotoWidget>) {
        Timber.d("Copying photo files...")

        widgets.keys.forEach { widgetId: Int ->
            photoWidgetStorage.exportWidgetDir(appWidgetId = widgetId, destinationDir = backupDir)
        }
    }
}
