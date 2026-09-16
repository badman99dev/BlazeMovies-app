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
import com.movie.app.best.data.model.PlaybackOption

@Composable
fun BoxScope.ServerSourceSelectorView(
    modifier: Modifier = Modifier,
    show: Boolean,
    options: List<PlaybackOption>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    OverlayView(modifier = modifier, show = show, title = "Server Sources") {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
                .padding(horizontal = 24.dp)
                .selectableGroup(),
        ) {
            options.forEach { option ->
                RadioButtonRow(
                    selected = option.id == selectedId,
                    text = option.label,
                    languages = option.languages,
                    onClick = {
                        onSelect(option.id)
                        onDismiss()
                    },
                )
            }
        }
    }
}
