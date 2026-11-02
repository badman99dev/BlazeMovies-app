package com.movie.app.best.ui.screens.player.engines.vlc

import org.videolan.libvlc.MediaPlayer

/**
 * Bridges LibVLC's callback API into a plain lambda so [VlcEngine] can stay focused on state.
 * Brother of [com.movie.app.best.ui.screens.player.engines.media3.Media3PlayerEvents].
 */
internal class VlcPlayerEvents(
    private val onEvent: (MediaPlayer.Event) -> Unit,
) : MediaPlayer.EventListener {
    override fun onEvent(event: MediaPlayer.Event) {
        onEvent.invoke(event)
    }
}
