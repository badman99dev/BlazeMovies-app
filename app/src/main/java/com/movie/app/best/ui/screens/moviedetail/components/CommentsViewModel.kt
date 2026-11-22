package com.movie.app.best.ui.screens.moviedetail.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.CommentApi
import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class CommentsViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    data class State(
        val comments: List<CommentApi> = emptyList(),
        val total: Int = 0,
        val hasMore: Boolean = false,
        val isLoading: Boolean = false,
        val isLoadingMore: Boolean = false,
        val isPosting: Boolean = false,
        val error: String? = null,
        val postError: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var movieId = 0
    private var slug = ""
    private var offset = 0
    private var started = false

    fun open(movieId: Int, slug: String) {
        if (started && this.movieId == movieId) return
        started = true
        this.movieId = movieId
        this.slug = slug
        offset = 0
        _state.value = State(isLoading = true)
        load(reset = true)
    }

    fun loadMore() {
        val s = _state.value
        if (s.isLoading || s.isLoadingMore || s.isPosting || !s.hasMore) return
        load(reset = false)
    }

    fun retry() {
        if (movieId <= 0) return
        val reset = _state.value.comments.isEmpty()
        _state.update { it.copy(error = null) }
        load(reset = reset)
    }

    fun clearPostError() {
        _state.update { it.copy(postError = null) }
    }

    fun post(msg: String) {
        val text = msg.trim()
        val current = _state.value
        if (text.isEmpty() || current.isPosting || movieId <= 0) return
        viewModelScope.launch {
            _state.update { it.copy(isPosting = true, postError = null) }
            val authHeader = try {
                val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val token = user?.getIdToken(false)?.await()?.token
                if (token.isNullOrBlank()) "" else "Bearer $token"
            } catch (_: Exception) {
                ""
            }
            when (val result = repository.postComment(authHeader, movieId, text)) {
                is Resource.Success -> {
                    val posted = result.data
                    _state.update { prev ->
                        prev.copy(
                            comments = if (posted != null) listOf(posted) + prev.comments else prev.comments,
                            total = if (posted != null) prev.total + 1 else prev.total,
                            isPosting = false,
                            postError = null
                        )
                    }
                }
                is Resource.Error -> _state.update {
                    it.copy(isPosting = false, postError = result.error ?: "Could not post your comment")
                }
                is Resource.Loading -> Unit
            }
        }
    }

    private fun load(reset: Boolean) {
        viewModelScope.launch {
            _state.update {
                it.copy(isLoading = reset, isLoadingMore = !reset, error = null)
            }
            val start = if (reset) 0 else offset
            when (val result = repository.getComments(movieId, slug, start, PAGE_SIZE)) {
                is Resource.Success -> {
                    val page = result.data
                    if (page != null) {
                        offset = start + page.comments.size
                        _state.update { prev ->
                            val merged = if (reset) page.comments else prev.comments + page.comments
                            prev.copy(
                                comments = merged,
                                total = page.total,
                                hasMore = page.hasMore,
                                isLoading = false,
                                isLoadingMore = false,
                                error = null
                            )
                        }
                    } else {
                        _state.update { it.copy(isLoading = false, isLoadingMore = false) }
                    }
                }
                is Resource.Error -> _state.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = result.error ?: "Could not load comments"
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    companion object {
        const val PAGE_SIZE = 20
    }
}