package com.movie.app.best.ui.screens.player.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Constraints
import androidx.media3.common.Player
import com.movie.app.best.ui.screens.player.extensions.next
import com.movie.app.best.ui.screens.player.model.VideoContentScale
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun rememberVideoZoomAndContentScaleState(
    player: Player,
    initialContentScale: VideoContentScale,
    enableZoomGesture: Boolean,
    enablePanGesture: Boolean,
): VideoZoomAndContentScaleState {
    val coroutineScope = rememberCoroutineScope()
    return remember(player, enableZoomGesture, enablePanGesture) {
        VideoZoomAndContentScaleState(
            player = player,
            initialContentScale = initialContentScale,
            enableZoomGesture = enableZoomGesture,
            enablePanGesture = enablePanGesture,
            coroutineScope = coroutineScope,
        )
    }
}

@Stable
class VideoZoomAndContentScaleState(
    private val player: Player,
    initialContentScale: VideoContentScale,
    private val enableZoomGesture: Boolean = true,
    private val enablePanGesture: Boolean = true,
    private val coroutineScope: CoroutineScope,
) {
    companion object {
        private const val MIN_ZOOM = 0.25f
        private const val MAX_ZOOM = 4f
    }

    var videoContentScale: VideoContentScale by mutableStateOf(initialContentScale)
        private set

    var zoom: Float by mutableFloatStateOf(1f)
        private set

    var offset: Offset by mutableStateOf(Offset.Zero)
        private set

    var isZooming: Boolean by mutableStateOf(false)
        private set

    var showContentScaleIndicator: Boolean by mutableStateOf(false)
        private set

    private var showContentScaleJob: Job? = null

    fun onVideoContentScaleChanged(newContentScale: VideoContentScale) {
        videoContentScale = newContentScale
        zoom = 1f
        offset = Offset.Zero
        showContentScaleIndicator()
    }

    private fun showContentScaleIndicator() {
        showContentScaleJob?.cancel()
        showContentScaleIndicator = true
        showContentScaleJob = coroutineScope.launch {
            delay(1000L)
            showContentScaleIndicator = false
            showContentScaleJob = null
        }
    }

    fun switchToNextVideoContentScale() {
        onVideoContentScaleChanged(videoContentScale.next())
    }

    fun onZoomPanGesture(constraints: Constraints, panChange: Offset, zoomChange: Float) {
        if (!enableZoomGesture) return
        isZooming = true
        zoom = (zoom * zoomChange).coerceIn(MIN_ZOOM, MAX_ZOOM)

        val extraWidth = (zoom - 1) * constraints.maxWidth
        val extraHeight = (zoom - 1) * constraints.maxHeight
        val maxX = abs(extraWidth / 2)
        val maxY = abs(extraHeight / 2)

        if (enablePanGesture) {
            offset = Offset(
                x = (offset.x + zoom * panChange.x).coerceIn(-maxX, maxX),
                y = (offset.y + zoom * panChange.y).coerceIn(-maxY, maxY),
            )
        }
    }

    fun onZoomPanGestureEnd() { isZooming = false }
}
