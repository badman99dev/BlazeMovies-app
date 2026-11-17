package com.movie.app.best.data.repository

import com.movie.app.best.data.debug.NetworkLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.FileHeader
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class ExtractionProgress(
    val percent: Int,
    val currentFile: String,
    val state: ExtractionState
)

enum class ExtractionState {
    IN_PROGRESS,
    COMPLETE,
    FAILED
}

@Singleton
class ZipExtractor @Inject constructor() {

    private val videoExtensions = listOf("mp4", "mkv", "avi", "webm", "mov", "flv", "3gp", "ts", "m4v")

    fun extractZipWithProgress(zipPath: String, baseDir: File): Flow<ExtractionProgress> = flow {
        val zipFile = File(zipPath)
        if (!zipFile.exists()) {
            NetworkLogger.logAction("ZIP_EXTRACT_ERR", "ZIP file not found: $zipPath")
            emit(ExtractionProgress(0, "", ExtractionState.FAILED))
            return@flow
        }

        val folderName = zipFile.nameWithoutExtension
        val extractDir = File(baseDir, folderName).apply { if (!exists()) mkdirs() }

        try {
            val zip = ZipFile(zipFile)

            val headers = zip.fileHeaders.toList()
            val totalFiles = headers.size

            if (totalFiles == 0) {
                NetworkLogger.logAction("ZIP_EXTRACT_ERR", "No files in ZIP")
                emit(ExtractionProgress(0, "", ExtractionState.FAILED))
                return@flow
            }

            NetworkLogger.logAction("ZIP_EXTRACT_START", "totalFiles=$totalFiles extractDir=${extractDir.path}")

            var extractedCount = 0

            headers.forEachIndexed { index, header ->
                val headerFileName = header.fileName

                if (!header.isDirectory) {
                    try {
                        zip.extractFile(header, extractDir.absolutePath)
                        extractedCount++
                        NetworkLogger.logAction("ZIP_EXTRACT_FILE", "[$extractedCount/$totalFiles] $headerFileName")
                    } catch (e: Exception) {
                        NetworkLogger.logAction("ZIP_EXTRACT_FILE_ERR", "$headerFileName: ${e.message}")
                    }
                }

                val percent = ((index + 1) * 100) / totalFiles
                emit(ExtractionProgress(percent, headerFileName, ExtractionState.IN_PROGRESS))
                delay(50)
            }

            val extractedVideos = mutableListOf<String>()
            scanForVideos(extractDir, extractedVideos)
            extractedVideos.sortBy { File(it).name }

            if (extractedVideos.isNotEmpty()) {
                NetworkLogger.logAction("ZIP_EXTRACT", "Done: ${extractedVideos.size} videos, $extractedCount files extracted to ${extractDir.path}")
                emit(ExtractionProgress(100, "", ExtractionState.COMPLETE))
            } else {
                NetworkLogger.logAction("ZIP_EXTRACT_ERR", "No videos found in ${extractDir.path}. Files extracted: $extractedCount")
                emit(ExtractionProgress(0, "", ExtractionState.FAILED))
            }
        } catch (e: Exception) {
            NetworkLogger.logAction("ZIP_EXTRACT_ERR", e.message ?: "unknown")
            throw e
        }
    }.catch { e ->
        // Never emit from inside the builder's catch (exception transparency); the
        // catch operator is the only safe place to report a failure.
        NetworkLogger.logAction("ZIP_FLOW_ERR", e.message ?: "unknown")
        emit(ExtractionProgress(0, "", ExtractionState.FAILED))
    }.flowOn(Dispatchers.IO)

    private fun scanForVideos(dir: File, result: MutableList<String>) {
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                scanForVideos(file, result)
            } else if (isVideoFile(file.name)) {
                result.add(file.absolutePath)
            }
        }
    }

    private fun isVideoFile(fileName: String): Boolean {
        val ext = fileName.substringAfterLast(".", "").lowercase()
        return videoExtensions.contains(ext)
    }
}
