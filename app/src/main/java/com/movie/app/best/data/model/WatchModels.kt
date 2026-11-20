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
    val languages: List<String>,
    val available: Boolean = true
) {
    val displayTitle: String get() = if (title.isNotBlank()) "E$episodeNo · $title" else "Episode $episodeNo"
    val displayMeta: String get() {
        val parts = mutableListOf<String>()
        if (runtimeMinutes > 0) parts.add("$runtimeMinutes min")
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

data class ExtractionState(
    val isLoading: Boolean = false,
    /** Bumped on every playOption() so the player subtree rebuilds even when the URL/id repeats. */
    val playToken: Long = 0L,
    /** Season/episode tree supplied by the source-hub (used to paint the episode list). */
    val hubTree: SourceHubTree? = null,
    val imdbEpisodes: Map<Int, List<ImdbEpisode>> = emptyMap(),
    val selectedSeason: Int = 1,
    val selectedLanguage: String = "Hindi",
    val availableLanguages: List<String> = emptyList(),
    val currentM3u8: String? = null,
    val currentPlaybackType: String = "hls",
    val currentHeaders: Map<String, String> = emptyMap(),
    val activeSource: String = "",
    val activeService: String = "",
    val selectedOptionId: String? = null,
    val options: List<PlaybackOption> = emptyList(),
    val currentEpisode: WatchEpisode? = null,
    val titleDetails: ImdbTitleDetails? = null,
    val crewCredits: List<CrewPerson> = emptyList(),
    val ageRating: String = "",
    val episodeNoSource: Boolean = false,
    val serverScan: List<ServerScanRow> = emptyList(),
    val error: String? = null,
    val backendCast: String = "",
    val backendDirector: String = "",
    val backendDescription: String = "",
    val backendGenres: String = ""
) {
    val mergedEpisodes: List<WatchEpisode>
        get() {
            val treeSeason = hubTree?.seasons?.firstOrNull { it.season == selectedSeason }
            val imdbEps = imdbEpisodes[selectedSeason] ?: emptyList()
            val imdbMap = imdbEps.associateBy { it.episodeNumber }

            if (treeSeason != null && treeSeason.episodes.isNotEmpty()) {
                return treeSeason.episodes.map { e ->
                    val imdb = imdbMap[e.episode]
                    WatchEpisode(
                        episodeNo = e.episode,
                        seasonNo = selectedSeason,
                        title = imdb?.title ?: e.title.replaceAfterLast(" - ", "").trim().ifBlank { "Episode ${e.episode}" },
                        stillImageUrl = imdb?.stillImageUrl ?: "",
                        runtimeMinutes = imdb?.runtimeMinutes ?: 0,
                        rating = imdb?.ratingValue ?: 0.0,
                        releaseDate = imdb?.formattedDate ?: "",
                        plot = imdb?.plot ?: "",
                        languages = e.languages,
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
                        languages = emptyList(),
                        available = true
                    )
                }.sortedBy { it.episodeNo }
            }

            return emptyList()
        }

    val seasonKeys: List<Int>
        get() = ((hubTree?.seasons?.map { it.season } ?: emptyList()) + imdbEpisodes.keys).toSortedSet().toList()

    val isSelectedSeasonAvailable: Boolean
        get() = hubTree?.seasons?.any { it.season == selectedSeason } == true
}
