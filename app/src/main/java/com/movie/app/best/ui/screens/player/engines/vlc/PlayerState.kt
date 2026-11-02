package com.movie.app.best.ui.screens.player.engines.vlc

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize

/**
 * Mutable state holder for [VlcEngine]. LibVLC has no observable player object, so the engine keeps
 * the current snapshot here and republishes it to media3 listeners via `invalidateState()`.
 *
 * Brother of [com.movie.app.best.ui.screens.player.engines.media3.Media3PlayerState] (which just
 * reads ExoPlayer, because ExoPlayer already *is* the observable state).
 */
internal class VlcPlayerState {
    var playWhenReady: Boolean = true
    var playbackState: Int = Player.STATE_IDLE
    var playerError: PlaybackException? = null
    var repeatMode: Int = Player.REPEAT_MODE_OFF
    var shuffleModeEnabled: Boolean = false
    var isLoading: Boolean = false
    var playbackParameters: PlaybackParameters = PlaybackParameters(1f)
    var trackSelectionParameters: TrackSelectionParameters =
        TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT
    var volume: Float = 1f
    var videoSize: VideoSize = VideoSize.UNKNOWN
    var currentTracks: Tracks = Tracks.EMPTY
    var mediaItem: MediaItem? = null
    var durationMs: Long = C.TIME_UNSET
    var positionMs: Long = 0L
    var isSeekable: Boolean = false
}
