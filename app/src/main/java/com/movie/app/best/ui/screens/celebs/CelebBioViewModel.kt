package com.movie.app.best.ui.screens.celebs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.ImdbRelationship
import com.movie.app.best.data.remote.ImdbApiService
import com.movie.app.best.data.remote.ImdxApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CelebBioUiState(
    val nameId: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val photoWidth: Int = 0,
    val photoHeight: Int = 0,
    val age: Int = 0,
    val birthDate: String = "",
    val birthLocation: String = "",
    val heightText: String = "",
    val biography: String = "",
    val relationships: List<ImdbRelationship> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CelebBioViewModel @Inject constructor(
    private val imdbApi: ImdbApiService,
    private val imdxApi: ImdxApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CelebBioUiState())
    val uiState: StateFlow<CelebBioUiState> = _uiState.asStateFlow()

    private var nameId: String = ""
    private var configured = false

    fun configure(nameId: String) {
        if (nameId.isBlank()) return
        if (this.nameId == nameId && configured) return
        this.nameId = nameId
        configured = true
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(nameId = nameId, isLoading = true) }

            launch {
                try {
                    val details = imdbApi.getNameDetails(nameId)
                    _uiState.update {
                        it.copy(
                            displayName = details.displayName,
                            photoUrl = details.photoUrl,
                            photoWidth = details.photoWidth,
                            photoHeight = details.photoHeight,
                            age = details.age,
                            birthDate = details.birthDateText,
                            birthLocation = details.birthLocation,
                            heightText = details.heightText,
                            biography = details.biography,
                            error = null
                        )
                    }
                } catch (_: Exception) {}
            }

            launch {
                try {
                    val rel = imdxApi.getRelationships(nameId)
                    _uiState.update {
                        it.copy(
                            relationships = rel.relationships,
                            error = null
                        )
                    }
                } catch (_: Exception) {}
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
