package com.movie.app.best.ui.screens.player.engines.vlc

import android.content.Context
import com.movie.app.best.data.settings.LocalPlaybackSettings
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
        mode: LocalPlaybackSettings.Mode = LocalPlaybackSettings.Mode.HW_PLUS,
        options: List<String> = DEFAULT_OPTIONS,
    ): VlcEngine {
        // Decoder options LibVLC instance ke saath fix hote hain — mode badalne pe
        // player recreate karna padta hai (UI wahi karta hai).
        val merged = ArrayList(options)
        LocalPlaybackSettings.libVlcOptions(mode)
            .filterNot { merged.contains(it) }
            .forEach { merged.add(it) }
        val libVlc = LibVLC(context.applicationContext, merged)
        val mediaPlayer = MediaPlayer(libVlc)
        return VlcEngine(libVlc, mediaPlayer, context.applicationContext, mode)
    }
}
