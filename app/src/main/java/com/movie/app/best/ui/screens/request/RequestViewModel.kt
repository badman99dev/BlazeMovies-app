package com.movie.app.best.ui.screens.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.movie.app.best.data.model.ImdbEpisode
import com.movie.app.best.data.model.ImdbSearchTitle
import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.remote.ImdbApiService
import com.movie.app.best.data.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

enum class RequestType { MOVIE, TV }

data class RequestUiState(
    val type: RequestType = RequestType.MOVIE,
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<ImdbSearchTitle> = emptyList(),
    val searchError: String? = null,
    val selected: ImdbSearchTitle? = null,
    val manualMode: Boolean = false,
    val title: String = "",
    val year: String = "",
    val imdbId: String = "",
    val season: String = "",
    val episode: String = "",
    val note: String = "",
    val seasons: List<Int> = emptyList(),
    val seasonEpisodes: List<ImdbEpisode> = emptyList(),
    val loadingEpisodes: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitted: Boolean = false,
    val error: String? = null
) {
    val canSubmit: Boolean get() = title.trim().length >= 2 && !isSubmitting
}

@HiltViewModel
class RequestViewModel @Inject constructor(
    private val imdbApi: ImdbApiService,
    private val repository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RequestUiState())
    val uiState: StateFlow<RequestUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var episodesJob: Job? = null

    fun setType(type: RequestType) {
        if (_uiState.value.type == type) return
        searchJob?.cancel()
        episodesJob?.cancel()
        _uiState.update {
            it.copy(
                type = type,
                results = emptyList(),
                searchError = null,
                isSearching = false,
                selected = null,
                manualMode = false,
                title = "",
                year = "",
                imdbId = "",
                season = "",
                episode = "",
                seasons = emptyList(),
                seasonEpisodes = emptyList(),
                loadingEpisodes = false,
                error = null
            )
        }
    }

    fun setQuery(value: String) {
        _uiState.update { it.copy(query = value, searchError = null) }
    }

    fun search() {
        val state = _uiState.value
        val q = state.query.trim()
        if (q.length < 2) {
            _uiState.update { it.copy(searchError = "Type at least 2 characters") }
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, searchError = null) }
            try {
                val response = imdbApi.searchTitles(q)
                val filtered = filterForType(response.titles, _uiState.value.type)
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        results = filtered,
                        searchError = if (filtered.isEmpty()) "No results found — use \"None of the above\"" else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        results = emptyList(),
                        searchError = e.message ?: "Search failed. Check your connection."
                    )
                }
            }
        }
    }

    private fun filterForType(titles: List<ImdbSearchTitle>, type: RequestType): List<ImdbSearchTitle> {
        val filtered = titles.filter {
            if (type == RequestType.TV) it.isSeries
            else !it.isSeries
        }
        return if (filtered.isEmpty()) titles else filtered
    }

    fun selectTitle(item: ImdbSearchTitle) {
        episodesJob?.cancel()
        _uiState.update {
            it.copy(
                selected = item,
                manualMode = false,
                title = item.displayTitle,
                year = item.displayYear,
                imdbId = item.id,
                season = "",
                episode = "",
                seasons = emptyList(),
                seasonEpisodes = emptyList(),
                loadingEpisodes = false,
                searchError = null,
                error = null
            )
        }
        if (_uiState.value.type == RequestType.TV && item.id.isNotBlank()) {
            loadEpisodes(item.id)
        }
    }

    fun useManualMode() {
        episodesJob?.cancel()
        _uiState.update {
            it.copy(
                manualMode = true,
                selected = null,
                title = "",
                year = "",
                imdbId = "",
                season = "",
                episode = "",
                seasons = emptyList(),
                seasonEpisodes = emptyList(),
                loadingEpisodes = false,
                searchError = null,
                error = null
            )
        }
    }

    fun backToSearch() {
        episodesJob?.cancel()
        _uiState.update {
            it.copy(
                manualMode = false,
                selected = null,
                title = "",
                year = "",
                imdbId = "",
                season = "",
                episode = "",
                seasons = emptyList(),
                seasonEpisodes = emptyList(),
                loadingEpisodes = false,
                error = null
            )
        }
    }

    private fun loadEpisodes(imdbId: String) {
        episodesJob = viewModelScope.launch {
            _uiState.update { it.copy(loadingEpisodes = true) }
            try {
                val response = imdbApi.getEpisodes(imdbId)
                val seasons = response.episodes
                    .mapNotNull { it.season.toIntOrNull() }
                    .distinct()
                    .sorted()
                _uiState.update {
                    it.copy(
                        loadingEpisodes = false,
                        seasons = seasons,
                        seasonEpisodes = response.episodes.sortedWith(
                            compareBy({ it.season.toIntOrNull() ?: 0 }, { it.episodeNumber })
                        )
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(loadingEpisodes = false) }
            }
        }
    }

    fun setSeason(value: String) {
        _uiState.update {
            it.copy(season = value.filter { c -> c.isDigit() }.take(4), episode = "", error = null)
        }
    }

    fun selectSeason(season: Int) {
        _uiState.update { it.copy(season = season.toString(), episode = "", error = null) }
    }

    fun setEpisode(value: String) {
        _uiState.update { it.copy(episode = value.filter { c -> c.isDigit() }.take(4), error = null) }
    }

    fun selectEpisode(item: ImdbEpisode) {
        _uiState.update {
            it.copy(
                season = item.season.toIntOrNull()?.toString() ?: it.season,
                episode = if (item.episodeNumber > 0) item.episodeNumber.toString() else it.episode,
                error = null
            )
        }
    }

    fun setTitle(value: String) {
        _uiState.update { it.copy(title = value.take(255), error = null) }
    }

    fun setYear(value: String) {
        _uiState.update { it.copy(year = value.filter { c -> c.isDigit() }.take(4), error = null) }
    }

    fun setImdbId(value: String) {
        _uiState.update { it.copy(imdbId = value.trim().take(20), error = null) }
    }

    fun setNote(value: String) {
        _uiState.update { it.copy(note = value.take(1000)) }
    }

    fun submit() {
        val state = _uiState.value
        if (state.isSubmitting) return

        val title = state.title.trim()
        if (title.length < 2) {
            _uiState.update { it.copy(error = "Enter a movie / show name") }
            return
        }
        val year = state.year.trim().toIntOrNull()
        if (year != null && (year < 1874 || year > 2100)) {
            _uiState.update { it.copy(error = "Enter a valid release year") }
            return
        }
        val imdbId = state.imdbId.trim()
        if (imdbId.isNotEmpty() && !Regex("^tt\\d{6,10}$").matches(imdbId)) {
            _uiState.update { it.copy(error = "IMDb id must look like tt1234567") }
            return
        }
        val season = state.season.trim().toIntOrNull() ?: 0
        val episode = state.episode.trim().toIntOrNull() ?: 0
        if (state.type == RequestType.TV && episode > 0 && season <= 0) {
            _uiState.update { it.copy(error = "Season is required when an episode is given") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }

            val authHeader = try {
                val token = FirebaseAuth.getInstance().currentUser
                    ?.getIdToken(false)
                    ?.await()
                    ?.token
                "Bearer ${token ?: ""}"
            } catch (_: Exception) {
                ""
            }

            val body = mutableMapOf<String, Any>(
                "media_type" to if (state.type == RequestType.TV) "tv" else "movie",
                "title" to title
            )
            year?.let { body["year"] = it }
            if (imdbId.isNotEmpty()) body["imdb_id"] = imdbId
            if (state.type == RequestType.TV) {
                if (season > 0) body["season"] = season
                if (episode > 0) body["episode"] = episode
            }
            state.note.trim().takeIf { it.isNotEmpty() }?.let { body["message"] = it }

            when (val result = repository.submitMovieRequest(authHeader, body)) {
                is Resource.Success -> _uiState.update {
                    it.copy(isSubmitting = false, submitted = true, error = null)
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isSubmitting = false, error = result.error ?: "Could not submit request")
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun resetAfterSubmit() {
        _uiState.update {
            RequestUiState(type = it.type)
        }
    }
}