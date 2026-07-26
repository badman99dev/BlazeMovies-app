package com.movie.app.best.ui.screens.player.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player

@Composable
fun BoxScope.QualitySelectorView(
    modifier: Modifier = Modifier,
    show: Boolean,
    player: Player,
    onDismiss: () -> Unit,
) {
    var selectedHeight by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        // Detect if player is in auto mode
        val params = player.trackSelectionParameters
        val hasVideoOverride = params.overrides.values.any { it.type == C.TRACK_TYPE_VIDEO }
        val isAuto = !hasVideoOverride &&
                !params.disabledTrackTypes.contains(C.TRACK_TYPE_VIDEO) &&
                params.maxVideoHeight == Int.MAX_VALUE &&
                params.minVideoHeight == 0 &&
                !params.forceLowestBitrate
        if (isAuto) {
            selectedHeight = 0
            return@LaunchedEffect
        }

        // Detect currently selected quality from player
        val groups = player.currentTracks.groups.filter { it.type == C.TRACK_TYPE_VIDEO }
        for (group in groups) {
            for (i in 0 until group.length) {
                if (group.isTrackSelected(i)) {
                    val fmt = group.getTrackFormat(i)
                    if (fmt.height > 0) {
                        selectedHeight = fmt.height
                        return@LaunchedEffect
                    }
                }
            }
        }
    }

    OverlayView(modifier = modifier, show = show, title = "Quality") {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
                .padding(horizontal = 24.dp)
                .selectableGroup(),
        ) {
            RadioButtonRow(
                selected = selectedHeight == 0,
                text = "Auto",
                onClick = {
                    selectedHeight = 0
                    setAutoMode(player)
                    onDismiss()
                },
            )

            val groups = player.currentTracks.groups.filter { it.type == C.TRACK_TYPE_VIDEO }
            val seenHeights = mutableSetOf<Int>()
            val sortedHeights = mutableListOf<Int>()
            for (group in groups) {
                for (i in 0 until group.length) {
                    val fmt = group.getTrackFormat(i)
                    if (fmt.height > 0 && seenHeights.add(fmt.height)) {
                        sortedHeights.add(fmt.height)
                    }
                }
            }
            sortedHeights.sortedDescending().forEach { height ->
                RadioButtonRow(
                    selected = selectedHeight == height,
                    text = "${height}p",
                    onClick = {
                        selectedHeight = height
                        setQualityByOverride(player, height)
                        onDismiss()
                    },
                )
            }
        }
    }
}

private fun setAutoMode(player: Player) {
    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
        .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
        .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
        .setMinVideoSize(0, 0)
        .setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
        .build()
}

private fun setQualityByOverride(player: Player, height: Int) {
    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
        .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
        .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
        .setMinVideoSize(0, height)
        .setMaxVideoSize(Int.MAX_VALUE, height)
        .build()
}
