package com.movie.app.best.ui.screens.livetv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.model.TvStreamCategory
import com.movie.app.best.data.model.UnifiedChannel
import com.movie.app.best.data.model.toUnified
import com.movie.app.best.data.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LiveTvUiState(
    val channels: List<UnifiedChannel> = emptyList(),
    val categories: List<TvStreamCategory> = emptyList(),
    val selectedCategory: String? = null,
    val totalChannels: Int = 0,
    val currentOffset: Int = 0,
    val canLoadMore: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isFiltering: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LiveTvViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    companion object {
        // 80 = 4-per-row × 20 rows → rows stay perfectly intact when paginating
        private const val PAGE_LIMIT = 80
    }

    private val _uiState = MutableStateFlow(LiveTvUiState())
    val uiState: StateFlow<LiveTvUiState> = _uiState.asStateFlow()

    init { loadInitial() }

    fun loadInitial() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, selectedCategory = null) }
            coroutineScope {
                val catsDeferred       = async { fetchCategoriesSafe() }
                val firstPageDeferred  = async { fetchPageSafe(null, 0) }

                val cats       = catsDeferred.await()
                val firstPage  = firstPageDeferred.await()

                _uiState.update {
                    it.copy(
                        channels        = firstPage.channels,
                        categories      = cats,
                        totalChannels   = firstPage.total,
                        currentOffset   = firstPage.offset + firstPage.channels.size,
                        canLoadMore     = firstPage.channels.size >= PAGE_LIMIT &&
                                (firstPage.offset + firstPage.channels.size) < firstPage.total,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun loadMore() {
        val current = _uiState.value
        if (current.isLoadingMore || !current.canLoadMore) return
        val nextOffset = current.currentOffset
        if (nextOffset >= current.totalChannels) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val result = fetchPageSafe(current.selectedCategory, nextOffset)
            _uiState.update {
                it.copy(
                    channels      = it.channels + result.channels,
                    currentOffset  = result.offset + result.channels.size,
                    canLoadMore    = result.channels.size >= PAGE_LIMIT &&
                            (result.offset + result.channels.size) < result.total,
                    isLoadingMore = false
                )
            }
        }
    }

    fun filterByCategory(category: String?) {
        val current = _uiState.value
        if (current.selectedCategory == category) return

        viewModelScope.launch {
            _uiState.update { it.copy(isFiltering = true, selectedCategory = category) }
            val result = fetchPageSafe(category, 0)
            _uiState.update {
                it.copy(
                    channels        = result.channels,
                    totalChannels   = result.total,
                    currentOffset   = result.offset + result.channels.size,
                    canLoadMore     = result.channels.size >= PAGE_LIMIT &&
                            (result.offset + result.channels.size) < result.total,
                    isFiltering = false
                )
            }
        }
    }

    private suspend fun fetchCategoriesSafe(): List<TvStreamCategory> = try {
        var result: List<TvStreamCategory> = emptyList()
        repository.getTvStreamCategories().collect { r ->
            if (r is Resource.Success) result = r.data?.categories ?: emptyList()
        }
        result
    } catch (_: Exception) { emptyList() }

    private suspend fun fetchPageSafe(category: String?, offset: Int): PageResult = try {
        var result = PageResult(emptyList(), 0, offset)
        repository.getTvStreams(category, offset, PAGE_LIMIT).collect { r ->
            if (r is Resource.Success) {
                val data = r.data
                if (data != null) {
                    val unified = data.channels.mapIndexed { idx, ch -> ch.toUnified(idx, offset) }
                    result = PageResult(unified, data.total, data.offset)
                }
            }
        }
        result
    } catch (_: Exception) { PageResult(emptyList(), 0, offset) }

    private data class PageResult(
        val channels: List<UnifiedChannel>,
        val total: Int,
        val offset: Int
    )
}
