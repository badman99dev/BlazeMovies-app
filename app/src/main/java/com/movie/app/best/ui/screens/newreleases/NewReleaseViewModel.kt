package com.movie.app.best.ui.screens.newreleases

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

data class NewReleaseTabState(
    val movies: List<Movie> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val currentOffset: Int = 0,
    val total: Int = 0,
    val canLoadMore: Boolean = false
)

data class NewReleaseUiState(
    val activeCountry: String = "in",
    val inTab: NewReleaseTabState = NewReleaseTabState(),
    val usTab: NewReleaseTabState = NewReleaseTabState()
)

@HiltViewModel
class NewReleaseViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    companion object {
        private const val PAGE_LIMIT = 45
        const val COUNTRY_IN = "in"
        const val COUNTRY_US = "us"
    }

    private val _uiState = MutableStateFlow(NewReleaseUiState())
    val uiState: StateFlow<NewReleaseUiState> = _uiState.asStateFlow()

    private val initialized = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            NetworkMonitor.refreshCounter.collect {
                if (it > 0) loadTab(_uiState.value.activeCountry, force = true)
            }
        }
    }

    fun setActiveCountry(country: String) {
        val c = if (country == COUNTRY_US) COUNTRY_US else COUNTRY_IN
        if (_uiState.value.activeCountry == c) {
            if (!initialized.contains(c)) loadTab(c)
            return
        }
        _uiState.update { it.copy(activeCountry = c) }
        if (!initialized.contains(c)) loadTab(c)
    }

    private fun tabFor(country: String): NewReleaseTabState =
        if (country == COUNTRY_US) _uiState.value.usTab else _uiState.value.inTab

    private fun updateTab(country: String, transform: (NewReleaseTabState) -> NewReleaseTabState) {
        _uiState.update { state ->
            if (country == COUNTRY_US) state.copy(usTab = transform(state.usTab))
            else state.copy(inTab = transform(state.inTab))
        }
    }

    fun loadTab(country: String, force: Boolean = false) {
        val current = tabFor(country)
        if (!force && (current.isLoading || initialized.contains(country))) return

        viewModelScope.launch {
            updateTab(country) { it.copy(isLoading = true) }
            repository.getNewRelease(country = country, offset = 0, limit = PAGE_LIMIT).collect { result ->
                when (result) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        val data = result.data
                        initialized.add(country)
                        updateTab(country) {
                            it.copy(
                                movies = data?.items ?: emptyList(),
                                isLoading = false,
                                error = null,
                                currentOffset = data?.offset ?: 0,
                                total = data?.total ?: 0,
                                canLoadMore = (data?.items?.size ?: 0) >= PAGE_LIMIT &&
                                    ((data?.offset ?: 0) + (data?.items?.size ?: 0)) < (data?.total ?: 0)
                            )
                        }
                    }
                    is Resource.Error -> {
                        updateTab(country) {
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

    fun loadMore(country: String) {
        val current = tabFor(country)
        if (current.isLoadingMore || !current.canLoadMore) return

        val nextOffset = current.currentOffset + current.movies.size
        if (nextOffset >= current.total) return

        viewModelScope.launch {
            updateTab(country) { it.copy(isLoadingMore = true) }
            repository.getNewRelease(country = country, offset = nextOffset, limit = PAGE_LIMIT).collect { result ->
                when (result) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        val newItems = result.data?.items ?: emptyList()
                        updateTab(country) {
                            it.copy(
                                movies = it.movies + newItems,
                                isLoadingMore = false,
                                currentOffset = result.data?.offset ?: nextOffset,
                                canLoadMore = newItems.size >= PAGE_LIMIT && (nextOffset + newItems.size) < (result.data?.total ?: 0)
                            )
                        }
                    }
                    is Resource.Error -> {
                        updateTab(country) { it.copy(isLoadingMore = false) }
                    }
                }
            }
        }
    }
}
