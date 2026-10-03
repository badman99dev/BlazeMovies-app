package com.movie.app.best.ui.screens.sports

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.PlaybackKind
import com.movie.app.best.data.model.PlaybackOption
import com.movie.app.best.data.model.SportStream
import com.movie.app.best.data.repository.SportsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SportsWatchUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val slug: String = "",
    val title: String = "",
    val eventCat: String = "",
    val eventName: String = "",
    val teamA: String = "",
    val teamB: String = "",
    val teamAFlag: String = "",
    val teamBFlag: String = "",
    val startTime: String = "",
    val isLive: Boolean = false,
    val streams: List<SportStream> = emptyList(),
    val playbackOptions: List<PlaybackOption> = emptyList(),
    val selectedOptionId: String? = null,
    val currentStream: SportStream? = null
)

@HiltViewModel
class SportsWatchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SportsRepository
) : ViewModel() {

    private val slug: String = savedStateHandle.get<String>("slug") ?: ""
    private val title: String = savedStateHandle.get<String>("title") ?: ""
    private val eventCat: String = savedStateHandle.get<String>("eventCat") ?: ""
    private val eventName: String = savedStateHandle.get<String>("eventName") ?: ""
    private val teamA: String = savedStateHandle.get<String>("teamA") ?: ""
    private val teamB: String = savedStateHandle.get<String>("teamB") ?: ""
    private val teamAFlag: String = savedStateHandle.get<String>("teamAFlag") ?: ""
    private val teamBFlag: String = savedStateHandle.get<String>("teamBFlag") ?: ""
    private val startTime: String = savedStateHandle.get<String>("startTime") ?: ""
    private val isLive: Boolean = savedStateHandle.get<Boolean>("isLive") ?: false

    private val _uiState = MutableStateFlow(
        SportsWatchUiState(
            slug = slug,
            title = title,
            eventCat = eventCat,
            eventName = eventName,
            teamA = teamA,
            teamB = teamB,
            teamAFlag = teamAFlag,
            teamBFlag = teamBFlag,
            startTime = startTime,
            isLive = isLive
        )
    )
    val uiState: StateFlow<SportsWatchUiState> = _uiState.asStateFlow()

    init {
        loadStreams()
    }

    fun loadStreams() {
        if (slug.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "Invalid event slug") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.getEventStreams(slug)
            result.fold(
                onSuccess = { streamList ->
                    if (streamList.isEmpty()) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                streams = emptyList(),
                                playbackOptions = emptyList(),
                                error = "No streaming servers available for this match right now."
                            )
                        }
                    } else {
                        val options = streamList.mapIndexed { idx, s ->
                            val label = s.title?.ifBlank { "Server ${idx + 1}" } ?: "Server ${idx + 1}"
                            PlaybackOption(
                                id = "sport_server_$idx",
                                label = label,
                                kind = PlaybackKind.STREAM,
                                url = s.url,
                                isHls = s.isHls,
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
                            error = err.message ?: "Failed to fetch stream servers. Tap retry."
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
}
