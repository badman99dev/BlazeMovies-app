package com.movie.app.best.ui.screens.notification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationRefreshBus {
    private val _signal = MutableStateFlow(0)
    val signal: StateFlow<Int> = _signal.asStateFlow()

    fun trigger() {
        _signal.value++
    }
}
