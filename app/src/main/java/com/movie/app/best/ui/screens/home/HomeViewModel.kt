package com.movie.app.best.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.model.Movie
import com.movie.app.best.data.model.AppNotification
import com.movie.app.best.data.model.SliderResult
import com.movie.app.best.data.model.UnifiedChannel
import com.movie.app.best.data.repository.PrefetchCache
import com.movie.app.best.data.debug.NetworkMonitor
import com.movie.app.best.data.repository.MeiliSearchRepository
import com.movie.app.best.data.repository.MovieRepository
import com.movie.app.best.data.repository.Zee5TokenRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val meiliRepository: MeiliSearchRepository,
    private val zee5TokenRepository: Zee5TokenRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    companion object {
        private const val PAGE_LIMIT = 45
    }

    init {
        loadAllContent()
        viewModelScope.launch { meiliRepository.pingAndPrefetchKey() }
        viewModelScope.launch { zee5TokenRepository.prefetchTokens() }
    }

    fun loadAllContent() {
        val cache = PrefetchCache

        if (cache.slider != null) {
            _uiState.update { it.copy(sliderMovies = cache.slider!!, isSliderLoading = false) }
        } else { loadSlider() }

        if (cache.trending != null) {
            _uiState.update { it.copy(trendingMovies = cache.trending!!, isTrendingLoading = false) }
        } else { loadTrending() }

        if (cache.latestUploads != null) {
            val data = cache.latestUploads!!
            _uiState.update {
                it.copy(
                    allTabMovies = data,
                    isAllTabLoading = false,
                    allTabOffset = 0,
                    allTabTotal = Int.MAX_VALUE,
                    canLoadMoreAllTab = data.size >= 45
                )
            }
        } else { loadAllTab() }

        if (cache.notification != null) {
            _uiState.update { it.copy(notification = cache.notification, isNotificationLoading = false) }
        } else { loadNotification() }

        if (cache.liveChannels != null) {
            _uiState.update { it.copy(liveChannels = cache.liveChannels!!, isLiveChannelsLoading = false) }
        } else { loadLiveChannels() }

        loadMyFeed()
    }

    init {
        viewModelScope.launch {
            NetworkMonitor.refreshCounter.collect {
                if (it > 0) loadAllContent()
            }
        }
    }

    fun loadTrending() {
        viewModelScope.launch {
            repository.getTrending(offset = 0, limit = 20).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isTrendingLoading = true) }
                    }
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                trendingMovies = result.data?.items ?: emptyList(),
                                isTrendingLoading = false,
                                trendingError = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isTrendingLoading = false,
                                trendingError = result.error
                            )
                        }
                    }
                }
            }
        }
    }

    fun loadMyFeed() {
        viewModelScope.launch {
            repository.getMyFeed(offset = 0, limit = 20).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isMyFeedLoading = true) }
                    }
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                myFeedMovies = result.data?.items ?: emptyList(),
                                isMyFeedLoading = false,
                                myFeedError = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isMyFeedLoading = false,
                                myFeedError = result.error
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

    fun loadSlider() {
        viewModelScope.launch {
            repository.getSlider().collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isSliderLoading = true) }
                    }
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                sliderMovies = (result.data?.movies ?: emptyList()).filter { it.hasStream },
                                isSliderLoading = false,
                                sliderError = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isSliderLoading = false,
                                sliderError = result.error
                            )
                        }
                    }
                }
            }
        }
    }

    fun loadAllTab() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAllTabLoading = true) }
            repository.getLatestUploads(offset = 0, limit = PAGE_LIMIT).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isAllTabLoading = true) }
                    }
                    is Resource.Success -> {
                        val data = result.data
                        _uiState.update {
                            it.copy(
                                allTabMovies = data?.items ?: emptyList(),
                                isAllTabLoading = false,
                                allTabError = null,
                                allTabOffset = data?.offset ?: 0,
                                allTabTotal = data?.total ?: 0,
                                canLoadMoreAllTab = (data?.items?.size ?: 0) >= PAGE_LIMIT && ((data?.offset ?: 0) + (data?.items?.size ?: 0)) < (data?.total ?: 0)
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isAllTabLoading = false,
                                allTabError = result.error
                            )
                        }
                    }
                }
            }
        }
    }

    fun loadMoreAllTab() {
        val current = _uiState.value
        if (current.isAllTabLoadingMore || !current.canLoadMoreAllTab) return

        val nextOffset = current.allTabOffset + current.allTabMovies.size
        if (nextOffset >= current.allTabTotal) return

        viewModelScope.launch {
            _uiState.update { it.copy(isAllTabLoadingMore = true) }
            repository.getLatestUploads(offset = nextOffset, limit = PAGE_LIMIT).collect { result ->
                when (result) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        val newItems = result.data?.items ?: emptyList()
                        _uiState.update {
                            it.copy(
                                allTabMovies = it.allTabMovies + newItems,
                                isAllTabLoadingMore = false,
                                allTabOffset = result.data?.offset ?: nextOffset,
                                canLoadMoreAllTab = newItems.size >= PAGE_LIMIT && (nextOffset + newItems.size) < (result.data?.total ?: 0)
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isAllTabLoadingMore = false) }
                    }
                }
            }
        }
    }

    fun loadNotification() {
        viewModelScope.launch {
            repository.getNotification().collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isNotificationLoading = true) }
                    }
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                notification = result.data,
                                isNotificationLoading = false,
                                notificationError = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isNotificationLoading = false,
                                notificationError = result.error
                            )
                        }
                    }
                }
            }
        }
    }

    fun dismissNotification() {
        _uiState.update { it.copy(notification = null) }
    }

    fun loadLiveChannels() {
        viewModelScope.launch {
            repository.getTvStreamsUnified(limit = 15).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLiveChannelsLoading = true) }
                    }
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                liveChannels = result.data ?: emptyList(),
                                isLiveChannelsLoading = false
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                liveChannels = emptyList(),
                                isLiveChannelsLoading = false
                            )
                        }
                    }
                }
            }
        }
    }
}

data class HomeUiState(
    val sliderMovies: List<Movie> = emptyList(),
    val isSliderLoading: Boolean = false,
    val sliderError: String? = null,

    val trendingMovies: List<Movie> = emptyList(),
    val isTrendingLoading: Boolean = false,
    val trendingError: String? = null,

    val myFeedMovies: List<Movie> = emptyList(),
    val isMyFeedLoading: Boolean = false,
    val myFeedError: String? = null,

    val allTabMovies: List<Movie> = emptyList(),
    val isAllTabLoading: Boolean = false,
    val isAllTabLoadingMore: Boolean = false,
    val allTabError: String? = null,
    val allTabOffset: Int = 0,
    val allTabTotal: Int = 0,
    val canLoadMoreAllTab: Boolean = false,

    val notification: AppNotification? = null,
    val isNotificationLoading: Boolean = false,
    val notificationError: String? = null,

    val liveChannels: List<UnifiedChannel> = emptyList(),
    val isLiveChannelsLoading: Boolean = false
)
