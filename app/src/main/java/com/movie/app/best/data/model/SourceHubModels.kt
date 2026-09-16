package com.movie.app.best.data.model

import com.google.gson.annotations.SerializedName

data class SourceHubPlayback(
    val type: String = "hls",
    val headers: Map<String, String>? = null,
    val cookies: String? = null,
    val note: String? = null
)

data class SourceHubSource(
    val service: String = "",
    val server: String? = null,
    val quality: String = "auto",
    val language: String? = null,
    val languages: List<String> = emptyList(),
    val url: String = "",
    val playback: SourceHubPlayback? = null
) {
    val id: String
        get() = "$service|${server ?: ""}|${language ?: ""}|$quality|$url"

    val allLanguages: List<String>
        get() = (languages + listOfNotNull(language)).map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }

    private val serviceDisplayName: String
        get() = when (service.lowercase()) {
            "vidsrc" -> "VidSrc"
            "filmu" -> "FilmU"
            "vidlove" -> "VidLove"
            "vidnest" -> "VidNest"
            "gemma" -> "Gemma"
            "native" -> "Native"
            else -> service.replaceFirstChar { it.uppercase() }
        }

    /** Server-only label — language(s) are rendered separately (red) by the Server picker. */
    val serverLabel: String
        get() {
            val parts = mutableListOf(serviceDisplayName)
            server?.takeIf { it.isNotBlank() }?.let { parts.add(it.replaceFirstChar { c -> c.uppercase() }) }
            val t = playback?.type?.lowercase()?.takeIf { it.isNotBlank() && it != "hls" }
            if (t != null) parts.add(if (url.substringBefore('?').lowercase().endsWith(".mkv")) "MKV" else t.uppercase())
            return parts.joinToString(" • ")
        }

    val displayLabel: String
        get() {
            val parts = mutableListOf(serverLabel)
            quality.takeIf { it.isNotBlank() && !it.equals("auto", true) }?.let { parts.add(it) }
            return parts.joinToString(" • ")
        }

    fun playbackHeaders(): Map<String, String> = playback?.headers ?: emptyMap()
}

/** All sources of one service+server collapse into one picker row; languages are surfaced separately. */
data class SourceHubGroup(
    val key: String,
    val label: String,
    val headers: Map<String, String>,
    val sources: List<SourceHubSource>,
    val languages: List<String> = emptyList()
) {
    fun fileBaseName(): String = key.map { if (it.isLetterOrDigit()) it else '_' }.joinToString("")
    val playbackType: String get() = sources.firstOrNull()?.playback?.type?.lowercase()?.takeIf { it.isNotBlank() } ?: "hls"
    val isProgressive: Boolean get() = playbackType != "hls"

    /** Hindi-first language order so the merged default matches the app's language priority. */
    fun sortedLanguages(): List<String> = languages.sortedWith(
        compareByDescending<String> { it.contains("Hindi", ignoreCase = true) }
    )

    /** The language whose variants get merged into one adaptive master (others stay as alternates). */
    fun primaryLanguage(): String? = sortedLanguages().firstOrNull()
}

fun qualityHeight(q: String?): Int? {
    if (q.isNullOrBlank()) return null
    val m = Regex("(\\d{3,4})\\s*p", RegexOption.IGNORE_CASE).find(q) ?: return null
    return m.groupValues[1].toIntOrNull()
}

private fun langOfSource(s: SourceHubSource): String? =
    s.allLanguages.firstOrNull()

fun List<SourceHubSource>.groupByServer(): List<SourceHubGroup> {
    val buckets = LinkedHashMap<String, MutableList<SourceHubSource>>()
    for (s in this) {
        if (s.url.isBlank()) continue
        val key = s.service.lowercase() + "|" + s.server.orEmpty().lowercase()
        val list = buckets.getOrPut(key) { mutableListOf() }
        if (list.none { it.url == s.url }) list.add(s)
    }
    return buckets.map { (key, srcs) ->
        val sorted = srcs.sortedByDescending { qualityHeight(it.quality) ?: -1 }
        val first = sorted.first()
        val langs = sorted.flatMap { it.allLanguages }
            .distinctBy { it.lowercase() }
            .sortedWith(
                compareByDescending<String> { it.contains("Hindi", ignoreCase = true) }
                    .thenByDescending { it.contains("English", ignoreCase = true) }
            )
        SourceHubGroup(
            key = key,
            label = first.serverLabel,
            headers = first.playbackHeaders(),
            sources = sorted,
            languages = langs
        )
    }
}

/** Merges distinct single-quality HLS variants of the primary language into a master. Never mixes languages or progressive. */
fun SourceHubGroup.buildMasterPlaylist(): String? {
    if (isProgressive) return null
    val primary = primaryLanguage()
    val sameLang = sources.filter { it.allLanguages.isEmpty() || langOfSource(it) == primary || it.allLanguages.any { l -> l.equals(primary, ignoreCase = true) } }
    val pool = if (primary == null) sources else sameLang.ifEmpty { sources }
    val variants = pool.map { (qualityHeight(it.quality) ?: 0) to it.url }.filter { it.first > 0 }
    if (variants.size < 2) return null
    val sb = StringBuilder("#EXTM3U\n#EXT-X-VERSION:3\n")
    for ((h, url) in variants) {
        sb.append("#EXT-X-STREAM-INF:BANDWIDTH=").append(h * 3000)
            .append(",RESOLUTION=").append(h * 16 / 9).append('x').append(h).append('\n')
            .append(url).append('\n')
    }
    return sb.toString()
}

data class SourceHubServiceResult(
    val service: String = "",
    val status: String? = null,
    val ok: Boolean = false,
    val sources: List<SourceHubSource> = emptyList(),
    @SerializedName("skipped") val skipped: String? = null,
    val error: String? = null
)

data class SourceHubResolveResult(
    val ok: Boolean = false,
    val title: String? = null,
    val count: Int = 0,
    val sources: List<SourceHubSource> = emptyList(),
    val services: Map<String, SourceHubServiceResult> = emptyMap(),
    val elapsedMs: Long = 0
)

data class SourceHubStartService(
    val name: String = "",
    val status: String = "resolving"
)

data class SourceHubStartEvent(
    val services: List<SourceHubStartService> = emptyList()
)

/** One live row in the player's server-scan animation. status: resolving | found | fail | na */
data class ServerScanRow(
    val name: String = "",
    val status: String = "resolving",
    val elapsedMs: Long = 0,
    val error: String? = null
)

data class SourceHubRequest(
    val id: String,
    val type: String = "movie",
    val season: Int? = null,
    val episode: Int? = null
)

object PlaybackKind {
    const val NATIVE = "native"
    const val SOURCE_HUB = "sourcehub"
    const val GEMMA = "gemma"
}

data class PlaybackOption(
    val id: String,
    val label: String,
    val kind: String,
    val url: String = "",
    val headers: Map<String, String> = emptyMap(),
    val language: String? = null,
    val languages: List<String> = emptyList(),
    val alternates: List<String> = emptyList(),
    val playbackType: String = "hls"
)
