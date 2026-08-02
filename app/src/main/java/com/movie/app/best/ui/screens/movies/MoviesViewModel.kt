package com.movie.app.best.ui.screens.movies

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
class MoviesViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    companion object {
        private const val PAGE_LIMIT = 45
    }

    private val _uiState = MutableStateFlow(MoviesUiState())
    val uiState: StateFlow<MoviesUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
        loadMovies()
    }

    fun loadCategories() {
        viewModelScope.launch {
            repository.getCategories().collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isCategoriesLoading = true) }
                    }
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                categories = result.data ?: emptyList(),
                                isCategoriesLoading = false,
                                categoriesError = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isCategoriesLoading = false,
                                categoriesError = result.error
                            )
                        }
                    }
                }
            }
        }
    }

    fun loadMovies(categorySlug: String? = null, offset: Int = 0) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMoviesLoading = true) }
            val slug = categorySlug ?: "bollywood"
            repository.getCategoryMovies(slug, offset, PAGE_LIMIT).collect { result ->
                when (result) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        val data = result.data
                        val movies = data?.items ?: emptyList()
                        val newOffset = data?.offset ?: offset
                        val total = data?.total ?: 0
                        _uiState.update {
                            it.copy(
                                movies = if (offset == 0) movies else it.movies + movies,
                                isMoviesLoading = false,
                                moviesError = null,
                                currentOffset = newOffset,
                                total = total,
                                canLoadMore = movies.size >= PAGE_LIMIT && (newOffset + movies.size) < total,
                                currentCategorySlug = categorySlug
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(isMoviesLoading = false, moviesError = result.error)
                        }
                    }
                }
            }
        }
    }

    fun loadNextPage() {
        val current = _uiState.value
        if (current.canLoadMore && !current.isMoviesLoading) {
            loadMovies(current.currentCategorySlug, current.currentOffset + current.movies.size)
        }
    }

    fun selectCategory(slug: String?) {
        loadMovies(categorySlug = slug, offset = 0)
    }
}

data class MoviesUiState(
    val categories: List<Category> = emptyList(),
    val isCategoriesLoading: Boolean = false,
    val categoriesError: String? = null,

    val movies: List<Movie> = emptyList(),
    val isMoviesLoading: Boolean = false,
    val moviesError: String? = null,

    val currentOffset: Int = 0,
    val total: Int = 0,
    val canLoadMore: Boolean = false,
    val currentCategorySlug: String? = null
)
