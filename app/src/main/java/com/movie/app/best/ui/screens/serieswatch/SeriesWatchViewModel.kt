package com.movie.app.best.ui.screens.serieswatch

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.ExtractionState
import com.movie.app.best.data.model.GemmaExtractionResult
import com.movie.app.best.data.model.ImdbEpisode
import com.movie.app.best.data.model.WatchEpisode
import com.movie.app.best.data.remote.GemmaExtractorService
import com.movie.app.best.data.remote.ImdbApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class SeriesWatchViewModel @Inject constructor(
    private val gemmaExtractor: GemmaExtractorService,
    private val imdbApi: ImdbApiService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val imdbId: String = savedStateHandle["imdbId"] ?: ""
    private val seriesTitle: String = savedStateHandle["title"] ?: ""
    private val movieId: String = savedStateHandle["movieId"] ?: ""
    private val slug: String = savedStateHandle["slug"] ?: ""
    private val targetSeason: Int = savedStateHandle["targetSeason"] ?: -1
    val backendCast: String = savedStateHandle["cast"] ?: ""
    val backendDirector: String = savedStateHandle["director"] ?: ""
    val backendDescription: String = savedStateHandle["description"] ?: ""

    private val _state = MutableStateFlow(ExtractionState())
    val state: StateFlow<ExtractionState> = _state.asStateFlow()

    private val imdbCache = mutableMapOf<Int, List<ImdbEpisode>>()

    init {
        loadAll()
    }

    private fun loadAll() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, backendCast = backendCast, backendDirector = backendDirector, backendDescription = backendDescription) }

            // Start IMDb title & certificate requests in viewModelScope so they survive
            // the 2-second wait and keep running in the background until they finish.
            val titleDetailsDeferred = viewModelScope.async {
                try { imdbApi.getTitleDetails(imdbId) } catch (_: Exception) { null }
            }
            val certificatesDeferred = viewModelScope.async {
                try { imdbApi.getCertificates(imdbId) } catch (_: Exception) { null }
            }

            // Later-arrival observers: update state when the responses finally come in
            viewModelScope.launch {
                try {
                    val details = titleDetailsDeferred.await()
                    if (details != null) _state.update { it.copy(titleDetails = details) }
                } catch (_: Exception) {}
            }
            viewModelScope.launch {
                try {
                    val certs = certificatesDeferred.await()
                    if (certs != null) _state.update { it.copy(ageRating = extractAgeRating(certs)) }
                } catch (_: Exception) {}
            }

            val gemmaResult = gemmaExtractor.extract(imdbId)

            if (gemmaResult.seasons.isEmpty()) {
                _state.update { it.copy(isLoading = false, error = "Source not found") }
                return@launch
            }

            val availableSeasons = gemmaResult.seasons.keys

            // If target season is not in the backend, try IMDb to confirm it exists
            var targetImdbEpisodes: List<ImdbEpisode>? = null
            if (targetSeason != -1 && targetSeason !in availableSeasons) {
                val targetDeferred = viewModelScope.async {
                    try { imdbApi.getEpisodes(imdbId, targetSeason) } catch (_: Exception) { null }
                }
                viewModelScope.launch {
                    try {
                        val resp = targetDeferred.await()
                        if (resp != null) {
                            imdbCache[targetSeason] = resp.episodes
                            _state.update { it.copy(imdbEpisodes = it.imdbEpisodes.toMutableMap().apply { put(targetSeason, resp.episodes) }) }
                        }
                    } catch (_: Exception) {}
                }
                val targetResponse = withTimeoutOrNull(2000) { targetDeferred.await() }
                targetImdbEpisodes = targetResponse?.episodes?.takeIf { it.isNotEmpty() }
                targetImdbEpisodes?.let { imdbCache[targetSeason] = it }
            }

            val selectedSeason = when {
                targetSeason != -1 && (targetSeason in availableSeasons || targetImdbEpisodes?.isNotEmpty() == true) -> targetSeason
                targetSeason != -1 && availableSeasons.isNotEmpty() -> availableSeasons.maxOrNull() ?: availableSeasons.firstOrNull() ?: 1
                availableSeasons.isNotEmpty() -> availableSeasons.maxOrNull() ?: availableSeasons.firstOrNull() ?: 1
                else -> {
                    _state.update { it.copy(isLoading = false, error = "Source not found") }
                    return@launch
                }
            }

            // Start IMDb episodes for the selected season; observer updates cache/state when it arrives
            val episodesDeferred = if (imdbCache[selectedSeason] == null) {
                viewModelScope.async {
                    try { imdbApi.getEpisodes(imdbId, selectedSeason) } catch (_: Exception) { null }
                }
            } else null

            episodesDeferred?.let { deferred ->
                viewModelScope.launch {
                    try {
                        val resp = deferred.await()
                        if (resp != null) {
                            imdbCache[selectedSeason] = resp.episodes
                            _state.update { it.copy(imdbEpisodes = it.imdbEpisodes.toMutableMap().apply { put(selectedSeason, resp.episodes) }) }
                        }
                    } catch (_: Exception) {}
                }
            }

            // Wait up to 2 seconds for the IMDb requests (not cancelling them)
            val titleDetails = withTimeoutOrNull(2000) { titleDetailsDeferred.await() }
            val certificates = withTimeoutOrNull(2000) { certificatesDeferred.await() }
            val episodesResponse = withTimeoutOrNull(2000) { episodesDeferred?.await() }

            episodesResponse?.let { resp ->
                imdbCache[selectedSeason] = resp.episodes
            }

            collectAvailableLanguages(gemmaResult)

            val imdbEpisodesMap = mutableMapOf<Int, List<ImdbEpisode>>()
            imdbCache[selectedSeason]?.let { imdbEpisodesMap[selectedSeason] = it }
            targetImdbEpisodes?.let { imdbEpisodesMap[targetSeason] = it }

            _state.update {
                it.copy(
                    isLoading = false,
                    result = gemmaResult,
                    selectedSeason = selectedSeason,
                    error = null,
                    imdbEpisodes = imdbEpisodesMap,
                    titleDetails = titleDetails,
                    ageRating = certificates?.let { extractAgeRating(it) } ?: ""
                )
            }

            // Auto-select the first episode of the selected season
            val episodes = _state.value.mergedEpisodes
            if (episodes.isNotEmpty()) {
                val first = episodes.first()
                _state.update { it.copy(currentEpisode = first) }
                if (first.available) {
                    onEpisodeClick(first)
                }
            }
        }
    }

    private fun collectAvailableLanguages(result: GemmaExtractionResult) {
        val langs = mutableSetOf<String>()
        for (season in result.seasons.values) {
            for (ep in season.episodes.values) {
                langs.addAll(ep.languages.keys)
            }
        }
        val sorted = langs.sortedWith(compareBy<String> { lang ->
            when {
                lang.contains("Hindi", ignoreCase = true) -> 0
                lang.contains("English", ignoreCase = true) -> 1
                else -> 2
            }
        }.thenBy { it })
        val default = sorted.firstOrNull { it.contains("Hindi", ignoreCase = true) } ?: sorted.firstOrNull() ?: "Hindi"
        _state.update { it.copy(availableLanguages = sorted, selectedLanguage = default) }
    }

    fun loadImdbEpisodes(seasonNo: Int, onLoaded: ((List<ImdbEpisode>) -> Unit)? = null) {
        if (imdbCache.containsKey(seasonNo)) {
            val cached = imdbCache[seasonNo]!!
            _state.update { it.copy(imdbEpisodes = it.imdbEpisodes.toMutableMap().apply { put(seasonNo, cached) }) }
            onLoaded?.invoke(cached)
            return
        }
        viewModelScope.launch {
            try {
                val resp = imdbApi.getEpisodes(imdbId, seasonNo)
                val eps = resp.episodes
                imdbCache[seasonNo] = eps
                _state.update { it.copy(imdbEpisodes = it.imdbEpisodes.toMutableMap().apply { put(seasonNo, eps) }) }
                onLoaded?.invoke(eps)
            } catch (_: Exception) { onLoaded?.invoke(emptyList()) }
        }
    }

    fun selectSeason(seasonNo: Int) {
        _state.update { it.copy(selectedSeason = seasonNo) }
        loadImdbEpisodes(seasonNo) { episodes ->
            if (episodes.isNotEmpty()) {
                val merged = _state.value.mergedEpisodes
                if (merged.isNotEmpty()) {
                    val first = merged.first()
                    _state.update { it.copy(currentEpisode = first) }
                    if (first.available) {
                        onEpisodeClick(first)
                    }
                }
            }
        }
    }

    fun selectLanguage(lang: String) {
        _state.update { it.copy(selectedLanguage = lang) }
        val currentEp = _state.value.currentEpisode
        if (currentEp != null) {
            val m3u8 = currentEp.languages[lang]
            if (m3u8 != null) {
                resolveAndPlay(m3u8, currentEp)
            }
        }
    }

    fun onEpisodeClick(episode: WatchEpisode) {
        if (!episode.available) {
            _state.update { it.copy(currentEpisode = episode) }
            return
        }
        val lang = _state.value.selectedLanguage
        val file = episode.languages[lang] ?: episode.languages.values.firstOrNull() ?: return
        _state.update { it.copy(currentEpisode = episode) }
        resolveAndPlay(file, episode)
    }

    private fun resolveAndPlay(file: String, episode: WatchEpisode) {
        val csrfKey = _state.value.result?.csrfKey ?: return
        viewModelScope.launch {
            _state.update { it.copy(currentM3u8 = null) }
            val m3u8 = gemmaExtractor.resolveFile(file, csrfKey)
            if (m3u8 != null) {
                _state.update { it.copy(currentM3u8 = m3u8) }
            }
        }
    }

    private fun extractAgeRating(response: com.movie.app.best.data.model.ImdbCertificatesResponse): String {
        return response.certificates.find { it.country?.code == "IN" }?.rating
            ?: response.certificates.find { it.country?.code == "US" }?.rating
            ?: ""
    }
}
