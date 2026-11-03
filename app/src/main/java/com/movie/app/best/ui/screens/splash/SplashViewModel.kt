package com.movie.app.best.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.repository.MeiliSearchRepository
import com.movie.app.best.data.repository.Zee5TokenRepository
import com.movie.app.best.data.update.UpdateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val meiliRepository: MeiliSearchRepository,
    private val zee5TokenRepository: Zee5TokenRepository,
    private val updateManager: UpdateManager
) : ViewModel() {

    fun prefetch() {
        viewModelScope.launch {
            // Config-dependent prefetches wait for the first config (max ~0.9s).
            // Splash animation/UI keeps running; nothing is blocked on the main thread.
            try {
                com.movie.app.best.data.config.RemoteConfig.awaitReady(900L)
            } catch (_: Exception) {}
            launch { try { meiliRepository.pingAndPrefetchKey() } catch (_: Exception) {} }
        }
        viewModelScope.launch {
            try {
                com.movie.app.best.data.config.RemoteConfig.awaitReady(900L)
            } catch (_: Exception) {}
            try { zee5TokenRepository.prefetchTokens() } catch (_: Exception) {}
        }

        // OTA check — background me, splash kabhi iska wait nahi karta.
        // ABI/device headers interceptor bhejta hai, backend best APK ka link deta hai.
        updateManager.checkInBackground("boot")
    }
}