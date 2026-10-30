package com.movie.app.best.ui.screens.player

import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.Replay30
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import com.movie.app.best.ui.screens.player.ui.icons.BrightnessMediumOutlined
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.Player
import com.movie.app.best.ui.screens.player.extensions.switchTrack
import com.movie.app.best.ui.screens.player.state.rememberTracksState
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.compose.ui.graphics.graphicsLayer
import com.movie.app.best.ui.screens.player.buttons.PlayerButton
import com.movie.app.best.ui.screens.player.extensions.toResizeMode
import com.movie.app.best.ui.screens.player.model.DoubleTapGesture
import com.movie.app.best.ui.screens.player.model.VideoContentScale
import com.movie.app.best.ui.screens.player.state.ControlsVisibilityState
import com.movie.app.best.ui.screens.player.state.VerticalGesture
import com.movie.app.best.ui.screens.player.extensions.resetScreenBrightness
import com.movie.app.best.ui.screens.player.state.rememberBrightnessState
import com.movie.app.best.ui.screens.player.state.rememberControlsVisibilityState
import com.movie.app.best.ui.screens.player.state.rememberMediaPresentationState
import com.movie.app.best.ui.screens.player.state.rememberSeekGestureState
import com.movie.app.best.ui.screens.player.state.rememberTapGestureState
import com.movie.app.best.ui.screens.player.state.rememberVideoZoomAndContentScaleState
import com.movie.app.best.ui.screens.player.state.rememberVolumeAndBrightnessGestureState
import com.movie.app.best.ui.screens.player.state.rememberVolumeState
import com.movie.app.best.ui.screens.player.state.seekAmountFormatted
import com.movie.app.best.ui.screens.player.state.seekToPositionFormated
import com.movie.app.best.ui.screens.player.ui.AudioTrackSelectorView
import com.movie.app.best.ui.screens.player.ui.CustomAudioTrackSelectorView
import com.movie.app.best.ui.screens.player.ui.SubtitleSelectorView
import com.movie.app.best.ui.screens.player.ui.DoubleTapIndicator
import com.movie.app.best.ui.screens.player.ui.OverlayShowView
import com.movie.app.best.ui.screens.player.ui.OverlayViewType
import com.movie.app.best.ui.screens.player.ui.PlaybackSpeedSelectorView
import com.movie.app.best.ui.screens.player.ui.PlayerGestures
import com.movie.app.best.ui.screens.player.ui.QualitySelectorView
import com.movie.app.best.ui.screens.player.ui.ServerSourceSelectorView
import com.movie.app.best.ui.screens.player.ui.ShutterView
import com.movie.app.best.data.settings.PlayerRotationSettings
import com.movie.app.best.ui.screens.player.ui.PlayerVignette
import com.movie.app.best.ui.screens.player.ui.VerticalGestureIndicator
import com.movie.app.best.ui.screens.player.ui.VideoContentScaleSelectorView
import com.movie.app.best.ui.screens.player.ui.controls.ControlsBottomView
import com.movie.app.best.ui.screens.player.ui.controls.ControlsTopView
import com.movie.app.best.ui.screens.player.extensions.nameRes
import kotlin.time.Duration.Companion.seconds

val LocalControlsVisibilityState = compositionLocalOf<ControlsVisibilityState?> { null }

