package com.movie.app.best.ui.screens.player.engines

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.movie.app.best.ui.screens.player.engines.media3.Media3Engine
import com.movie.app.best.ui.screens.player.engines.media3.Media3VideoSurface
import com.movie.app.best.ui.screens.player.engines.vlc.VlcEngine
import com.movie.app.best.ui.screens.player.engines.vlc.VlcVideoSurface
import com.movie.app.best.ui.screens.player.model.VideoContentScale

/** Which backend an input should be played with. */
enum class EngineKind { MEDIA3, VLC }

/**
 * Decides which engine plays a given source.
 *
 * Phase 1: only local/downloaded files ("file://", "content://", bare paths) use the LibVLC
 * "Advance Decoder". Everything streamed (HTTP/HLS/DASH + DRM) stays on Media3.
 */
object EngineRouter {

    fun kindFor(url: String): EngineKind {
        val u = url.trim()
        if (u.isEmpty()) return EngineKind.MEDIA3
        return when {
            u.startsWith("file://", ignoreCase = true) -> EngineKind.VLC
            u.startsWith("content://", ignoreCase = true) -> EngineKind.VLC
            u.startsWith("/") -> EngineKind.VLC
            else -> EngineKind.MEDIA3
        }
    }

    fun isLocal(url: String): Boolean = kindFor(url) == EngineKind.VLC
}

/**
 * Renders the correct hardware video surface for [player].
 *
 * Kept engine-agnostic so [com.movie.app.best.ui.screens.player.MediaPlayerScreen] never has to
 * know about SurfaceView/PlayerView vs. VLCVideoLayout.
 */
@Composable
fun EngineVideoSurface(
    player: Player?,
    contentScale: VideoContentScale,
    zoom: Float,
    offset: Offset,
    modifier: Modifier = Modifier,
) {
    when (player) {
        is VlcEngine -> VlcVideoSurface(player, contentScale, zoom, offset, modifier)
        is Media3Engine -> Media3VideoSurface(player.exo, contentScale, zoom, offset, modifier)
        is ExoPlayer -> Media3VideoSurface(player, contentScale, zoom, offset, modifier)
        else -> Unit
    }
}
