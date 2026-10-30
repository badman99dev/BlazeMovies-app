package com.movie.app.best.ui.screens.player.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.movie.app.best.ui.screens.player.extensions.detectCustomHorizontalDragGestures
import com.movie.app.best.ui.screens.player.extensions.detectCustomTransformGestures
import com.movie.app.best.ui.screens.player.extensions.detectCustomVerticalDragGestures
import com.movie.app.best.ui.screens.player.state.ControlsVisibilityState
import com.movie.app.best.ui.screens.player.state.SeekGestureState
import com.movie.app.best.ui.screens.player.state.TapGestureState
import com.movie.app.best.ui.screens.player.state.VideoZoomAndContentScaleState
import com.movie.app.best.ui.screens.player.state.VolumeAndBrightnessGestureState

@Composable
fun PlayerGestures(
    modifier: Modifier = Modifier,
    controlsVisibilityState: ControlsVisibilityState,
    tapGestureState: TapGestureState,
    seekGestureState: SeekGestureState,
    videoZoomAndContentScaleState: VideoZoomAndContentScaleState,
    volumeAndBrightnessGestureState: VolumeAndBrightnessGestureState,
) {
    BoxWithConstraints {
        Box(
            modifier = modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            if (tapGestureState.seekMillis != 0L) return@detectTapGestures
                            controlsVisibilityState.toggleControlsVisibility()
                        },
                        onDoubleTap = {
                            if (controlsVisibilityState.controlsLocked) return@detectTapGestures
                            controlsVisibilityState.hideControls()
                            tapGestureState.handleDoubleTap(offset = it, size = size)
                        },
                        onPress = {
                            tryAwaitRelease()
                            tapGestureState.handleOnLongPressRelease()
                        },
                        onLongPress = {
                            if (controlsVisibilityState.controlsLocked) return@detectTapGestures
                            tapGestureState.handleLongPress(offset = it)
                        },
                    )
                }
                .pointerInput(controlsVisibilityState.controlsLocked) {
                    if (controlsVisibilityState.controlsLocked) return@pointerInput
                    detectCustomHorizontalDragGestures(
                        onDragStart = {
                            seekGestureState.onDragStart(it)
                            if (seekGestureState.isSeeking) controlsVisibilityState.hideControls()
                        },
                        onHorizontalDrag = seekGestureState::onDrag,
                        onDragCancel = seekGestureState::onDragEnd,
                        onDragEnd = seekGestureState::onDragEnd,
                    )
                }
                .pointerInput(controlsVisibilityState.controlsLocked) {
                    if (controlsVisibilityState.controlsLocked) return@pointerInput
                    detectCustomVerticalDragGestures(
                        onDragStart = {
                            controlsVisibilityState.hideControls()
                            volumeAndBrightnessGestureState.onDragStart(it, size)
                        },
                        onVerticalDrag = volumeAndBrightnessGestureState::onDrag,
                        onDragCancel = volumeAndBrightnessGestureState::onDragEnd,
                        onDragEnd = volumeAndBrightnessGestureState::onDragEnd,
                    )
                }
                .pointerInput(controlsVisibilityState.controlsLocked) {
                    if (controlsVisibilityState.controlsLocked) return@pointerInput
                    var zoomGestureControlsHidden = false
                    detectCustomTransformGestures(
                        onGestureStart = { zoomGestureControlsHidden = false },
                        onGesture = { _, panChange, zoomChange, _ ->
                            if (tapGestureState.isLongPressGestureInAction) return@detectCustomTransformGestures
                            if (!zoomGestureControlsHidden) {
                                zoomGestureControlsHidden = true
                                controlsVisibilityState.hideControls()
                            }
                            videoZoomAndContentScaleState.onZoomPanGesture(
                                constraints = this@BoxWithConstraints.constraints,
                                panChange = panChange,
                                zoomChange = zoomChange,
                            )
                        },
                        onGestureEnd = {
                            zoomGestureControlsHidden = false
                            videoZoomAndContentScaleState.onZoomPanGestureEnd()
                        },
                    )
                },
        )
    }
}
