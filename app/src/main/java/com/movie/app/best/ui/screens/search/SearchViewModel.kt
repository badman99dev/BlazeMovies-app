package com.movie.app.best.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.MeiliHit
import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.model.UnifiedChannel
import com.movie.app.best.data.model.Zee5Bucket
import com.movie.app.best.data.remote.Zee5ApiService
import com.movie.app.best.data.remote.Zee5SuggestionApiService
import com.movie.app.best.data.repository.MeiliSearchRepository
import com.movie.app.best.data.repository.MeiliSearchResponse
import com.movie.app.best.data.repository.MovieRepository
import com.movie.app.best.data.repository.Zee5TokenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val zee5SuggestionApi: Zee5SuggestionApiService,
    private val zee5Api: Zee5ApiService,
    private val meiliRepository: MeiliSearchRepository,
    private val zee5TokenRepository: Zee5TokenRepository,
    private val movieRepository: MovieRepository
) : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 20
    }

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var suggestionJob: Job? = null
    private var searchJob: Job? = null

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query, isShowingSuggestions = true) }
        suggestionJob?.cancel()
        if (query.length >= 2) {
            suggestionJob = viewModelScope.launch {
                delay(400)
                getSuggestions(query)
            }
        } else {
            clearSuggestions()
        }
    }

    private suspend fun getSuggestions(query: String) {
        _uiState.update { it.copy(isSuggestionLoading = true) }
        coroutineScope {
            val meiliDeferred = async {
                try { meiliRepository.suggest(query, limit = 8) } catch (_: Exception) { null }
            }
            val zee5Deferred = async {
                try {
                    val tokens = zee5TokenRepository.getTokens()
                    val headers = if (tokens != null) zee5TokenRepository.buildAuthHeaders(tokens) else emptyMap()
                    val variables = org.json.JSONObject().apply {
                        put("searchSuggestionInput", org.json.JSONObject().apply {
                            put("query", query)
                            put("limit", 10)
                            put("page", 0)
                            put("country", "IN")
                            put("languages", org.json.JSONArray().put("en"))
                            put("profileType", 1)
                        })
                    }.toString()
                    val extensions = """{"persistedQuery":{"version":1,"sha256Hash":"33f0ac3d9172ff39e53d7a83883da324e305a0713465509798ccfdf7b81ca117"}}"""
                    zee5SuggestionApi.getSuggestions(
                        headers = headers,
                        variables = java.net.URLEncoder.encode(variables, "UTF-8"),
                        extensions = java.net.URLEncoder.encode(extensions, "UTF-8")
                    )
                } catch (_: Exception) { null }
            }

            val meiliResult = meiliDeferred.await()
            val zee5Result = zee5Deferred.await()

            _uiState.update {
                it.copy(
                    meiliSuggestions = meiliResult?.hits ?: emptyList(),
                    zee5Suggestions = zee5Result?.data?.searchSuggestions?.suggestions
                        ?.map { s -> s.text }
                        ?.filter { s -> s.isNotBlank() }
                        ?: emptyList(),
                    isSuggestionLoading = false
                )
            }
        }
    }

    fun clearSuggestions() {
        _uiState.update {
            it.copy(
                meiliSuggestions = emptyList(),
                zee5Suggestions = emptyList(),
                isSuggestionLoading = false
            )
        }
    }

    fun searchUniversal(query: String) {
        if (query.isBlank()) return
        _uiState.update {
            it.copy(
                searchQuery = query,
                isShowingSuggestions = false,
                ownResults = emptyList(),
                zee5Results = emptyList(),
                tvChannels = emptyList(),
                currentOffset = 0,
                estimatedTotalHits = 0,
                canLoadMore = false
            )
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isOwnLoading = true, isZee5Loading = true, isTvLoading = true, error = null) }
            coroutineScope {
                launch {
                    try {
                        val resp = meiliRepository.search(query, limit = PAGE_SIZE, offset = 0)
                        _uiState.update {
                            it.copy(
                                ownResults = resp.hits,
                                isOwnLoading = false,
                                currentOffset = PAGE_SIZE,
                                estimatedTotalHits = resp.estimatedTotalHits,
                                canLoadMore = PAGE_SIZE < resp.estimatedTotalHits
                            )
                        }
                    } catch (_: Exception) {
                        _uiState.update { it.copy(isOwnLoading = false) }
                    }
                }
                launch {
                    try {
                        val resp = zee5Api.search(query, limit = 20)
                        val rails = resp.data?.hybridSearchResults?.rails ?: emptyList()
                        val buckets = rails.mapNotNull { rail ->
                            val items = rail.contents?.mapNotNull { c -> c.effectiveItem }
                                ?.filter { it.id != null }
                                ?: emptyList()
                            if (items.isEmpty()) null
                            else Zee5Bucket(
                                id = rail.id,
                                title = rail.title ?: rail.originalTitle,
                                items = items
                            )
                        }
                        _uiState.update {
                            it.copy(zee5Results = buckets, isZee5Loading = false)
                        }
                    } catch (_: Exception) {
                        _uiState.update { it.copy(isZee5Loading = false) }
                    }
                }
                // ── 3rd parallel search: IPTV live TV channels (tv-stream endpoint) ──
                launch {
                    try {
                        var result: List<UnifiedChannel> = emptyList()
                        movieRepository.searchTvStreams(query).collect { r ->
                            if (r is Resource.Success) result = r.data ?: emptyList()
                        }
                        _uiState.update { it.copy(tvChannels = result, isTvLoading = false) }
                    } catch (_: Exception) {
                        _uiState.update { it.copy(isTvLoading = false) }
                    }
                }
            }
        }
    }

    fun loadNextPage() {
        val current = _uiState.value
        if (!current.canLoadMore || current.isLoadingMore || current.searchQuery.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            try {
                val resp = meiliRepository.search(
                    current.searchQuery, limit = PAGE_SIZE, offset = current.currentOffset
                )
                _uiState.update {
                    it.copy(
                        ownResults = it.ownResults + resp.hits,
                        currentOffset = it.currentOffset + PAGE_SIZE,
                        canLoadMore = it.currentOffset + PAGE_SIZE < it.estimatedTotalHits,
                        isLoadingMore = false
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    fun onSearchSubmit() {
        val query = _uiState.value.searchQuery
        if (query.isNotBlank()) {
            searchUniversal(query)
        }
    }
}

data class SearchUiState(
    val searchQuery: String = "",
    val isShowingSuggestions: Boolean = true,

    val meiliSuggestions: List<MeiliHit> = emptyList(),
    val zee5Suggestions: List<String> = emptyList(),
    val isSuggestionLoading: Boolean = false,

    val ownResults: List<MeiliHit> = emptyList(),
    val zee5Results: List<Zee5Bucket> = emptyList(),
    val tvChannels: List<UnifiedChannel> = emptyList(),
    val isOwnLoading: Boolean = false,
    val isZee5Loading: Boolean = false,
    val isTvLoading: Boolean = false,
    val error: String? = null,

    val currentOffset: Int = 0,
    val estimatedTotalHits: Int = 0,
    val canLoadMore: Boolean = false,
    val isLoadingMore: Boolean = false
)
