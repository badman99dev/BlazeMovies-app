package com.movie.app.best.ui.screens.player.engines.vlc

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
    // engine badla (decoder switch) to surface bhi naya chahiye — warna AndroidView
    // purana VLCVideoLayout reuse kar leta hai aur naya engine attach hi nahi hota
    // (audio chalta hai, picture freeze rehti hai).
    key(engine) {
        val lifecycleOwner = LocalLifecycleOwner.current
        val currentScale = rememberUpdatedState(contentScale)
        val layoutRef = remember { mutableStateOf<VLCVideoLayout?>(null) }

        // Screen band / recents jaate hi system surface destroy karta hai, aur libVLC khud
        // detachViews() kar deta hai (video track bhi disable: audio chalta rehta, picture
        // black). Wapas aane par attach dobara karna zaroori hai, warna SurfaceHolder
        // callback registered hi nahi rehta aur vout ko naya surface milta hi nahi.
        // (Media3 ka PlayerView ye khud karta hai, isliye online player theek chalta tha.)
        DisposableEffect(lifecycleOwner, engine) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> layoutRef.value?.let { layout ->
                        engine.attachSurface(layout)
                        engine.setContentScale(currentScale.value)
                    }
                    Lifecycle.Event.ON_STOP -> engine.detachSurface()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        AndroidView(
        factory = { context ->
            VLCVideoLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                layoutRef.value = this
                engine.attachSurface(this)
            }
        },
        update = { engine.setContentScale(contentScale) },
        onRelease = {
            layoutRef.value = null
            engine.detachSurface()
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
