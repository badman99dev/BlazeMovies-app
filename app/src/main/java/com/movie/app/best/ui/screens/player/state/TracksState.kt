package com.movie.app.best.ui.screens.player.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import com.movie.app.best.ui.screens.player.engines.media3.Media3TrackMappers
import com.movie.app.best.ui.screens.player.extensions.switchTrack

@Composable
fun rememberTracksState(player: Player, trackType: @C.TrackType Int): TracksState {
    val state = remember(player, trackType) { TracksState(player, trackType) }
    LaunchedEffect(player) {
        state.updateTracks()
        val listener = object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                state.updateTracks()
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

class TracksState(private val player: Player, private val trackType: @C.TrackType Int) {
    var tracks: List<Tracks.Group> by mutableStateOf(emptyList())
        private set

    fun switchTrack(index: Int) { player.switchTrack(trackType, index) }

    fun updateTracks() {
        tracks = Media3TrackMappers.supportedGroups(player, trackType)
    }
}
