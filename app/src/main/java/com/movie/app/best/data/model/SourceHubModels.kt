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
            "vidly" -> "Vidly"
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

/**
 * A ready-to-render picker row composed by the source-hub (the old client-side
 * `groupByServer()` / `buildMasterPlaylist()` live on the server now). The client only writes
 * [master] to a local file (or plays [variants].first()) and attaches [headers].
 */
data class SourceHubGroup(
    val key: String = "",
    val service: String = "",
    val label: String = "",
    val headers: Map<String, String> = emptyMap(),
    val playbackType: String = "hls",
    val languages: List<String> = emptyList(),
    val master: String? = null,
    val variants: List<String> = emptyList()
) {
    fun fileBaseName(): String = key.map { if (it.isLetterOrDigit()) it else '_' }.joinToString("")
    val sortedLanguages: List<String> get() = languages
    val language: String? get() = languages.firstOrNull()
}

/** Gemma's season/episode tree, carried on the `gemma` service event. */
data class SourceHubTree(val seasons: List<SourceHubTreeSeason> = emptyList())

data class SourceHubTreeSeason(
    val season: Int = 0,
    val title: String = "",
    val episodes: List<SourceHubTreeEpisode> = emptyList()
)

data class SourceHubTreeEpisode(
    val episode: Int = 0,
    val title: String = "",
    val languages: List<String> = emptyList()
)

data class SourceHubServiceResult(
    val service: String = "",
    val status: String? = null,
    val ok: Boolean = false,
    val sources: List<SourceHubSource> = emptyList(),
    val groups: List<SourceHubGroup> = emptyList(),
    val tree: SourceHubTree? = null,
    @SerializedName("skipped") val skipped: String? = null,
    val error: String? = null
)

data class SourceHubResolveResult(
    val ok: Boolean = false,
    val title: String? = null,
    val count: Int = 0,
    val sources: List<SourceHubSource> = emptyList(),
    val groups: List<SourceHubGroup> = emptyList(),
    val tree: SourceHubTree? = null,
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
}

data class PlaybackOption(
    val id: String,
    val label: String,
    val kind: String,
    val url: String = "",
    val headers: Map<String, String> = emptyMap(),
    val service: String = "",
    val language: String? = null,
    val languages: List<String> = emptyList(),
    val alternates: List<String> = emptyList(),
    val playbackType: String = "hls"
)
