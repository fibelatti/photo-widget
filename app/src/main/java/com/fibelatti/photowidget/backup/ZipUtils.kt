package com.fibelatti.photowidget.backup

import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class ZipUtils @Inject constructor() {

    suspend fun writeToZip(sourceDir: File, destinationDir: File): File = withContext(Dispatchers.IO) {
        Timber.d("Zipping content...")

        val zipFile: File = File("$destinationDir/${sourceDir.name}.zip")
            .apply {
                if (exists()) delete()
                createNewFile()
            }

        val (count: Int, size: Long) = ZipOutputStream(zipFile.outputStream()).use { zipOutputStream ->
            zipDirectory(sourceDir = sourceDir, parentPath = sourceDir.name, zipOutputStream = zipOutputStream)
        }

        if (count == 0) {
            error("No files found in $sourceDir.")
        }

        Timber.i("Successfully zipped $count files to $zipFile ($size bytes total)")

        return@withContext zipFile
    }

    private fun zipDirectory(sourceDir: File, parentPath: String, zipOutputStream: ZipOutputStream): Pair<Int, Long> {
        val files: Array<File> = sourceDir.listFiles()
            ?: return 0 to 0L

        val filesToSize: List<Pair<Int, Long>> = files.map { file: File ->
            val entryName = "$parentPath/${file.name}"
            if (file.isDirectory) {
                zipOutputStream.putNextEntry(ZipEntry("$entryName/"))
                zipOutputStream.closeEntry()
                zipDirectory(sourceDir = file, parentPath = entryName, zipOutputStream = zipOutputStream)
            } else {
                zipOutputStream.putNextEntry(ZipEntry(entryName))
                FileInputStream(file).buffered().use { bufferedInputStream ->
                    bufferedInputStream.copyTo(zipOutputStream)
                }

                Timber.d("Zipped $entryName (${file.length()} bytes)")
                zipOutputStream.closeEntry()
                1 to file.length()
            }
        }

        return filesToSize.reduce { (count1, size1), (count2, size2) ->
            count1 + count2 to size1 + size2
        }
    }

    suspend fun extractZip(zipInputStream: ZipInputStream, destinationDir: File): File = withContext(Dispatchers.IO) {
        Timber.d("Unzipping content...")

        val extractedFiles: MutableList<String> = mutableListOf()
        var totalSize = 0L
        val parentDir: File

        try {
            var zipEntry: ZipEntry?
            while (zipInputStream.nextEntry.also { zipEntry = it } != null) {
                val currentEntry: ZipEntry = zipEntry ?: continue
                val sanitizedName: String = sanitizeFileName(fileName = currentEntry.name)
                val destFile = File(destinationDir, sanitizedName)

                if (!destFile.canonicalPath.startsWith(destinationDir.canonicalPath)) {
                    throw SecurityException("Zip slip attack detected: ${currentEntry.name}")
                }

                if (zipEntry.isDirectory) {
                    zipInputStream.closeEntry()
                    continue
                }

                destFile.parentFile?.mkdirs()

                val extractedSize: Long = extractEntry(
                    zipInputStream = zipInputStream,
                    destFile = destFile,
                    entry = currentEntry,
                )

                if (extractedSize >= 0) {
                    totalSize += extractedSize
                    extractedFiles += destFile.absolutePath

                    Timber.d("Extracted ${destFile.absolutePath} ($extractedSize bytes)")
                }

                zipInputStream.closeEntry()
            }

            val parentDirPath: String = extractedFiles
                .zipWithNext()
                .map { (first, second) -> first.commonPrefixWith(second) }
                .minBy { common -> common.length }

            parentDir = File(parentDirPath)

            if (!parentDir.exists()) {
                error("Expected parent directory does not exist (path=$parentDirPath)")
            }
        } catch (e: Exception) {
            cleanupFiles(extractedFiles)
            throw e
        }

        Timber.i("Successfully extracted ${extractedFiles.size} files to $parentDir ($totalSize bytes total)")

        return@withContext parentDir
    }

    private fun extractEntry(zipInputStream: ZipInputStream, destFile: File, entry: ZipEntry): Long {
        var totalBytesRead = 0L
        val buffer = ByteArray(size = 8_192)

        destFile.outputStream().use { outputStream ->
            var bytesRead: Int

            while (zipInputStream.read(buffer).also { bytesRead = it } != -1) {
                totalBytesRead += bytesRead
                outputStream.write(buffer, 0, bytesRead)
            }
        }

        val mimeType: String = Files.probeContentType(Paths.get(destFile.path))

        if (mimeType != "application/json" && !mimeType.startsWith("image/")) {
            Timber.w("Unexpected file found ($destFile)")
            destFile.delete()
            return -1
        }

        if (entry.size >= 0 && totalBytesRead != entry.size) {
            Timber.w("Size mismatch for ${entry.name} (declared=${entry.size}, actual=$totalBytesRead)")
        }

        return totalBytesRead
    }

    private fun sanitizeFileName(fileName: String): String {
        var sanitized = fileName.replace(oldValue = "../", newValue = "").replace(oldValue = "..\\", newValue = "")

        sanitized = sanitized.trimStart('/', '\\')

        sanitized = sanitized.replace(regex = Regex("[<>:\"|?*]"), replacement = "_")

        if (sanitized.isBlank()) {
            sanitized = "unnamed_file"
        }

        if (sanitized.length > 255) {
            val extension = sanitized.substringAfterLast('.', "")
            val nameWithoutExt = sanitized.substringBeforeLast('.')

            sanitized = nameWithoutExt.take(255 - extension.length - 1)

            if (extension.isNotEmpty()) {
                sanitized += ".$extension"
            }
        }

        return sanitized
    }

    private fun cleanupFiles(filePaths: List<String>) {
        filePaths.forEach { path ->
            try {
                File(path).delete()
            } catch (e: Exception) {
                Timber.w(e, "Failed to cleanup file (path=$path)")
            }
        }
    }
}
