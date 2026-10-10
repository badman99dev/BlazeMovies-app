package com.movie.app.best.ui.screens.sports

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.PlaybackKind
import com.movie.app.best.data.model.PlaybackOption
import com.movie.app.best.data.model.SportEvent
import com.movie.app.best.data.model.SportStream
import com.movie.app.best.data.repository.CricketScoreRepository
import com.movie.app.best.data.repository.CricketScoreUiData
import com.movie.app.best.data.repository.EventWatchResult
import com.movie.app.best.data.repository.SportsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SportsWatchUiState(
    val isLoading: Boolean = true,
    val isNotFound: Boolean = false,
    val error: String? = null,
    val slug: String = "",
    val event: SportEvent? = null,
    val streams: List<SportStream> = emptyList(),
    val playbackOptions: List<PlaybackOption> = emptyList(),
    val selectedOptionId: String? = null,
    val currentStream: SportStream? = null,
    val isSwitchingServer: Boolean = false,
    val playToken: Long = 0,
    val timeTick: Long = System.currentTimeMillis(),
    val isCricket: Boolean = false,
    val cricketMatchId: String? = null,
    val isCricketLoading: Boolean = false,
    val cricketScore: CricketScoreUiData? = null,
    val syncWithStream: Boolean = true
) {
    val displayTitle: String
        get() {
            val teamA = event?.eventInfo?.teamA?.trim().orEmpty()
            val teamB = event?.eventInfo?.teamB?.trim().orEmpty()
            val eventTitle = event?.title?.trim().orEmpty()
            val eventName = event?.eventInfo?.eventName?.trim().orEmpty()
            return when {
                teamA.isNotBlank() && teamB.isNotBlank() -> "$teamA vs $teamB"
                teamA.isNotBlank() -> teamA
                teamB.isNotBlank() -> teamB
                eventTitle.isNotBlank() -> eventTitle
                eventName.isNotBlank() -> eventName
                else -> ""
            }
        }

    val isUpcoming: Boolean get() = event?.isUpcoming == true
    val isLive: Boolean get() = event?.isLive == true
}

