package com.movie.app.best.ui.screens.zee5

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.Zee5DetailResponse
import com.movie.app.best.data.model.Zee5Item
import com.movie.app.best.data.model.Zee5Season
import com.movie.app.best.data.remote.Zee5ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.movie.app.best.data.telemetry.IncidentReporter
import com.movie.app.best.data.telemetry.PlayerFailure
import com.movie.app.best.data.telemetry.PlayerIncidents
import javax.inject.Inject

data class Zee5WatchState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val detail: Zee5DetailResponse? = null,
    val seasons: List<Zee5Season> = emptyList(),
    val selectedSeasonId: String? = null,
    val episodes: List<Zee5Item> = emptyList(),
    val hasMoreEpisodes: Boolean = true,
    val currentEpisode: Zee5Item? = null,
    val currentM3u8: String? = null,
    val isFetchingStream: Boolean = false,
    val isPlaying: Boolean = false
)

@HiltViewModel
class Zee5WatchViewModel @Inject constructor(
    val apiService: Zee5ApiService,
    private val incidentReporter: IncidentReporter,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val contentId: String = savedStateHandle["contentId"] ?: ""
    private val epId: String? = savedStateHandle["epId"]
    private val epNum: Int = savedStateHandle["epNum"] ?: -1

    /** Ship a playback incident for the single Zee5 source (no failover available). */
    fun reportPlayerFailure(error: PlaybackException?, player: Player?) {
        incidentReporter.report(
            PlayerIncidents.build(
                PlayerFailure(
                    slug = contentId,
                    contentTitle = _state.value.detail?.title ?: _state.value.currentEpisode?.title ?: "",
                    serverName = "zee5",
                    serverKind = "zee5",
                    serverService = "zee5",
                    url = _state.value.currentM3u8 ?: "",
                    serverIndex = 0,
                    totalServers = 1,
                    hasMoreServers = false,
                    error = error,
                    player = player
                )
            )
        )
    }

    private val _state = MutableStateFlow(Zee5WatchState())
    private val loadedEpisodeIds = mutableSetOf<String>()
    private var totalEpisodes = 0
    private var pageSize = 25
    private var nextPageToLoad = 1
    private val loadedPages = mutableSetOf<Int>()
    private var isOnAir = false
    val state: StateFlow<Zee5WatchState> = _state.asStateFlow()

    init {
        loadContent()
    }

    private fun loadContent() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            resetEpisodeState()
            try {
                val detail = apiService.getDetails(contentId)
                isOnAir = detail.onAir?.toString()?.equals("true", ignoreCase = true) ?: false
                _state.update { it.copy(detail = detail) }
                Log.d("Zee5Watch", "loadContent contentId=$contentId isTvShow=${detail.isTvShow} onAir=$isOnAir")

                if (detail.isTvShow) {
                    val seasonData = apiService.getSeasons(contentId)
                    val seasons = seasonData.seasons ?: emptyList()
                    val season = seasons.lastOrNull()
                    val seasonId = season?.id ?: contentId
                    totalEpisodes = season?.totalEpisodes ?: 0
                    _state.update { it.copy(seasons = seasons, selectedSeasonId = seasonId) }

                    val targetPage = if (epId != null && epNum > 0) {
                        (epNum + pageSize - 1) / pageSize
                    } else {
                        computeInitialPage()
                    }

                    loadEpisodesPage(seasonId, targetPage)

                    if (epId != null) {
                        val ep = _state.value.episodes.find { it.id == epId }
                        if (ep != null) {
                            onEpisodeClick(ep)
                        } else {
                            Log.w("Zee5Watch", "epId=$epId not found on page $targetPage, falling back to first")
                            val firstEp = _state.value.episodes.firstOrNull()
                            if (firstEp != null) onEpisodeClick(firstEp)
                        }
                    } else {
                        val firstEp = _state.value.episodes.firstOrNull()
                        Log.d("Zee5Watch", "auto-play firstEp=${firstEp?.id} epNum=${firstEp?.episodeNumber}")
                        if (firstEp != null) onEpisodeClick(firstEp)
                    }
                } else {
                    _state.update { it.copy(isLoading = false) }
                    playContent(contentId)
                }
            } catch (e: Exception) {
                Log.e("Zee5Watch", "loadContent failed", e)
                _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to load") }
            }
        }
    }

    fun selectSeason(seasonId: String) {
        val season = _state.value.seasons.find { it.id == seasonId } ?: return
        viewModelScope.launch {
            resetEpisodeState()
            totalEpisodes = season.totalEpisodes ?: 0
            _state.update { it.copy(selectedSeasonId = seasonId, isLoadingMore = false) }
            computeInitialPage()
            try {
                loadEpisodesPage(seasonId, nextPageToLoad)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Failed to load season") }
            }
        }
    }

    fun loadMoreEpisodes() {
        val seasonId = _state.value.selectedSeasonId ?: return
        if (_state.value.isLoadingMore || !_state.value.hasMoreEpisodes) return

        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            try {
                loadEpisodesPage(seasonId, nextPageToLoad)
            } catch (e: Exception) {
                Log.e("Zee5Watch", "loadMoreEpisodes failed page=$nextPageToLoad", e)
                _state.update { it.copy(isLoadingMore = false, hasMoreEpisodes = false) }
            }
        }
    }

    private fun resetEpisodeState() {
        loadedEpisodeIds.clear()
        totalEpisodes = 0
        pageSize = 25
        nextPageToLoad = 1
        loadedPages.clear()
        _state.update {
            it.copy(
                episodes = emptyList(),
                hasMoreEpisodes = true,
                isLoadingMore = false,
                currentEpisode = null,
                currentM3u8 = null
            )
        }
    }

    private fun computeInitialPage(): Int {
        val totalPages = if (totalEpisodes > 0) ((totalEpisodes + pageSize - 1) / pageSize) else 1
        nextPageToLoad = if (isOnAir) totalPages else 1
        return nextPageToLoad
    }

    private fun computeHasMore(): Boolean {
        if (totalEpisodes <= 0) return false
        val totalPages = (totalEpisodes + pageSize - 1) / pageSize
        return if (isOnAir) {
            nextPageToLoad >= 1
        } else {
            nextPageToLoad <= totalPages
        }
    }

    private fun advancePage() {
        if (isOnAir) nextPageToLoad-- else nextPageToLoad++
    }

    private suspend fun loadEpisodesPage(seasonId: String, page: Int) {
        if (page < 1 || loadedPages.contains(page)) {
            val hasMore = computeHasMore()
            _state.update { it.copy(isLoadingMore = false, hasMoreEpisodes = hasMore) }
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
            id != null && !loadedEpisodeIds.contains(id)
        }
        loadedEpisodeIds.addAll(newEpisodes.mapNotNull { it.id })

        // For on-air: reverse each page so the combined list is latest -> oldest
        // For archived: keep ascending order (oldest -> newest)
        val pageEpisodes = if (isOnAir) newEpisodes.reversed() else newEpisodes
        val combinedEpisodes = _state.value.episodes + pageEpisodes

        loadedPages.add(page)
        advancePage()

        if (total > 0) totalEpisodes = total
        val hasMore = computeHasMore()
        _state.update {
            it.copy(
                episodes = combinedEpisodes,
                hasMoreEpisodes = hasMore,
                isLoadingMore = false,
                isLoading = false
            )
        }
        Log.d("Zee5Watch", "loadEpisodesPage page=$page new=${newEpisodes.size} totalLoaded=${combinedEpisodes.size} hasMore=$hasMore onAir=$isOnAir")
    }

    fun onEpisodeClick(episode: Zee5Item) {
        val epId = episode.id ?: return
        _state.update { it.copy(currentEpisode = episode, isPlaying = false) }
        playContent(epId)
    }

    private fun playContent(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(currentM3u8 = null, isFetchingStream = true, error = null) }
            var lastError: Throwable? = null
            repeat(3) { attempt ->
                try {
                    Log.d("Zee5Watch", "playContent id=$id attempt=${attempt + 1}")
                    val playback = apiService.getPlayback(id)
                    val url = playback.effectiveStreamUrl
                    Log.d("Zee5Watch", "playback url=${url != null} isDrm=${playback.isDrm}")
                    if (url != null) {
                        _state.update { it.copy(currentM3u8 = url, isFetchingStream = false, isPlaying = true) }
                        return@launch
                    } else {
                        lastError = Exception("No stream URL available")
                    }
                } catch (e: Exception) {
                    lastError = e
                    Log.e("Zee5Watch", "playContent failed id=$id attempt=${attempt + 1}", e)
                    if (attempt < 2) kotlinx.coroutines.delay(800)
                }
            }
            _state.update { it.copy(isFetchingStream = false, error = lastError?.message ?: "Failed to load stream after 3 retries") }
        }
    }

    fun onPlaybackReady() {
        _state.update { it.copy(error = null) }
    }
}
