package com.movie.app.best.ui.screens.player.engines.media3

import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.movie.app.best.ui.screens.player.engines.PlayerEngine

/**
 * Media3 (ExoPlayer) brother of [com.movie.app.best.ui.screens.player.engines.vlc.VlcEngine].
 *
 * ExoPlayer already implements the whole media3 [Player] contract, so this is a thin identity
 * wrapper — every call is delegated straight through. It exists so the shared UI can talk to a
 * single [PlayerEngine] type and the engine can be swapped without touching the rest of the app.
 */
class Media3Engine(val exo: ExoPlayer) : PlayerEngine, Player by exo
