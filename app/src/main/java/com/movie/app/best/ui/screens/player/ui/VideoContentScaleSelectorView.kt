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
import com.movie.app.best.ui.screens.player.extensions.nameRes
import com.movie.app.best.ui.screens.player.model.VideoContentScale

@Composable
fun BoxScope.VideoContentScaleSelectorView(
    modifier: Modifier = Modifier,
    show: Boolean,
    videoContentScale: VideoContentScale,
    onVideoContentScaleChanged: (VideoContentScale) -> Unit,
    onDismiss: () -> Unit,
) {
    OverlayView(modifier = modifier, show = show, title = "Video Zoom") {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .sheetBottomInset()
                .padding(horizontal = 24.dp)
                .selectableGroup(),
        ) {
            VideoContentScale.entries.forEach { contentScale ->
                RadioButtonRow(
                    selected = contentScale == videoContentScale,
                    text = contentScale.nameRes(),
                    onClick = {
                        onVideoContentScaleChanged(contentScale)
                        onDismiss()
                    },
                )
            }
        }
    }
}
