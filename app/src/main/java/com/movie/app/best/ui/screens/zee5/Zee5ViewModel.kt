package com.movie.app.best.ui.screens.zee5

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.Zee5Bucket
import com.movie.app.best.data.model.Zee5CollectionResponse
import com.movie.app.best.data.model.Zee5DetailResponse
import com.movie.app.best.data.model.Zee5Item
import com.movie.app.best.data.model.Zee5Season
import com.movie.app.best.data.remote.Zee5ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class Zee5UiState {
    object Loading : Zee5UiState()
    data class Success(
        val buckets: List<Zee5Bucket>,
        val isLoadMore: Boolean = false,
        val hasMore: Boolean = true
    ) : Zee5UiState()
    data class Error(val message: String) : Zee5UiState()
}

sealed class Zee5DetailState {
    object Loading : Zee5DetailState()
    data class Success(val detail: Zee5DetailResponse) : Zee5DetailState()
    data class Error(val message: String) : Zee5DetailState()
}

sealed class Zee5EpisodesState {
    object Loading : Zee5EpisodesState()
    data class Success(val episodes: List<Zee5Item>, val hasMore: Boolean, val seasonId: String) : Zee5EpisodesState()
    data class Error(val message: String) : Zee5EpisodesState()
}

enum class Zee5Tab {
    ALL, TV_SHOWS, MOVIES
}

