package com.movie.app.best.ui.screens.player

import android.content.Context
import androidx.media3.common.C
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.movie.app.best.data.settings.VideoQualitySettings

object PlayerFactory {

    const val BUFFER_MIN_MS = 5000
    const val BUFFER_MAX_MS = 30000
    const val BACK_BUFFER_MS = 300_000L
    const val SPORTS_BACK_BUFFER_MS = 30_000L

    fun trackSelector(
        context: Context,
        disableSubtitles: Boolean = true,
    ): DefaultTrackSelector = DefaultTrackSelector(context).apply {
        var params = buildUponParameters()
        if (disableSubtitles) params = params.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        setParameters(VideoQualitySettings.applyTo(params).build())
    }

    fun defaultLoadControl(backBufferMs: Long = BACK_BUFFER_MS): DefaultLoadControl =
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                BUFFER_MIN_MS,
                BUFFER_MAX_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .setBackBuffer(backBufferMs, true)
            .build()

    fun build(
        context: Context,
        trackSelector: DefaultTrackSelector,
        mediaSourceFactory: MediaSource.Factory? = null,
        backBufferMs: Long = BACK_BUFFER_MS,
        playWhenReady: Boolean = true,
    ): ExoPlayer {
        val builder = ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setLoadControl(defaultLoadControl(backBufferMs))
        if (mediaSourceFactory != null) builder.setMediaSourceFactory(mediaSourceFactory)
        return builder.build().apply { this.playWhenReady = playWhenReady }
    }
}