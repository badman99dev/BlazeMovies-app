package com.movie.app.best.ui.screens.livetv

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.chat.LiveChatManager
import com.movie.app.best.data.chat.LiveChatState
import com.movie.app.best.data.model.UnifiedChannel
import com.movie.app.best.data.repository.AuthRepository
import com.movie.app.best.data.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LiveTvWatchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MovieRepository,
    private val chatManager: LiveChatManager,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val channelId: String = savedStateHandle.get<String>("channelId") ?: ""
    val channelName: String = savedStateHandle.get<String>("name") ?: "Live TV"
    val streamUrl: String = savedStateHandle.get<String>("url") ?: ""
    val category: String = savedStateHandle.get<String>("category") ?: ""
    val logoUrl: String = savedStateHandle.get<String>("logo") ?: ""

    /** One chat room per channel, same convention as sports_<slug>. */
    val chatRoomId: String = "livetv_" + (channelId.ifBlank { channelName })
        .trim()
        .replace("/", "_")
        .replace(" ", "_")

    val liveChatState: StateFlow<LiveChatState> = chatManager.chatState
    val floatingReactions = chatManager.floatingReactions

    private val _chatHandle = MutableStateFlow(authRepository.getHandle())
    val chatHandle: StateFlow<String?> = _chatHandle.asStateFlow()

    private val _similarChannels = MutableStateFlow<List<UnifiedChannel>>(emptyList())
    val similarChannels: StateFlow<List<UnifiedChannel>> = _similarChannels.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.ensureHandle()?.let { _chatHandle.value = it }
        }
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

    private fun loadSimilarChannels() {
        viewModelScope.launch {
            try {
                repository.getTvStreamsUnified(category.ifBlank { null }, 0, 20).collect { res ->
                    if (res is Resource.Success) {
                        _similarChannels.value = (res.data ?: emptyList())
                            .filter { it.id != channelId && it.streamUrl != streamUrl }
                            .take(12)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }
}