@HiltViewModel
class SportsWatchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SportsRepository,
    private val cricketRepository: CricketScoreRepository
) : ViewModel() {

    val slug: String = savedStateHandle.get<String>("slug") ?: ""

    private val _uiState = MutableStateFlow(SportsWatchUiState(slug = slug))
    val uiState: StateFlow<SportsWatchUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null
    private var cricketPollingJob: Job? = null

    init {
        loadMatchAndStreams()
        startTicker()
    }

    private fun checkIsCricket(event: SportEvent?): Boolean {
        if (event == null) return false
        val cat = event.cat?.lowercase().orEmpty()
        val eventCat = event.eventInfo?.eventCat?.lowercase().orEmpty()
        val eventName = event.eventInfo?.eventName?.lowercase().orEmpty()
        val title = event.title.lowercase()
        return cat.contains("cricket") ||
                eventCat.contains("cricket") ||
                eventName.contains("cricket") ||
                title.contains("cricket") ||
                cat == "2" || eventCat == "2"
    }

    private fun buildOptions(streams: List<SportStream>): List<PlaybackOption> =
        streams.mapIndexed { idx, st ->
            val label = st.title?.ifBlank { "Server ${idx + 1}" } ?: "Server ${idx + 1}"
            PlaybackOption(
                id = "sport_server_$idx",
                label = label,
                kind = PlaybackKind.NATIVE,
                url = st.url,
                playbackType = if (st.isDash) "dash" else "hls",
                languages = listOf(if (st.isDash) "DASH" else "HLS")
            )
        }

    private fun startTicker() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            _uiState.update { it.copy(timeTick = System.currentTimeMillis()) }
            while (isActive) {
                delay(1000)
                val now = System.currentTimeMillis()
                val s = _uiState.value
                val event = s.event
                if (event != null && s.currentStream == null && s.error == null && event.isLive && s.streams.isNotEmpty()) {
                    val options = buildOptions(s.streams)
                    val firstOpt = options.firstOrNull()
                    val firstStream = s.streams.firstOrNull()
                    _uiState.update {
                        it.copy(
                            timeTick = now,
                            playbackOptions = options,
                            selectedOptionId = firstOpt?.id,
                            currentStream = firstStream,
                            isSwitchingServer = false,
                            playToken = it.playToken + 1
                        )
                    }
                } else {
                    _uiState.update { it.copy(timeTick = now) }
                }
            }
        }
    }

    fun loadMatchAndStreams() {
        if (slug.isBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isNotFound = true,
                    error = "Invalid sports event link"
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    isNotFound = false,
                    error = null
                )
            }

            when (val result = repository.getWatchEvent(slug)) {
                is EventWatchResult.NotFound -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isNotFound = true,
                            error = "This event is not available or has already ended."
                        )
                    }
                }
                is EventWatchResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
                is EventWatchResult.Success -> {
                    val event = result.event
                    val streamList = result.streams
                    val isLive = event.isLive
                    val isCricketEvent = checkIsCricket(event)
                    val options = if (isLive && streamList.isNotEmpty()) {
                        buildOptions(streamList)
                    } else {
                        emptyList()
                    }
                    val firstOpt = options.firstOrNull()
                    val firstStream = if (isLive) streamList.firstOrNull() else null

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            event = event,
                            isCricket = isCricketEvent,
                            streams = streamList,
                            playbackOptions = options,
                            selectedOptionId = firstOpt?.id,
                            currentStream = firstStream,
                            isSwitchingServer = false,
                            error = if (isLive && streamList.isEmpty()) "No stream servers available at the moment." else null
                        )
                    }

                    if (isCricketEvent) {
                        startCricketScoreTracking(event)
                    }
                }
            }
        }
    }

    private fun startCricketScoreTracking(event: SportEvent) {
        cricketPollingJob?.cancel()
        cricketPollingJob = viewModelScope.launch {
            _uiState.update { it.copy(isCricketLoading = true) }

            val teamA = event.eventInfo?.teamA?.trim().orEmpty()
            val teamB = event.eventInfo?.teamB?.trim().orEmpty()
            val seriesName = event.eventInfo?.eventName?.trim()?.ifBlank { event.title.trim() } ?: event.title.trim()
            val startTime = event.eventInfo?.startTime?.trim().orEmpty()

            val matchId = cricketRepository.resolveMatchId(
                teamA = teamA,
                teamB = teamB,
                seriesName = seriesName,
                startTime = startTime
            )
            if (matchId.isNullOrBlank()) {
                _uiState.update { it.copy(isCricketLoading = false) }
                return@launch
            }

            _uiState.update { it.copy(cricketMatchId = matchId) }

            while (isActive) {
                val sync = _uiState.value.syncWithStream
                val scoreData = cricketRepository.fetchMatchDetail(matchId, syncWithStream = sync)
                if (scoreData != null) {
                    _uiState.update {
                        it.copy(
                            cricketScore = scoreData,
                            isCricketLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isCricketLoading = false) }
                }

                // Poll every 10s when syncWithStream is on (to build buffer), or 5s when real-time
                val interval = if (sync) 10_000L else 5_000L
                delay(interval)
            }
        }
    }

    fun toggleStreamSync(enabled: Boolean) {
        _uiState.update { it.copy(syncWithStream = enabled) }
        val matchId = _uiState.value.cricketMatchId ?: return
        viewModelScope.launch {
            val scoreData = cricketRepository.fetchMatchDetail(matchId, syncWithStream = enabled)
            if (scoreData != null) {
                _uiState.update { it.copy(cricketScore = scoreData) }
            }
        }
    }

    fun selectServer(optionId: String) {
        val s = _uiState.value
        val idx = s.playbackOptions.indexOfFirst { it.id == optionId }
        if (idx >= 0 && idx < s.streams.size) {
            val selectedStream = s.streams[idx]
            _uiState.update {
                it.copy(
                    selectedOptionId = optionId,
                    currentStream = selectedStream,
                    isSwitchingServer = false,
                    playToken = it.playToken + 1
                )
            }
        }
    }

    private fun advanceFrom(failedId: String?): Boolean {
        val s = _uiState.value
        val idx = failedId?.let { id -> s.playbackOptions.indexOfFirst { it.id == id } } ?: -1
        val nextIdx = idx + 1
        val nextStream = s.streams.getOrNull(nextIdx)
        if (nextStream == null) {
            _uiState.update {
                it.copy(
                    currentStream = null,
                    isSwitchingServer = false,
                    error = "All stream servers failed. Please try again later."
                )
            }
            return false
        }
        val nextOption = s.playbackOptions.getOrNull(nextIdx)
        _uiState.update {
            it.copy(
                selectedOptionId = nextOption?.id,
                currentStream = nextStream,
                isSwitchingServer = true,
                error = null,
                playToken = it.playToken + 1
            )
        }
        return true
    }

    fun onPlaybackError() {
        advanceFrom(_uiState.value.selectedOptionId)
    }

    fun onPlaybackReady() {
        if (_uiState.value.isSwitchingServer) {
            _uiState.update { it.copy(isSwitchingServer = false) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
        cricketPollingJob?.cancel()
        cricketRepository.resetBuffer()
    }
}
