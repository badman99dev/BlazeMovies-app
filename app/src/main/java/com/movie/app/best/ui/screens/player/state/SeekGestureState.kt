package com.movie.app.best.ui.screens.player.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.media3.common.C
import androidx.media3.common.Player
import com.movie.app.best.ui.screens.player.extensions.formatted
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun rememberSeekGestureState(
    player: Player,
    enableSeekGesture: Boolean,
    sensitivity: Float = 0.5f,
    isLive: Boolean = false,
): SeekGestureState {
    return remember(player, isLive) { SeekGestureState(player, enableSeekGesture, sensitivity, isLive) }
}

@Stable
class SeekGestureState(
    private val player: Player,
    private val enableSeekGesture: Boolean = true,
    private val sensitivity: Float = 0.5f,
    val isLive: Boolean = false,
) {
    var isSeeking: Boolean by mutableStateOf(false)
        private set

    var seekStartPosition: Long? by mutableStateOf(null)
        private set

    var seekAmount: Long? by mutableStateOf(null)
        private set

    private var seekStartX = 0f

    fun onSeek(value: Long) {
        val startPos = seekStartPosition ?: player.currentPosition
        if (!isSeeking) {
            if (startPos == C.TIME_UNSET) return
            if (player.duration == C.TIME_UNSET) return
            isSeeking = true
            seekStartPosition = startPos
        }
        val duration = player.duration
        if (duration == C.TIME_UNSET) return
        val isLiveStream = isLive || player.isCurrentMediaItemLive
        val maxTarget = if (isLiveStream && duration > 6000L) (duration - 6000L) else duration
        val target = value.coerceIn(0L, maxTarget)
        seekAmount = (target - startPos)
        player.seekTo(target)
    }

    fun onSeekEnd() { reset() }

    fun onDragStart(offset: Offset) {
        if (!enableSeekGesture) return
        if (player.currentPosition == C.TIME_UNSET) return
        if (player.duration == C.TIME_UNSET) return
        if (!player.isCurrentMediaItemSeekable) return
        isSeeking = true
        seekStartX = offset.x
        seekStartPosition = player.currentPosition
    }

    fun onDrag(change: PointerInputChange, dragAmount: Float) {
        val startPos = seekStartPosition ?: return
        if (player.duration == C.TIME_UNSET) return
        if (!player.isCurrentMediaItemSeekable) return
        if (change.isConsumed) return

        val duration = player.duration
        val isLiveStream = isLive || player.isCurrentMediaItemLive
        val maxTarget = if (isLiveStream && duration > 6000L) (duration - 6000L) else duration

        val newPosition = (startPos + ((change.position.x - seekStartX) * (sensitivity * 100)).toInt())
            .coerceIn(0L, maxTarget)
        seekAmount = (newPosition - startPos)
        player.seekTo(newPosition)
    }

    fun onDragEnd() { reset() }

    private fun reset() {
        isSeeking = false
        seekStartPosition = null
        seekAmount = null
        seekStartX = 0f
    }
}

val SeekGestureState.seekAmountFormatted: String
    get() {
        val seekAmount = seekAmount ?: return ""
        val sign = if (seekAmount < 0) "-" else "+"
        return sign + abs(seekAmount).milliseconds.formatted()
    }

val SeekGestureState.seekToPositionFormated: String
    get() {
        val position = seekStartPosition ?: return ""
        val seekAmount = seekAmount ?: return ""
        return (position + seekAmount).milliseconds.formatted()
    }
