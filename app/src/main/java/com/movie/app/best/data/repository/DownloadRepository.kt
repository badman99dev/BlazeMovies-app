package com.movie.app.best.data.repository

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.movie.app.best.data.debug.NetworkLogger
import com.movie.app.best.data.model.DownloadMetadata
import com.movie.app.best.data.model.DownloadPhase
import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.remote.BypassApiService
import com.movie.app.best.data.remote.BypassDoneEvent
import com.movie.app.best.data.remote.BypassErrorEvent
import com.movie.app.best.data.remote.BypassLogEvent
import com.movie.app.best.data.remote.BypassRequest
import com.movie.app.best.data.remote.BypassResult
import com.google.gson.Gson
import com.ketch.Ketch
import com.ketch.Status
import com.ketch.DownloadModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.File
import java.net.URL
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

const val DOWNLOAD_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"

private data class CachedCookie(val header: String, val expiresAtMs: Long)

data class ResolvedMirror(
    val jackpot: String,
    val fileName: String,
    val size: String,
    val sizeBytes: Long?,
    val resumable: Boolean,
    val quality: String,
    val sourceLabel: String,
    val isZip: Boolean = false
)

data class DownloadStatusInfo(
    val phase: DownloadPhase,
    val progress: Int = 0,
    val ketchId: Int = -1,
    val failureReason: String? = null
)

