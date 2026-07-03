package com.movie.app.best.data.repository

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.movie.app.best.data.model.LiveChannel
import com.movie.app.best.data.model.UpdateResponse
import com.movie.app.best.data.model.WasmerMovie
import com.movie.app.best.data.model.WasmerNotification

object PrefetchCache {
    var slider: List<WasmerMovie>? = null
    var trending: List<WasmerMovie>? = null
    var latestUploads: List<WasmerMovie>? = null
    var notification: WasmerNotification? = null
    var liveChannels: List<LiveChannel>? = null

    var updateResponse: UpdateResponse? by mutableStateOf(null)

    fun clear() {
        slider = null
        trending = null
        latestUploads = null
        notification = null
        liveChannels = null
        updateResponse = null
    }
}
