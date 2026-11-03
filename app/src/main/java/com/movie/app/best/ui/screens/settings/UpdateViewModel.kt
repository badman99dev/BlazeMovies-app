package com.movie.app.best.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.UpdateResponse
import com.movie.app.best.data.repository.ApkUpdateRepository
import com.movie.app.best.data.repository.ApkUpdateState
import com.movie.app.best.data.update.UpdateManager
import com.movie.app.best.data.update.UpdateState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val apkRepo: ApkUpdateRepository,
    private val updateManager: UpdateManager
) : ViewModel() {

    /** App-wide availability (boot / resume / manual sab isi ko update karte hain). */
    val availability: StateFlow<UpdateState> = updateManager.state

    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    private var currentKetchId: Int = -1

    fun checkForUpdate() {
        _state.value = UpdateUiState.Checking
        viewModelScope.launch {
            val data = updateManager.check("settings", force = true)
            _state.value = when {
                data == null -> UpdateUiState.Error("Update check failed")
                data.updateAvailable || data.forceUpdate -> UpdateUiState.UpdateAvailable(data)
                else -> UpdateUiState.UpToDate
            }
        }
    }

    /** Auto-prompt throttle manager me hai — UI sirf mark karta hai. */
    fun markPrompted(versionCode: Int) = updateManager.markPrompted(versionCode)

    fun checkIfStale() = updateManager.checkIfStale("resume")

    fun resetAvailability() = updateManager.resetAvailability()

    fun startDownload(context: Context, url: String) {
        apkRepo.cleanupOldApk(context)
        currentKetchId = apkRepo.downloadApk(context, url)
        _state.value = UpdateUiState.Downloading(progress = 0)

        viewModelScope.launch {
            apkRepo.observeUpdateProgress(context, currentKetchId).collect { state ->
                when (state) {
                    is ApkUpdateState.Downloading -> {
                        _state.value = UpdateUiState.Downloading(state.progress)
                    }
                    is ApkUpdateState.Completed -> {
                        _state.value = UpdateUiState.DownloadComplete(state.file)
                    }
                    is ApkUpdateState.Error -> {
                        _state.value = UpdateUiState.Error(state.message)
                    }
                    is ApkUpdateState.Cancelled -> {
                        _state.value = UpdateUiState.Error("Download cancelled")
                    }
                    else -> {}
                }
            }
        }
    }

    fun installApk(context: Context, file: java.io.File) {
        apkRepo.installApk(context, file)
    }

    fun resetState() {
        _state.value = UpdateUiState.Idle
    }
}

sealed class UpdateUiState {
    data object Idle : UpdateUiState()
    data object Checking : UpdateUiState()
    data object UpToDate : UpdateUiState()
    data class UpdateAvailable(val data: UpdateResponse) : UpdateUiState()
    data class Downloading(val progress: Int) : UpdateUiState()
    data class DownloadComplete(val file: java.io.File) : UpdateUiState()
    data class Error(val message: String) : UpdateUiState()
}
