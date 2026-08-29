package com.movie.app.best.data.repository

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.movie.app.best.data.model.UnifiedChannel
import com.movie.app.best.data.model.UpdateResponse
import com.movie.app.best.data.model.Movie
import com.movie.app.best.data.model.AppNotification

object PrefetchCache {
    var slider: List<Movie>? = null
    var trending: List<Movie>? = null
    var latestUploads: List<Movie>? = null
    var notification: AppNotification? = null
    var liveChannels: List<UnifiedChannel>? = null
    var newIndiaReleases: List<Movie>? = null
    var newUsReleases: List<Movie>? = null

    var updateResponse: UpdateResponse? by mutableStateOf(null)

    fun clear() {
        slider = null
        trending = null
        latestUploads = null
        notification = null
        liveChannels = null
        newIndiaReleases = null
        newUsReleases = null
        updateResponse = null
    }
}