@Singleton
class DownloadRepository @Inject constructor(
    private val bypassApiService: BypassApiService,
    private val gson: Gson,
    private val ketch: Ketch,
    private val metadataStore: DownloadMetadataStore,
    private val zipExtractor: ZipExtractor,
    @ApplicationContext private val context: Context
) {

    private val extractingKeys = Collections.synchronizedSet(mutableSetOf<String>())
    private val cookieCache = Collections.synchronizedMap(mutableMapOf<String, CachedCookie>())

    private fun hostOf(url: String): String? = runCatching { URL(url).host }.getOrNull()

    fun cookieHeaderForUrl(url: String): String? {
        val host = hostOf(url) ?: return null
        val cached = cookieCache[host] ?: return null
        if (System.currentTimeMillis() > cached.expiresAtMs) {
            cookieCache.remove(host)
            return null
        }
        return cached.header
    }

    fun cacheCookieForUrl(url: String, header: String) {
        val host = hostOf(url) ?: return
        cookieCache[host] = CachedCookie(header, System.currentTimeMillis() + 1_700_000L)
    }

    fun isCloudflareFailure(reason: String?): Boolean {
        if (reason.isNullOrBlank()) return false
        val r = reason.lowercase()
        return r.contains("403") || r.contains("forbidden") ||
            r.contains("cloudflare") || r.contains("just a moment")
    }

    fun resolveDownloadUrls(linkUrl: String, onLog: (String) -> Unit = {}): Flow<Resource<List<ResolvedMirror>>> = flow {
        emit(Resource.Loading())
        try {
            NetworkLogger.logAction("RESOLVE", "linkUrl=$linkUrl")
            val body = bypassApiService.bypassUrl(BypassRequest(linkUrl, fetchInfo = true))
            val results = parseSse(body, onLog)
            val successItems = results.filter { it.status == "SUCCESS" && !it.jackpot.isNullOrEmpty() }
            if (successItems.isEmpty()) {
                emit(Resource.Error("No direct download URL found"))
                return@flow
            }
            val mirrors = successItems.mapIndexedNotNull { idx, item ->
                val jackpot = item.jackpot ?: return@mapIndexedNotNull null
                if (!jackpot.startsWith("http")) return@mapIndexedNotNull null
                val fi = item.fileInfo
                val fileName = fi?.filename ?: extractFileNameFromUrl(jackpot)
                val quality = detectQuality(fileName)
                val sourceLabel = if (successItems.size == 1) "Download Now" else "Server ${idx + 1}"
                ResolvedMirror(
                    jackpot = jackpot,
                    fileName = fileName,
                    size = fi?.size ?: "",
                    sizeBytes = fi?.sizeBytes,
                    resumable = fi?.resumable ?: false,
                    quality = quality,
                    sourceLabel = sourceLabel,
                    isZip = isZipFileName(fileName)
                )
            }.distinctBy { it.jackpot }
            if (mirrors.size == 1) {
                mirrors.mapIndexed { idx, m -> m.copy(sourceLabel = "Download Now") }
            } else {
                mirrors.mapIndexed { idx, m -> m.copy(sourceLabel = "Server ${idx + 1}") }
            }
            if (mirrors.isEmpty()) {
                emit(Resource.Error("No valid direct download URLs found"))
                return@flow
            }
            NetworkLogger.logAction("RESOLVE", "mirrors=${mirrors.size}")
            emit(Resource.Success(mirrors))
        } catch (e: Exception) {
            NetworkLogger.logAction("RESOLVE_ERR", e.message ?: "unknown")
            emit(Resource.Error(e.message ?: "Failed to resolve download URL"))
        }
    }

    private suspend fun parseSse(body: ResponseBody, onLog: (String) -> Unit): List<BypassResult> =
        withContext(Dispatchers.IO) {
            var eventType: String? = null
            val dataLines = StringBuilder()
            var results: List<BypassResult> = emptyList()
            var streamError: String? = null

            body.byteStream().bufferedReader(Charsets.UTF_8).use { reader ->
                var line: String?
                while (true) {
                    line = reader.readLine() ?: break
                    if (line.isEmpty()) {
                        if (dataLines.isNotEmpty()) {
                            val data = dataLines.toString()
                            runCatching {
                                when (eventType) {
                                    "log" -> {
                                        val ev = gson.fromJson(data, BypassLogEvent::class.java)
                                        onLog(ev.message)
                                    }
                                    "done" -> {
                                        results = gson.fromJson(data, BypassDoneEvent::class.java).results
                                    }
                                    "error" -> {
                                        streamError = gson.fromJson(data, BypassErrorEvent::class.java).message
                                    }
                                }
                            }
                            dataLines.setLength(0)
                            eventType = null
                        }
                        continue
                    }
                    when {
                        line.startsWith("event:") -> eventType = line.substringAfter("event:").trim()
                        line.startsWith("data:") -> {
                            if (dataLines.isNotEmpty()) dataLines.append('\n')
                            dataLines.append(line.substringAfter("data:").trimStart())
                        }
                    }
                }
            }

            if (streamError != null) throw Exception(streamError)
            results
        }

    suspend fun startDownloadWithMetadata(
        mirror: ResolvedMirror,
        slug: String,
        posterUrl: String,
        title: String,
        contentType: String = "movie",
        episodeId: Int? = null,
        episodeLabel: String? = null
    ): Int {
        val safeFileName = sanitizeFileName(mirror.fileName)
        val isZip = metadataStore.isZipFile(safeFileName)

        val downloadPath = if (isZip) {
            File(context.cacheDir, "app_zips").apply { if (!exists()) mkdirs() }.path
        } else {
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "BlazeMovies"
            ).apply { if (!exists()) mkdirs() }.path
        }

        val filePath = File(downloadPath, safeFileName).path
        val metaKey = slug + (episodeLabel ?: "")

        NetworkLogger.logAction("DOWNLOAD_INIT", "slug=$slug poster=$posterUrl isZip=$isZip path=$downloadPath")

        val localPosterPath = metadataStore.downloadPoster(posterUrl, slug)

        val metadata = DownloadMetadata(
            slug = slug,
            title = title,
            posterUrl = posterUrl,
            localPosterPath = localPosterPath,
            fileName = safeFileName,
            filePath = filePath,
            isZip = isZip,
            contentType = contentType,
            episodeId = episodeId,
            episodeLabel = episodeLabel,
            status = "initializing",
            url = mirror.jackpot,
            bypassAttempts = 0
        )
        metadataStore.saveMetadata(metadata)

        val headers = HashMap<String, String>().apply {
            put("User-Agent", DOWNLOAD_USER_AGENT)
            put("Referer", "https://blazemovies.vercel.app/")
            put("Accept", "*/*")
            cookieHeaderForUrl(mirror.jackpot)?.let { put("Cookie", it) }
        }

        val id = ketch.download(
            url = mirror.jackpot,
            path = downloadPath,
            fileName = safeFileName,
            tag = "app_download",
            headers = headers
        )

        metadataStore.updateMetadata(metaKey) { it.copy(ketchId = id, status = "downloading") }
        NetworkLogger.logAction("DOWNLOAD_QUEUED", "id=$id file=$safeFileName slug=$slug")
        return id
    }

    fun startDownload(url: String, fileName: String, title: String): Int {
        val safeFileName = sanitizeFileName(fileName)
        val actualFileName = if (safeFileName.isNotBlank()) safeFileName else extractFileNameFromUrl(url)

        val downloadPath = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "BlazeMovies"
        ).apply { if (!exists()) mkdirs() }.path

        val headers = HashMap<String, String>().apply {
            put("User-Agent", DOWNLOAD_USER_AGENT)
            put("Referer", "https://blazemovies.vercel.app/")
            put("Accept", "*/*")
            cookieHeaderForUrl(url)?.let { put("Cookie", it) }
        }

        val id = ketch.download(
            url = url,
            path = downloadPath,
            fileName = actualFileName,
            tag = "app_download",
            headers = headers
        )

        NetworkLogger.logAction("DOWNLOAD_QUEUED", "id=$id file=$actualFileName")
        return id
    }

    fun retryDownloadWithUrl(
        ketchId: Int,
        directUrl: String,
        cookies: String? = null,
        originUrl: String? = null
    ): Int? {
        val allMeta = getAllMetadata()
        val meta = allMeta.find { it.ketchId == ketchId } ?: return null

        val safeFileName = sanitizeFileName(meta.fileName.ifBlank { extractFileNameFromUrl(directUrl) })
        val isZip = meta.isZip
        val downloadPath = if (isZip) {
            File(context.cacheDir, "app_zips").apply { if (!exists()) mkdirs() }.path
        } else {
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "BlazeMovies"
            ).apply { if (!exists()) mkdirs() }.path
        }
        val filePath = File(downloadPath, safeFileName).path

        try { ketch.cancel(ketchId) } catch (_: Exception) {}
        try { ketch.clearDb(ketchId) } catch (_: Exception) {}

        val headers = HashMap<String, String>().apply {
            put("User-Agent", DOWNLOAD_USER_AGENT)
            put("Referer", originUrl ?: "https://blazemovies.vercel.app/")
            put("Accept", "*/*")
            if (cookies != null) put("Cookie", cookies)
        }

        val newId = ketch.download(
            url = directUrl,
            path = downloadPath,
            fileName = safeFileName,
            tag = "app_download",
            headers = headers
        )

        val metaKey = meta.slug + (meta.episodeLabel ?: "")
        metadataStore.updateMetadata(metaKey) {
            it.copy(
                ketchId = newId,
                status = "downloading",
                url = directUrl,
                filePath = filePath
            )
        }
        NetworkLogger.logAction("DOWNLOAD_RETRY_DIRECT", "oldId=$ketchId newId=$newId host=${hostOf(directUrl)}")
        return newId
    }

    fun observeDownloads(): Flow<List<DownloadModel>> {
        return ketch.observeDownloads()
    }

    fun observeDownloadStatus(ketchId: Int): Flow<DownloadStatusInfo> {
        return ketch.observeDownloads().map { downloads ->
            val dl = downloads.find { it.id == ketchId }
            if (dl == null) {
                DownloadStatusInfo(phase = DownloadPhase.NONE, ketchId = ketchId)
            } else {
                val phase = when (dl.status) {
                    Status.QUEUED -> DownloadPhase.DOWNLOADING
                    Status.STARTED -> DownloadPhase.DOWNLOADING
                    Status.PROGRESS -> DownloadPhase.DOWNLOADING
                    Status.SUCCESS -> DownloadPhase.COMPLETE
                    Status.CANCELLED -> DownloadPhase.CANCELLED
                    Status.FAILED -> DownloadPhase.FAILED
                    Status.PAUSED -> DownloadPhase.DOWNLOADING
                    Status.DEFAULT -> DownloadPhase.NONE
                }
                DownloadStatusInfo(
                    phase = phase,
                    progress = dl.progress,
                    ketchId = ketchId,
                    failureReason = if (dl.status == Status.FAILED) dl.failureReason else null
                )
            }
        }
    }

    fun postProcessDownload(ketchId: Int, metaKey: String): Flow<ExtractionProgress> = flow {
        val meta = metadataStore.getMetadata(metaKey) ?: return@flow
        if (!meta.isZip) {
            metadataStore.updateMetadata(metaKey) { it.copy(status = "complete") }
            return@flow
        }

        if (!extractingKeys.add(metaKey)) {
            NetworkLogger.logAction("ZIP_SKIP", "key=$metaKey already being extracted")
            return@flow
        }

        try {
            metadataStore.updateMetadata(metaKey) { it.copy(status = "extracting", extractionProgress = 0) }

            val downloadDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "BlazeMovies"
            ).apply { if (!exists()) mkdirs() }

            zipExtractor.extractZipWithProgress(meta.filePath, downloadDir).collect { progress ->
                when (progress.state) {
                    ExtractionState.IN_PROGRESS -> {
                        metadataStore.updateMetadata(metaKey) {
                            it.copy(status = "extracting", extractionProgress = progress.percent)
                        }
                        emit(progress)
                    }
                    ExtractionState.COMPLETE -> {
                        val extractPath = findExtractPath(downloadDir, meta.fileName)
                        if (extractPath != null) {
                            try { File(meta.filePath).delete() } catch (_: Exception) {}
                            try { ketch.clearDb(ketchId) } catch (_: Exception) {}
                            metadataStore.updateMetadata(metaKey) {
                                it.copy(status = "complete", extractPath = extractPath, filePath = extractPath, extractionProgress = 100)
                            }
                            NetworkLogger.logAction("ZIP_POSTPROCESS", "key=$metaKey extractPath=$extractPath ZIP_DELETED=true")
                        } else {
                            metadataStore.updateMetadata(metaKey) { it.copy(status = "failed") }
                            NetworkLogger.logAction("ZIP_POSTPROCESS", "key=$metaKey extraction FAILED, ZIP kept")
                        }
                        emit(progress)
                    }
                    ExtractionState.FAILED -> {
                        metadataStore.updateMetadata(metaKey) { it.copy(status = "failed") }
                        NetworkLogger.logAction("ZIP_POSTPROCESS", "key=$metaKey extraction FAILED")
                        emit(progress)
                    }
                }
            }
        } finally {
            extractingKeys.remove(metaKey)
        }
    }.catch { e ->
        NetworkLogger.logAction("ZIP_FLOW_ERR", e.message ?: "unknown")
        emit(ExtractionProgress(0, "", ExtractionState.FAILED))
    }.flowOn(Dispatchers.IO)

    private fun findExtractPath(baseDir: File, zipFileName: String): String? {
        val folderName = zipFileName.substringBeforeLast(".")
        val extractDir = File(baseDir, folderName)
        if (!extractDir.exists() || !extractDir.isDirectory) return null
        val videoExts = setOf("mp4", "mkv", "avi", "webm", "mov", "flv", "3gp", "ts", "m4v")
        val hasVideos = extractDir.walkTopDown().any {
            it.isFile && it.extension.lowercase() in videoExts
        }
        return if (hasVideos) extractDir.absolutePath else null
    }

    fun pauseDownload(id: Int) {
        ketch.pause(id)
        NetworkLogger.logAction("DOWNLOAD_PAUSE", "id=$id")
    }

    fun resumeDownload(id: Int) {
        ketch.resume(id)
        NetworkLogger.logAction("DOWNLOAD_RESUME", "id=$id")
    }

    fun cancelDownload(id: Int) {
        ketch.cancel(id)
        NetworkLogger.logAction("DOWNLOAD_CANCEL", "id=$id")
    }

    fun retryDownload(id: Int) {
        ketch.retry(id)
        NetworkLogger.logAction("DOWNLOAD_RETRY", "id=$id")
    }

    fun deleteDownload(id: Int) {
        ketch.clearDb(id)
        NetworkLogger.logAction("DOWNLOAD_DELETE", "id=$id")
    }

    fun getAllMetadata(): List<DownloadMetadata> = metadataStore.getAllMetadata()

    fun getMetadata(key: String): DownloadMetadata? = metadataStore.getMetadata(key)

    fun saveMetadataDirect(key: String, metadata: DownloadMetadata) {
        metadataStore.saveMetadataByKey(key, metadata)
    }

    fun rescanDownloads() {
        val downloadDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "BlazeMovies"
        )
        metadataStore.rescanAndCleanup(downloadDir)
    }

    private fun isZipFileName(fileName: String): Boolean {
        val f = fileName.lowercase()
        return f.endsWith(".zip") || f.endsWith(".rar") || f.endsWith(".7z")
    }

    private fun detectQuality(filename: String): String {
        val f = filename.lowercase()
        return when {
            f.contains("4k") || f.contains("2160p") || f.contains("uhd") -> "4K"
            f.contains("2k") || f.contains("1440p") -> "2K"
            f.contains("1080p") || f.contains("fhd") -> "1080p"
            f.contains("720p") || f.contains("hd") -> "720p"
            f.contains("480p") -> "480p"
            f.contains("360p") -> "360p"
            else -> "Unknown"
        }
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    }

    private fun extractFileNameFromUrl(url: String): String {
        return try {
            val path = Uri.parse(url).path ?: "download"
            path.substringAfterLast("/").ifEmpty { "download" }
        } catch (_: Exception) {
            "download"
        }
    }
}
