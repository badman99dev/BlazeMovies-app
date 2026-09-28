package com.movie.app.best.ui.screens.library

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.movie.app.best.data.model.AppUser
import com.movie.app.best.data.model.BookmarkItem
import com.movie.app.best.data.model.FirebaseHistoryItem
import com.movie.app.best.data.model.LikeItem
import com.movie.app.best.data.repository.AuthRepository
import com.movie.app.best.data.settings.ModerationSettings
import com.movie.app.best.data.repository.FirebaseRepository
import com.movie.app.best.data.repository.LibraryRepository
import com.movie.app.best.data.repository.MyListRefreshState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val history: List<FirebaseHistoryItem> = emptyList(),
    val likedPlaylist: List<LikeItem> = emptyList(),
    val watchLaterPlaylist: List<BookmarkItem> = emptyList(),
    val user: AppUser? = null,
    val userTier: String = "normal_user",
    val isLoggedIn: Boolean = false,
    val isLoggingOut: Boolean = false,
    val isOnline: Boolean = true,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: LibraryRepository,
    private val firebaseRepository: FirebaseRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        // Immediate local cached user info (works offline)
        syncLocalAuthState()

        viewModelScope.launch {
            authRepository.authEvent.collect { event ->
                syncLocalAuthState()
                loadLibrary(showLoading = false)
            }
        }

        viewModelScope.launch {
            MyListRefreshState.isMyListRefreshed.collect { refreshed ->
                if (!refreshed) {
                    loadLibrary(showLoading = true)
                }
            }
        }
        val currentRefreshed = MyListRefreshState.isMyListRefreshed.value
        loadLibrary(showLoading = !currentRefreshed)
    }

    private fun syncLocalAuthState() {
        val cachedUser = authRepository.getUser()
        val token = authRepository.getToken()
        val fbUser = FirebaseAuth.getInstance().currentUser
        val loggedIn = token != null || fbUser != null
        _uiState.update {
            it.copy(
                user = cachedUser,
                isLoggedIn = loggedIn,
                userTier = cachedUser?.tier ?: "normal_user",
                isOnline = isOnline(context)
            )
        }
    }

    fun loadLibrary(showLoading: Boolean = false) {
        viewModelScope.launch {
            val online = isOnline(context)
            if (showLoading) {
                _uiState.update { it.copy(isLoading = true, isOnline = online) }
            } else {
                _uiState.update { it.copy(isOnline = online) }
            }

            syncLocalAuthState()

            if (online && _uiState.value.isLoggedIn) {
                try {
                    val profile = firebaseRepository.getOrCreateUserProfile()
                    if (profile != null) {
                        _uiState.update { it.copy(userTier = profile.tier) }
                    }
                } catch (_: Exception) {}
            }

            if (!online) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isOnline = false
                    )
                }
                return@launch
            }

            val isLoggedIn = FirebaseAuth.getInstance().currentUser != null || authRepository.getToken() != null
            if (isLoggedIn) {
                try {
                    val bookmarks = firebaseRepository.getBookmarks()
                    val history = firebaseRepository.getHistory()
                    val likes = firebaseRepository.getLikes()
                    _uiState.update {
                        it.copy(
                            history = applyModerationFilterHistory(history),
                            watchLaterPlaylist = applyModerationFilterBookmarks(bookmarks),
                            likedPlaylist = applyModerationFilterLikes(likes),
                            isOnline = true,
                            isLoading = false,
                            isRefreshing = false
                        )
                    }
                    MyListRefreshState.markRefreshed()
                } catch (_: Exception) {
                    _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
                }
            } else {
                try {
                    val history = repository.getHistory().map { h ->
                        FirebaseHistoryItem(slug = h.slug, title = h.title, posterUrl = h.posterUrl, isSeries = h.isSeries, watchedAt = h.timestamp)
                    }
                    val likes = repository.getPlaylist("liked").map { p ->
                        LikeItem(slug = p.slug, title = p.title, posterUrl = p.posterUrl, isSeries = p.isSeries)
                    }
                    val bookmarks = repository.getPlaylist("watch_later").map { p ->
                        BookmarkItem(slug = p.slug, title = p.title, posterUrl = p.posterUrl, isSeries = p.isSeries)
                    }
                    _uiState.update {
                        it.copy(
                            history = history,
                            likedPlaylist = likes,
                            watchLaterPlaylist = bookmarks,
                            isOnline = true,
                            isLoading = false,
                            isRefreshing = false
                        )
                    }
                    MyListRefreshState.markRefreshed()
                } catch (_: Exception) {
                    _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true) }
            try {
                authRepository.logout()
            } catch (_: Exception) {}
            _uiState.update {
                it.copy(
                    isLoggingOut = false,
                    user = null,
                    isLoggedIn = false,
                    history = emptyList(),
                    likedPlaylist = emptyList(),
                    watchLaterPlaylist = emptyList()
                )
            }
            loadLibrary(showLoading = false)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadLibrary(showLoading = false)
        }
    }

    fun removeFromHistory(slug: String) {
        viewModelScope.launch {
            val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
            if (isLoggedIn) {
                firebaseRepository.removeFromHistory(slug)
            } else {
                repository.removeFromHistory(slug)
            }
            loadLibrary()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
            if (isLoggedIn) {
                firebaseRepository.clearHistory()
            } else {
                repository.clearHistory()
            }
            loadLibrary()
        }
    }

    fun removeFromLiked(slug: String) {
        viewModelScope.launch {
            val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
            if (isLoggedIn) {
                firebaseRepository.removeLike(slug)
            } else {
                repository.removeFromPlaylist("liked", slug)
            }
            loadLibrary()
        }
    }

    fun removeFromWatchLater(slug: String) {
        viewModelScope.launch {
            val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
            if (isLoggedIn) {
                firebaseRepository.removeBookmark(slug)
            } else {
                repository.removeFromPlaylist("watch_later", slug)
            }
            loadLibrary()
        }
    }

    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun applyModerationFilterHistory(items: List<FirebaseHistoryItem>): List<FirebaseHistoryItem> {
        return items.filter { !ModerationSettings.shouldHide(context, it.contentModeration) }
    }

    private fun applyModerationFilterBookmarks(items: List<BookmarkItem>): List<BookmarkItem> {
        return items.filter { !ModerationSettings.shouldHide(context, it.contentModeration) }
    }

    private fun applyModerationFilterLikes(items: List<LikeItem>): List<LikeItem> {
        return items.filter { !ModerationSettings.shouldHide(context, it.contentModeration) }
    }
}
