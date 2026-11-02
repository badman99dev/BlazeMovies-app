package com.movie.app.best.ui.screens.player.engines.media3

import androidx.media3.common.Player
import androidx.media3.common.Timeline

/**
 * Media3 event helpers. Brother of
 * [com.movie.app.best.ui.screens.player.engines.vlc.VlcPlayerEvents], which bridges LibVLC's
 * callback API into the same media3 [Player.Listener] world.
 */
object Media3PlayerEvents {

    /**
     * Builds a [Player.Listener] that jump-starts a live stream near the live edge as soon as the
     * active window reports `isLive()`. This is Media3-only behaviour because only Media3 exposes
     * a [Timeline.Window] with live metadata.
     */
    fun liveEdgeSyncListener(
        isLive: Boolean,
        hasSynced: () -> Boolean,
        markSynced: () -> Unit,
        targetOffsetMs: Long = 8_000L,
        onSeekTo: (Long) -> Unit,
    ): Player.Listener = object : Player.Listener {
        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            if (!isLive || timeline.isEmpty || hasSynced()) return
            val window = Timeline.Window()
            timeline.getWindow(0, window)
            if (window.isLive()) {
                markSynced()
                val duration = window.durationMs
                if (duration > targetOffsetMs) onSeekTo(duration - targetOffsetMs)
            }
        }
    }
}
