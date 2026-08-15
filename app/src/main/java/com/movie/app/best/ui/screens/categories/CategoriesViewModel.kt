package com.movie.app.best.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.model.Category
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

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    init {
        viewModelScope.launch {
            NetworkMonitor.refreshCounter.collect { if (it > 0) loadCategories() }
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            repository.getCategories().collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                categories = (result.data ?: emptyList())
                                    .distinctBy { c -> c.slug }
                                    .sortedByDescending { cat -> cat.count },
                                isLoading = false,
                                error = null
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
}

data class CategoriesUiState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CategoryPageViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    companion object {
        private const val PAGE_LIMIT = 45
    }

    private val _uiState = MutableStateFlow(CategoryPageUiState())
    val uiState: StateFlow<CategoryPageUiState> = _uiState.asStateFlow()

    private var currentCategorySlug: String? = null

    fun loadMovies(categorySlug: String, offset: Int = 0) {
        if (categorySlug == currentCategorySlug && offset == 0 && _uiState.value.movies.isNotEmpty()) return
        currentCategorySlug = categorySlug
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getCategoryMovies(categorySlug, offset, PAGE_LIMIT).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        val data = result.data
                        val movies = data?.items ?: emptyList()
                        val newOffset = data?.offset ?: offset
                        val total = data?.total ?: 0
                        _uiState.update {
                            it.copy(
                                movies = if (offset == 0) movies else it.movies + movies,
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
            loadMovies(currentCategorySlug ?: return, current.currentOffset + current.movies.size)
        }
    }
}

data class CategoryPageUiState(
    val movies: List<Movie> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentOffset: Int = 0,
    val total: Int = 0,
    val canLoadMore: Boolean = false
)
