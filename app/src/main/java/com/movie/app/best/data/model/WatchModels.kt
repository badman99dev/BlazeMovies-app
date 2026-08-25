package com.movie.app.best.data.model

import java.text.SimpleDateFormat
import java.util.Locale

data class WatchEpisode(
    val episodeNo: Int,
    val seasonNo: Int,
    val title: String,
    val stillImageUrl: String,
    val runtimeMinutes: Int,
    val rating: Double,
    val releaseDate: String,
    val plot: String,
    val languages: Map<String, String>,
    val available: Boolean = true
) {
    val displayTitle: String get() = if (title.isNotBlank()) "E$episodeNo · $title" else "Episode $episodeNo"
    val displayMeta: String get() {
        val parts = mutableListOf<String>()
        if (runtimeMinutes > 0) parts.add("${runtimeMinutes} min")
        if (rating > 0) parts.add("⭐ ${String.format("%.1f", rating)}")
        if (releaseDate.isNotBlank()) parts.add(releaseDate)
        return parts.joinToString(" · ")
    }
    val releaseYear: String get() {
        if (releaseDate.isBlank()) return ""
        return try {
            val parsed = SimpleDateFormat("dd MMM yyyy", Locale.US).parse(releaseDate) ?: return ""
            SimpleDateFormat("yyyy", Locale.US).format(parsed)
        } catch (_: Exception) { "" }
    }
}

data class GemmaExtractionResult(
    val seasons: Map<Int, GemmaSeasonInfo>,
    val csrfKey: String = "",
    val error: String? = null
)

data class GemmaSeasonInfo(
    val title: String,
    val episodes: Map<Int, GemmaEpisodeInfo>
)

data class GemmaEpisodeInfo(
    val title: String = "",
    val languages: Map<String, String> = emptyMap()
)

data class ExtractionState(
    val isLoading: Boolean = false,
    val result: GemmaExtractionResult? = null,
    val imdbEpisodes: Map<Int, List<ImdbEpisode>> = emptyMap(),
    val selectedSeason: Int = 1,
    val selectedLanguage: String = "Hindi",
    val availableLanguages: List<String> = emptyList(),
    val currentM3u8: String? = null,
    val currentEpisode: WatchEpisode? = null,
    val titleDetails: ImdbTitleDetails? = null,
    val ageRating: String = "",
    val error: String? = null,
    val backendCast: String = "",
    val backendDirector: String = "",
    val backendDescription: String = "",
    val backendGenres: String = ""
) {
    val mergedEpisodes: List<WatchEpisode>
        get() {
            val gemmaSeason = result?.seasons?.get(selectedSeason)
            val imdbEps = imdbEpisodes[selectedSeason] ?: emptyList()
            val imdbMap = imdbEps.associateBy { it.episodeNumber }

            if (gemmaSeason != null) {
                return gemmaSeason.episodes.map { (epNo, gemmaEp) ->
                    val imdb = imdbMap[epNo]
                    WatchEpisode(
                        episodeNo = epNo,
                        seasonNo = selectedSeason,
                        title = imdb?.title ?: gemmaEp.title.replaceAfterLast(" - ", "").trim().ifBlank { "Episode $epNo" },
                        stillImageUrl = imdb?.stillImageUrl ?: "",
                        runtimeMinutes = imdb?.runtimeMinutes ?: 0,
                        rating = imdb?.ratingValue ?: 0.0,
                        releaseDate = imdb?.formattedDate ?: "",
                        plot = imdb?.plot ?: "",
                        languages = gemmaEp.languages,
                        available = true
                    )
                }.sortedBy { it.episodeNo }
            }

            if (imdbEps.isNotEmpty()) {
                return imdbEps.map { imdb ->
                    WatchEpisode(
                        episodeNo = imdb.episodeNumber,
                        seasonNo = selectedSeason,
                        title = imdb.title,
                        stillImageUrl = imdb.stillImageUrl,
                        runtimeMinutes = imdb.runtimeMinutes,
                        rating = imdb.ratingValue,
                        releaseDate = imdb.formattedDate,
                        plot = imdb.plot,
                        languages = emptyMap(),
                        available = false
                    )
                }.sortedBy { it.episodeNo }
            }

            return emptyList()
        }

    val seasonKeys: List<Int>
        get() = ((result?.seasons?.keys ?: emptySet()) + imdbEpisodes.keys).toSortedSet().toList()

    val isSelectedSeasonAvailable: Boolean
        get() = result?.seasons?.containsKey(selectedSeason) == true
}
