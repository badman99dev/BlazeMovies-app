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
    fun playbackHeaders(): Map<String, String> = playback?.headers ?: emptyMap()
}

/** One independent audio stream inside a group (service-driven, no client-side language logic). */
data class SourceHubTrack(
    val language: String = "",
    val url: String = ""
)

/**
 * A ready-to-render picker row composed by the source-hub (the old client-side
 * `groupByServer()` / `buildMasterPlaylist()` live on the server now). The client only prints
 * [label] + [languages] (the red text — verbatim from the server), writes [master] to a local
 * file (or plays a [tracks] entry / [variants].first()), and attaches [headers].
 *
 * [audio]/[tracks] are generic: when the service exposes independent per-language streams the
 * row declares `audio = true` and lists them in [tracks]. [languages] never follows the playing
 * track — it is exactly what the server sent.
 */
data class SourceHubGroup(
    val key: String = "",
    val service: String = "",
    val label: String = "",
    val headers: Map<String, String> = emptyMap(),
    @SerializedName("playbackType") val playbackType: String = "hls",
    val languages: List<String> = emptyList(),
    val master: String? = null,
    val variants: List<String> = emptyList(),
    val tracks: List<SourceHubTrack> = emptyList(),
    val audio: Boolean = false,
    val pinned: Int = 0
) {
    fun fileBaseName(): String = key.map { if (it.isLetterOrDigit()) it else '_' }.joinToString("")
    val sortedLanguages: List<String> get() = languages
    val language: String? get() = languages.firstOrNull()
}

/** Season/episode tree for episodic content, carried on a service event. */
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
    val tracks: List<SourceHubTrack> = emptyList(),
    val audio: Boolean = false,
    val pinned: Int = 0,
    val alternates: List<String> = emptyList(),
    val playbackType: String = "hls"
)