@HiltViewModel
class Zee5ViewModel @Inject constructor(
    val apiService: Zee5ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<Zee5UiState>(Zee5UiState.Loading)
    val uiState: StateFlow<Zee5UiState> = _uiState.asStateFlow()

    private val _detailState = MutableStateFlow<Zee5DetailState>(Zee5DetailState.Loading)
    val detailState: StateFlow<Zee5DetailState> = _detailState.asStateFlow()

    private val _episodesState = MutableStateFlow<Zee5EpisodesState>(Zee5EpisodesState.Loading)
    val episodesState: StateFlow<Zee5EpisodesState> = _episodesState.asStateFlow()

    private val _currentTab = MutableStateFlow(Zee5Tab.ALL)
    val currentTab: StateFlow<Zee5Tab> = _currentTab.asStateFlow()

    // Pagination state for merged ALL tab (homepage + free5)
    private val loadedBuckets = mutableListOf<Zee5Bucket>()
    private val seenBucketIds = mutableSetOf<String>()
    private val seenItemIds = mutableSetOf<String>()
    private var isLoadingMore = false
    private var hasMoreBuckets = true
    private var homePageIndex = 0
    private var free5PageIndex = 0
    private val homePageQueue = listOf(0, 2)
    private val free5PageQueue = listOf(0, 2)

    // Collection IDs for different tabs
    companion object {
        const val MOVIES_COLLECTION = "0-8-movies"
        const val TV_SHOWS_COLLECTION = "0-8-tvshows"
        const val HOME_COLLECTION = "0-8-homepage"
        const val EPISODE_PAGE_LIMIT = 25
    }

    init {
        loadTab(Zee5Tab.ALL)
    }

    fun switchTab(tab: Zee5Tab) {
        if (_currentTab.value == tab) return
        _currentTab.value = tab
        resetPagination()
        loadTab(tab)
    }

    private fun resetPagination() {
        isLoadingMore = false
        hasMoreBuckets = true
        homePageIndex = 0
        free5PageIndex = 0
        loadedBuckets.clear()
        seenBucketIds.clear()
        seenItemIds.clear()
    }

    fun loadTab(tab: Zee5Tab) {
        viewModelScope.launch {
            _uiState.value = Zee5UiState.Loading
            try {
                val result = when (tab) {
                    Zee5Tab.ALL -> loadAllBuckets()
                    Zee5Tab.TV_SHOWS -> loadCollection(TV_SHOWS_COLLECTION)
                    Zee5Tab.MOVIES -> loadCollection(MOVIES_COLLECTION)
                }
                loadedBuckets.clear()
                loadedBuckets.addAll(result)
                _uiState.value = Zee5UiState.Success(
                    buckets = result,
                    hasMore = tab == Zee5Tab.ALL && hasMoreBuckets
                )
            } catch (e: Exception) {
                _uiState.value = Zee5UiState.Error(e.message ?: "Failed to load")
            }
        }
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refresh() {
        if (_isRefreshing.value) return
        val tab = _currentTab.value
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                resetPagination()
                val result = when (tab) {
                    Zee5Tab.ALL -> loadAllBuckets()
                    Zee5Tab.TV_SHOWS -> loadCollection(TV_SHOWS_COLLECTION)
                    Zee5Tab.MOVIES -> loadCollection(MOVIES_COLLECTION)
                }
                loadedBuckets.clear()
                loadedBuckets.addAll(result)
                _uiState.value = Zee5UiState.Success(
                    buckets = result,
                    hasMore = tab == Zee5Tab.ALL && hasMoreBuckets
                )
            } catch (_: Exception) {
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun loadMore() {
        if (isLoadingMore || !hasMoreBuckets) return
        if (_currentTab.value != Zee5Tab.ALL) return

        viewModelScope.launch {
            isLoadingMore = true
            _uiState.value = Zee5UiState.Success(
                loadedBuckets.toList(),
                isLoadMore = true,
                hasMore = true
            )
            try {
                val newBuckets = loadNextPage()
                if (newBuckets.isNotEmpty()) {
                    loadedBuckets.addAll(newBuckets)
                }
                hasMoreBuckets = homePageIndex < homePageQueue.size || free5PageIndex < free5PageQueue.size
            } catch (e: Exception) {
                hasMoreBuckets = false
            } finally {
                isLoadingMore = false
                _uiState.value = Zee5UiState.Success(
                    loadedBuckets.toList(),
                    isLoadMore = false,
                    hasMore = hasMoreBuckets
                )
            }
        }
    }

    private suspend fun loadAllBuckets(): List<Zee5Bucket> {
        // Start with first homepage page
        val page = homePageQueue[homePageIndex]
        homePageIndex++
        val response = apiService.getCollection(HOME_COLLECTION, page = page, limit = 25)
        val buckets = response.buckets?.filter { addBucket(it) } ?: emptyList()
        hasMoreBuckets = homePageIndex < homePageQueue.size || free5PageIndex < free5PageQueue.size
        return buckets
    }

    private suspend fun loadNextPage(): List<Zee5Bucket> {
        return when {
            homePageIndex < homePageQueue.size -> {
                val page = homePageQueue[homePageIndex]
                homePageIndex++
                val response = apiService.getCollection(HOME_COLLECTION, page = page, limit = 25)
                response.buckets?.filter { addBucket(it) } ?: emptyList()
            }
            free5PageIndex < free5PageQueue.size -> {
                val page = free5PageQueue[free5PageIndex]
                free5PageIndex++
                val response = apiService.getFree5(page = page)
                response.buckets?.filter { addBucket(it) } ?: emptyList()
            }
            else -> {
                hasMoreBuckets = false
                emptyList()
            }
        }
    }

    private suspend fun loadCollection(id: String): List<Zee5Bucket> {
        val response = apiService.getCollection(id, page = 0, limit = 25)
        val buckets = response.buckets?.filter { addBucket(it) } ?: emptyList()
        return buckets
    }

    private fun addBucket(bucket: Zee5Bucket): Boolean {
        val id = bucket.id ?: bucket.collectionId ?: bucket.title ?: "bucket_${seenBucketIds.size}"
        return if (seenBucketIds.contains(id)) {
            false
        } else {
            seenBucketIds.add(id)
            true
        }
    }

    fun loadDetails(contentId: String) {
        viewModelScope.launch {
            _detailState.value = Zee5DetailState.Loading
            seenItemIds.clear()
            loadedEpisodes.clear()
            currentLoadedShowId = null
            currentShowOnAir = false
            try {
                val detail = apiService.getDetails(contentId)
                currentShowOnAir = detail.onAir?.toString()?.equals("true", ignoreCase = true) ?: false
                _detailState.value = Zee5DetailState.Success(detail)
            } catch (e: Exception) {
                _detailState.value = Zee5DetailState.Error(e.message ?: "Failed to load details")
            }
        }
    }

    private val loadedEpisodes = mutableListOf<Zee5Item>()
    private var loadedSeasonId: String? = null
    private var currentLoadedShowId: String? = null
    private var currentShowOnAir = false
    private var totalEpisodes = 0
    private var pageSize = EPISODE_PAGE_LIMIT
    private var nextPageToLoad = 1
    private val loadedPages = mutableSetOf<Int>()
    private var isLoadingEpisodes = false
    private var hasMoreEpisodes = true
    private var seasons = listOf<Zee5Season>()

    fun loadEpisodes(showId: String) {
        // Guard against re-loading the same show when navigating back from player
        if (showId == currentLoadedShowId && _episodesState.value is Zee5EpisodesState.Success) return
        currentLoadedShowId = showId

        viewModelScope.launch {
            resetEpisodeState()
            try {
                val seasonData = apiService.getSeasons(showId)
                seasons = seasonData.seasons ?: emptyList()
                val season = seasons.lastOrNull()
                loadedSeasonId = season?.id ?: showId
                totalEpisodes = season?.totalEpisodes ?: 0
                computeInitialPage()
                loadEpisodesPage(loadedSeasonId ?: showId, nextPageToLoad)
            } catch (e: Exception) {
                _episodesState.value = Zee5EpisodesState.Error(e.message ?: "Failed to load episodes")
            }
        }
    }

    fun loadEpisodesForSeason(seasonId: String) {
        viewModelScope.launch {
            currentLoadedShowId = null
            resetEpisodeState()
            loadedSeasonId = seasonId
            val season = seasons.find { it.id == seasonId }
            totalEpisodes = season?.totalEpisodes ?: 0
            computeInitialPage()
            try {
                loadEpisodesPage(seasonId, nextPageToLoad)
            } catch (e: Exception) {
                _episodesState.value = Zee5EpisodesState.Error(e.message ?: "Failed to load episodes")
            }
        }
    }

    fun loadMoreEpisodes() {
        val seasonId = loadedSeasonId ?: return
        if (isLoadingEpisodes || !hasMoreEpisodes) return

        viewModelScope.launch {
            isLoadingEpisodes = true
            try {
                loadEpisodesPage(seasonId, nextPageToLoad)
            } catch (e: Exception) {
                hasMoreEpisodes = false
                _episodesState.value = (episodesState.value as? Zee5EpisodesState.Success)?.copy(hasMore = false)
                    ?: Zee5EpisodesState.Error(e.message ?: "Failed to load more episodes")
            } finally {
                isLoadingEpisodes = false
            }
        }
    }

    private fun resetEpisodeState() {
        loadedEpisodes.clear()
        loadedSeasonId = null
        loadedPages.clear()
        totalEpisodes = 0
        pageSize = EPISODE_PAGE_LIMIT
        nextPageToLoad = 1
        isLoadingEpisodes = false
        hasMoreEpisodes = true
        _episodesState.value = Zee5EpisodesState.Loading
    }

    private fun computeInitialPage() {
        val totalPages = if (totalEpisodes > 0) ((totalEpisodes + pageSize - 1) / pageSize) else 1
        nextPageToLoad = if (currentShowOnAir) totalPages else 1
    }

    private fun computeHasMore(): Boolean {
        if (totalEpisodes <= 0) return false
        val totalPages = (totalEpisodes + pageSize - 1) / pageSize
        return if (currentShowOnAir) {
            nextPageToLoad >= 1
        } else {
            nextPageToLoad <= totalPages
        }
    }

    private fun advancePage() {
        if (currentShowOnAir) nextPageToLoad-- else nextPageToLoad++
    }

    private suspend fun loadEpisodesPage(seasonId: String, page: Int) {
        if (page < 1 || loadedPages.contains(page)) {
            hasMoreEpisodes = computeHasMore()
            _episodesState.value = Zee5EpisodesState.Success(loadedEpisodes.toList(), hasMoreEpisodes, seasonId)
            return
        }

        val response = apiService.getEpisodes(
            seasonId = seasonId,
            limit = pageSize,
            page = page
        )
        val eps = response.episode
            ?: response.items
            ?: response.buckets?.flatMap { it.items ?: emptyList() }
            ?: emptyList()
        val total = response.total ?: 0

        if (total > 0 && totalEpisodes == 0) {
            totalEpisodes = total
            computeInitialPage()
        }

        val newEpisodes = eps.filter { item ->
            val id = item.id
            id != null && loadedEpisodes.none { it.id == id }
        }

        // Append in display order: reversed for on-air (latest first), as-is for archived
        val pageEpisodes = if (currentShowOnAir) newEpisodes.reversed() else newEpisodes
        loadedEpisodes.addAll(pageEpisodes)

        loadedPages.add(page)
        advancePage()

        if (total > 0) totalEpisodes = total
        hasMoreEpisodes = computeHasMore()
        _episodesState.value = Zee5EpisodesState.Success(loadedEpisodes.toList(), hasMoreEpisodes, seasonId)
    }

    fun search(query: String) {
        viewModelScope.launch {
            _uiState.value = Zee5UiState.Loading
            try {
                val response = apiService.search(query, limit = 20)
                val rails = response.data?.hybridSearchResults?.rails ?: emptyList()

                val buckets = rails.map { rail ->
                    Zee5Bucket(
                        id = rail.id,
                        title = rail.title ?: rail.originalTitle,
                        items = rail.contents?.mapNotNull { it.effectiveItem }
                    )
                }.filter { it.items?.isNotEmpty() == true }

                loadedBuckets.clear()
                loadedBuckets.addAll(buckets)
                _uiState.value = Zee5UiState.Success(buckets, hasMore = false)
            } catch (e: Exception) {
                _uiState.value = Zee5UiState.Error(e.message ?: "Search failed")
            }
        }
    }

    fun resetDetail() {
        _detailState.value = Zee5DetailState.Loading
        _episodesState.value = Zee5EpisodesState.Loading
    }
}
