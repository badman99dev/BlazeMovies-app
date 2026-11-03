package com.movie.app.best.ui.screens.player.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

@Composable
fun rememberPlaybackParametersState(player: Player): PlaybackParametersState {
    val state = remember(player) { PlaybackParametersState(player) }
    LaunchedEffect(player) {
        state.speed = player.playbackParameters.speed
        state.skipSilenceEnabled = (player as? ExoPlayer)?.skipSilenceEnabled ?: false

        val listener = object : Player.Listener {
            override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) {
                state.speed = playbackParameters.speed
            }
        }
        player.addListener(listener)
        try {
            while (true) {
                kotlinx.coroutines.delay(Long.MAX_VALUE)
            }
        } finally {
            player.removeListener(listener)
        }
    }
    return state
}

class PlaybackParametersState(private val player: Player) {
    var speed: Float by mutableFloatStateOf(1f)
        internal set
    var skipSilenceEnabled: Boolean by mutableStateOf(false)
        internal set

    fun setPlaybackSpeed(speed: Float) { player.setPlaybackSpeed(speed) }

    fun setIsSkipSilenceEnabled(enabled: Boolean) {
        if (player is ExoPlayer) player.skipSilenceEnabled = enabled
        skipSilenceEnabled = enabled
    }
}
