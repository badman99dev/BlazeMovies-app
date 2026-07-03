package com.movie.app.best.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.ImdbCertificate
import com.movie.app.best.data.model.ImdbCredit
import com.movie.app.best.data.model.ImdbTitleDetails
import com.movie.app.best.data.remote.ImdbApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrailerViewModel @Inject constructor(
    private val imdbApi: ImdbApiService
) : ViewModel() {

    private val _state = MutableStateFlow<TrailerUiState>(TrailerUiState.Idle)
    val state: StateFlow<TrailerUiState> = _state.asStateFlow()

    fun loadData(imdbId: String) {
        if (imdbId.isBlank()) {
            _state.value = TrailerUiState.Error("No IMDb ID")
            return
        }
        _state.value = TrailerUiState.Loading
        viewModelScope.launch {
            try {
                val title = safeApiCall { imdbApi.getTitleDetails(imdbId) }
                val credits = safeApiCall { imdbApi.getCredits(imdbId) }
                val certs = safeApiCall { imdbApi.getCertificates(imdbId) }

                if (title == null) {
                    _state.value = TrailerUiState.Error("Failed to load details")
                    return@launch
                }

                val usCert = certs?.certificates?.firstOrNull { it.country?.code == "US" }
                    ?: certs?.certificates?.firstOrNull()
                val cast = credits?.credits?.filter { it.isActor }?.take(15) ?: emptyList()

                _state.value = TrailerUiState.Success(
                    title = title,
                    cast = cast,
                    certificate = usCert
                )
            } catch (e: Exception) {
                _state.value = TrailerUiState.Error(e.message ?: "Failed to load")
            }
        }
    }

    private suspend inline fun <T> safeApiCall(block: suspend () -> T): T? {
        return try { block() } catch (e: Exception) { null }
    }
}

sealed class TrailerUiState {
    data object Idle : TrailerUiState()
    data object Loading : TrailerUiState()
    data class Success(
        val title: ImdbTitleDetails,
        val cast: List<ImdbCredit>,
        val certificate: ImdbCertificate?
    ) : TrailerUiState()
    data class Error(val message: String) : TrailerUiState()
}
