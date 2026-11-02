package com.movie.app.best.ui.screens.player.engines.vlc

import android.content.Context
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.MediaPlayer

/**
 * Builds the LibVLC "Advance Decoder" engine. Brother of
 * [com.movie.app.best.ui.screens.player.engines.media3.Media3EngineFactory].
 */
object VlcEngineFactory {

    private val DEFAULT_OPTIONS = listOf(
        "--no-drop-late-frames",
        "--no-skip-frames",
        "--no-sub-autodetect-file",
    )

    fun create(
        context: Context,
        options: List<String> = DEFAULT_OPTIONS,
    ): VlcEngine {
        val libVlc = LibVLC(context.applicationContext, ArrayList(options))
        val mediaPlayer = MediaPlayer(libVlc)
        return VlcEngine(libVlc, mediaPlayer)
    }
}
