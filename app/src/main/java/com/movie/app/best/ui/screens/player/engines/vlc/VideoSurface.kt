package com.movie.app.best.ui.screens.player.engines.vlc

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.viewinterop.AndroidView
import com.movie.app.best.ui.screens.player.model.VideoContentScale
import org.videolan.libvlc.util.VLCVideoLayout

/** LibVLC brother of [com.movie.app.best.ui.screens.player.engines.media3.Media3VideoSurface]. */
@Composable
fun VlcVideoSurface(
    engine: VlcEngine,
    contentScale: VideoContentScale,
    zoom: Float,
    offset: Offset,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            VLCVideoLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                engine.attachSurface(this)
            }
        },
        update = { engine.setContentScale(contentScale) },
        onRelease = { engine.detachSurface() },
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = zoom
                scaleY = zoom
                translationX = offset.x
                translationY = offset.y
            },
    )
}
