package com.movie.app.best.ui.screens.player

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.movie.app.best.data.model.Episode
import com.movie.app.best.data.model.Season
import com.movie.app.best.data.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class VideoPlayerUiState(
    val playerUrl: String = "",
    val streamUrl: String = "",
    val title: String = "",
    val selectedSeason: Int = 1,
    val selectedEpisode: Int = 1,
    val seasons: List<Season> = emptyList(),
    val episodes: List<Episode> = emptyList(),
    val youtubeId: String = ""
)

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val libraryRepo = LibraryRepository(context)

    private val _uiState = MutableStateFlow(VideoPlayerUiState())
    val uiState: StateFlow<VideoPlayerUiState> = _uiState.asStateFlow()

    init {
        savedStateHandle.get<String>("playerUrl")?.let { url ->
            if (url.isNotEmpty()) {
                _uiState.update { it.copy(playerUrl = url) }
            }
        }
        savedStateHandle.get<String>("streamUrl")?.let { url ->
            if (url.isNotEmpty()) {
                _uiState.update { it.copy(streamUrl = url) }
            }
        }
        savedStateHandle.get<String>("title")?.let { title ->
            _uiState.update { it.copy(title = title) }
        }
        savedStateHandle.get<String>("youtubeId")?.let { ytId ->
            if (ytId.isNotEmpty()) {
                _uiState.update { it.copy(youtubeId = ytId) }
            }
        }
    }

    fun setSeasons(seasons: List<Season>) {
        _uiState.update { it.copy(seasons = seasons) }
    }

    fun setEpisodes(episodes: List<Episode>) {
        _uiState.update { it.copy(episodes = episodes) }
    }

    fun updateSeason(seasonNumber: Int) {
        _uiState.update {
            it.copy(
                selectedSeason = seasonNumber,
                selectedEpisode = 1
            )
        }
    }

    fun updateEpisode(episodeNumber: Int) {
        _uiState.update { it.copy(selectedEpisode = episodeNumber) }
    }

    fun updatePlayerUrl(url: String) {
        _uiState.update { it.copy(playerUrl = url) }
    }

    fun getDefaultPlayer(): Int = libraryRepo.getDefaultPlayer()
}
