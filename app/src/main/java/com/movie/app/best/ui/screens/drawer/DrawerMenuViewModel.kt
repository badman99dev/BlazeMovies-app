package com.movie.app.best.ui.screens.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.debug.NetworkMonitor
import com.movie.app.best.data.repository.DrawerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DrawerMenuViewModel @Inject constructor(
    private val repository: DrawerRepository
) : ViewModel() {

    val items = repository.items

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            NetworkMonitor.refreshCounter.collect { if (it > 0) refresh() }
        }
    }

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.refresh()
            _isRefreshing.value = false
        }
    }
}