package com.movie.app.best.ui.screens.player.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import com.movie.app.best.ui.screens.player.extensions.getName
import com.movie.app.best.ui.screens.player.state.rememberTracksState

@Composable
fun BoxScope.SubtitleSelectorView(
    modifier: Modifier = Modifier,
    show: Boolean,
    player: Player,
    onDismiss: () -> Unit,
) {
    val textTracksState = rememberTracksState(player, C.TRACK_TYPE_TEXT)

    OverlayView(modifier = modifier, show = show, title = "Subtitles") {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .sheetBottomInset()
                .padding(horizontal = 24.dp)
                .selectableGroup(),
        ) {
            textTracksState.tracks.forEachIndexed { index, track ->
                RadioButtonRow(
                    selected = track.isSelected,
                    text = track.mediaTrackGroup.getName(C.TRACK_TYPE_TEXT, index),
                    onClick = {
                        textTracksState.switchTrack(index)
                        onDismiss()
                    },
                )
            }
            RadioButtonRow(
                selected = textTracksState.tracks.none { it.isSelected },
                text = "Off",
                onClick = {
                    textTracksState.switchTrack(-1)
                    onDismiss()
                },
            )
        }
    }
}
