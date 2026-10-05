package com.movie.app.best.ui.screens.player.state

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.media3.common.Player
import com.movie.app.best.ui.screens.player.model.DoubleTapGesture
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun rememberTapGestureState(
    player: Player,
    doubleTapGesture: DoubleTapGesture,
    seekIncrementMillis: Long,
    useLongPressGesture: Boolean,
    longPressSpeed: Float,
    isLive: Boolean = false,
): TapGestureState {
    val coroutineScope = rememberCoroutineScope()
    return remember(player, isLive) {
        TapGestureState(
            player = player,
            doubleTapGesture = doubleTapGesture,
            seekIncrementMillis = seekIncrementMillis,
            useLongPressGesture = useLongPressGesture,
            longPressSpeed = longPressSpeed,
            coroutineScope = coroutineScope,
            isLive = isLive,
        )
    }
}

@Stable
class TapGestureState(
    private val player: Player,
    private val seekIncrementMillis: Long,
    private val useLongPressGesture: Boolean = true,
    private val coroutineScope: CoroutineScope,
    val longPressSpeed: Float = 2.0f,
    val doubleTapGesture: DoubleTapGesture,
    val interactionSource: MutableInteractionSource = MutableInteractionSource(),
    val isLive: Boolean = false,
) {
    var seekMillis by mutableLongStateOf(0L)
    var isLongPressGestureInAction by mutableStateOf(false)

    private var resetJob: Job? = null
    private var currentSpeed: Float = player.playbackParameters.speed

    fun handleDoubleTap(offset: Offset, size: IntSize) {
        if (!player.isCurrentMediaItemSeekable) return

        val action = when (doubleTapGesture) {
            DoubleTapGesture.FAST_FORWARD_AND_REWIND -> {
                when {
                    offset.x < size.width / 2 -> DoubleTapAction.SEEK_BACKWARD
                    else -> DoubleTapAction.SEEK_FORWARD
                }
            }
            DoubleTapGesture.BOTH -> {
                val eventPositionX = offset.x / size.width
                when {
                    eventPositionX < 0.35 -> DoubleTapAction.SEEK_BACKWARD
                    eventPositionX > 0.65 -> DoubleTapAction.SEEK_FORWARD
                    else -> DoubleTapAction.PLAY_PAUSE
                }
            }
            DoubleTapGesture.PLAY_PAUSE -> DoubleTapAction.PLAY_PAUSE
            DoubleTapGesture.NONE -> return
        }

        when (action) {
            DoubleTapAction.SEEK_BACKWARD -> {
                val isLiveStream = isLive || player.isCurrentMediaItemLive
                val minSafe = if (isLiveStream && player.duration > 12000L) 6000L else 0L
                val target = (player.currentPosition - seekIncrementMillis).coerceAtLeast(minSafe)
                player.seekTo(target)
                if (seekMillis > 0L) seekMillis = 0L
                seekMillis -= seekIncrementMillis
                interactionSource.tryEmit(PressInteraction.Press(offset))
            }
            DoubleTapAction.SEEK_FORWARD -> {
                val isLiveStream = isLive || player.isCurrentMediaItemLive
                val target = if (isLiveStream && player.duration > 0) {
                    val maxSafe = (player.duration - 6000L).coerceAtLeast(0L)
                    (player.currentPosition + seekIncrementMillis).coerceAtMost(maxSafe)
                } else {
                    player.currentPosition + seekIncrementMillis
                }
                player.seekTo(target)
                if (seekMillis < 0L) seekMillis = 0L
                seekMillis += seekIncrementMillis
                interactionSource.tryEmit(PressInteraction.Press(offset))
            }
            DoubleTapAction.PLAY_PAUSE -> {
                if (player.isPlaying) player.pause() else player.play()
            }
        }
        resetDoubleTapSeekState()
    }

    fun handleLongPress(offset: Offset) {
        if (!useLongPressGesture) return
        if (!player.isPlaying) return
        val isLiveStream = isLive || player.isCurrentMediaItemLive
        if (isLiveStream && player.duration > 0) {
            val offsetFromLive = player.duration - player.currentPosition
            // 6-second safety wall: cannot overdrive past live edge
            if (offsetFromLive <= 6000L) return
        }
        isLongPressGestureInAction = true
        currentSpeed = player.playbackParameters.speed
        player.setPlaybackSpeed(longPressSpeed)
    }

    fun handleOnLongPressRelease() {
        if (isLongPressGestureInAction) {
            isLongPressGestureInAction = false
            if (player.playbackParameters.speed == longPressSpeed) {
                player.setPlaybackSpeed(currentSpeed)
            }
        }
    }

    private fun resetDoubleTapSeekState() {
        resetJob?.cancel()
        resetJob = coroutineScope.launch {
            delay(750.milliseconds)
            seekMillis = 0L
        }
    }
}

enum class DoubleTapAction {
    SEEK_BACKWARD,
    SEEK_FORWARD,
    PLAY_PAUSE,
}
