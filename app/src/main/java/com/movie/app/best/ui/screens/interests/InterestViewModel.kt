package com.movie.app.best.ui.screens.interests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.Movie
import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.debug.NetworkMonitor
import com.movie.app.best.data.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InterestUiState(
    val movies: List<Movie> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val page: Int = 1,
    val hasMore: Boolean = false
)

@HiltViewModel
class InterestViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InterestUiState())
    val uiState: StateFlow<InterestUiState> = _uiState.asStateFlow()

    private var interestId: String = ""
    private var configured = false

    init {
        viewModelScope.launch {
            NetworkMonitor.refreshCounter.collect {
                if (it > 0 && configured) load()
            }
        }
    }

    fun configure(interestId: String) {
        if (interestId.isBlank()) return
        if (this.interestId == interestId && configured) return
        this.interestId = interestId
        configured = true
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getInterestMovies(interestId, page = 1).collect { result ->
                when (result) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        val data = result.data
                        _uiState.update {
                            it.copy(
                                movies = data?.items ?: emptyList(),
                                isLoading = false,
                                error = null,
                                page = data?.page ?: 1,
                                hasMore = data?.hasMore == true
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
    }

    fun loadMore() {
        val current = _uiState.value
        if (current.isLoadingMore || !current.hasMore) return

        val nextPage = current.page + 1

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            repository.getInterestMovies(interestId, page = nextPage).collect { result ->
                when (result) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        val newItems = result.data?.items ?: emptyList()
                        _uiState.update {
                            it.copy(
                                movies = it.movies + newItems,
                                isLoadingMore = false,
                                page = result.data?.page ?: nextPage,
                                hasMore = result.data?.hasMore == true
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
