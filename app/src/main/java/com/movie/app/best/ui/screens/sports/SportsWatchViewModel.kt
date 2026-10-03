package com.movie.app.best.ui.screens.sports

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.PlaybackKind
import com.movie.app.best.data.model.PlaybackOption
import com.movie.app.best.data.model.SportEvent
import com.movie.app.best.data.model.SportStream
import com.movie.app.best.data.repository.SlugDetailsResult
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
    val timeTick: Long = 0L
) {
    val displayTitle: String
        get() {
            val teamA = event?.eventInfo?.teamA ?: ""
            val teamB = event?.eventInfo?.teamB ?: ""
            return if (teamA.isNotBlank() && teamB.isNotBlank()) {
                "$teamA vs $teamB"
            } else {
                event?.title?.ifBlank { "Live Sports" } ?: "Live Sports"
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
            while (isActive) {
                delay(1000)
                _uiState.update { it.copy(timeTick = System.currentTimeMillis()) }
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

            // 1. Fetch match details from /api/slug-details?slug=...
            val detailsResult = repository.getSlugDetails(slug)

            when (detailsResult) {
                is SlugDetailsResult.NotFound -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isNotFound = true,
                            error = "This event is not available or has already ended."
                        )
                    }
                    return@launch
                }
                is SlugDetailsResult.Error -> {
                    // Try fallback: streams can still be attempted
                }
                is SlugDetailsResult.Success -> {
                    _uiState.update { it.copy(event = detailsResult.event) }
                }
            }

            // 2. Fetch streams from /api/event?slug=...
            val streamsResult = repository.getEventStreams(slug)
            streamsResult.fold(
                onSuccess = { streamList ->
                    if (streamList.isEmpty()) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                streams = emptyList(),
                                playbackOptions = emptyList(),
                                error = if (_uiState.value.isUpcoming) null else "No stream servers available at the moment."
                            )
                        }
                    } else {
                        val options = streamList.mapIndexed { idx, s ->
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
                        val firstOpt = options.firstOrNull()
                        val firstStream = streamList.firstOrNull()

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                streams = streamList,
                                playbackOptions = options,
                                selectedOptionId = firstOpt?.id,
                                currentStream = firstStream
                            )
                        }
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = err.message ?: "Failed to load match streams. Tap retry."
                        )
                    }
                }
            )
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
