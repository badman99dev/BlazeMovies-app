package com.movie.app.best.ui.screens.player.engines

import androidx.media3.common.Player

/**
 * Common supertype for every playback backend the shared player UI can talk to.
 *
 * The whole player layer (controls, gestures, track selectors, overlays) is written against the
 * media3 [Player] contract. Because *both* engines below implement [Player], the UI stays 100%
 * shared — only the engine-specific files in `media3/` and `vlc/` differ.
 *
 * The two brothers:
 *  - [com.movie.app.best.ui.screens.player.engines.media3.Media3Engine] — wraps ExoPlayer.
 *  - [com.movie.app.best.ui.screens.player.engines.vlc.VlcEngine] — wraps LibVLC.
 */
interface PlayerEngine : Player
