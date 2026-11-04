package com.movie.app.best.ui.screens.livetv

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.chat.LiveChatManager
import com.movie.app.best.data.chat.LiveChatState
import com.movie.app.best.data.model.LiveChannelDetail
import com.movie.app.best.data.model.LiveStreamSource
import com.movie.app.best.data.model.PlaybackKind
import com.movie.app.best.data.model.PlaybackOption
import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.model.UnifiedChannel
import com.movie.app.best.data.repository.AuthRepository
import com.movie.app.best.data.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LiveTvWatchUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val canRetry: Boolean = true,
    val channel: LiveChannelDetail? = null,
    val streams: List<LiveStreamSource> = emptyList(),
    val serverOptions: List<PlaybackOption> = emptyList(),
    val selectedOptionId: String? = null,
    val currentStream: LiveStreamSource? = null,
    val isSwitchingServer: Boolean = false,
    val playToken: Long = 0,
)

@HiltViewModel
class LiveTvWatchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MovieRepository,
    private val chatManager: LiveChatManager,
    private val authRepository: AuthRepository,
) : ViewModel() {

    /** Channel identity — the list only ships slug/name/logo; servers come from the detail call. */
    val slug: String = savedStateHandle.get<String>("slug") ?: ""
    val channelName: String = savedStateHandle.get<String>("name") ?: "Live TV"
    val logoUrl: String = savedStateHandle.get<String>("logo") ?: ""

    val displayName: String get() = channelName

    /** Backend-assigned room (same convention as sports_<slug>); local fallback if absent. */
    private var backendChatRid: String? = null

    val chatRoomId: String
        get() = backendChatRid?.takeIf { it.isNotBlank() }
            ?: ("livetv_" + (slug.ifBlank { channelName })
                .trim()
                .replace("/", "_")
                .replace(" ", "_"))

    val liveChatState: StateFlow<LiveChatState> = chatManager.chatState
    val floatingReactions = chatManager.floatingReactions

    private val _chatHandle = MutableStateFlow(authRepository.getHandle())
    val chatHandle: StateFlow<String?> = _chatHandle.asStateFlow()

    private val _similarChannels = MutableStateFlow<List<UnifiedChannel>>(emptyList())
    val similarChannels: StateFlow<List<UnifiedChannel>> = _similarChannels.asStateFlow()

    private val _uiState = MutableStateFlow(LiveTvWatchUiState())
    val uiState: StateFlow<LiveTvWatchUiState> = _uiState.asStateFlow()

    private var loadAttempts = 0

    init {
        viewModelScope.launch {
            authRepository.ensureHandle()?.let { _chatHandle.value = it }
        }
        loadChannel()
        loadSimilarChannels()
    }

    fun isUserLoggedIn(): Boolean = authRepository.isLoggedIn()

    fun getUserFirstName(): String? {
        val user = authRepository.getUser() ?: return null
        return user.firstName?.trim()?.ifBlank { null }
            ?: user.username.split(" ").firstOrNull()?.trim()?.ifBlank { null }
    }

    fun connectChat() {
        chatManager.connectToRoom(chatRoomId)
    }

    fun disconnectChat() {
        chatManager.disconnect()
    }

    suspend fun sendChatMessage(text: String): Boolean = chatManager.sendMessage(text)

    fun sendChatReaction(id: String, emoji: String) {
        viewModelScope.launch {
            chatManager.sendReaction(id, emoji)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Channel + servers
    // ---------------------------------------------------------------------------------------------

    fun loadChannel() {
        if (slug.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "Channel not found", canRetry = false) }
            return
        }
        loadAttempts++
        _uiState.update { it.copy(isLoading = true, error = null, canRetry = loadAttempts < 2) }
        viewModelScope.launch {
            repository.getLiveChannelDetail(slug).collect { result ->
                when (result) {
                    is Resource.Loading -> Unit
                    is Resource.Success -> {
                        val detail = result.data
                        if (detail == null || detail.streams.isEmpty()) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "This channel has no working server right now.",
                                    canRetry = loadAttempts < 2,
                                )
                            }
                            return@collect
                        }
                        backendChatRid = detail.chatRid
                        val options = buildOptions(detail.streams)
                        val firstOption = options.firstOrNull()
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = null,
                                canRetry = true,
                                channel = detail,
                                streams = detail.streams,
                                serverOptions = options,
                                selectedOptionId = firstOption?.id,
                                currentStream = detail.streams.firstOrNull(),
                                isSwitchingServer = false,
                                playToken = it.playToken + 1,
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = if (loadAttempts >= 2) {
                                    "Unable to load this channel. Please try again later."
                                } else {
                                    "Couldn't reach the server."
                                },
                                canRetry = loadAttempts < 2,
                            )
                        }
                    }
                }
            }
        }
    }

    /** Retry once from the UI; a second failure stays on the error screen. */
    fun retry() {
        loadChannel()
    }

    private fun buildOptions(streams: List<LiveStreamSource>): List<PlaybackOption> =
        streams.mapIndexed { idx, stream ->
            val label = stream.label.ifBlank {
                stream.host.ifBlank { "Server ${idx + 1}" }
            }
            // Quality + language stay as-is; the shared server sheet paints them red,
            // same style as the movie server rows.
            val highlight = buildList {
                if (stream.quality.isNotBlank()) add(stream.quality)
                if (stream.language.isNotBlank() && !label.contains(stream.language, ignoreCase = true)) {
                    add(stream.language)
                }
            }
            PlaybackOption(
                id = "livetv_server_$idx",
                label = label,
                kind = PlaybackKind.NATIVE,
                url = stream.url,
                languages = highlight,
                playbackType = if (stream.url.contains(".mpd", ignoreCase = true)) "dash" else "hls",
            )
        }

    fun selectServer(optionId: String) {
        val s = _uiState.value
        val idx = s.serverOptions.indexOfFirst { it.id == optionId }
        val stream = s.streams.getOrNull(idx) ?: return
        _uiState.update {
            it.copy(
                selectedOptionId = optionId,
                currentStream = stream,
                isSwitchingServer = false,
                playToken = it.playToken + 1,
            )
        }
    }

    /** Player failed on [failedOptionId] → move to the next server (user notices nothing). */
    fun advanceFrom(failedOptionId: String?) {
        val s = _uiState.value
        val idx = failedOptionId?.let { id -> s.serverOptions.indexOfFirst { it.id == id } } ?: -1
        val nextIdx = idx + 1
        val nextStream = s.streams.getOrNull(nextIdx)
        if (nextStream == null) {
            _uiState.update {
                it.copy(
                    currentStream = null,
                    isSwitchingServer = false,
                    error = "All servers failed. Please try again later.",
                    canRetry = true,
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                selectedOptionId = s.serverOptions.getOrNull(nextIdx)?.id,
                currentStream = nextStream,
                isSwitchingServer = true,
                error = null,
                playToken = it.playToken + 1,
            )
        }
    }

    fun switchServer(optionId: String) = selectServer(optionId)

    private fun loadSimilarChannels() {
        viewModelScope.launch {
            try {
                repository.getTvStreamsUnified(null, 0, 20).collect { res ->
                    if (res is Resource.Success) {
                        _similarChannels.value = (res.data ?: emptyList())
                            .filter { it.slug != slug }
                            .take(12)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }
}