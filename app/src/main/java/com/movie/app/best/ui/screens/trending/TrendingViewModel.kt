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
    val currentOffset: Int = 0,
    val total: Int = 0,
    val canLoadMore: Boolean = false
)

@HiltViewModel
class TrendingViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    companion object {
        private const val PAGE_LIMIT = 45
    }

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
            repository.getTrending(offset = 0, limit = PAGE_LIMIT).collect { result ->
                when (result) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        val data = result.data
                        _uiState.update {
                            it.copy(
                                popularMovies = data?.items ?: emptyList(),
                                isPopularLoading = false,
                                popularError = null,
                                currentOffset = data?.offset ?: 0,
                                total = data?.total ?: 0,
                                canLoadMore = (data?.items?.size ?: 0) >= PAGE_LIMIT && ((data?.offset ?: 0) + (data?.items?.size ?: 0)) < (data?.total ?: 0)
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
        if (current.isPopularLoadingMore || !current.canLoadMore) return

        val nextOffset = current.currentOffset + current.popularMovies.size
        if (nextOffset >= current.total) return

        viewModelScope.launch {
            _uiState.update { it.copy(isPopularLoadingMore = true) }
            repository.getTrending(offset = nextOffset, limit = PAGE_LIMIT).collect { result ->
                when (result) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        val newItems = result.data?.items ?: emptyList()
                        _uiState.update {
                            it.copy(
                                popularMovies = it.popularMovies + newItems,
                                isPopularLoadingMore = false,
                                currentOffset = result.data?.offset ?: nextOffset,
                                canLoadMore = newItems.size >= PAGE_LIMIT && (nextOffset + newItems.size) < (result.data?.total ?: 0)
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
