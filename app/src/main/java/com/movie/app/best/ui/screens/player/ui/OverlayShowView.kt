package com.movie.app.best.ui.screens.player.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.media3.common.Player
import com.movie.app.best.ui.screens.player.extensions.noRippleClickable
import com.movie.app.best.ui.screens.player.model.VideoContentScale

@Composable
fun BoxScope.OverlayShowView(
    player: Player,
    overlayView: OverlayViewType?,
    videoContentScale: VideoContentScale,
    onDismiss: () -> Unit = {},
    onVideoContentScaleChanged: (VideoContentScale) -> Unit = {},
    customAudioTracks: List<String>? = null,
    selectedAudioTrack: String? = null,
    onAudioTrackSelected: ((String) -> Unit)? = null,
    serverOptions: List<com.movie.app.best.data.model.PlaybackOption>? = null,
    selectedServerOptionId: String? = null,
    onServerOptionSelected: ((String) -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .then(
                if (overlayView != null) Modifier.noRippleClickable(onClick = onDismiss) else Modifier,
            ),
    )

    if (customAudioTracks != null) {
        CustomAudioTrackSelectorView(
            show = overlayView == OverlayViewType.AUDIO_SELECTOR,
            tracks = customAudioTracks,
            selected = selectedAudioTrack,
            onSelect = { onAudioTrackSelected?.invoke(it) },
            onDismiss = onDismiss,
        )
    } else {
        AudioTrackSelectorView(
            show = overlayView == OverlayViewType.AUDIO_SELECTOR,
            player = player,
            onDismiss = onDismiss,
        )
    }

    SubtitleSelectorView(
        show = overlayView == OverlayViewType.SUBTITLE_SELECTOR,
        player = player,
        onDismiss = onDismiss,
    )

    QualitySelectorView(
        show = overlayView == OverlayViewType.QUALITY_SELECTOR,
        player = player,
        onDismiss = onDismiss,
    )

    if (!serverOptions.isNullOrEmpty()) {
        ServerSourceSelectorView(
            show = overlayView == OverlayViewType.SERVER_SELECTOR,
            options = serverOptions,
            selectedId = selectedServerOptionId,
            onSelect = { onServerOptionSelected?.invoke(it) },
            onDismiss = onDismiss,
        )
    }

    PlaybackSpeedSelectorView(
        show = overlayView == OverlayViewType.PLAYBACK_SPEED,
        player = player,
    )

    VideoContentScaleSelectorView(
        show = overlayView == OverlayViewType.VIDEO_CONTENT_SCALE,
        videoContentScale = videoContentScale,
        onVideoContentScaleChanged = onVideoContentScaleChanged,
        onDismiss = onDismiss,
    )
}