@Composable
fun MediaPlayerScreen(
    player: Player?,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onPlayInBackgroundClick: () -> Unit,
    onFullscreenClick: (() -> Unit)? = null,
    isInline: Boolean = false,
    isLive: Boolean = false,
    title: String = "",
    customAudioTracks: List<String>? = null,
    selectedAudioTrack: String? = null,
    onAudioTrackSelected: ((String) -> Unit)? = null,
    serverOptions: List<com.movie.app.best.data.model.PlaybackOption>? = null,
    selectedServerOptionId: String? = null,
    onServerOptionSelected: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    var isAutoRotate by remember { mutableStateOf(PlayerRotationSettings.isAutoRotate(context)) }

    // Restore the saved rotation mode when the expanded (fullscreen) player opens.
    // Fullscreen is always landscape: auto mode follows the device between both
    // landscape sides, locked mode freezes the current side.
    LaunchedEffect(Unit) {
        if (!isInline && isAutoRotate) {
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    // Never leak the brightness override outside the fullscreen player.
    DisposableEffect(Unit) {
        onDispose { activity?.resetScreenBrightness() }
    }

    val volumeState = rememberVolumeState(player = player)
    player ?: return
    val mediaPresentationState = rememberMediaPresentationState(player = player, isLive = isLive)

    // Captions: outlined icon when off/unavailable, filled when a text track is rendering.
    val textTracksState = rememberTracksState(player, C.TRACK_TYPE_TEXT)
    val isCaptionOn = textTracksState.tracks.any { it.isSelected }
    val controlsVisibilityState = rememberControlsVisibilityState(
        player = player,
        hideAfter = 4.seconds,
        autoToggleSystemBars = false,
    )
    val tapGestureState = rememberTapGestureState(
        player = player,
        doubleTapGesture = DoubleTapGesture.BOTH,
        seekIncrementMillis = 10000L,
        useLongPressGesture = true,
        longPressSpeed = 2.0f,
        isLive = isLive,
    )
    val seekGestureState = rememberSeekGestureState(
        player = player,
        sensitivity = 0.5f,
        enableSeekGesture = !isInline,
        isLive = isLive,
    )
    val videoZoomAndContentScaleState = rememberVideoZoomAndContentScaleState(
        player = player,
        initialContentScale = VideoContentScale.BEST_FIT,
        enableZoomGesture = !isInline,
        enablePanGesture = !isInline,
    )
    val brightnessState = rememberBrightnessState(enabled = !isInline)
    val volumeAndBrightnessGestureState = rememberVolumeAndBrightnessGestureState(
        volumeState = volumeState,
        brightnessState = brightnessState,
        enableVolumeGesture = !isInline,
        enableBrightnessGesture = !isInline,
        volumeGestureSensitivity = 1.25f,
        brightnessGestureSensitivity = 1.25f,
    )

    LaunchedEffect(tapGestureState.isLongPressGestureInAction) {
        if (tapGestureState.isLongPressGestureInAction) controlsVisibilityState.hideControls()
    }

    var overlayView by remember { mutableStateOf<OverlayViewType?>(null) }

    // Tap = toggle captions, long press = subtitle track sheet.
    val toggleCaptions = {
        when {
            textTracksState.tracks.any { it.isSelected } -> textTracksState.switchTrack(-1)
            textTracksState.tracks.isNotEmpty() -> textTracksState.switchTrack(0)
            else -> {
                controlsVisibilityState.hideControls()
                overlayView = OverlayViewType.SUBTITLE_SELECTOR
            }
        }
    }

    CompositionLocalProvider(LocalControlsVisibilityState provides controlsVisibilityState) {
        Box {
            Box(
                modifier = modifier.fillMaxSize().background(Color.Black),
            ) {
                AndroidView(
                    factory = { context ->
                        PlayerView(context).apply {
                            useController = false
                            this.player = player
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                        }
                    },
                    update = { view ->
                        view.player = player
                        view.resizeMode = videoZoomAndContentScaleState.videoContentScale.toResizeMode()
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = videoZoomAndContentScaleState.zoom
                            scaleY = videoZoomAndContentScaleState.zoom
                            translationX = videoZoomAndContentScaleState.offset.x
                            translationY = videoZoomAndContentScaleState.offset.y
                        },
                )

                PlayerGestures(
                    controlsVisibilityState = controlsVisibilityState,
                    tapGestureState = tapGestureState,
                    seekGestureState = seekGestureState,
                    videoZoomAndContentScaleState = videoZoomAndContentScaleState,
                    volumeAndBrightnessGestureState = volumeAndBrightnessGestureState,
                )

                AnimatedVisibility(
                    visible = controlsVisibilityState.controlsVisible && !controlsVisibilityState.controlsLocked,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    PlayerVignette()
                }

                if (mediaPresentationState.isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center).size(72.dp),
                        color = Color.White,
                    )
                }

                DoubleTapIndicator(tapGestureState = tapGestureState)

                AnimatedVisibility(
                    modifier = Modifier.padding(top = 24.dp).align(Alignment.TopCenter),
                    visible = tapGestureState.isLongPressGestureInAction,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF141416).copy(alpha = 0.5f),
                        contentColor = Color.White,
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text(
                                text = "${tapGestureState.longPressSpeed}x speed",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                            )
                        }
                    }
                }

                if (controlsVisibilityState.controlsVisible && controlsVisibilityState.controlsLocked) {
                    Column(
                        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(top = 24.dp),
                    ) {
                        PlayerButton(
                            containerColor = Color.Black.copy(0.5f),
                            onClick = { controlsVisibilityState.unlockControls() },
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = "Unlock")
                        }
                    }
                } else {
                    PlayerControlsView(
                        topView = {
                            AnimatedVisibility(
                                visible = controlsVisibilityState.controlsVisible,
                                enter = fadeIn(),
                                exit = fadeOut(),
                            ) {
                                ControlsTopView(
                                    title = title,
                                    isInline = isInline,
                                    onAudioClick = {
                                        controlsVisibilityState.hideControls()
                                        overlayView = OverlayViewType.AUDIO_SELECTOR
                                    },
                                    onQualityClick = {
                                        controlsVisibilityState.hideControls()
                                        overlayView = OverlayViewType.QUALITY_SELECTOR
                                    },
                                    onPlaybackSpeedClick = {
                                        controlsVisibilityState.hideControls()
                                        overlayView = OverlayViewType.PLAYBACK_SPEED
                                    },
                                    onSubtitleClick = toggleCaptions,
                                    onSubtitleLongClick = {
                                        controlsVisibilityState.hideControls()
                                        overlayView = OverlayViewType.SUBTITLE_SELECTOR
                                    },
                                    isCaptionOn = isCaptionOn,
                                    onServerClick = {
                                        controlsVisibilityState.hideControls()
                                        overlayView = OverlayViewType.SERVER_SELECTOR
                                    },
                                    showServerButton = !serverOptions.isNullOrEmpty(),
                                    onBackClick = onBackClick,
                                )
                            }
                        },
                        middleView = {
                            when {
                                seekGestureState.seekAmount != null -> InfoView(info = "${seekGestureState.seekAmountFormatted}\n[${seekGestureState.seekToPositionFormated}]")
                                videoZoomAndContentScaleState.isZooming -> InfoView(info = "${(videoZoomAndContentScaleState.zoom * 100).toInt()}%")
                                videoZoomAndContentScaleState.showContentScaleIndicator -> InfoView(info = videoZoomAndContentScaleState.videoContentScale.nameRes())
                                controlsVisibilityState.controlsVisible -> ControlsMiddleView(player = player, isPlaying = mediaPresentationState.isPlaying)
                                else -> Unit
                            }
                        },
                        bottomView = {
                            AnimatedVisibility(
                                visible = controlsVisibilityState.controlsVisible && !controlsVisibilityState.controlsLocked,
                                enter = fadeIn(),
                                exit = fadeOut(),
                            ) {
                                val context = LocalContext.current
                                ControlsBottomView(
                                    player = player,
                                    mediaPresentationState = mediaPresentationState,
                                    controlsAlignment = Alignment.Start,
                                    videoContentScale = videoZoomAndContentScaleState.videoContentScale,
                                    isPipSupported = true,
                                    isInline = isInline,
                                    isLive = isLive,
                                    onSeek = seekGestureState::onSeek,
                                    onSeekEnd = seekGestureState::onSeekEnd,
                                    isAutoRotate = isAutoRotate,
                                    onRotateClick = {
                                        isAutoRotate = !isAutoRotate
                                        PlayerRotationSettings.setAutoRotate(context, isAutoRotate)
                                        activity?.requestedOrientation = if (isAutoRotate) {
                                            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                        } else {
                                            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LOCKED
                                        }
                                    },
                                    onFullscreenClick = {
                                        if (onFullscreenClick != null) {
                                            onFullscreenClick()
                                        } else {
                                            val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                                            if (isLandscape) {
                                                activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                            } else {
                                                activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                            }
                                        }
                                    },
                                    onPlayInBackgroundClick = onPlayInBackgroundClick,
                                    onLockControlsClick = {
                                        controlsVisibilityState.showControls()
                                        controlsVisibilityState.lockControls()
                                    },
                                    onVideoContentScaleClick = {
                                        controlsVisibilityState.showControls()
                                        videoZoomAndContentScaleState.switchToNextVideoContentScale()
                                    },
                                    onVideoContentScaleLongClick = {
                                        controlsVisibilityState.hideControls()
                                        overlayView = OverlayViewType.VIDEO_CONTENT_SCALE
                                    },
                                    onPictureInPictureClick = { },
                                )
                            }
                        },
                    )
                }

                val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .displayCutoutPadding()
                        .padding(start = systemBarsPadding.calculateLeftPadding(androidx.compose.ui.platform.LocalLayoutDirection.current), end = systemBarsPadding.calculateRightPadding(androidx.compose.ui.platform.LocalLayoutDirection.current), top = 0.dp, bottom = 0.dp)
                        .padding(horizontal = 22.dp),
                ) {
                    val gestureBarHeight = minOf(170.dp, maxHeight * 0.42f)
                    AnimatedVisibility(
                        modifier = Modifier.align(Alignment.CenterStart),
                        visible = volumeAndBrightnessGestureState.activeGesture == VerticalGesture.VOLUME,
                        enter = fadeIn() + slideInHorizontally { -it / 4 },
                        exit = fadeOut() + slideOutHorizontally { -it / 4 },
                    ) {
                        VerticalGestureIndicator(
                            percent = volumeState.volumePercentage.toFloat(),
                            label = "Volume",
                            icon = if (volumeState.currentVolume <= 0) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            barHeight = gestureBarHeight,
                            labelOnRight = true,
                        )
                    }

                    AnimatedVisibility(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        visible = volumeAndBrightnessGestureState.activeGesture == VerticalGesture.BRIGHTNESS,
                        enter = fadeIn() + slideInHorizontally { it / 4 },
                        exit = fadeOut() + slideOutHorizontally { it / 4 },
                    ) {
                        VerticalGestureIndicator(
                            percent = brightnessState.brightnessPercentage.toFloat(),
                            label = "Brightness",
                            icon = BrightnessMediumOutlined,
                            barHeight = gestureBarHeight,
                        )
                    }
                }
            }

            if (isInline && overlayView != null) {
                Dialog(
                    onDismissRequest = { overlayView = null },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.7f))
                                .clickable { overlayView = null }
                        )
                        when (overlayView) {
                            OverlayViewType.AUDIO_SELECTOR -> if (customAudioTracks != null) {
                                CustomAudioTrackSelectorView(
                                    show = true,
                                    tracks = customAudioTracks,
                                    selected = selectedAudioTrack,
                                    onSelect = { onAudioTrackSelected?.invoke(it) },
                                    onDismiss = { overlayView = null }
                                )
                            } else {
                                AudioTrackSelectorView(
                                    show = true,
                                    player = player,
                                    onDismiss = { overlayView = null }
                                )
                            }
                            OverlayViewType.SUBTITLE_SELECTOR -> SubtitleSelectorView(
                                show = true,
                                player = player,
                                onDismiss = { overlayView = null }
                            )
                            OverlayViewType.QUALITY_SELECTOR -> QualitySelectorView(
                                show = true,
                                player = player,
                                onDismiss = { overlayView = null }
                            )
                            OverlayViewType.SERVER_SELECTOR -> if (!serverOptions.isNullOrEmpty()) {
                                ServerSourceSelectorView(
                                    show = true,
                                    options = serverOptions,
                                    selectedId = selectedServerOptionId,
                                    onSelect = { onServerOptionSelected?.invoke(it) },
                                    onDismiss = { overlayView = null }
                                )
                            }
                            OverlayViewType.PLAYBACK_SPEED -> PlaybackSpeedSelectorView(
                                show = true,
                                player = player
                            )
                            OverlayViewType.VIDEO_CONTENT_SCALE -> VideoContentScaleSelectorView(
                                show = true,
                                videoContentScale = videoZoomAndContentScaleState.videoContentScale,
                                onVideoContentScaleChanged = { videoZoomAndContentScaleState.onVideoContentScaleChanged(it) },
                                onDismiss = { overlayView = null }
                            )
                            else -> {}
                        }
                    }
                }
            } else {
                OverlayShowView(
                    player = player,
                    overlayView = overlayView,
                    videoContentScale = videoZoomAndContentScaleState.videoContentScale,
                    onDismiss = { overlayView = null },
                    onVideoContentScaleChanged = { videoZoomAndContentScaleState.onVideoContentScaleChanged(it) },
                    customAudioTracks = customAudioTracks,
                    selectedAudioTrack = selectedAudioTrack,
                    onAudioTrackSelected = onAudioTrackSelected,
                    serverOptions = serverOptions,
                    selectedServerOptionId = selectedServerOptionId,
                    onServerOptionSelected = onServerOptionSelected,
                )
            }
        }
    }
}

