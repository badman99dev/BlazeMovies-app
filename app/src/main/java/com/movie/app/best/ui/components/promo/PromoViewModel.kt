package com.movie.app.best.ui.components.promo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.PromoAd
import com.movie.app.best.data.repository.PromoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the home promo popup. [sync] is called once per app open (MainScreen), which
 * also re-checks cached creatives whose display_after window has since opened.
 */
@HiltViewModel
class PromoViewModel @Inject constructor(
    private val repository: PromoRepository
) : ViewModel() {

    val current: StateFlow<PromoAd?> = repository.current

    fun sync() {
        viewModelScope.launch { repository.sync() }
    }

    fun dismiss(ad: PromoAd) {
        repository.dismiss(ad)
    }
}
