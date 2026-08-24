package com.movie.app.best.ui.screens.trending

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.model.Movie
import com.movie.app.best.data.debug.NetworkMonitor
import com.movie.app.best.data.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrendingUiState(
    val popularMovies: List<Movie> = emptyList(),
    val isPopularLoading: Boolean = false,
    val isPopularLoadingMore: Boolean = false,
    val popularError: String? = null,
    val page: Int = 1,
    val hasMore: Boolean = false
)

@HiltViewModel
class TrendingViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrendingUiState())
    val uiState: StateFlow<TrendingUiState> = _uiState.asStateFlow()

    init {
        loadPopular()
    }

    init {
        viewModelScope.launch {
            NetworkMonitor.refreshCounter.collect { if (it > 0) loadPopular() }
        }
    }

    fun loadPopular() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPopularLoading = true) }
            repository.getTrending(page = 1).collect { result ->
                when (result) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        val data = result.data
                        _uiState.update {
                            it.copy(
                                popularMovies = data?.items ?: emptyList(),
                                isPopularLoading = false,
                                popularError = null,
                                page = data?.page ?: 1,
                                hasMore = data?.hasMore == true
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isPopularLoading = false,
                                popularError = result.error
                            )
                        }
                    }
                }
            }
        }
    }

    fun loadMorePopular() {
        val current = _uiState.value
        if (current.isPopularLoadingMore || !current.hasMore) return

        val nextPage = current.page + 1

        viewModelScope.launch {
            _uiState.update { it.copy(isPopularLoadingMore = true) }
            repository.getTrending(page = nextPage).collect { result ->
                when (result) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        val newItems = result.data?.items ?: emptyList()
                        _uiState.update {
                            it.copy(
                                popularMovies = it.popularMovies + newItems,
                                isPopularLoadingMore = false,
                                page = result.data?.page ?: nextPage,
                                hasMore = result.data?.hasMore == true
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isPopularLoadingMore = false) }
                    }
                }
            }
        }
    }
}