package com.movie.app.best.data.repository

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.movie.app.best.data.debug.NetworkLogger
import com.movie.app.best.data.model.DownloadMetadata
import com.movie.app.best.data.model.DownloadPhase
import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.remote.BypassConfirmEvent
import com.movie.app.best.data.remote.BypassWsClient
import com.movie.app.best.data.remote.ConfirmCancelledException
import com.movie.app.best.data.telemetry.Incident
import com.movie.app.best.data.telemetry.IncidentReporter
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
import java.io.File
import java.net.URL
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

const val DOWNLOAD_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"

const val CONFIRM_CANCELLED = "__confirm_cancelled__"

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
    private val bypassWsClient: BypassWsClient,
    private val ketch: Ketch,
    private val metadataStore: DownloadMetadataStore,
    private val zipExtractor: ZipExtractor,
    private val incidentReporter: IncidentReporter,
    @ApplicationContext private val context: Context
) {

    private val extractingKeys = Collections.synchronizedSet(mutableSetOf<String>())
    private val cookieCache = Collections.synchronizedMap(mutableMapOf<String, CachedCookie>())

    private fun hostOf(url: String): String? = runCatching { URL(url).host }.getOrNull()

    private fun publicDownloadDir(): File =
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "BlazeMovies")

    private fun isWritableDir(dir: File): Boolean {
        if (!dir.exists() && !dir.mkdirs()) return false
        if (!dir.isDirectory) return false
        return try {
            val probe = File(dir, ".bm_write_probe")
            probe.writeText("ok")
            probe.delete()
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Scoped storage (Android 11+) blocks direct writes to the public Downloads folder
     * unless the app has all-files access, and mkdirs() fails silently there. Ketch then
     * fails with "No such file or directory" (ENOENT). Probe the public dir first and
     * fall back to the app-specific external dir, which is always writable.
     */
    private fun resolveDownloadDir(isZip: Boolean): File {
        if (isZip) {
            val dir = File(context.cacheDir, "app_zips")
            dir.mkdirs()
            return dir
        }
        val public = publicDownloadDir()
        if (isWritableDir(public)) return public
        val fallback = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "BlazeMovies")
        if (isWritableDir(fallback)) {
            NetworkLogger.logAction("DOWNLOAD_DIR_FALLBACK", "public not writable, using ${fallback.path}")
            return fallback
        }
        NetworkLogger.logAction("DOWNLOAD_DIR_ERR", "no writable download dir: public=$public fallback=$fallback")
        return fallback
    }

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

    fun resolveDownloadUrls(
        linkUrl: String,
        slug: String = "",
        movieId: Int = 0,
        linkId: Int = 0,
        onLog: (String) -> Unit = {},
        onConfirm: (suspend (BypassConfirmEvent) -> String?)? = null
    ): Flow<Resource<List<ResolvedMirror>>> {
        // Capture the full resolver log so a failure can be reported verbatim.
        val logLines: MutableList<String> = Collections.synchronizedList(ArrayList<String>())
        val teeLog: (String) -> Unit = { line -> logLines.add(line); onLog(line) }
        return flow {
            emit(Resource.Loading())
            emit(resolveMirrors(linkUrl, slug, movieId, linkId, teeLog, logLines, onConfirm))
        }.catch { e ->
            // Emissions from a flow builder's catch block are forbidden: if the collector
            // throws, that failure travels back through emit, gets caught here and the
            // second emit crashes with a Flow exception-transparency violation. The catch
            // operator only sees upstream failures, which is exactly what we want.
            if (e is ConfirmCancelledException) {
                NetworkLogger.logAction("RESOLVE_CANCELLED", "confirm cancelled by user/timeout")
                emit(Resource.Error(CONFIRM_CANCELLED))
            } else {
                NetworkLogger.logAction("RESOLVE_ERR", e.message ?: "unknown")
                reportDownloadIncident(
                    reason = "resolve_exception",
                    title = "Download link resolve failed",
                    linkUrl = linkUrl, slug = slug, movieId = movieId, linkId = linkId,
                    logLines = logLines,
                    extra = mapOf("error" to (e.message ?: "unknown"))
                )
                emit(Resource.Error(e.message ?: "Failed to resolve download URL"))
            }
        }
    }

    private suspend fun resolveMirrors(
        linkUrl: String,
        slug: String,
        movieId: Int,
        linkId: Int,
        onLog: (String) -> Unit,
        logLines: MutableList<String>,
        onConfirm: (suspend (BypassConfirmEvent) -> String?)?
    ): Resource<List<ResolvedMirror>> {
        NetworkLogger.logAction("RESOLVE", "linkUrl=$linkUrl (ws)")
        val done = bypassWsClient.resolve(linkUrl, fetchInfo = true, onLog = onLog, onConfirm = onConfirm)
        val results = done.results
        val successItems = results.filter { it.status == "SUCCESS" && !it.jackpot.isNullOrEmpty() }
        if (successItems.isEmpty()) {
            // State 1: the link was resolved but no direct URL came back.
            reportDownloadIncident(
                reason = "no_link_found",
                title = "No download link found",
                linkUrl = linkUrl, slug = slug, movieId = movieId, linkId = linkId,
                logLines = logLines,
                extra = mapOf("results" to results.size)
            )
            return Resource.Error("No direct download URL found")
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

        if (mirrors.isEmpty()) {
            reportDownloadIncident(
                reason = "no_valid_url",
                title = "No valid download link found",
                linkUrl = linkUrl, slug = slug, movieId = movieId, linkId = linkId,
                logLines = logLines,
                extra = mapOf("successItems" to successItems.size)
            )
            return Resource.Error("No valid direct download URLs found")
        }
        NetworkLogger.logAction("RESOLVE", "mirrors=${mirrors.size}")
        return Resource.Success(mirrors)
    }

    /**
     * Ship a download-pipeline incident to the backend (fire-and-forget).
     * Both states the app cares about funnel through here:
     *   • resolve finished but no link      → reason "no_link_found" / "no_valid_url"
     *   • link found but start failed       → reason "download_start_failed"
     */
    private fun reportDownloadIncident(
        reason: String,
        title: String,
        linkUrl: String,
        slug: String,
        movieId: Int,
        linkId: Int,
        logLines: List<String>,
        extra: Map<String, Any?> = emptyMap()
    ) {
        incidentReporter.report(
            Incident(
                category = "download_link",
                severity = "high",
                title = title,
                linkUrl = linkUrl,
                slug = slug,
                movieId = movieId,
                linkId = linkId,
                source = "download_bypass",
                reason = reason,
                log = logLines.joinToString("\n"),
                extra = extra
            )
        )
    }

    suspend fun startDownloadWithMetadata(
        mirror: ResolvedMirror,
        slug: String,
        posterUrl: String,
        title: String,
        contentType: String = "movie",
        episodeId: Int? = null,
        episodeLabel: String? = null,
        movieId: Int = 0
    ): Int {
        val safeFileName = sanitizeFileName(mirror.fileName)
        val isZip = metadataStore.isZipFile(safeFileName)

        val downloadPath = resolveDownloadDir(isZip).path

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
            put("Referer", com.movie.app.best.data.config.RemoteConfig.apiOrigin() + "/")
            put("Accept", "*/*")
            cookieHeaderForUrl(mirror.jackpot)?.let { put("Cookie", it) }
        }

        val id = try {
            ketch.download(
                url = mirror.jackpot,
                path = downloadPath,
                fileName = safeFileName,
                tag = "app_download",
                headers = headers
            )
        } catch (e: Exception) {
            NetworkLogger.logAction("DOWNLOAD_ENQUEUE_ERR", "path=$downloadPath file=$safeFileName err=${e.message}")
            metadataStore.updateMetadata(metaKey) { it.copy(status = "failed") }
            // State 2: the link was found but starting the download failed.
            reportDownloadIncident(
                reason = "download_start_failed",
                title = "Download could not start",
                linkUrl = mirror.jackpot, slug = slug, movieId = movieId, linkId = 0,
                logLines = emptyList(),
                extra = mapOf("error" to (e.message ?: "unknown"), "file" to safeFileName)
            )
            return -1
        }

        metadataStore.updateMetadata(metaKey) { it.copy(ketchId = id, status = "downloading") }
        NetworkLogger.logAction("DOWNLOAD_QUEUED", "id=$id file=$safeFileName slug=$slug")
        return id
    }

    fun startDownload(url: String, fileName: String, title: String): Int {
        val safeFileName = sanitizeFileName(fileName)
        val actualFileName = if (safeFileName.isNotBlank()) safeFileName else extractFileNameFromUrl(url)

        val downloadPath = resolveDownloadDir(false).path

        val headers = HashMap<String, String>().apply {
            put("User-Agent", DOWNLOAD_USER_AGENT)
            put("Referer", com.movie.app.best.data.config.RemoteConfig.apiOrigin() + "/")
            put("Accept", "*/*")
            cookieHeaderForUrl(url)?.let { put("Cookie", it) }
        }

        val id = try {
            ketch.download(
                url = url,
                path = downloadPath,
                fileName = actualFileName,
                tag = "app_download",
                headers = headers
            )
        } catch (e: Exception) {
            NetworkLogger.logAction("DOWNLOAD_ENQUEUE_ERR", "path=$downloadPath file=$actualFileName err=${e.message}")
            return -1
        }

        NetworkLogger.logAction("DOWNLOAD_QUEUED", "id=$id file=$actualFileName")
        return id
    }

    fun retryDownloadWithUrl(
        ketchId: Int,
        directUrl: String,
        cookies: String? = null,
        originUrl: String? = null,
        fileNameOverride: String? = null
    ): Int? {
        val allMeta = getAllMetadata()
        val meta = allMeta.find { it.ketchId == ketchId } ?: return null

        val safeFileName = sanitizeFileName(
            if (!fileNameOverride.isNullOrBlank() && fileNameOverride.contains("."))
                fileNameOverride
            else
                meta.fileName.ifBlank { extractFileNameFromUrl(directUrl) }
        )
        val isZip = isZipFileName(safeFileName)
        val downloadPath = resolveDownloadDir(isZip).path
        val filePath = File(downloadPath, safeFileName).path

        try { ketch.cancel(ketchId) } catch (_: Exception) {}
        try { ketch.clearDb(ketchId) } catch (_: Exception) {}

        val headers = HashMap<String, String>().apply {
            put("User-Agent", DOWNLOAD_USER_AGENT)
            put("Referer", originUrl ?: (com.movie.app.best.data.config.RemoteConfig.apiOrigin() + "/"))
            put("Accept", "*/*")
            if (cookies != null) put("Cookie", cookies)
        }

        val newId = try {
            ketch.download(
                url = directUrl,
                path = downloadPath,
                fileName = safeFileName,
                tag = "app_download",
                headers = headers
            )
        } catch (e: Exception) {
            NetworkLogger.logAction("DOWNLOAD_ENQUEUE_ERR", "path=$downloadPath file=$safeFileName err=${e.message}")
            return null
        }

        val metaKey = meta.slug + (meta.episodeLabel ?: "")
        metadataStore.updateMetadata(metaKey) {
            it.copy(
                ketchId = newId,
                status = "downloading",
                url = directUrl,
                fileName = safeFileName,
                filePath = filePath,
                isZip = isZip
            )
        }
        NetworkLogger.logAction("DOWNLOAD_RETRY_DIRECT", "oldId=$ketchId newId=$newId host=${hostOf(directUrl)} file=$safeFileName")
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

            val downloadDir = resolveDownloadDir(false)

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
        metadataStore.updateMetadata(metaKey) { it.copy(status = "failed") }
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
        metadataStore.rescanAndCleanup(publicDownloadDir())
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
