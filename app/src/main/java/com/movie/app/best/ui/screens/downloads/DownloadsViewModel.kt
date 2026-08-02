package com.movie.app.best.ui.screens.downloads

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.DownloadMetadata
import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.repository.DownloadRepository
import com.movie.app.best.data.repository.ExtractionProgress
import com.movie.app.best.data.repository.ExtractionState
import com.ketch.DownloadModel
import com.ketch.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class UnifiedDownloadItem(
    val id: String,
    val title: String,
    val fileName: String,
    val posterPath: String,
    val isZip: Boolean,
    val phase: UnifiedDownloadPhase,
    val progress: Int,
    val speedBytesPerSec: Long,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val ketchId: Int,
    val filePath: String,
    val extractPath: String?,
    val slug: String,
    val failureReason: String?,
    val episodeCount: Int,
    val extractionProgress: Int = 0
)

enum class UnifiedDownloadPhase {
    DOWNLOADING, PAUSED, EXTRACTING, COMPLETE, FAILED
}

data class DownloadsUiState(
    val unifiedDownloads: List<UnifiedDownloadItem> = emptyList(),
    val isResolving: Boolean = false,
    val resolveError: String? = null,
    val resolveSuccess: Boolean = false,
    val isRescanning: Boolean = false
)

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val repository: DownloadRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    private val processedKetchIds = mutableSetOf<Int>()

    init {
        rescanDownloads()
        observeDownloads()
    }

    private fun buildUnifiedList(downloads: List<DownloadModel>?): List<UnifiedDownloadItem> {
        val allMeta = repository.getAllMetadata()
        val scannedVideos = scanBlazeMoviesVideos(context)
        val result = mutableListOf<UnifiedDownloadItem>()
        val seenKetchIds = mutableSetOf<Int>()

        if (downloads != null) {
            downloads.filter {
                it.status == Status.QUEUED || it.status == Status.STARTED ||
                it.status == Status.PROGRESS || it.status == Status.PAUSED ||
                it.status == Status.FAILED
            }.forEach { dl ->
                seenKetchIds.add(dl.id)
                val meta = allMeta.find { it.ketchId == dl.id }
                val isZip = meta?.isZip ?: false
                val phase = when (dl.status) {
                    Status.PAUSED -> UnifiedDownloadPhase.PAUSED
                    Status.FAILED -> UnifiedDownloadPhase.FAILED
                    else -> UnifiedDownloadPhase.DOWNLOADING
                }
                result.add(
                    UnifiedDownloadItem(
                        id = "ketch_${dl.id}",
                        title = meta?.title ?: dl.fileName.substringBeforeLast("."),
                        fileName = dl.fileName.ifEmpty { dl.url.substringAfterLast("/") },
                        posterPath = meta?.localPosterPath ?: "",
                        isZip = isZip,
                        phase = phase,
                        progress = dl.progress,
                        speedBytesPerSec = (dl.speedInBytePerMs * 1000).toLong(),
                        totalBytes = dl.total,
                        downloadedBytes = (dl.progress.toLong() * dl.total) / 100,
                        ketchId = dl.id,
                        filePath = meta?.filePath ?: "",
                        extractPath = meta?.extractPath,
                        slug = meta?.slug ?: "",
                        failureReason = if (dl.status == Status.FAILED) dl.failureReason else null,
                        episodeCount = 0
                    )
                )
            }

            // Safety net: SUCCESS ZIP downloads that haven't been extracted yet
            downloads?.filter { it.status == Status.SUCCESS }?.forEach { dl ->
                if (dl.id in seenKetchIds) return@forEach
                val meta = allMeta.find { it.ketchId == dl.id }
                if (meta?.isZip == true && meta.extractPath == null) {
                    seenKetchIds.add(dl.id)
                    result.add(
                        UnifiedDownloadItem(
                            id = "extracting_${meta.slug}_${meta.episodeLabel ?: ""}",
                            title = meta.title,
                            fileName = meta.fileName,
                            posterPath = meta.localPosterPath,
                            isZip = true,
                            phase = UnifiedDownloadPhase.EXTRACTING,
                            progress = meta.extractionProgress,
                            speedBytesPerSec = 0,
                            totalBytes = 0,
                            downloadedBytes = 0,
                            ketchId = meta.ketchId,
                            filePath = meta.filePath,
                            extractPath = null,
                            slug = meta.slug,
                            failureReason = null,
                            episodeCount = 0,
                            extractionProgress = meta.extractionProgress
                        )
                    )
                }
            }
        }

        allMeta.filter { it.status == "extracting" }.forEach { meta ->
            if (meta.ketchId in seenKetchIds) return@forEach
            result.add(
                UnifiedDownloadItem(
                    id = "extracting_${meta.slug}_${meta.episodeLabel ?: ""}",
                    title = meta.title,
                    fileName = meta.fileName,
                    posterPath = meta.localPosterPath,
                    isZip = true,
                    phase = UnifiedDownloadPhase.EXTRACTING,
                    progress = meta.extractionProgress,
                    speedBytesPerSec = 0,
                    totalBytes = 0,
                    downloadedBytes = 0,
                    ketchId = meta.ketchId,
                    filePath = meta.filePath,
                    extractPath = null,
                    slug = meta.slug,
                    failureReason = null,
                    episodeCount = 0,
                    extractionProgress = meta.extractionProgress
                )
            )
        }

        allMeta.filter { it.isZip && it.extractPath != null }.forEach { meta ->
            val episodeCount = countVideosInDir(meta.extractPath!!)
            result.add(
                UnifiedDownloadItem(
                    id = "pack_${meta.slug}_${meta.episodeLabel ?: ""}",
                    title = meta.title,
                    fileName = meta.fileName,
                    posterPath = meta.localPosterPath,
                    isZip = true,
                    phase = UnifiedDownloadPhase.COMPLETE,
                    progress = 100,
                    speedBytesPerSec = 0,
                    totalBytes = 0,
                    downloadedBytes = 0,
                    ketchId = meta.ketchId,
                    filePath = meta.extractPath!!,
                    extractPath = meta.extractPath,
                    slug = meta.slug,
                    failureReason = null,
                    episodeCount = episodeCount
                )
            )
        }

        scannedVideos.forEach { video ->
            val isInExtractedPack = allMeta.any {
                it.isZip && it.extractPath != null && video.path.startsWith(it.extractPath!!)
            }
            if (isInExtractedPack) return@forEach

            val meta = allMeta.find { it.fileName == video.name }
            result.add(
                UnifiedDownloadItem(
                    id = "video_${video.path}",
                    title = meta?.title ?: video.name.substringBeforeLast("."),
                    fileName = video.name,
                    posterPath = meta?.localPosterPath ?: "",
                    isZip = false,
                    phase = UnifiedDownloadPhase.COMPLETE,
                    progress = 100,
                    speedBytesPerSec = 0,
                    totalBytes = video.size,
                    downloadedBytes = video.size,
                    ketchId = meta?.ketchId ?: -1,
                    filePath = video.path,
                    extractPath = null,
                    slug = meta?.slug ?: "",
                    failureReason = null,
                    episodeCount = 0
                )
            )
        }

        return result.sortedBy { item ->
            when (item.phase) {
                UnifiedDownloadPhase.DOWNLOADING -> 0
                UnifiedDownloadPhase.PAUSED -> 1
                UnifiedDownloadPhase.FAILED -> 2
                UnifiedDownloadPhase.EXTRACTING -> 3
                UnifiedDownloadPhase.COMPLETE -> 4
            }
        }
    }

    private fun countVideosInDir(dirPath: String): Int {
        val dir = File(dirPath)
        if (!dir.exists() || !dir.isDirectory) return 0
        val videoExts = setOf("mp4", "mkv", "avi", "webm", "mov", "flv", "3gp", "ts", "m4v")
        return dir.walkTopDown().count {
            it.isFile && it.extension.lowercase() in videoExts
        }
    }

    private fun refreshUiState(downloads: List<DownloadModel>? = null) {
        val unified = buildUnifiedList(downloads)
        _uiState.update {
            it.copy(unifiedDownloads = unified)
        }
    }

    private fun observeDownloads() {
        viewModelScope.launch {
            repository.observeDownloads().collect { downloads ->
                val completed = downloads.filter { it.status == Status.SUCCESS }

                completed.forEach { dl ->
                    if (dl.id !in processedKetchIds) {
                        processedKetchIds.add(dl.id)
                        val meta = repository.getAllMetadata().find { it.ketchId == dl.id }
                        if (meta?.isZip == true && meta.extractPath == null) {
                            val metaKey = meta.slug + (meta.episodeLabel ?: "")
                            repository.saveMetadataDirect(metaKey, meta.copy(status = "extracting"))
                        }
                        checkAndExtractZip(dl.id, dl.fileName)
                    }
                }

                refreshUiState(downloads)
            }
        }
    }

    private fun checkAndExtractZip(ketchId: Int, fileName: String) {
        viewModelScope.launch {
            val allMeta = repository.getAllMetadata()
            val meta = allMeta.find { it.ketchId == ketchId }

            if (meta != null && meta.isZip && meta.extractPath == null) {
                val metaKey = meta.slug + (meta.episodeLabel ?: "")
                repository.saveMetadataDirect(metaKey, meta.copy(status = "extracting", extractionProgress = 0))
                refreshUiState()
                try {
                    repository.postProcessDownload(ketchId, metaKey).collect { progress ->
                        refreshUiState()
                    }
                } catch (e: Exception) {
                    com.movie.app.best.data.debug.NetworkLogger.logAction("EXTRACT_ERR_DL1", e.message ?: "unknown")
                }
                refreshUiState()
            } else if (meta == null && isZipFile(fileName)) {
                val slug = fileName.substringBeforeLast(".").replace(Regex("[^a-zA-Z0-9-]"), "-").lowercase()
                val metaKey = slug
                val newMeta = DownloadMetadata(
                    slug = slug,
                    title = fileName.substringBeforeLast("."),
                    fileName = fileName,
                    filePath = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                    ).path + "/BlazeMovies/$fileName",
                    ketchId = ketchId,
                    isZip = true,
                    contentType = "series",
                    status = "extracting",
                    extractionProgress = 0
                )
                repository.saveMetadataDirect(metaKey, newMeta)
                refreshUiState()
                try {
                    repository.postProcessDownload(ketchId, metaKey).collect { progress ->
                        refreshUiState()
                    }
                } catch (e: Exception) {
                    com.movie.app.best.data.debug.NetworkLogger.logAction("EXTRACT_ERR_DL2", e.message ?: "unknown")
                }
                refreshUiState()
            }
        }
    }

    private fun isZipFile(fileName: String): Boolean {
        val f = fileName.lowercase()
        return f.endsWith(".zip") || f.endsWith(".rar") || f.endsWith(".7z")
    }

    fun rescanDownloads() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRescanning = true) }
            repository.rescanDownloads()

            val allMeta = repository.getAllMetadata()
            allMeta.filter { it.isZip && it.extractPath == null && it.ketchId >= 0 }.forEach { meta ->
                val metaKey = meta.slug + (meta.episodeLabel ?: "")
                try {
                    repository.postProcessDownload(meta.ketchId, metaKey).collect { }
                } catch (e: Exception) {
                    com.movie.app.best.data.debug.NetworkLogger.logAction("EXTRACT_ERR_DL3", e.message ?: "unknown")
                }
            }

            refreshUiState()
            _uiState.update { it.copy(isRescanning = false) }
        }
    }

    fun deleteUnifiedItem(item: UnifiedDownloadItem) {
        viewModelScope.launch {
            if (item.ketchId >= 0 && item.phase != UnifiedDownloadPhase.COMPLETE) {
                repository.cancelDownload(item.ketchId)
                repository.deleteDownload(item.ketchId)
            }
            if (item.extractPath != null) {
                try { File(item.extractPath).deleteRecursively() } catch (_: Exception) {}
            }
            if (item.filePath.isNotEmpty() && File(item.filePath).isFile) {
                try { File(item.filePath).delete() } catch (_: Exception) {}
            }
            if (item.slug.isNotEmpty()) {
                val metaKey = item.slug
                try {
                    val meta = repository.getMetadata(metaKey)
                    if (meta != null) {
                        repository.saveMetadataDirect(metaKey, meta.copy(
                            extractPath = null,
                            filePath = "",
                            status = "deleted"
                        ))
                    }
                } catch (_: Exception) {}
            }
            refreshUiState()
        }
    }

    fun resolveAndDownload(linkUrl: String, title: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isResolving = true, resolveError = null, resolveSuccess = false) }
            repository.resolveDownloadUrls(linkUrl).collect { result ->
                when (result) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        val mirrors = result.data ?: emptyList()
                        val first = mirrors.firstOrNull()
                        if (first != null) {
                            repository.startDownload(first.jackpot, first.fileName, title)
                            _uiState.update { it.copy(isResolving = false, resolveSuccess = true) }
                        } else {
                            _uiState.update { it.copy(isResolving = false, resolveError = "No mirrors found") }
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isResolving = false, resolveError = result.error) }
                    }
                }
            }
        }
    }

    fun pauseDownload(id: Int) { repository.pauseDownload(id) }
    fun resumeDownload(id: Int) { repository.resumeDownload(id) }
    fun cancelDownload(id: Int) { repository.cancelDownload(id) }
    fun retryDownload(id: Int) { repository.retryDownload(id) }
    fun deleteDownload(id: Int) { repository.deleteDownload(id) }
}
