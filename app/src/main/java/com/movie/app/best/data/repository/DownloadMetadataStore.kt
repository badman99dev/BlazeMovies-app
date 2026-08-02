package com.movie.app.best.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.movie.app.best.data.model.DownloadMetadata
import com.movie.app.best.data.debug.NetworkLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadMetadataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()
    private val prefs by lazy {
        context.getSharedPreferences("app_download_meta", Context.MODE_PRIVATE)
    }

    private val appDataDir by lazy {
        File(context.getExternalFilesDir(null), "downloads").apply { if (!exists()) mkdirs() }
    }

    private val postersDir by lazy {
        File(appDataDir, "posters").apply { if (!exists()) mkdirs() }
    }

    private val httpClient by lazy { OkHttpClient() }

    fun saveMetadata(metadata: DownloadMetadata) {
        val all = getAllMetadataMap().toMutableMap()
        all[metadata.slug + (metadata.episodeLabel ?: "")] = metadata
        prefs.edit().putString("metadata", gson.toJson(all)).apply()
    }

    fun saveMetadataByKey(key: String, metadata: DownloadMetadata) {
        val all = getAllMetadataMap().toMutableMap()
        all[key] = metadata
        prefs.edit().putString("metadata", gson.toJson(all)).apply()
    }

    fun updateMetadata(key: String, update: (DownloadMetadata) -> DownloadMetadata) {
        val all = getAllMetadataMap().toMutableMap()
        val existing = all[key] ?: return
        all[key] = update(existing)
        prefs.edit().putString("metadata", gson.toJson(all)).apply()
    }

    fun getMetadata(key: String): DownloadMetadata? {
        return getAllMetadataMap()[key]
    }

    fun getAllMetadata(): List<DownloadMetadata> {
        return getAllMetadataMap().values.toList()
    }

    fun removeMetadata(key: String) {
        val all = getAllMetadataMap().toMutableMap()
        val meta = all.remove(key)
        if (meta != null) {
            deletePoster(meta.localPosterPath)
            prefs.edit().putString("metadata", gson.toJson(all)).apply()
        }
    }

    suspend fun downloadPoster(posterUrl: String, slug: String): String = withContext(Dispatchers.IO) {
        if (posterUrl.isEmpty()) return@withContext ""
        try {
            val posterFile = File(postersDir, "$slug.jpg")
            if (posterFile.exists()) return@withContext posterFile.absolutePath

            val request = Request.Builder().url(posterUrl).build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.byteStream()?.use { input ->
                    FileOutputStream(posterFile).use { output ->
                        input.copyTo(output)
                    }
                }
                posterFile.absolutePath
            } else ""
        } catch (e: Exception) {
            NetworkLogger.logAction("POSTER_DL_ERR", e.message ?: "unknown")
            ""
        }
    }

    private fun deletePoster(localPosterPath: String) {
        if (localPosterPath.isNotEmpty()) {
            try {
                File(localPosterPath).delete()
            } catch (_: Exception) {}
        }
    }

    fun rescanAndCleanup(downloadDir: File) {
        val all = getAllMetadataMap().toMutableMap()
        val iterator = all.entries.iterator()
        while (iterator.hasNext()) {
            val (_, meta) = iterator.next()
            val fileExists = File(meta.filePath).exists()
            val extractExists = meta.extractPath == null || File(meta.extractPath).exists()
            if (!fileExists && !extractExists) {
                deletePoster(meta.localPosterPath)
                iterator.remove()
            }
        }
        prefs.edit().putString("metadata", gson.toJson(all)).apply()
    }

    fun isZipFile(fileName: String): Boolean {
        val f = fileName.lowercase()
        return f.endsWith(".zip") || f.endsWith(".rar") || f.endsWith(".7z")
    }

    fun getPosterPath(slug: String): String {
        return File(postersDir, "$slug.jpg").absolutePath
    }

    private fun getAllMetadataMap(): Map<String, DownloadMetadata> {
        val json = prefs.getString("metadata", null) ?: return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, DownloadMetadata>>() {}.type
            gson.fromJson(json, type)
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
