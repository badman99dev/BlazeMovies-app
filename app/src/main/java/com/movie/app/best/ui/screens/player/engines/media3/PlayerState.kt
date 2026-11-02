package com.movie.app.best.ui.screens.player.engines.media3

import androidx.media3.common.Player

/**
 * Safe read-side accessors for the Media3 engine. Brother of
 * [com.movie.app.best.ui.screens.player.engines.vlc.VlcPlayerState] (which holds LibVLC's live
 * state, because LibVLC has no observable Player of its own).
 */
object Media3PlayerState {

    fun isBuffering(player: Player): Boolean = player.playbackState == Player.STATE_BUFFERING

    fun safePosition(player: Player): Long = player.currentPosition.coerceAtLeast(0L)

    fun safeDuration(player: Player): Long = player.duration.coerceAtLeast(0L)

    fun isSeekable(player: Player): Boolean = player.isCurrentMediaItemSeekable

    fun isLiveItem(player: Player): Boolean = player.isCurrentMediaItemLive
}
