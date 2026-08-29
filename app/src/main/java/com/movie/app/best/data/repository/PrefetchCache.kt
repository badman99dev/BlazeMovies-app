package com.movie.app.best.data.repository

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.movie.app.best.data.model.UpdateResponse

object PrefetchCache {
    var updateResponse: UpdateResponse? by mutableStateOf(null)

    fun clear() {
        updateResponse = null
    }
}