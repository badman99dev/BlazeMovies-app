package com.movie.app.best.ui.screens.player.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import com.movie.app.best.ui.screens.player.extensions.formatted
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

@Composable
fun rememberMediaPresentationState(player: Player, isLive: Boolean = false): MediaPresentationState {
    val state = remember { MediaPresentationState(player, isLive) }
    LaunchedEffect(isLive) {
        state.isLive = isLive
    }
    LaunchedEffect(player) {
        state.updatePosition()
        state.updateDuration()
        state.isPlaying = player.isPlaying
        state.isBuffering = player.playbackState == Player.STATE_BUFFERING

        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                state.isBuffering = playbackState == Player.STATE_BUFFERING
                state.updateDuration()
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                state.isPlaying = isPlaying
            }
            override fun onPositionDiscontinuity(reason: Int) {
                state.updatePosition()
            }
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                state.updateDuration()
                state.updatePosition()
            }
            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                state.updateDuration()
            }
        }
        player.addListener(listener)

        try {
            while (true) {
                delay(500)
                if (player.isPlaying) {
                    state.updatePosition()
                    // Speed auto-governor for live streams: drop to 1.0x when reaching 6s safety wall
                    if ((player.isCurrentMediaItemLive || state.isLive) && player.playbackParameters.speed > 1.0f) {
                        if (state.liveOffsetMs <= 6000L) {
                            player.setPlaybackSpeed(1.0f)
                        }
                    }
                }
            }
        } finally {
            player.removeListener(listener)
        }
    }
    return state
}

@Stable
class MediaPresentationState(private val player: Player, initialIsLive: Boolean = false) {
    var isLive: Boolean by mutableStateOf(initialIsLive)
        internal set
    var position: Long by mutableLongStateOf(0L)
        internal set
    var duration: Long by mutableLongStateOf(0L)
        internal set
    var liveOffsetMs: Long by mutableLongStateOf(0L)
        internal set
    var isPlaying: Boolean by mutableStateOf(false)
        internal set
    var isBuffering: Boolean by mutableStateOf(false)
        internal set

    val isAtLiveEdge: Boolean get() = !isLive || liveOffsetMs <= 10_000L

    fun updatePosition() {
        position = player.currentPosition.coerceAtLeast(0L)
        updateLiveOffset()
    }
    fun updateDuration() {
        duration = player.duration.coerceAtLeast(0L)
        updateLiveOffset()
    }

    private fun updateLiveOffset() {
        val currentLive = player.currentLiveOffset
        val dur = player.duration.coerceAtLeast(0L)
        val pos = player.currentPosition.coerceAtLeast(0L)
        liveOffsetMs = when {
            currentLive != androidx.media3.common.C.TIME_UNSET && currentLive >= 0L -> currentLive
            dur > 0L && pos > 0L -> (dur - pos).coerceAtLeast(0L)
            else -> 0L
        }
    }
}

val MediaPresentationState.positionFormatted: String get() = position.milliseconds.formatted()
val MediaPresentationState.durationFormatted: String get() = duration.milliseconds.formatted()
val MediaPresentationState.pendingPositionFormatted: String get() = (duration - position).milliseconds.formatted()
