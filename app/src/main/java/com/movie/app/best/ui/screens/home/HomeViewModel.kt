package com.movie.app.best.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.model.Movie
import com.movie.app.best.data.model.AppNotification
import com.movie.app.best.data.model.SliderResult
import com.movie.app.best.data.model.UnifiedChannel
import com.movie.app.best.data.model.toUnified
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

    private var homeCallDone = false
    private var allTabCallDone = false
    private var seriesCallDone = false
    private var myFeedCallDone = false

    private fun checkPageReady() {
        if (homeCallDone && allTabCallDone && seriesCallDone && myFeedCallDone) {
            _uiState.update { it.copy(isPageLoading = false) }
        }
    }

    init {
        loadAllContent()
        viewModelScope.launch { meiliRepository.pingAndPrefetchKey() }
        viewModelScope.launch { zee5TokenRepository.prefetchTokens() }
    }

    fun loadAllContent() {
        _uiState.update { it.copy(isPageLoading = true) }
        homeCallDone = false
        allTabCallDone = false
        seriesCallDone = false
        myFeedCallDone = false

        loadHomeFeed()
        loadAllTab()
        loadSeries()
        loadMyFeed()
        loadNotification()
    }

    init {
        viewModelScope.launch {
            NetworkMonitor.refreshCounter.collect {
                if (it > 0) loadAllContent()
            }
        }
    }

    fun loadHomeFeed() {
        viewModelScope.launch {
            repository.getHomeFeed().collect { result ->
                when (result) {
                    is Resource.Loading -> return@collect
                    is Resource.Success -> {
                        val data = result.data
                        _uiState.update { state ->
                            state.copy(
                                sliderMovies = data?.slider?.movies?.filter { it.hasStream } ?: state.sliderMovies,
                                isSliderLoading = false,
                                sliderError = null,
                                trendingMovies = data?.trending?.items ?: state.trendingMovies,
                                isTrendingLoading = false,
                                trendingError = null,
                                newIndiaReleases = data?.newReleaseIn?.items ?: state.newIndiaReleases,
                                isNewIndiaLoading = false,
                                newUsReleases = data?.newReleaseUs?.items ?: state.newUsReleases,
                                isNewUsLoading = false,
                                liveChannels = data?.liveChannels?.let { tv ->
                                    tv.channels.mapIndexed { idx, ch -> ch.toUnified(idx) }
                                } ?: state.liveChannels,
                                isLiveChannelsLoading = false
                            )
                        }
                    }
                    is Resource.Error -> {}
                }
                homeCallDone = true
                checkPageReady()
            }
        }
    }

    fun loadMyFeed() {
        viewModelScope.launch {
            repository.getMyFeed(offset = 0, limit = 20).collect { result ->
                when (result) {
                    is Resource.Loading -> return@collect
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
                myFeedCallDone = true
                checkPageReady()
            }
        }
        viewModelScope.launch {
            delay(3000)
            repository.recreateMyFeed()
        }
    }

    fun loadAllTab() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAllTabLoading = true) }
            repository.getLatestUploads(offset = 0, limit = PAGE_LIMIT).collect { result ->
                when (result) {
                    is Resource.Loading -> return@collect
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
                allTabCallDone = true
                checkPageReady()
            }
        }
    }

    fun loadSeries() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSeriesLoading = true) }
            repository.getLatestUploads(offset = 0, limit = 12, type = "series").collect { result ->
                when (result) {
                    is Resource.Loading -> return@collect
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                seriesMovies = result.data?.items ?: emptyList(),
                                isSeriesLoading = false,
                                seriesError = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isSeriesLoading = false,
                                seriesError = result.error
                            )
                        }
                    }
                }
                seriesCallDone = true
                checkPageReady()
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
}

data class HomeUiState(
    val isPageLoading: Boolean = true,

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

    val seriesMovies: List<Movie> = emptyList(),
    val isSeriesLoading: Boolean = false,
    val seriesError: String? = null,

    val newIndiaReleases: List<Movie> = emptyList(),
    val isNewIndiaLoading: Boolean = false,

    val newUsReleases: List<Movie> = emptyList(),
    val isNewUsLoading: Boolean = false,

    val notification: AppNotification? = null,
    val isNotificationLoading: Boolean = false,
    val notificationError: String? = null,

    val liveChannels: List<UnifiedChannel> = emptyList(),
    val isLiveChannelsLoading: Boolean = false
)