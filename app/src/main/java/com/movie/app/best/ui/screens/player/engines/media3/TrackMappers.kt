package com.movie.app.best.ui.screens.player.engines.media3

import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Tracks

/**
 * Reads the media3 track model. Media3 is the "native" model the shared UI understands, so there
 * is nothing to translate here — this is the read-side brother of
 * [com.movie.app.best.ui.screens.player.engines.vlc.VlcTrackMappers], which must build this very
 * model out of LibVLC's flat track descriptions.
 */
object Media3TrackMappers {

    fun supportedGroups(player: Player, trackType: Int): List<Tracks.Group> =
        player.currentTracks.groups.filter { it.type == trackType && it.isSupported }

    fun videoHeights(player: Player): List<Int> {
        val heights = LinkedHashSet<Int>()
        player.currentTracks.groups
            .filter { it.type == C.TRACK_TYPE_VIDEO }
            .forEach { group ->
                for (i in 0 until group.length) {
                    val h = group.getTrackFormat(i).height
                    if (h > 0) heights.add(h)
                }
            }
        return heights.sortedDescending()
    }

    fun selectedVideoHeight(player: Player): Int {
        player.currentTracks.groups
            .filter { it.type == C.TRACK_TYPE_VIDEO }
            .forEach { group ->
                for (i in 0 until group.length) {
                    if (group.isTrackSelected(i)) {
                        val h = group.getTrackFormat(i).height
                        if (h > 0) return h
                    }
                }
            }
        return 0
    }
}
