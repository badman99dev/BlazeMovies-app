package com.movie.app.best.ui.screens.myfeed

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
import kotlinx.coroutines.delay
import javax.inject.Inject

data class MyFeedUiState(
    val movies: List<Movie> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val currentOffset: Int = 0,
    val total: Int = 0,
    val canLoadMore: Boolean = false
)

@HiltViewModel
class MyFeedViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    companion object {
        private const val PAGE_LIMIT = 45
    }

    private val _uiState = MutableStateFlow(MyFeedUiState())
    val uiState: StateFlow<MyFeedUiState> = _uiState.asStateFlow()

    init {
        loadFeed()
    }

    init {
        viewModelScope.launch {
            NetworkMonitor.refreshCounter.collect { if (it > 0) loadFeed() }
        }
    }

    fun loadFeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getMyFeed(offset = 0, limit = PAGE_LIMIT).collect { result ->
                when (result) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        val data = result.data
                        _uiState.update {
                            it.copy(
                                movies = data?.items ?: emptyList(),
                                isLoading = false,
                                error = null,
                                currentOffset = data?.offset ?: 0,
                                total = data?.total ?: 0,
                                canLoadMore = (data?.items?.size ?: 0) >= PAGE_LIMIT && ((data?.offset ?: 0) + (data?.items?.size ?: 0)) < (data?.total ?: 0)
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.error
                            )
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            delay(3000)
            repository.recreateMyFeed()
        }
    }

    fun loadMore() {
        val current = _uiState.value
        if (current.isLoadingMore || !current.canLoadMore) return

        val nextOffset = current.currentOffset + current.movies.size
        if (nextOffset >= current.total) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            repository.getMyFeed(offset = nextOffset, limit = PAGE_LIMIT).collect { result ->
                when (result) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        val newItems = result.data?.items ?: emptyList()
                        _uiState.update {
                            it.copy(
                                movies = it.movies + newItems,
                                isLoadingMore = false,
                                currentOffset = result.data?.offset ?: nextOffset,
                                canLoadMore = newItems.size >= PAGE_LIMIT && (nextOffset + newItems.size) < (result.data?.total ?: 0)
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoadingMore = false) }
                    }
                }
            }
        }
    }
}
