package com.movie.app.best.util

import android.net.Uri

/** What a home-slider deep link points at. */
enum class SliderKind { MOVIE, SERIES, SPORTS, OTHER }

data class SliderTarget(
    val kind: SliderKind,
    val slug: String,
    val isSeries: Boolean
) {
    val isContent: Boolean get() = kind == SliderKind.MOVIE || kind == SliderKind.SERIES
}

/**
 * Classifies a slider item's `deep_link` so the card can decide which glass actions
 * to show (share + My List + Watch for movie/series, "Watch Live" for sports, none
 * for anything else) and where to route.
 */
object SliderLink {

    fun parse(deepLink: String?): SliderTarget {
        val raw = deepLink?.trim().orEmpty()
        if (raw.isEmpty()) return SliderTarget(SliderKind.OTHER, "", false)

        val uri = runCatching { Uri.parse(raw) }.getOrNull()
            ?: return SliderTarget(SliderKind.OTHER, "", false)

        val host = uri.host?.lowercase().orEmpty()
        val segments = uri.pathSegments.orEmpty()
        fun seg(i: Int): String = segments.getOrNull(i).orEmpty()

        return when {
            host == "watch-movie" -> SliderTarget(SliderKind.MOVIE, seg(0), false)
            host == "watch-series" -> SliderTarget(SliderKind.SERIES, seg(0), true)

            host == "content" && seg(0).equals("series", true) -> SliderTarget(SliderKind.SERIES, seg(1), true)
            host == "content" && seg(0).equals("movie", true) -> SliderTarget(SliderKind.MOVIE, seg(1), false)

            host == "movie" -> SliderTarget(SliderKind.MOVIE, seg(0), false)
            host == "series" -> SliderTarget(SliderKind.SERIES, seg(0), true)

            host == "sport" || host == "sports" || host == "watch-sports" ->
                SliderTarget(SliderKind.SPORTS, seg(0), false)

            else -> SliderTarget(SliderKind.OTHER, "", false)
        }
    }
}