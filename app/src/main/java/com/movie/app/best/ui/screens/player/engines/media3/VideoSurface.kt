package com.movie.app.best.ui.screens.player.engines.media3

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.movie.app.best.ui.screens.player.extensions.toResizeMode
import com.movie.app.best.ui.screens.player.model.VideoContentScale

/** Media3 brother of [com.movie.app.best.ui.screens.player.engines.vlc.VlcVideoSurface]. */
@Composable
fun Media3VideoSurface(
    exo: ExoPlayer,
    contentScale: VideoContentScale,
    zoom: Float,
    offset: Offset,
    modifier: Modifier = Modifier,
) {
    key(exo) {
        AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                useController = false
                player = exo
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
        },
        update = { view ->
            view.player = exo
            view.resizeMode = contentScale.toResizeMode()
        },
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
}
