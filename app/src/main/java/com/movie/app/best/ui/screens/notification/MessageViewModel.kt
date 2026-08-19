package com.movie.app.best.ui.screens.notification

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.repository.FirebaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MessageUiState(
    val markdown: String? = null,
    val title: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessageUiState())
    val uiState: StateFlow<MessageUiState> = _uiState.asStateFlow()

    init {
        val docId = savedStateHandle.get<String>("docId")
        val md = savedStateHandle.get<String>("md")
        val title = savedStateHandle.get<String>("t") ?: "Message"

        _uiState.update {
            it.copy(
                markdown = if (!md.isNullOrBlank()) md else null,
                title = title,
                isLoading = md.isNullOrBlank()
            )
        }

        // docId diya hai (broadcast/targeted ref) aur markdown sath nahi -> Firestore se load
        if (!docId.isNullOrBlank() && md.isNullOrBlank()) {
            loadFromFirestore(docId)
        }
    }

    private fun loadFromFirestore(docId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val text = firebaseRepository.getNotificationMessage(docId)
            _uiState.update {
                if (text != null) {
                    it.copy(markdown = text, isLoading = false)
                } else {
                    it.copy(isLoading = false, error = "Message not found (expired or still syncing).")
                }
            }
        }
    }
}