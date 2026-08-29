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