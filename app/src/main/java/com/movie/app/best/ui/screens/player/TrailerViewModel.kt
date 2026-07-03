package com.movie.app.best.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.ImdbCertificate
import com.movie.app.best.data.model.ImdbCredit
import com.movie.app.best.data.model.ImdbTitleDetails
import com.movie.app.best.data.remote.ImdbApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
                val deferredTitle = async { imdbApi.getTitleDetails(imdbId) }
                val deferredCredits = async { imdbApi.getCredits(imdbId) }
                val deferredCerts = async { imdbApi.getCertificates(imdbId) }

                val title = deferredTitle.await()
                val credits = deferredCredits.await()
                val certs = deferredCerts.await()

                val usCert = certs.certificates.firstOrNull { it.country?.code == "US" }
                    ?: certs.certificates.firstOrNull()
                val cast = credits.credits.filter { it.isActor }.take(15)

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
