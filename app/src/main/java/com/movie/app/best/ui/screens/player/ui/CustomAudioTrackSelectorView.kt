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

@Composable
fun BoxScope.CustomAudioTrackSelectorView(
    modifier: Modifier = Modifier,
    show: Boolean,
    tracks: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    OverlayView(modifier = modifier, show = show, title = "Audio Track") {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .sheetBottomInset()
                .padding(horizontal = 24.dp)
                .selectableGroup(),
        ) {
            tracks.forEach { track ->
                RadioButtonRow(
                    selected = track == selected,
                    text = track,
                    onClick = {
                        onSelect(track)
                        onDismiss()
                    },
                )
            }
        }
    }
}
