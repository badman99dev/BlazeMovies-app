package com.movie.app.best.ui.screens.livetv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.model.TvStreamCategory
import com.movie.app.best.data.model.UnifiedChannel
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
    val broadcastChannels: List<UnifiedChannel> = emptyList(),
    val tvChannels: List<UnifiedChannel> = emptyList(),
    val categories: List<TvStreamCategory> = emptyList(),
    val selectedCategory: String? = null,
    val totalTvChannels: Int = 0,
    val currentTvOffset: Int = 0,
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
        private const val PAGE_LIMIT = 70
    }

    private val _uiState = MutableStateFlow(LiveTvUiState())
    val uiState: StateFlow<LiveTvUiState> = _uiState.asStateFlow()

    init { loadInitial() }

    fun loadInitial() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, selectedCategory = null) }
            coroutineScope {
                val broadcastDeferred = async { fetchBroadcastsSafe() }
                val catsDeferred       = async { fetchCategoriesSafe() }
                val firstPageDeferred  = async { fetchTvPageSafe(null, 0) }

                val broadcasts = broadcastDeferred.await()
                val cats       = catsDeferred.await()
                val firstPage  = firstPageDeferred.await()

                _uiState.update {
                    it.copy(
                        broadcastChannels = broadcasts,
                        categories        = cats,
                        tvChannels        = firstPage.channels,
                        totalTvChannels   = firstPage.total,
                        currentTvOffset   = firstPage.offset + firstPage.channels.size,
                        canLoadMore       = firstPage.channels.size >= PAGE_LIMIT &&
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
        val nextOffset = current.currentTvOffset
        if (nextOffset >= current.totalTvChannels) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val result = fetchTvPageSafe(current.selectedCategory, nextOffset)
            _uiState.update {
                it.copy(
                    tvChannels      = it.tvChannels + result.channels,
                    currentTvOffset = result.offset + result.channels.size,
                    canLoadMore     = result.channels.size >= PAGE_LIMIT &&
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
            val result = fetchTvPageSafe(category, 0)
            _uiState.update {
                it.copy(
                    tvChannels      = result.channels,
                    totalTvChannels = result.total,
                    currentTvOffset = result.offset + result.channels.size,
                    canLoadMore     = result.channels.size >= PAGE_LIMIT &&
                            (result.offset + result.channels.size) < result.total,
                    isFiltering = false
                )
            }
        }
    }

    private suspend fun fetchBroadcastsSafe(): List<UnifiedChannel> = try {
        var result: List<UnifiedChannel> = emptyList()
        repository.getBroadcastsUnified().collect { r ->
            if (r is Resource.Success) result = r.data ?: emptyList()
        }
        result
    } catch (_: Exception) { emptyList() }

    private suspend fun fetchCategoriesSafe(): List<TvStreamCategory> = try {
        var result: List<TvStreamCategory> = emptyList()
        repository.getTvStreamCategories().collect { r ->
            if (r is Resource.Success) result = r.data?.categories ?: emptyList()
        }
        result
    } catch (_: Exception) { emptyList() }

    private suspend fun fetchTvPageSafe(category: String?, offset: Int): TvPageResult = try {
        var result = TvPageResult(emptyList(), 0, offset)
        repository.getTvStreams(category, offset, PAGE_LIMIT).collect { r ->
            if (r is Resource.Success) {
                val data = r.data
                if (data != null) result = TvPageResult(data.channels, data.total, data.offset)
            }
        }
        result
    } catch (_: Exception) { TvPageResult(emptyList(), 0, offset) }

    private data class TvPageResult(
        val channels: List<UnifiedChannel>,
        val total: Int,
        val offset: Int
    )
}
