package com.movie.app.best.ui.screens.moviewatch

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.BookmarkItem
import com.movie.app.best.data.model.GemmaEpisodeInfo
import com.movie.app.best.data.model.GemmaExtractionResult
import com.movie.app.best.data.model.ImdbCertificatesResponse
import com.movie.app.best.data.model.ImdbTitleDetails
import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.model.WasmerMovie
import com.movie.app.best.data.remote.GemmaExtractorService
import com.movie.app.best.data.remote.ImdbApiService
import com.movie.app.best.data.remote.StreamRequestApiResponse
import com.movie.app.best.data.repository.FirebaseRepository
import com.movie.app.best.data.repository.MovieRepository
import com.movie.app.best.data.repository.MyListRefreshState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

private sealed class StreamCandidate {
    data class Backend(val url: String) : StreamCandidate()
    object Gemma : StreamCandidate()
}

@HiltViewModel
class MovieWatchViewModel @Inject constructor(
    private val gemmaExtractor: GemmaExtractorService,
    private val imdbApi: ImdbApiService,
    private val repository: MovieRepository,
    private val firebaseRepository: FirebaseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val imdbId: String = savedStateHandle["imdbId"] ?: ""
    private val movieTitle: String = savedStateHandle["title"] ?: ""
    private val movieId: String = savedStateHandle["movieId"] ?: ""
    private val slug: String = savedStateHandle["slug"] ?: ""
    private val hasStream: Boolean = savedStateHandle["hasStream"] ?: false
    private val playerUrl: String = savedStateHandle["playerUrl"] ?: ""
    val posterUrl: String = savedStateHandle["posterUrl"] ?: ""
    val backendCast: String = savedStateHandle["cast"] ?: ""
    val backendDirector: String = savedStateHandle["director"] ?: ""
    val title: String get() = movieTitle
    val contentSlug: String get() = slug

    private val _state = MutableStateFlow(MovieWatchState())
    val state: StateFlow<MovieWatchState> = _state.asStateFlow()

    private var backendTried = false
    private var gemmaTried = false
    private var gemmaResult: GemmaExtractionResult? = null

    init {
        MyListRefreshState.markStale()
        loadAll()
    }

    private fun buildCandidates(): List<StreamCandidate> {
        val list = mutableListOf<StreamCandidate>()
        if (hasStream || playerUrl.isNotEmpty()) {
            list.add(StreamCandidate.Backend(
                "https://sparkling-breeze-1ad6.badman993944.workers.dev/?id=${movieId}"
            ))
        }
        if (imdbId.startsWith("tt")) {
            list.add(StreamCandidate.Gemma)
        }
        return list
    }

    private fun loadAll() {
        val needsImdb = imdbId.startsWith("tt")
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    showBuffering = needsImdb,
                    error = null,
                    backendCast = backendCast,
                    backendDirector = backendDirector
                )
            }

            // IMDb enrichment (title + certificates) in parallel asyncs
            val titleDetailsDeferred = viewModelScope.async {
                try { imdbApi.getTitleDetails(imdbId) } catch (_: Exception) { null }
            }
            val certificatesDeferred = viewModelScope.async {
                try { imdbApi.getCertificates(imdbId) } catch (_: Exception) { null }
            }

            // Stream resolution runs in parallel in the background (does not gate overlay)
            viewModelScope.launch { tryNextCandidate() }

            // Similar movies + bookmark status in parallel background
            loadSimilarMovies(imdbId)
            checkBookmarkStatus()

            // Gate overlay ONLY on IMDb responses, max 4 seconds
            if (needsImdb) {
                val titleDetails = withTimeoutOrNull(4000) { titleDetailsDeferred.await() }
                val certificates = withTimeoutOrNull(4000) { certificatesDeferred.await() }
                _state.update {
                    it.copy(
                        showBuffering = false,
                        titleDetails = titleDetails,
                        ageRating = certificates?.let { c -> extractAgeRating(c) } ?: ""
                    )
                }
            } else {
                _state.update { it.copy(showBuffering = false) }
            }
        }
    }

    private suspend fun tryNextCandidate() {
        val candidates = buildCandidates()
        val next = candidates.firstOrNull { candidate ->
            when (candidate) {
                is StreamCandidate.Backend -> !backendTried
                is StreamCandidate.Gemma -> !gemmaTried
            }
        }

        if (next == null) {
            _state.update { it.copy(isLoading = false, currentM3u8 = null, error = "Source not found") }
            return
        }

        when (next) {
            is StreamCandidate.Backend -> {
                backendTried = true
                _state.update {
                    it.copy(
                        isLoading = false,
                        currentM3u8 = next.url,
                        activeSource = "backend",
                        error = null
                    )
                }
            }
            is StreamCandidate.Gemma -> {
                gemmaTried = true
                val result = gemmaExtractor.extract(imdbId)
                gemmaResult = result
                if (result.seasons.isEmpty()) {
                    tryNextCandidate()
                    return
                }
                collectAvailableLanguages(result)
                val episode = getMovieEpisode(result)
                if (episode == null) {
                    tryNextCandidate()
                    return
                }
                val lang = _state.value.selectedLanguage
                val file = episode.languages[lang] ?: episode.languages.values.firstOrNull()
                if (file == null) {
                    tryNextCandidate()
                    return
                }
                val m3u8 = gemmaExtractor.resolveFile(file, result.csrfKey)
                if (m3u8 == null) {
                    tryNextCandidate()
                    return
                }
                _state.update {
                    it.copy(
                        isLoading = false,
                        currentM3u8 = m3u8,
                        activeSource = "gemma",
                        error = null
                    )
                }
            }
        }
    }

    fun onPlaybackError() {
        _state.update { it.copy(showBuffering = false, currentM3u8 = null) }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            tryNextCandidate()
        }
    }

    private fun getMovieEpisode(result: GemmaExtractionResult): GemmaEpisodeInfo? {
        val season = result.seasons.values.firstOrNull() ?: return null
        return season.episodes.values.firstOrNull()
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

    fun selectLanguage(lang: String) {
        _state.update { it.copy(selectedLanguage = lang) }
        if (_state.value.activeSource != "gemma") return
        val result = gemmaResult ?: return
        val episode = getMovieEpisode(result) ?: return
        val file = episode.languages[lang] ?: return
        viewModelScope.launch {
            _state.update { it.copy(currentM3u8 = null) }
            val m3u8 = gemmaExtractor.resolveFile(file, result.csrfKey)
            if (m3u8 != null) {
                _state.update { it.copy(currentM3u8 = m3u8) }
            }
        }
    }

    fun toggleBookmark() {
        viewModelScope.launch {
            val currentlyBookmarked = _state.value.isBookmarked
            if (currentlyBookmarked) {
                firebaseRepository.removeBookmark(slug)
            } else {
                firebaseRepository.addBookmark(
                    BookmarkItem(
                        slug = slug,
                        title = movieTitle,
                        posterUrl = posterUrl,
                        isSeries = false
                    )
                )
            }
            _state.update { it.copy(isBookmarked = !currentlyBookmarked) }
            MyListRefreshState.markStale()
        }
    }

    private fun checkBookmarkStatus() {
        viewModelScope.launch {
            val bookmarked = firebaseRepository.isBookmarked(slug)
            _state.update { it.copy(isBookmarked = bookmarked) }
        }
    }

    private fun loadSimilarMovies(imdbId: String) {
        if (!imdbId.startsWith("tt")) return
        viewModelScope.launch {
            _state.update { it.copy(isSimilarLoading = true) }
            repository.getSimilar(imdbId).collect { result ->
                when (result) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        _state.update {
                            it.copy(
                                similarMovies = result.data?.items ?: emptyList(),
                                isSimilarLoading = false,
                                similarError = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _state.update {
                            it.copy(
                                isSimilarLoading = false,
                                similarError = result.error
                            )
                        }
                    }
                }
            }
        }
    }

    fun requestStream() {
        viewModelScope.launch {
            _state.update { it.copy(isStreamRequesting = true) }
            try {
                val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val tokenResult = user?.getIdToken(false)?.await()
                val authHeader = "Bearer ${tokenResult?.token ?: ""}"
                val response = repository.submitStreamRequest(authHeader, slug)
                _state.update {
                    it.copy(
                        isStreamRequesting = false,
                        streamRequested = !response.already_requested && !response.has_stream,
                        streamRequestResult = response,
                        showStreamRequestResult = true
                    )
                }
                if (response.already_requested) {
                    _state.update { it.copy(streamRequested = true) }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isStreamRequesting = false,
                        streamRequestError = e.message
                    )
                }
            }
        }
    }

    fun dismissStreamRequestResult() {
        _state.update { it.copy(showStreamRequestResult = false) }
    }

    private fun extractAgeRating(response: ImdbCertificatesResponse): String {
        return response.certificates.find { it.country?.code == "IN" }?.rating
            ?: response.certificates.find { it.country?.code == "US" }?.rating
            ?: ""
    }
}

data class MovieWatchState(
    val isLoading: Boolean = false,
    val showBuffering: Boolean = false,
    val currentM3u8: String? = null,
    val activeSource: String = "",
    val titleDetails: ImdbTitleDetails? = null,
    val ageRating: String = "",
    val availableLanguages: List<String> = emptyList(),
    val selectedLanguage: String = "Hindi",
    val similarMovies: List<WasmerMovie> = emptyList(),
    val isSimilarLoading: Boolean = false,
    val similarError: String? = null,
    val error: String? = null,
    val streamRequested: Boolean = false,
    val isStreamRequesting: Boolean = false,
    val streamRequestResult: StreamRequestApiResponse? = null,
    val showStreamRequestResult: Boolean = false,
    val streamRequestError: String? = null,
    val isBookmarked: Boolean = false,
    val backendCast: String = "",
    val backendDirector: String = ""
)
