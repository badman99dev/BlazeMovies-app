package com.movie.app.best.ui.screens.sports

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.PlaybackKind
import com.movie.app.best.data.model.PlaybackOption
import com.movie.app.best.data.model.SportEvent
import com.movie.app.best.data.model.SportStream
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
    val timeTick: Long = System.currentTimeMillis()
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
    private val repository: SportsRepository
) : ViewModel() {

    val slug: String = savedStateHandle.get<String>("slug") ?: ""

    private val _uiState = MutableStateFlow(SportsWatchUiState(slug = slug))
    val uiState: StateFlow<SportsWatchUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    init {
        loadMatchAndStreams()
        startTicker()
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

                if (event != null && s.currentStream == null && event.isLive && s.streams.isNotEmpty()) {
                    // Match transitioned from UPCOMING to LIVE in real time!
                    val options = s.streams.mapIndexed { idx, st ->
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
                    val firstOpt = options.firstOrNull()
                    val firstStream = s.streams.firstOrNull()
                    _uiState.update {
                        it.copy(
                            timeTick = now,
                            playbackOptions = options,
                            selectedOptionId = firstOpt?.id,
                            currentStream = firstStream
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

            // Single unified call to /api/event?slug=... (contains both event details and streams)
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

                    val options = if (isLive && streamList.isNotEmpty()) {
                        streamList.mapIndexed { idx, s ->
                            val label = s.title?.ifBlank { "Server ${idx + 1}" } ?: "Server ${idx + 1}"
                            PlaybackOption(
                                id = "sport_server_$idx",
                                label = label,
                                kind = PlaybackKind.NATIVE,
                                url = s.url,
                                playbackType = if (s.isDash) "dash" else "hls",
                                languages = listOf(if (s.isDash) "DASH" else "HLS")
                            )
                        }
                    } else {
                        emptyList()
                    }

                    val firstOpt = options.firstOrNull()
                    val firstStream = if (isLive) streamList.firstOrNull() else null

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            event = event,
                            streams = streamList,
                            playbackOptions = options,
                            selectedOptionId = firstOpt?.id,
                            currentStream = firstStream,
                            error = if (isLive && streamList.isEmpty()) "No stream servers available at the moment." else null
                        )
                    }
                }
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
                    currentStream = selectedStream
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