@Composable
fun InfoView(
    modifier: Modifier = Modifier,
    info: String,
    textStyle: TextStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = info, style = textStyle, color = Color.White, textAlign = TextAlign.Center)
    }
}

@Composable
fun ControlsMiddleView(modifier: Modifier = Modifier, player: Player, isPlaying: Boolean) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(40.dp, alignment = Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerButton(onClick = { player.seekBack() }) {
            Icon(imageVector = Icons.Default.Replay30, contentDescription = "Rewind 30 seconds", modifier = Modifier.size(28.dp))
        }
        PlayerButton(
            modifier = Modifier.size(64.dp),
            onClick = {
                try {
                    if (isPlaying) {
                        player.pause()
                    } else {
                        if (player.playbackState == Player.STATE_IDLE) {
                            player.prepare()
                        }
                        player.play()
                    }
                } catch (_: Exception) {}
            },
        ) {
            PlayPauseMorphIcon(
                isPlaying = isPlaying,
                size = 44.dp,
                tint = Color.White,
            )
        }
        PlayerButton(onClick = { player.seekForward() }) {
            Icon(imageVector = Icons.Default.Forward30, contentDescription = "Forward 30 seconds", modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
fun PlayerControlsView(
    modifier: Modifier = Modifier,
    topView: @Composable () -> Unit,
    middleView: @Composable BoxScope.() -> Unit,
    bottomView: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column {
            topView()
            Spacer(modifier = Modifier.weight(1f))
            bottomView()
        }
        middleView()
    }
}
