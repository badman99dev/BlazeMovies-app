package com.movie.app.best.ui.screens.tvshows

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.model.Category
import com.movie.app.best.data.model.Movie
import com.movie.app.best.data.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TVShowsViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    companion object {
        private const val PAGE_LIMIT = 45
    }

    private val _uiState = MutableStateFlow(TVShowsUiState())
    val uiState: StateFlow<TVShowsUiState> = _uiState.asStateFlow()

    init {
        loadTVShows()
    }

    fun loadTVShows(offset: Int = 0) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getCategoryMovies("web-series", offset, PAGE_LIMIT).collect { result ->
                when (result) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        val data = result.data
                        val movies = data?.items ?: emptyList()
                        val newOffset = data?.offset ?: offset
                        val total = data?.total ?: 0
                        _uiState.update {
                            it.copy(
                                tvShows = if (offset == 0) movies else it.tvShows + movies,
                                isLoading = false,
                                error = null,
                                currentOffset = newOffset,
                                total = total,
                                canLoadMore = movies.size >= PAGE_LIMIT && (newOffset + movies.size) < total
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(isLoading = false, error = result.error)
                        }
                    }
                }
            }
        }
    }

    fun loadNextPage() {
        val current = _uiState.value
        if (current.canLoadMore && !current.isLoading) {
            loadTVShows(current.currentOffset + current.tvShows.size)
        }
    }
}

data class TVShowsUiState(
    val tvShows: List<Movie> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentOffset: Int = 0,
    val total: Int = 0,
    val canLoadMore: Boolean = false
)
