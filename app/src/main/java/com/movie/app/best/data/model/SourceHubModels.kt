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
    val url: String = "",
    val playback: SourceHubPlayback? = null
) {
    val id: String
        get() = "$service|${server ?: ""}|$quality|$url"

    val displayLabel: String
        get() {
            val svc = when (service.lowercase()) {
                "vidsrc" -> "VidSrc"
                "filmu" -> "FilmU"
                "vidlove" -> "VidLove"
                "vidnest" -> "VidNest"
                "gemma" -> "Gemma"
                "native" -> "Native"
                else -> service.replaceFirstChar { it.uppercase() }
            }
            val parts = mutableListOf(svc)
            server?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
            quality.takeIf { it.isNotBlank() && !it.equals("auto", true) }?.let { parts.add(it) }
            return parts.joinToString(" • ")
        }

    fun playbackHeaders(): Map<String, String> = playback?.headers ?: emptyMap()
}

data class SourceHubServiceResult(
    val service: String = "",
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
    val language: String? = null
)
