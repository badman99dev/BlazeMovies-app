package com.movie.app.best.data.repository

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.movie.app.best.data.debug.NetworkLogger
import com.movie.app.best.data.model.GemmaExtractionResult
import com.movie.app.best.data.model.SourceHubSource
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class SourceCacheEntry(
    val savedAt: Long = 0L,
    val sources: List<SourceHubSource> = emptyList(),
    val gemma: GemmaExtractionResult? = null,
    val resolved: Map<String, String> = emptyMap()
)

@Singleton
class SourceCacheStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()
    private val prefs by lazy {
        context.getSharedPreferences("source_cache", Context.MODE_PRIVATE)
    }

    private val memory = mutableMapOf<String, SourceCacheEntry>()

    @Synchronized
    fun get(key: String): SourceCacheEntry? {
        val now = System.currentTimeMillis()
        val m = memory[key]
        if (m != null) {
            if (now - m.savedAt <= TTL_MS) return m
            memory.remove(key)
        }
        val entry = readAll()[key] ?: return null
        if (now - entry.savedAt > TTL_MS) {
            remove(key)
            return null
        }
        memory[key] = entry
        return entry
    }

    @Synchronized
    fun put(key: String, entry: SourceCacheEntry) {
        val stamped = entry.copy(savedAt = System.currentTimeMillis())
        memory[key] = stamped
        val all = readAll().toMutableMap()
        all[key] = stamped
        writeAll(all)
    }

    @Synchronized
    fun update(key: String, transform: (SourceCacheEntry?) -> SourceCacheEntry) {
        val current = get(key)
        val next = transform(current)
        put(key, next)
    }

    @Synchronized
    fun invalidate(key: String) {
        memory.remove(key)
        val all = readAll().toMutableMap()
        if (all.remove(key) != null) writeAll(all)
    }

    @Synchronized
    fun remove(key: String) {
        memory.remove(key)
        val all = readAll().toMutableMap()
        if (all.remove(key) != null) writeAll(all)
    }

    /** Hosts a synthetic multi-quality HLS master as a local .m3u8 and returns its file URI. */
    @Synchronized
    fun writeHlsMaster(name: String, content: String): String {
        val dir = File(context.cacheDir, "hls_master")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "$name.m3u8")
        file.writeText(content)
        return Uri.fromFile(file).toString()
    }

    private fun readAll(): Map<String, SourceCacheEntry> {
        val json = prefs.getString(KEY_DATA, null) ?: return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, SourceCacheEntry>>() {}.type
            gson.fromJson<Map<String, SourceCacheEntry>>(json, type) ?: emptyMap()
        } catch (e: Exception) {
            NetworkLogger.logAction("SOURCE_CACHE_READ_ERR", e.message ?: "unknown")
            emptyMap()
        }
    }

    private fun writeAll(all: Map<String, SourceCacheEntry>) {
        try {
            prefs.edit().putString(KEY_DATA, gson.toJson(all)).apply()
        } catch (e: Exception) {
            NetworkLogger.logAction("SOURCE_CACHE_WRITE_ERR", e.message ?: "unknown")
        }
    }

    companion object {
        const val TTL_MS = 30L * 60L * 1000L
        private const val KEY_DATA = "entries"

        fun movieKey(imdbId: String) = "$imdbId:movie"
        fun episodeKey(imdbId: String, season: Int, episode: Int) = "$imdbId:tv:$season:$episode"
        fun gemmaTreeKey(imdbId: String) = "$imdbId:gemma-tree"
    }
}
