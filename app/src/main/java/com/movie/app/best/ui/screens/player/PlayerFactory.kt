package com.movie.app.best.ui.screens.player

import android.content.Context
import androidx.media3.common.C
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.movie.app.best.data.settings.VideoQualitySettings

object PlayerFactory {

    const val BUFFER_MIN_MS = 15_000
    const val BUFFER_MAX_MS = 30_000
    const val BUFFER_FOR_PLAYBACK_MS = 1_500
    const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 2_000
    const val BACK_BUFFER_MS = 300_000
    const val SPORTS_BACK_BUFFER_MS = 30_000
    const val SEEK_INCREMENT_MS = 30_000

    fun trackSelector(
        context: Context,
        disableSubtitles: Boolean = true,
    ): DefaultTrackSelector = DefaultTrackSelector(context).apply {
        var params = buildUponParameters()
        if (disableSubtitles) params = params.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        setParameters(VideoQualitySettings.applyTo(params).build())
    }

    fun defaultLoadControl(backBufferMs: Int = BACK_BUFFER_MS): DefaultLoadControl =
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                BUFFER_MIN_MS,
                BUFFER_MAX_MS,
                BUFFER_FOR_PLAYBACK_MS,
                BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .setBackBuffer(backBufferMs, true)
            .build()

    fun build(
        context: Context,
        trackSelector: DefaultTrackSelector? = null,
        mediaSourceFactory: MediaSource.Factory? = null,
        backBufferMs: Int = BACK_BUFFER_MS,
        playWhenReady: Boolean = true,
        disableSubtitles: Boolean = true,
    ): ExoPlayer {
        // Media3 binds a TrackSelector to exactly one player (TrackSelector.init does
        // checkState(listener == null)). ExoPlayer.release() is async, so a shared
        // selector crashes the next build with IllegalStateException. Always hand
        // each player its own selector unless the caller explicitly creates one.
        val selector = trackSelector ?: trackSelector(context, disableSubtitles)
        val builder = ExoPlayer.Builder(context)
            .setTrackSelector(selector)
            .setLoadControl(defaultLoadControl(backBufferMs))
            .setSeekBackIncrementMs(SEEK_INCREMENT_MS.toLong())
            .setSeekForwardIncrementMs(SEEK_INCREMENT_MS.toLong())
        if (mediaSourceFactory != null) builder.setMediaSourceFactory(mediaSourceFactory)
        return builder.build().apply { this.playWhenReady = playWhenReady }
    }
}