package com.movie.app.best.ui.screens.player.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.movie.app.best.data.settings.VlcDecoderSettings
import com.movie.app.best.ui.screens.player.icons.DecoderIcons

/** Decoder mode → icon (state ke hisaab se badalta hai, captions icon jaisa). */
fun VlcDecoderSettings.Mode.icon(): ImageVector = when (this) {
    VlcDecoderSettings.Mode.HW_PLUS -> DecoderIcons.HwPlus
    VlcDecoderSettings.Mode.HW -> DecoderIcons.Hw
    VlcDecoderSettings.Mode.SW -> DecoderIcons.Sw
}

/**
 * Local playback ka decoder chooser — quality sheet ka hi look & feel.
 * Switching se engine recreate hota hai (LibVLC options per-instance hote hain).
 */
@Composable
fun BoxScope.DecoderSelectorView(
    modifier: Modifier = Modifier,
    show: Boolean,
    selected: VlcDecoderSettings.Mode,
    onSelect: (VlcDecoderSettings.Mode) -> Unit,
    onDismiss: () -> Unit,
) {
    OverlayView(modifier = modifier, show = show, title = "Decoder") {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .sheetBottomInset()
                .padding(horizontal = 24.dp)
                .selectableGroup(),
        ) {
            VlcDecoderSettings.Mode.entries.forEach { mode ->
                DecoderRow(
                    mode = mode,
                    selected = mode == selected,
                    onClick = {
                        if (mode != selected) onSelect(mode)
                        onDismiss()
                    },
                )
            }
            Text(
                text = "Changing the decoder restarts this video.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.45f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun DecoderRow(
    mode: VlcDecoderSettings.Mode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .selectable(selected = selected, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = mode.icon(),
            contentDescription = mode.label,
            tint = Color.White.copy(alpha = if (selected) 0.95f else 0.65f),
            modifier = Modifier.size(24.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mode.label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = if (selected) 1f else 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = mode.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.45f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        RadioButton(selected = selected, onClick = null)
    }
}