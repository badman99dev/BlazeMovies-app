package com.movie.app.best.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.BuildConfig
import com.movie.app.best.data.repository.MeiliSearchRepository
import com.movie.app.best.data.repository.MovieRepository
import com.movie.app.best.data.repository.PrefetchCache
import com.movie.app.best.data.repository.Zee5TokenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val meiliRepository: MeiliSearchRepository,
    private val zee5TokenRepository: Zee5TokenRepository
) : ViewModel() {

    fun prefetch() {
        viewModelScope.launch {
            try { meiliRepository.pingAndPrefetchKey() } catch (_: Exception) {}
        }
        viewModelScope.launch {
            try { zee5TokenRepository.prefetchTokens() } catch (_: Exception) {}
        }

        viewModelScope.launch {
            try {
                repository.getSlider().collect { result ->
                    if (result is com.movie.app.best.data.model.Resource.Success) {
                        PrefetchCache.slider = (result.data?.movies ?: emptyList()).filter { it.hasStream }
                    }
                }
            } catch (_: Exception) {}
        }

        viewModelScope.launch {
            try {
                repository.getTrending(0, 20).collect { result ->
                    if (result is com.movie.app.best.data.model.Resource.Success) {
                        PrefetchCache.trending = result.data?.items ?: emptyList()
                    }
                }
            } catch (_: Exception) {}
        }

        viewModelScope.launch {
            try {
                repository.getLatestUploads(0, 45).collect { result ->
                    if (result is com.movie.app.best.data.model.Resource.Success) {
                        PrefetchCache.latestUploads = result.data?.items ?: emptyList()
                    }
                }
            } catch (_: Exception) {}
        }

        viewModelScope.launch {
            try {
                repository.getNotification().collect { result ->
                    if (result is com.movie.app.best.data.model.Resource.Success) {
                        PrefetchCache.notification = result.data
                    }
                }
            } catch (_: Exception) {}
        }

        viewModelScope.launch {
            try {
                repository.getBroadcasts().collect { result ->
                    if (result is com.movie.app.best.data.model.Resource.Success) {
                        PrefetchCache.liveChannels = result.data ?: emptyList()
                    }
                }
            } catch (_: Exception) {}
        }

        viewModelScope.launch {
            try {
                val result = repository.checkForUpdate(BuildConfig.VERSION_CODE)
                when (result) {
                    is com.movie.app.best.data.model.Resource.Success -> {
                        PrefetchCache.updateResponse = result.data
                    }
                    is com.movie.app.best.data.model.Resource.Error -> {
                        android.util.Log.e("SplashVM", "Update check failed: ${result.error}")
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                android.util.Log.e("SplashVM", "Update check exception: ${e.message}", e)
            }
        }
    }
}
